package com.oralable.app202060114.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun HealthMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Icon(icon, contentDescription = title, tint = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Row {
                Text(text = value, style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
