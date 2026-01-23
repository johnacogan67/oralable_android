package com.oralable.app202060114.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oralable.app202060114.bluetooth.BLEEvent
import com.oralable.app202060114.bluetooth.BluetoothLeManager
import com.oralable.app202060114.bluetooth.SensorDataParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.LinkedList
import kotlin.math.sqrt

data class DashboardUiState(
    val isRecording: Boolean = false,
    val oralableConnected: Boolean = false,
    val anrConnected: Boolean = false,
    val duration: String = "00:00:00",
    val ppgValue: String = "0",
    val movementValue: String = "0.00",
    val movementStatus: String = "Still",
    val temperatureValue: String = "0.0",
    val emgValue: String = "0.00"
)

@SuppressLint("MissingPermission")
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothLeManager = BluetoothLeManager.getInstance(application)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val movementHistory = LinkedList<Double>()
    private var stillnessBaseline: Double? = null
    private val CALIBRATION_SIZE = 250

    init {
        if (bluetoothLeManager.hasPermissions()) {
            bluetoothLeManager.reconnectToSavedDevices()

            bluetoothLeManager.eventFlow
                .onEach { event ->
                    when (event) {
                        is BLEEvent.DeviceConnected -> {
                            if (event.device.name?.contains("Oralable", ignoreCase = true) == true) {
                                _uiState.value = _uiState.value.copy(oralableConnected = true)
                            } else if (event.device.name?.contains("ANR", ignoreCase = true) == true) {
                                _uiState.value = _uiState.value.copy(anrConnected = true)
                            }
                        }
                        is BLEEvent.DeviceDisconnected -> {
                            if (event.device.name?.contains("Oralable", ignoreCase = true) == true) {
                                _uiState.value = _uiState.value.copy(oralableConnected = false)
                            } else if (event.device.name?.contains("ANR", ignoreCase = true) == true) {
                                _uiState.value = _uiState.value.copy(anrConnected = false)
                            }
                        }
                        is BLEEvent.DataReceived -> {
                            handleData(event.device, event.characteristic, event.value)
                        }
                        else -> {}
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    
    private fun handleData(device: BluetoothDevice, characteristic: android.bluetooth.BluetoothGattCharacteristic, data: ByteArray) {
        val hexString = data.joinToString(separator = " ") { "%02X".format(it) }
        Log.d("DashboardViewModel", "Received data from ${device.address} - ${characteristic.uuid}: $hexString")

        when (characteristic.uuid) {
            BluetoothLeManager.SENSOR_DATA_CHAR_UUID -> {
                if (!_uiState.value.oralableConnected) return
                val ppgData = SensorDataParser.parsePpgData(data)
                if (ppgData != null) {
                    _uiState.value = _uiState.value.copy(ppgValue = ppgData.ppgIr.toString())
                }
            }
            BluetoothLeManager.ACCELEROMETER_CHAR_UUID -> {
                if (!_uiState.value.oralableConnected) return
                val accelerometerData = SensorDataParser.parseAccelerometerData(data)
                if (accelerometerData != null) {
                    val x = accelerometerData.x.toDouble()
                    val y = accelerometerData.y.toDouble()
                    val z = accelerometerData.z.toDouble()
                    val magnitude = sqrt(x*x + y*y + z*z) / 16384.0
                    updateMovement(magnitude)
                }
            }
            BluetoothLeManager.TEMPERATURE_CHAR_UUID -> {
                if (!_uiState.value.oralableConnected) return
                val temperatureData = SensorDataParser.parseTemperatureData(data)
                if (temperatureData != null) {
                    _uiState.value = _uiState.value.copy(temperatureValue = String.format("%.1f", temperatureData.celsius))
                }
            }
            BluetoothLeManager.EMG_CHAR_UUID -> {
                if (!_uiState.value.anrConnected) return
                val emgData = SensorDataParser.parseEmgData(data)
                if (emgData != null) {
                    _uiState.value = _uiState.value.copy(emgValue = String.format("%.2f", emgData.value))
                }
            }
        }
    }
    
    private fun updateMovement(magnitude: Double) {
        movementHistory.add(magnitude)
        if (movementHistory.size > CALIBRATION_SIZE) {
            movementHistory.removeFirst()
        }

        if (stillnessBaseline == null && movementHistory.size == CALIBRATION_SIZE) {
            stillnessBaseline = movementHistory.average() * 1.2
        }

        val status = stillnessBaseline?.let {
            if (magnitude > it * 1.5) "Active" else "Still"
        } ?: "Calibrating..."

        _uiState.value = _uiState.value.copy(
            movementValue = String.format("%.2f", magnitude),
            movementStatus = status
        )
    }

    fun toggleRecording() {
        _uiState.value = _uiState.value.copy(isRecording = !_uiState.value.isRecording)
    }
}
