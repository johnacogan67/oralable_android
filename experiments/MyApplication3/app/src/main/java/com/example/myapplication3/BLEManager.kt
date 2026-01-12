package com.example.myapplication3

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

sealed class BLEEvent {
    data class DeviceDiscovered(val device: BluetoothDevice, val name: String, val rssi: Int) : BLEEvent()
    data class DeviceConnected(val device: BluetoothDevice) : BLEEvent()
    data class DeviceDisconnected(val device: BluetoothDevice) : BLEEvent()
    data class DataReceived(val ir: Long, val red: Long, val green: Long, val accX: Int, val accY: Int, val accZ: Int) : BLEEvent()
    data class BatteryReceived(val percentage: Int) : BLEEvent()
    data class TemperatureReceived(val celsius: Float) : BLEEvent()
}

@SuppressLint("MissingPermission")
class BLEManager(private val context: Context) {
    private val TAG = "BLEManager"

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

    private val _events = MutableSharedFlow<BLEEvent>(extraBufferCapacity = 100)
    val events = _events.asSharedFlow()

    private val TGM_SERVICE_UUID = UUID.fromString("3A0FF000-98C4-46B2-94AF-1AEE0FD4C48E")
    private val SENSOR_DATA_CHAR_UUID = UUID.fromString("3A0FF001-98C4-46B2-94AF-1AEE0FD4C48E")
    private val ACCELEROMETER_CHAR_UUID = UUID.fromString("3A0FF002-98C4-46B2-94AF-1AEE0FD4C48E")
    private val COMMAND_CHAR_UUID = UUID.fromString("3A0FF003-98C4-46B2-94AF-1AEE0FD4C48E")
    private val TGM_BATTERY_CHAR_UUID = UUID.fromString("3A0FF004-98C4-46B2-94AF-1AEE0FD4C48E")

    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    fun startScanning() {
        if (isScanning) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        isScanning = true
        scanner.startScan(scanCallback)
        Log.i(TAG, "Scan started")
    }

    fun stopScanning() {
        if (!isScanning) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        isScanning = false
        scanner.stopScan(scanCallback)
        Log.i(TAG, "Scan stopped")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown"
            if (name.lowercase().contains("oralable")) {
                _events.tryEmit(BLEEvent.DeviceDiscovered(device, name, result.rssi))
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        stopScanning()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.")
                _events.tryEmit(BLEEvent.DeviceConnected(gatt.device))
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.")
                _events.tryEmit(BLEEvent.DeviceDisconnected(gatt.device))
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(TGM_SERVICE_UUID)
                if (service != null) {
                    Log.i(TAG, "TGM Service discovered")
                    
                    // Enable notifications sequentially
                    enableNotification(gatt, service.getCharacteristic(SENSOR_DATA_CHAR_UUID))
                    
                    // We need to wait for onDescriptorWrite before enabling the next one
                    // For simplicity in this basic version, we'll just delay or chain them.
                    Handler(Looper.getMainLooper()).postDelayed({
                        enableNotification(gatt, service.getCharacteristic(ACCELEROMETER_CHAR_UUID))
                    }, 500)
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        enableNotification(gatt, service.getCharacteristic(TGM_BATTERY_CHAR_UUID))
                    }, 1000)

                    Handler(Looper.getMainLooper()).postDelayed({
                        enableNotification(gatt, service.getCharacteristic(COMMAND_CHAR_UUID))
                    }, 1500)
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value ?: return
            
            when (characteristic.uuid) {
                SENSOR_DATA_CHAR_UUID -> parsePPGData(data)
                ACCELEROMETER_CHAR_UUID -> parseAccelerometerData(data)
                TGM_BATTERY_CHAR_UUID -> parseBatteryData(data)
                COMMAND_CHAR_UUID -> parseTemperatureData(data)
            }
        }

        // New API for Android 13+
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            when (characteristic.uuid) {
                SENSOR_DATA_CHAR_UUID -> parsePPGData(value)
                ACCELEROMETER_CHAR_UUID -> parseAccelerometerData(value)
                TGM_BATTERY_CHAR_UUID -> parseBatteryData(value)
                COMMAND_CHAR_UUID -> parseTemperatureData(value)
            }
        }
    }

    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
        if (characteristic == null) return
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun parsePPGData(data: ByteArray) {
        if (data.size < 244) return
        // Matching Swift logic: 
        // Bytes 0-3: Frame counter
        // Each sample is 12 bytes: Red(4), IR(4), Green(4) [Based on Swift Fix 6 mapping]
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val frameCounter = buffer.getInt(0)
        
        // Just take the first sample for the UI display in this basic app
        val red = buffer.getInt(4).toLong() and 0xFFFFFFFFL
        val ir = buffer.getInt(8).toLong() and 0xFFFFFFFFL
        val green = buffer.getInt(12).toLong() and 0xFFFFFFFFL
        
        _events.tryEmit(BLEEvent.DataReceived(ir, red, green, 0, 0, 0))
    }

    private fun parseAccelerometerData(data: ByteArray) {
        if (data.size < 154) return
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        // First sample at offset 4
        val x = buffer.getShort(4).toInt()
        val y = buffer.getShort(6).toInt()
        val z = buffer.getShort(8).toInt()
        
        // We'll emit this to update just the accelerometer part later if needed, 
        // but for now let's just log or merge
    }

    private fun parseBatteryData(data: ByteArray) {
        if (data.size < 4) return
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val mv = buffer.getInt(0)
        // Simple conversion: 4200mv = 100%, 3400mv = 0%
        val percentage = ((mv - 3400) * 100 / (4200 - 3400)).coerceIn(0, 100)
        _events.tryEmit(BLEEvent.BatteryReceived(percentage))
    }

    private fun parseTemperatureData(data: ByteArray) {
        if (data.size < 6) return
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val centiTemp = buffer.getShort(4).toShort()
        val celsius = centiTemp.toFloat() / 100f
        _events.tryEmit(BLEEvent.TemperatureReceived(celsius))
    }
}
