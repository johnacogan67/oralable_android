package com.oralable.app202060114.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DeviceStatusIndicator(
    deviceName: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color.Green else Color.Gray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = deviceName)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (isConnected) "Ready" else "Not Connected",
            color = if (isConnected) Color.Green else Color.Gray
        )
    }
}
