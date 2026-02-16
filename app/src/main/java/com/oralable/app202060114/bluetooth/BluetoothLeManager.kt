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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed class BLEError(val message: String) {
    object BluetoothNotReady : BLEError("Bluetooth is not ready.")
    object BluetoothUnauthorized : BLEError("Bluetooth permissions are not granted.")
    object BluetoothUnsupported : BLEError("Bluetooth LE is not supported on this device.")
    object BluetoothResetting : BLEError("Bluetooth is resetting.")
    data class ConnectionFailed(val device: BluetoothDevice, val reason: String) : BLEError("Connection failed to ${device.address}: $reason")
    data class UnexpectedDisconnection(val device: BluetoothDevice, val reason: String?) : BLEError("Unexpected disconnection from ${device.address}: ${reason ?: "No reason given"}")
}

sealed class BLEEvent {
    data class DeviceDiscovered(val device: BluetoothDevice, val name: String, val rssi: Int) : BLEEvent()
    data class DeviceConnected(val device: BluetoothDevice) : BLEEvent()
    data class DeviceDisconnected(val device: BluetoothDevice, val error: BLEError?) : BLEEvent()
    data class BluetoothStateChanged(val state: Int) : BLEEvent()
    data class DataReceived(val device: BluetoothDevice, val characteristic: BluetoothGattCharacteristic, val value: ByteArray) : BLEEvent()
    data class Error(val error: BLEError) : BLEEvent()
}

data class DiscoveredDevice(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int
)

@SuppressLint("MissingPermission")
class BluetoothLeManager private constructor(private val context: Context) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private val devicePersistence = DevicePersistence(context)

    private val _eventFlow = MutableSharedFlow<BLEEvent>(replay = 5)
    val eventFlow = _eventFlow.asSharedFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _bluetoothState = MutableStateFlow(bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF)
    val bluetoothState = _bluetoothState.asStateFlow()

    private val gattMap = ConcurrentHashMap<String, BluetoothGatt>()
    private val notificationQueueMap = ConcurrentHashMap<String, Queue<BluetoothGattDescriptor>>()
    private val disconnectingDevices = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

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
        val BATTERY_CHAR_UUID: UUID = UUID.fromString("3A0FF004-98C4-46B2-94AF-1AEE0FD4C48E")

        val ANR_SERVICE_UUID: UUID = UUID.fromString("00001815-0000-1000-8000-00805f9b34fb")
        val EMG_CHAR_UUID: UUID = UUID.fromString("00002a58-0000-1000-8000-00805f9b34fb")

        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

        val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val name = result.device.name ?: result.scanRecord?.deviceName ?: "Unknown"
            
            val discoveredDevice = DiscoveredDevice(result.device, name, result.rssi)
            if (_discoveredDevices.value.none { it.device.address == discoveredDevice.device.address }) {
                _discoveredDevices.value = _discoveredDevices.value + discoveredDevice
            }
            
            _eventFlow.tryEmit(BLEEvent.DeviceDiscovered(result.device, name, result.rssi))
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BluetoothLeManager", "Scan failed with error code: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                disconnectingDevices.remove(address)
                gattMap[address] = gatt
                gatt.discoverServices()
            } else {
                val closedGatt = gattMap.remove(address)
                closedGatt?.close()
                // Do NOT clear the disconnecting flag here, leave it tainted
                val error = if (status != BluetoothGatt.GATT_SUCCESS) {
                    BLEError.UnexpectedDisconnection(gatt.device, "GATT Status: $status")
                } else null
                _eventFlow.tryEmit(BLEEvent.DeviceDisconnected(gatt.device, error))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            enableNotifications(gatt)
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (disconnectingDevices.contains(gatt.device.address)) {
                return
            }
            _eventFlow.tryEmit(BLEEvent.DataReceived(gatt.device, characteristic, value))
        }
        
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            processNotificationQueue(gatt.device.address)
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

    fun reconnectToSavedDevices() {
        if (!isBluetoothEnabled() || !hasPermissions()) {
            return
        }
        val savedDevices = devicePersistence.getSavedDeviceAddresses()
        savedDevices.forEach { address ->
            connectToAddress(address)
        }
    }

    fun startScan() {
        if (!hasPermissions()) {
            _eventFlow.tryEmit(BLEEvent.Error(BLEError.BluetoothUnauthorized))
            return
        }
        if (!isBluetoothEnabled()) {
            _eventFlow.tryEmit(BLEEvent.Error(BLEError.BluetoothNotReady))
            return
        }
        _discoveredDevices.value = emptyList()
        bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        if (!hasPermissions()) return
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun connect(device: BluetoothDevice) {
        if (!hasPermissions() || !isBluetoothEnabled()) return
        disconnectingDevices.remove(device.address) // Clear the flag on a new connection attempt
        device.connectGatt(context, false, gattCallback)
    }

    fun connectToAddress(address: String) {
        if (!hasPermissions() || !isBluetoothEnabled()) return
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device != null) {
            connect(device)
        }
    }

    fun disconnect(address: String) {
        if (!hasPermissions()) return
        disconnectingDevices.add(address)
        gattMap[address]?.disconnect()
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        val address = gatt.device.address
        val queue = notificationQueueMap.getOrPut(address) { LinkedList() }
        
        val tgmService = gatt.getService(TGM_SERVICE_UUID)
        if (tgmService != null) {
            val characteristics = listOf(
                tgmService.getCharacteristic(SENSOR_DATA_CHAR_UUID),
                tgmService.getCharacteristic(ACCELEROMETER_CHAR_UUID),
                tgmService.getCharacteristic(TEMPERATURE_CHAR_UUID),
                tgmService.getCharacteristic(BATTERY_CHAR_UUID)
            )
            for (characteristic in characteristics) {
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    queue.add(descriptor)
                }
            }
        }
        
        val anrService = gatt.getService(ANR_SERVICE_UUID)
        if (anrService != null) {
            val emgCharacteristic = anrService.getCharacteristic(EMG_CHAR_UUID)
            if (emgCharacteristic != null) {
                gatt.setCharacteristicNotification(emgCharacteristic, true)
                val descriptor = emgCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                queue.add(descriptor)
            }
        }

        val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
        if (batteryService != null) {
            val batteryLevelCharacteristic = batteryService.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
            if (batteryLevelCharacteristic != null) {
                gatt.setCharacteristicNotification(batteryLevelCharacteristic, true)
                val descriptor = batteryLevelCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                queue.add(descriptor)
            }
        }
        
        processNotificationQueue(address)
    }
    
    private fun processNotificationQueue(address: String) {
        val queue = notificationQueueMap[address]
        if (queue != null && queue.isNotEmpty()) {
            val descriptor = queue.poll()
            gattMap[address]?.writeDescriptor(descriptor)
        } else {
            gattMap[address]?.let {
                _eventFlow.tryEmit(BLEEvent.DeviceConnected(it.device))
            }
        }
    }
}
