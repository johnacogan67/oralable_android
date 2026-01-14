package com.oralable.app202060114.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oralable.app202060114.bluetooth.BluetoothLeManager
import com.oralable.app202060114.bluetooth.ConnectionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Device(
    val name: String,
    val address: String,
    val status: String,
    val statusColor: Color,
    val onInfoClick: (() -> Unit)?
)

data class DevicesUiState(
    val myDevices: List<Device> = emptyList(),
    val otherDevices: List<Device> = emptyList(),
    val isScanning: Boolean = false,
    val needsPermissions: Boolean = false,
    val bluetoothEnabled: Boolean = true
)

@SuppressLint("MissingPermission")
class DevicesViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothLeManager = BluetoothLeManager.getInstance(application)
    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    private val discoveredDevices = mutableMapOf<String, BluetoothDevice>()
    private val myDevices = mutableMapOf<String, Device>()

    init {
        _uiState.value = DevicesUiState(
            myDevices = myDevices.values.toList(),
            bluetoothEnabled = bluetoothLeManager.isBluetoothEnabled()
        )
    }

    fun startScan() {
        if (!bluetoothLeManager.hasPermissions()) {
            _uiState.value = _uiState.value.copy(needsPermissions = true)
            return
        }
        if (!bluetoothLeManager.isBluetoothEnabled()) {
            _uiState.value = _uiState.value.copy(bluetoothEnabled = false)
            return
        }
        _uiState.value = _uiState.value.copy(isScanning = true, otherDevices = emptyList())
        discoveredDevices.clear()

        bluetoothLeManager.startScan { device ->
            if (device.name != null && !myDevices.containsKey(device.address)) {
                discoveredDevices[device.address] = device
                updateOtherDevicesList()
            }
        }

        viewModelScope.launch {
            delay(10000) // Scan for 10 seconds
            stopScan()
        }
    }

    private fun stopScan() {
        bluetoothLeManager.stopScan()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun connectToDevice(address: String) {
        val device = discoveredDevices[address] ?: return
        updateDeviceStatus(address, "Connecting...", Color(0xFFFFA500))

        bluetoothLeManager.connect(device) { connectedDevice, isConnected, servicesDiscovered ->
            if (isConnected) {
                if (servicesDiscovered) {
                    val newDevice = Device(
                        name = connectedDevice.name,
                        address = connectedDevice.address,
                        status = "Ready",
                        statusColor = Color.Green,
                        onInfoClick = {}
                    )
                    myDevices[address] = newDevice
                    _uiState.value = _uiState.value.copy(myDevices = myDevices.values.toList())
                    removeDeviceFromOtherDevices(connectedDevice.address)
                    if (connectedDevice.name.contains("Oralable", ignoreCase = true)) {
                        ConnectionManager.setOralableConnected(true)
                    } else if (connectedDevice.name.contains("ANR", ignoreCase = true)) {
                        ConnectionManager.setAnrConnected(true)
                    }
                } else {
                    updateDeviceStatus(connectedDevice.address, "Discovering Services...", Color(0xFFFFA500))
                }
            } else {
                handleDisconnection(address, "Failed", Color.Red)
            }
        }
    }
    
    fun disconnect(address: String) {
        bluetoothLeManager.disconnect()
        handleDisconnection(address, "Not Connected", Color.Gray)
    }
    
    fun forgetDevice(address: String) {
        disconnect(address)
        myDevices.remove(address)
        _uiState.value = _uiState.value.copy(myDevices = myDevices.values.toList())
    }

    private fun handleDisconnection(address: String, status: String, color: Color) {
        updateDeviceStatus(address, status, color)
        if (myDevices.containsKey(address)) {
            myDevices[address] = myDevices[address]!!.copy(status = status, statusColor = color)
            _uiState.value = _uiState.value.copy(myDevices = myDevices.values.toList())
            if (myDevices[address]?.name?.contains("Oralable", ignoreCase = true) == true) {
                ConnectionManager.setOralableConnected(false)
            } else if (myDevices[address]?.name?.contains("ANR", ignoreCase = true) == true) {
                ConnectionManager.setAnrConnected(false)
            }
        }
    }

    private fun updateDeviceStatus(address: String, status: String, color: Color) {
        val currentList = _uiState.value.otherDevices.toMutableList()
        val index = currentList.indexOfFirst { it.address == address }
        if (index != -1) {
            currentList[index] = currentList[index].copy(status = status, statusColor = color)
            _uiState.value = _uiState.value.copy(otherDevices = currentList)
        }
    }
    
    private fun removeDeviceFromOtherDevices(address: String) {
        val currentList = _uiState.value.otherDevices.toMutableList()
        currentList.removeAll { it.address == address }
        _uiState.value = _uiState.value.copy(otherDevices = currentList)
    }
    
    private fun updateOtherDevicesList() {
        _uiState.value = _uiState.value.copy(otherDevices = discoveredDevices.values.map {
            Device(it.name, it.address, "Not Connected", Color.Gray, null)
        })
    }

    fun permissionsGranted() {
        _uiState.value = _uiState.value.copy(needsPermissions = false)
        startScan()
    }
}
