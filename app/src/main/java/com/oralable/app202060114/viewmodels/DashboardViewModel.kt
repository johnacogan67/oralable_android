package com.oralable.app202060114.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oralable.app202060114.bluetooth.BluetoothLeManager
import com.oralable.app202060114.bluetooth.ConnectionManager
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
    val temperatureValue: String = "0.0"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothLeManager = BluetoothLeManager.getInstance(application)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val movementHistory = LinkedList<Double>()
    private var stillnessBaseline: Double? = null
    private val CALIBRATION_SIZE = 250

    init {
        ConnectionManager.connectionState
            .onEach { connectionState ->
                _uiState.value = _uiState.value.copy(
                    oralableConnected = connectionState.oralableConnected,
                    anrConnected = connectionState.anrConnected
                )
            }
            .launchIn(viewModelScope)

        bluetoothLeManager.setOnDataReceivedListener { characteristic, data ->
            val hexString = data.joinToString(separator = " ") { "%02X".format(it) }
            Log.d("DashboardViewModel", "Received data from ${characteristic.uuid}: $hexString")

            when (characteristic.uuid) {
                BluetoothLeManager.SENSOR_DATA_CHAR_UUID -> {
                    val ppgData = SensorDataParser.parsePpgData(data)
                    if (ppgData != null) {
                        Log.d("DashboardViewModel", "Parsed PPG data: IR=${ppgData.ppgIr}, Red=${ppgData.ppgRed}, Green=${ppgData.ppgGreen}")
                        _uiState.value = _uiState.value.copy(ppgValue = ppgData.ppgIr.toString())
                    } else {
                        Log.w("DashboardViewModel", "Failed to parse PPG data.")
                    }
                }
                BluetoothLeManager.ACCELEROMETER_CHAR_UUID -> {
                    val accelerometerData = SensorDataParser.parseAccelerometerData(data)
                    if (accelerometerData != null) {
                        Log.d("DashboardViewModel", "Parsed Accelerometer data: X=${accelerometerData.x}, Y=${accelerometerData.y}, Z=${accelerometerData.z}")
                        
                        val x = accelerometerData.x.toDouble()
                        val y = accelerometerData.y.toDouble()
                        val z = accelerometerData.z.toDouble()

                        val magnitude = sqrt(x*x + y*y + z*z) / 16384.0
                        
                        updateMovement(magnitude)
                    } else {
                        Log.w("DashboardViewModel", "Failed to parse Accelerometer data.")
                    }
                }
                BluetoothLeManager.TEMPERATURE_CHAR_UUID -> {
                    val temperatureData = SensorDataParser.parseTemperatureData(data)
                    if (temperatureData != null) {
                        Log.d("DashboardViewModel", "Parsed Temperature data: ${temperatureData.celsius}°C")
                        _uiState.value = _uiState.value.copy(temperatureValue = String.format("%.1f", temperatureData.celsius))
                    } else {
                        Log.w("DashboardViewModel", "Failed to parse Temperature data.")
                    }
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
