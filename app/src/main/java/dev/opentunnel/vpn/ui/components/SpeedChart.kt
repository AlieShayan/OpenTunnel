package dev.opentunnel.vpn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.opentunnel.vpn.core.TrafficStats
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.util.Formatters

enum class ChartRange(val label: String, val maxPoints: Int) {
    M1("1m", 60),
    M10("10m", 600),
    H1("1h", 3600),
    H2("2h", 7200),
    H5("5h", 18000),
}

private const val MAX_STORED_POINTS = 18000

/**
 * Real-time traffic speed graph showing Download (RX) and Upload (TX) rates
 * over time with selectable range (1m, 10m, 1h, 2h, 5h), peak download indicator line,
 * and increased height for optimal visibility.
 */
@Composable
fun SpeedChart(
    stats: TrafficStats,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    rxHistoryList: List<Long> = emptyList(),
    txHistoryList: List<Long> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val isPersian = appLanguage == AppLanguage.PERSIAN
    val rxBuffer = remember { mutableStateListOf<Long>() }
    val txBuffer = remember { mutableStateListOf<Long>() }
    var selectedRange by remember { mutableStateOf(ChartRange.M1) }
    var showRangeDropdown by remember { mutableStateOf(false) }

    val currentRx = if (rxHistoryList.isNotEmpty()) rxHistoryList else rxBuffer
    val currentTx = if (txHistoryList.isNotEmpty()) txHistoryList else txBuffer

    LaunchedEffect(stats.rxRate, stats.txRate) {
        if (rxHistoryList.isEmpty()) {
            rxBuffer.add(stats.rxRate)
            txBuffer.add(stats.txRate)
            if (rxBuffer.size > MAX_STORED_POINTS) rxBuffer.removeAt(0)
            if (txBuffer.size > MAX_STORED_POINTS) txBuffer.removeAt(0)
        }
    }

    val visibleRx by remember(currentRx, selectedRange) {
        derivedStateOf {
            if (currentRx.size <= selectedRange.maxPoints) currentRx else currentRx.takeLast(selectedRange.maxPoints)
        }
    }
    val visibleTx by remember(currentTx, selectedRange) {
        derivedStateOf {
            if (currentTx.size <= selectedRange.maxPoints) currentTx else currentTx.takeLast(selectedRange.maxPoints)
        }
    }
    val peakRxRate by remember(visibleRx) {
        derivedStateOf { visibleRx.maxOrNull() ?: 0L }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
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
                    text = if (isPersian) "ترافیک زنده" else "Live Traffic",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val downloadColor = dev.opentunnel.vpn.ui.theme.LocalStatusPalette.current.connected
                val uploadColor = MaterialTheme.colorScheme.secondary

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendItem(color = downloadColor, label = "DL: ${Formatters.rate(stats.rxRate)}")
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendItem(color = uploadColor, label = "UL: ${Formatters.rate(stats.txRate)}")
                    Spacer(modifier = Modifier.width(10.dp))

                    // Range Selector Chip / Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showRangeDropdown = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedRange.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Select Time Range",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showRangeDropdown,
                            onDismissRequest = { showRangeDropdown = false }
                        ) {
                            ChartRange.entries.forEach { range ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = range.label,
                                            fontWeight = if (range == selectedRange) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        selectedRange = range
                                        showRangeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-header showing Peak Download rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isPersian) "بیشینه دانلود: ${Formatters.rate(peakRxRate)}" else "Peak DL: ${Formatters.rate(peakRxRate)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val downloadColor = dev.opentunnel.vpn.ui.theme.LocalStatusPalette.current.connected
            val uploadColor = MaterialTheme.colorScheme.secondary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (visibleRx.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val maxVal = (visibleRx.maxOrNull() ?: 1L)
                    .coerceAtLeast(visibleTx.maxOrNull() ?: 1L)
                    .coerceAtLeast(1024L)
                    .toFloat()

                val maxPointsToDraw = selectedRange.maxPoints.coerceAtMost(200)
                val step = (visibleRx.size.toFloat() / maxPointsToDraw).coerceAtLeast(1f)

                val sampledRx = mutableListOf<Long>()
                val sampledTx = mutableListOf<Long>()
                val sampledIndices = mutableListOf<Int>()
                var idx = 0f
                while (idx < visibleRx.size) {
                    val i = idx.toInt().coerceIn(0, visibleRx.lastIndex)
                    sampledRx.add(visibleRx[i])
                    sampledTx.add(visibleTx[i])
                    sampledIndices.add(i)
                    idx += step
                }

                fun getPoints(data: List<Long>): List<Offset> {
                    val totalRangeSec = (selectedRange.maxPoints - 1).coerceAtLeast(1).toFloat()
                    val totalDataSize = visibleRx.size
                    return data.mapIndexed { i, valBps ->
                        val origIdx = sampledIndices.getOrElse(i) { i }
                        val ageInSec = (totalDataSize - 1 - origIdx).coerceAtLeast(0)
                        val fractionFromLeft = (totalRangeSec - ageInSec) / totalRangeSec
                        val x = (width * fractionFromLeft).coerceIn(0f, width)
                        val y = height - (valBps.toFloat() / maxVal * (height - 18.dp.toPx())) - 9.dp.toPx()
                        Offset(x, y.coerceIn(0f, height))
                    }
                }

                val rxPoints = getPoints(sampledRx)
                val txPoints = getPoints(sampledTx)

                // Grid background lines
                val gridColor = uploadColor.copy(alpha = 0.08f)
                drawLine(gridColor, Offset(0f, height / 3), Offset(width, height / 3), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, height * 2 / 3), Offset(width, height * 2 / 3), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1.dp.toPx())

                // Peak Download Horizontal Dashed Line (Subtle Alpha)
                if (peakRxRate > 0L) {
                    val yPeak = height - (peakRxRate.toFloat() / maxVal * (height - 18.dp.toPx())) - 9.dp.toPx()
                    val dashedPath = Path().apply {
                        moveTo(0f, yPeak)
                        lineTo(width, yPeak)
                    }
                    drawPath(
                        path = dashedPath,
                        color = downloadColor.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )
                }

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
                            colors = listOf(downloadColor.copy(alpha = 0.24f), Color.Transparent),
                            startY = 0f,
                            endY = height,
                        )
                    )
                    drawPath(
                        path = rxPath,
                        color = downloadColor,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Endpoint indicator dot
                    val lastRx = rxPoints.last()
                    drawCircle(
                        color = downloadColor.copy(alpha = 0.3f),
                        radius = 6.dp.toPx(),
                        center = lastRx
                    )
                    drawCircle(
                        color = downloadColor,
                        radius = 3.5.dp.toPx(),
                        center = lastRx
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
                            colors = listOf(uploadColor.copy(alpha = 0.16f), Color.Transparent),
                            startY = 0f,
                            endY = height,
                        )
                    )
                    drawPath(
                        path = txPath,
                        color = uploadColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Endpoint indicator dot
                    val lastTx = txPoints.last()
                    drawCircle(
                        color = uploadColor.copy(alpha = 0.3f),
                        radius = 5.dp.toPx(),
                        center = lastTx
                    )
                    drawCircle(
                        color = uploadColor,
                        radius = 3.dp.toPx(),
                        center = lastTx
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis Time Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "-${selectedRange.label}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
                Text(
                    text = if (isPersian) "اکنون" else "now",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
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
