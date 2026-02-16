package com.oralable.app202060114.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oralable.app202060114.data.SensorDataPoint
import com.oralable.app202060114.data.SensorDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ShareUiState(
    val hasData: Boolean = false,
    val errorMessage: String? = null
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> get() = _uiState.asStateFlow()

    init {
        SensorDataStore.recordedData
            .onEach { data ->
                _uiState.update { it.copy(hasData = data.isNotEmpty()) }
            }
            .launchIn(viewModelScope)
    }

    fun generateCsvString(): String {
        val data = SensorDataStore.getRecordedData()
        if (data.isEmpty()) {
            return ""
        }

        val header = "Timestamp,Device_Type,EMG,PPG_IR,PPG_Red,PPG_Green,Accel_X,Accel_Y,Accel_Z,Temperature,Battery,Heart_Rate"
        val rows = data.map { it.toCsvRow() }

        return (listOf(header) + rows).joinToString("\n")
    }

    private fun SensorDataPoint.toCsvRow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formattedDate = sdf.format(Date(timestamp))
        return "$formattedDate,$deviceName,${emgValue ?: ""},${ppgIr ?: ""},${ppgRed ?: ""},${ppgGreen ?: ""},${accelX ?: ""},${accelY ?: ""},${accelZ ?: ""},${temperature ?: ""},${battery ?: ""},${heartRate ?: ""}"
    }

    fun writeCsvToFile(contentResolver: ContentResolver, uri: Uri, csvString: String) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.writer().use {
                    it.write(csvString)
                }
            }
        } catch (e: IOException) {
            Log.e("ShareViewModel", "Error writing CSV file", e)
            _uiState.update { it.copy(errorMessage = "Failed to save CSV file.") }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
