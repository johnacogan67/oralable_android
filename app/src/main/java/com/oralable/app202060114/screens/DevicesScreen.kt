package com.oralable.app202060114.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oralable.app202060114.composables.DeviceDialog
import com.oralable.app202060114.composables.DeviceRow
import com.oralable.app202060114.viewmodels.Device
import com.oralable.app202060114.viewmodels.DevicesViewModel

@Composable
fun DevicesScreen(
    modifier: Modifier = Modifier,
    viewModel: DevicesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDevice by remember { mutableStateOf<Device?>(null) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.permissionsGranted()
    }

    LaunchedEffect(uiState.needsPermissions) {
        if (uiState.needsPermissions) {
            permissionLauncher.launch(permissions)
        }
    }

    if (selectedDevice != null) {
        DeviceDialog(
            device = selectedDevice!!,
            onDismiss = { selectedDevice = null },
            onDisconnect = {
                viewModel.disconnect(selectedDevice!!.address)
                selectedDevice = null
            },
            onForget = {
                viewModel.forgetDevice(selectedDevice!!.address)
                selectedDevice = null
            }
        )
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "My Devices", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        uiState.myDevices.forEach { device ->
            DeviceRow(
                name = device.name,
                address = device.address,
                status = device.status,
                statusColor = device.statusColor,
                onClick = { if (device.status == "Ready") selectedDevice = device },
                onInfoClick = device.onInfoClick
            )
        }
        Row(modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Other Devices", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            if (uiState.isScanning) {
                CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (!uiState.bluetoothEnabled) {
            Text("Please turn on Bluetooth", color = androidx.compose.ui.graphics.Color.Red)
        }
        uiState.otherDevices.forEach { device ->
            DeviceRow(
                name = device.name,
                address = device.address,
                status = device.status,
                statusColor = device.statusColor,
                onClick = { viewModel.connectToDevice(device.address) },
                onInfoClick = device.onInfoClick
            )
        }
        Button(onClick = { viewModel.startScan() }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (uiState.isScanning) "Scanning..." else "Scan")
        }
    }
}
