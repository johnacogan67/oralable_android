package com.oralable.app202060114.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothLeManager private constructor(private val context: Context) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    private var onConnectionStateChange: ((BluetoothDevice, Boolean, Boolean) -> Unit)? = null
    private var onDataReceived: ((BluetoothGattCharacteristic, ByteArray) -> Unit)? = null
    private var gatt: BluetoothGatt? = null
    private val notificationQueue: Queue<BluetoothGattDescriptor> = LinkedList()

    companion object {
        @Volatile
        private var INSTANCE: BluetoothLeManager? = null

        fun getInstance(context: Context): BluetoothLeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothLeManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        val TGM_SERVICE_UUID: UUID = UUID.fromString("3A0FF000-98C4-46B2-94AF-1AEE0FD4C48E")
        val SENSOR_DATA_CHAR_UUID: UUID = UUID.fromString("3A0FF001-98C4-46B2-94AF-1AEE0FD4C48E")
        val ACCELEROMETER_CHAR_UUID: UUID = UUID.fromString("3A0FF002-98C4-46B2-94AF-1AEE0FD4C48E")
        val TEMPERATURE_CHAR_UUID: UUID = UUID.fromString("3A0FF003-98C4-46B2-94AF-1AEE0FD4C48E")
        val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            onDeviceFound?.invoke(result.device)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                this@BluetoothLeManager.gatt = gatt
                gatt.discoverServices()
                onConnectionStateChange?.invoke(gatt.device, true, false)
            } else {
                onConnectionStateChange?.invoke(gatt.device, false, false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            enableNotifications(gatt)
            onConnectionStateChange?.invoke(gatt.device, true, true)
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            onDataReceived?.invoke(characteristic, value)
        }
        
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.d("BluetoothLeManager", "Descriptor written for ${descriptor.characteristic.uuid}")
            processNotificationQueue()
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (!hasPermissions() || !isBluetoothEnabled()) return
        this.onDeviceFound = onDeviceFound
        bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        if (!hasPermissions()) return
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun connect(device: BluetoothDevice, onConnectionStateChange: (BluetoothDevice, Boolean, Boolean) -> Unit) {
        if (!hasPermissions()) return
        this.onConnectionStateChange = onConnectionStateChange
        device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        if (!hasPermissions()) return
        gatt?.disconnect()
    }
    
    fun setOnDataReceivedListener(listener: (BluetoothGattCharacteristic, ByteArray) -> Unit) {
        onDataReceived = listener
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(TGM_SERVICE_UUID)
        if (service == null) {
            Log.e("BluetoothLeManager", "TGM service not found")
            return
        }

        val characteristics = listOf(
            service.getCharacteristic(SENSOR_DATA_CHAR_UUID),
            service.getCharacteristic(ACCELEROMETER_CHAR_UUID),
            service.getCharacteristic(TEMPERATURE_CHAR_UUID)
        )

        for (characteristic in characteristics) {
            if (characteristic == null) {
                Log.w("BluetoothLeManager", "A characteristic was not found")
                continue
            }
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            notificationQueue.add(descriptor)
        }
        processNotificationQueue()
    }
    
    private fun processNotificationQueue() {
        if (notificationQueue.isNotEmpty()) {
            val descriptor = notificationQueue.poll()
            gatt?.writeDescriptor(descriptor)
            Log.d("BluetoothLeManager", "Writing descriptor for ${descriptor.characteristic.uuid}")
        }
    }
}
