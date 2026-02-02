package com.oralable.app202060114.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oralable.app202060114.data.SensorDataPoint
import com.oralable.app202060114.data.SensorDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ShareUiState(
    val hasData: Boolean = false
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<ShareUiState> = SensorDataStore.recordedData
        .map { ShareUiState(hasData = it.isNotEmpty()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ShareUiState()
        )

    fun generateCsvString(): String {
        val data = SensorDataStore.getRecordedData()
        if (data.isEmpty()) {
            return ""
        }

        val header = "Timestamp,Device_Type,EMG,PPG_IR,PPG_Red,PPG_Green,Accel_X,Accel_Y,Accel_Z,Temperature"
        val rows = data.map { it.toCsvRow() }

        return (listOf(header) + rows).joinToString("\n")
    }

    private fun SensorDataPoint.toCsvRow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formattedDate = sdf.format(Date(timestamp))
        return "$formattedDate,$deviceName,${emgValue ?: ""},${ppgIr ?: ""},${ppgRed ?: ""},${ppgGreen ?: ""},${accelX ?: ""},${accelY ?: ""},${accelZ ?: ""},${temperature ?: ""}"
    }

    fun writeCsvToFile(contentResolver: ContentResolver, uri: Uri, csvString: String) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.writer().use {
                    it.write(csvString)
                }
            }
        } catch (e: IOException) {
            // Handle exception
        }
    }
}
