package com.oralable.app202060114.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oralable.app202060114.bluetooth.BLEEvent
import com.oralable.app202060114.bluetooth.BluetoothLeManager
import com.oralable.app202060114.bluetooth.DevicePersistence
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val devicePersistence = DevicePersistence(application)
    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    private val myDevices = mutableMapOf<String, Device>()

    init {
        _uiState.value = DevicesUiState(
            myDevices = myDevices.values.toList(),
            bluetoothEnabled = bluetoothLeManager.isBluetoothEnabled()
        )

        if (bluetoothLeManager.hasPermissions()) {
            bluetoothLeManager.reconnectToSavedDevices()
        }

        bluetoothLeManager.discoveredDevices
            .onEach { devices ->
                val otherDevices = devices
                    .filter { !myDevices.containsKey(it.device.address) && it.name != "Unknown" }
                    .map {
                        Device(
                            name = it.name,
                            address = it.device.address,
                            status = "Not Connected",
                            statusColor = Color.Gray,
                            onInfoClick = null
                        )
                    }
                _uiState.value = _uiState.value.copy(otherDevices = otherDevices)
            }
            .launchIn(viewModelScope)

        bluetoothLeManager.eventFlow
            .onEach { event ->
                when (event) {
                    is BLEEvent.DeviceConnected -> {
                        val connectedDevice = event.device
                        devicePersistence.saveDevice(connectedDevice.address)
                        val newDevice = Device(
                            name = connectedDevice.name ?: "Unknown",
                            address = connectedDevice.address,
                            status = "Ready",
                            statusColor = Color.Green,
                            onInfoClick = {}
                        )
                        myDevices[connectedDevice.address] = newDevice

                        val newOtherDevices = _uiState.value.otherDevices.filterNot { it.address == connectedDevice.address }

                        _uiState.value = _uiState.value.copy(
                            myDevices = myDevices.values.toList(),
                            otherDevices = newOtherDevices
                        )
                    }
                    is BLEEvent.DeviceDisconnected -> {
                        handleDisconnection(event.device.address)
                    }
                    is BLEEvent.Error -> {
                        if (event.error is com.oralable.app202060114.bluetooth.BLEError.BluetoothUnauthorized) {
                            _uiState.value = _uiState.value.copy(needsPermissions = true)
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
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
        _uiState.value = _uiState.value.copy(isScanning = true)

        bluetoothLeManager.startScan()

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
        val device = bluetoothLeManager.discoveredDevices.value.find { it.device.address == address }?.device ?: return
        updateDeviceStatus(address, "Connecting...", Color(0xFFFFA500))
        bluetoothLeManager.connect(device)
    }
    
    fun disconnect(address: String) {
        bluetoothLeManager.disconnect(address)
    }
    
    fun forgetDevice(address: String) {
        devicePersistence.removeDevice(address)
        disconnect(address)
    }

    private fun handleDisconnection(address: String) {
        if (myDevices.containsKey(address)) {
            myDevices.remove(address)
            _uiState.value = _uiState.value.copy(myDevices = myDevices.values.toList())
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
    
    fun permissionsGranted() {
        _uiState.value = _uiState.value.copy(needsPermissions = false)
        bluetoothLeManager.reconnectToSavedDevices()
        startScan()
    }
}
