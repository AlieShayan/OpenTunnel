package dev.opentunnel.vpn.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.opentunnel.vpn.core.TrafficStats
import dev.opentunnel.vpn.util.Formatters

private const val MAX_HISTORY_POINTS = 30

/**
 * Real-time traffic speed graph showing Download (RX) and Upload (TX) rates
 * over time using a smooth Canvas line chart.
 */
@Composable
fun SpeedChart(
    stats: TrafficStats,
    modifier: Modifier = Modifier,
) {
    val rxHistory = remember { mutableStateListOf<Long>() }
    val txHistory = remember { mutableStateListOf<Long>() }

    LaunchedEffect(stats.rxRate, stats.txRate) {
        rxHistory.add(stats.rxRate)
        txHistory.add(stats.txRate)
        if (rxHistory.size > MAX_HISTORY_POINTS) rxHistory.removeAt(0)
        if (txHistory.size > MAX_HISTORY_POINTS) txHistory.removeAt(0)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Live Traffic",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendItem(color = Color(0xFF4CAF50), label = "DL: ${Formatters.rate(stats.rxRate)}")
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendItem(color = Color(0xFF2196F3), label = "UL: ${Formatters.rate(stats.txRate)}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val downloadColor = dev.opentunnel.vpn.ui.theme.LocalStatusPalette.current.connected
            val uploadColor = MaterialTheme.colorScheme.secondary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                if (rxHistory.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val maxVal = (rxHistory.maxOrNull() ?: 1L).coerceAtLeast(txHistory.maxOrNull() ?: 1L).coerceAtLeast(1024L).toFloat()

                fun getPoints(data: List<Long>): List<Offset> {
                    val stepX = width / (MAX_HISTORY_POINTS - 1).coerceAtLeast(1)
                    val startOffset = MAX_HISTORY_POINTS - data.size
                    return data.mapIndexed { idx, valBps ->
                        val x = (startOffset + idx) * stepX
                        val y = height - (valBps.toFloat() / maxVal * (height - 10.dp.toPx())) - 5.dp.toPx()
                        Offset(x, y.coerceIn(0f, height))
                    }
                }

                val rxPoints = getPoints(rxHistory)
                val txPoints = getPoints(txHistory)

                // Grid background lines
                val gridColor = uploadColor.copy(alpha = 0.10f)
                drawLine(gridColor, Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1.dp.toPx())

                // Draw Download (RX) Path with vertical gradient fill
                if (rxPoints.size >= 2) {
                    val rxPath = Path().apply {
                        moveTo(rxPoints.first().x, rxPoints.first().y)
                        for (i in 1 until rxPoints.size) {
                            val prev = rxPoints[i - 1]
                            val current = rxPoints[i]
                            val controlX = (prev.x + current.x) / 2f
                            cubicTo(controlX, prev.y, controlX, current.y, current.x, current.y)
                        }
                    }
                    val rxFillPath = Path().apply {
                        addPath(rxPath)
                        lineTo(rxPoints.last().x, height)
                        lineTo(rxPoints.first().x, height)
                        close()
                    }
                    drawPath(
                        path = rxFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(downloadColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = height,
                        )
                    )
                    drawPath(
                        path = rxPath,
                        color = downloadColor,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Draw Upload (TX) Path with vertical gradient fill
                if (txPoints.size >= 2) {
                    val txPath = Path().apply {
                        moveTo(txPoints.first().x, txPoints.first().y)
                        for (i in 1 until txPoints.size) {
                            val prev = txPoints[i - 1]
                            val current = txPoints[i]
                            val controlX = (prev.x + current.x) / 2f
                            cubicTo(controlX, prev.y, controlX, current.y, current.x, current.y)
                        }
                    }
                    val txFillPath = Path().apply {
                        addPath(txPath)
                        lineTo(txPoints.last().x, height)
                        lineTo(txPoints.first().x, height)
                        close()
                    }
                    drawPath(
                        path = txFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(uploadColor.copy(alpha = 0.15f), Color.Transparent),
                            startY = 0f,
                            endY = height,
                        )
                    )
                    drawPath(
                        path = txPath,
                        color = uploadColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
