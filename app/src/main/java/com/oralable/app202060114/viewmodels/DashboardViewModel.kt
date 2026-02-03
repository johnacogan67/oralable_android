package com.oralable.app202060114.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oralable.app202060114.bluetooth.BLEEvent
import com.oralable.app202060114.bluetooth.BluetoothLeManager
import com.oralable.app202060114.bluetooth.SensorDataParser
import com.oralable.app202060114.data.SensorDataPoint
import com.oralable.app202060114.data.SensorDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.math.sqrt
import java.util.concurrent.TimeUnit

data class DashboardUiState(
    val isRecording: Boolean = false,
    val oralableConnected: Boolean = false,
    val anrConnected: Boolean = false,
    val duration: String = "00:00:00",
    val ppgValue: String = "0",
    val movementValue: String = "0.00",
    val movementStatus: String = "Still",
    val temperatureValue: String = "0.0",
    val emgValue: String = "0.00",
    val movementHistory: List<Double> = emptyList(),
    val ppgHistory: List<Double> = emptyList(),
    val emgHistory: List<Double> = emptyList()
)

@SuppressLint("MissingPermission")
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothLeManager = BluetoothLeManager.getInstance(application)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val movementHistory = LinkedList<Double>()
    private val ppgHistory = LinkedList<Double>()
    private val emgHistory = LinkedList<Double>()
    private var stillnessBaseline: Double? = null
    private val CALIBRATION_SIZE = 50
    private val GRAPH_HISTORY_SIZE = 50 

    private var timerJob: Job? = null
    private var recordingStartTime: Long = 0

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
        val timestamp = System.currentTimeMillis()
        val deviceName = if (device.name.contains("Oralable", true)) "Oralable" else "ANR M40"

        when (characteristic.uuid) {
            BluetoothLeManager.SENSOR_DATA_CHAR_UUID -> {
                if (!_uiState.value.oralableConnected) return
                val ppgData = SensorDataParser.parsePpgData(data)
                if (ppgData != null) {
                    updatePpg(ppgData.ppgIr.toDouble())
                    if (_uiState.value.isRecording) {
                        SensorDataStore.add(SensorDataPoint(timestamp, deviceName, ppgIr = ppgData.ppgIr, ppgRed = ppgData.ppgRed, ppgGreen = ppgData.ppgGreen))
                    }
                }
            }
            BluetoothLeManager.ACCELEROMETER_CHAR_UUID -> {
                if (!_uiState.value.oralableConnected) return
                val accelerometerData = SensorDataParser.parseAccelerometerData(data)
                if (accelerometerData != null) {
                    if (_uiState.value.isRecording) {
                        SensorDataStore.add(SensorDataPoint(timestamp, deviceName, accelX = accelerometerData.x, accelY = accelerometerData.y, accelZ = accelerometerData.z))
                    }
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
                    if (_uiState.value.isRecording) {
                        SensorDataStore.add(SensorDataPoint(timestamp, deviceName, temperature = temperatureData.celsius))
                    }
                }
            }
            BluetoothLeManager.EMG_CHAR_UUID -> {
                if (!_uiState.value.anrConnected) return
                val emgData = SensorDataParser.parseEmgData(data)
                if (emgData != null) {
                    updateEmg(emgData.value)
                    if (_uiState.value.isRecording) {
                        SensorDataStore.add(SensorDataPoint(timestamp, deviceName, emgValue = emgData.value))
                    }
                }
            }
        }
    }
    
    private fun updatePpg(value: Double) {
        ppgHistory.add(value)
        while (ppgHistory.size > GRAPH_HISTORY_SIZE) {
            ppgHistory.removeFirst()
        }
        _uiState.value = _uiState.value.copy(
            ppgValue = String.format("%.0f", value),
            ppgHistory = ppgHistory.toList()
        )
    }

    private fun updateEmg(value: Double) {
        emgHistory.add(value)
        while (emgHistory.size > GRAPH_HISTORY_SIZE) {
            emgHistory.removeFirst()
        }
        _uiState.value = _uiState.value.copy(
            emgValue = String.format("%.2f", value),
            emgHistory = emgHistory.toList()
        )
    }

    private fun updateMovement(magnitude: Double) {
        movementHistory.add(magnitude)
        while (movementHistory.size > CALIBRATION_SIZE) {
            movementHistory.removeFirst()
        }

        if (stillnessBaseline == null && movementHistory.size == CALIBRATION_SIZE) {
            stillnessBaseline = movementHistory.average() * 1.2
        }

        val status = stillnessBaseline?.let {
            if (magnitude > it * 1.5) "Active" else "Still"
        } ?: "Calibrating..."

        val graphData = movementHistory.takeLast(GRAPH_HISTORY_SIZE)

        _uiState.value = _uiState.value.copy(
            movementValue = String.format("%.2f", magnitude),
            movementStatus = status,
            movementHistory = graphData
        )
    }

    fun toggleRecording() {
        val isCurrentlyRecording = _uiState.value.isRecording
        _uiState.value = _uiState.value.copy(isRecording = !isCurrentlyRecording)
        if (!isCurrentlyRecording) {
            SensorDataStore.startRecording()
            startTimer()
        } else {
            SensorDataStore.stopRecording()
            stopTimer()
        }
    }

    private fun startTimer() {
        recordingStartTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsedTime = System.currentTimeMillis() - recordingStartTime
                _uiState.value = _uiState.value.copy(duration = formatDuration(elapsedTime))
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(duration = "00:00:00")
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
