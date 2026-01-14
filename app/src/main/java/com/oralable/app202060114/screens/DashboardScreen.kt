package com.oralable.app202060114.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oralable.app202060114.composables.DeviceStatusIndicator
import com.oralable.app202060114.composables.HealthMetricCard
import com.oralable.app202060114.composables.MovementMetricCard
import com.oralable.app202060114.composables.RecordingButton
import com.oralable.app202060114.viewmodels.DashboardViewModel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.padding(16.dp),
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
        HealthMetricCard(
            icon = Icons.Default.Timeline,
            title = "PPG Sensor",
            value = uiState.ppgValue,
            unit = "Oralable IR",
            color = Color.Magenta
        )
        MovementMetricCard(
            value = uiState.movementValue,
            unit = "g",
            status = uiState.movementStatus
        )
        HealthMetricCard(
            icon = Icons.Default.Thermostat,
            title = "Temperature",
            value = uiState.temperatureValue,
            unit = "°C",
            color = Color.Red
        )
    }
}
