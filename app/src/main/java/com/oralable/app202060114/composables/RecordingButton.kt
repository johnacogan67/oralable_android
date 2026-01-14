package com.oralable.app202060114.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecordingButton(
    isRecording: Boolean,
    isConnected: Boolean,
    duration: String,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor = if (!isConnected) Color.Gray else if (isRecording) Color.Red else Color.Black
    val icon = if (isRecording) Icons.Default.Stop else Icons.Default.Circle

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = action,
            enabled = isConnected,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            modifier = Modifier.size(70.dp)
        ) {
            Icon(icon, contentDescription = "Record", tint = Color.White)
        }
        if (isRecording) {
            Text(text = duration, color = Color.Red)
        } else {
            Text(text = if (isConnected) "Record" else "Not Connected")
        }
    }
}
