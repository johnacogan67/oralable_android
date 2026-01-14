package com.oralable.app202060114.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.oralable.app202060114.viewmodels.Device

@Composable
fun DeviceDialog(
    device: Device,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = device.name) },
        text = { Text(text = "What would you like to do with this device?") },
        confirmButton = {
            TextButton(onClick = onDisconnect) {
                Text("Disconnect")
            }
        },
        dismissButton = {
            TextButton(onClick = onForget) {
                Text("Forget this device")
            }
        }
    )
}
