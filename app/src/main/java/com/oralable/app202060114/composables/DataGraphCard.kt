package com.oralable.app202060114.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DataGraphCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String? = null,
    data: List<Double>,
    icon: ImageVector,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp),
                        color = lineColor.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = lineColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            LineGraph(
                data = data,
                lineColor = lineColor,
                modifier = Modifier
                    .height(60.dp)
                    .width(100.dp)
            )
        }
    }
}

@Composable
private fun LineGraph(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val maxVal = data.maxOrNull() ?: 1.0
        val minVal = data.minOrNull() ?: 0.0
        val valueRange = if (maxVal - minVal > 0) maxVal - minVal else 1.0

        val path = Path()
        
        val firstX = 0f
        val firstY = size.height - ((data.first() - minVal) / valueRange * size.height).toFloat()
        path.moveTo(firstX, firstY.coerceIn(0f, size.height))

        data.forEachIndexed { index, value ->
            if (index > 0) {
                val x = (index.toFloat() / (data.size - 1)) * size.width
                val y = size.height - ((value - minVal) / valueRange * size.height).toFloat()
                path.lineTo(x, y.coerceIn(0f, size.height))
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
