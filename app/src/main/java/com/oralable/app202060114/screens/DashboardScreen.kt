package com.oralable.app202060114.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Waves
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oralable.app202060114.composables.DataGraphCard
import com.oralable.app202060114.composables.DeviceStatusIndicator
import com.oralable.app202060114.composables.MetricCard
import com.oralable.app202060114.composables.RecordingButton
import com.oralable.app202060114.viewmodels.DashboardViewModel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DeviceStatusIndicator(deviceName = "Oralable", isConnected = uiState.oralableConnected)
        DeviceStatusIndicator(deviceName = "ANR M40", isConnected = uiState.anrConnected)
        RecordingButton(
            isRecording = uiState.isRecording,
            isConnected = uiState.oralableConnected || uiState.anrConnected,
            duration = uiState.duration,
            action = { viewModel.toggleRecording() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        DataGraphCard(
            title = "",
            value = uiState.ppgValue,
            unit = "",
            icon = Icons.Default.Timeline,
            lineColor = Color.Magenta,
            data = uiState.ppgHistory
        )
        DataGraphCard(
            title = "EMG",
            value = uiState.emgValue,
            unit = "mV",
            icon = Icons.Default.Waves,
            lineColor = Color.Cyan,
            data = uiState.emgHistory
        )
        DataGraphCard(
            title = "Movement",
            value = uiState.movementValue,
            unit = "g",
            subtitle = uiState.movementStatus,
            icon = Icons.Default.OpenWith,
            lineColor = Color.Blue,
            data = uiState.movementHistory
        )
        MetricCard(
            title = "Temperature",
            value = uiState.temperatureValue,
            unit = "°C",
            icon = Icons.Default.Thermostat,
            iconColor = Color.Red
        )
    }
}
