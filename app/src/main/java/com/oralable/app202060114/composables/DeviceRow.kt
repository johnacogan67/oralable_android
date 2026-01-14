package com.oralable.app202060114.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DeviceRow(
    name: String,
    address: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name)
            Text(text = address, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(text = status, color = statusColor, modifier = Modifier.padding(start = 6.dp))
        }
        if (onInfoClick != null) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Info",
                tint = Color.Blue,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
