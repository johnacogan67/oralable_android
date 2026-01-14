package com.oralable.app202060114.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MovementMetricCard(
    value: String,
    unit: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsRun, contentDescription = "Movement", tint = Color.Blue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Movement", style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = status, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 4.dp), color = Color.Gray)
            }
        }
    }
}
