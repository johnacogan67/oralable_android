package com.oralable.app202060114.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SensorDataStore {
    private val _recordedData = MutableStateFlow<List<SensorDataPoint>>(emptyList())
    val recordedData = _recordedData.asStateFlow()

    private var isRecording = false

    fun startRecording() {
        if (!isRecording) {
            _recordedData.value = emptyList()
            isRecording = true
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    fun add(dataPoint: SensorDataPoint) {
        if (isRecording) {
            _recordedData.value = _recordedData.value + dataPoint
        }
    }

    fun getRecordedData(): List<SensorDataPoint> {
        return _recordedData.value
    }
}
