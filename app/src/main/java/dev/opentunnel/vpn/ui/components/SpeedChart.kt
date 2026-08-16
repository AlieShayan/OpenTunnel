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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.opentunnel.vpn.core.TrafficStats
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import dev.opentunnel.vpn.ui.theme.MonoNumberStyle
import dev.opentunnel.vpn.util.Formatters
import dev.opentunnel.vpn.util.Strings

enum class ChartRange(val label: String, val maxPoints: Int) {
    M1("1m", 60),
    M10("10m", 600),
    H1("1h", 3600),
    H2("2h", 7200),
    H5("5h", 18000),
}

private const val MAX_STORED_POINTS = 18000

/**
 * High-performance real-time traffic speed graph showing Download (RX) and Upload (TX) rates
 * over time with selectable range (1m, 10m, 1h, 2h, 5h), peak download indicator line,
 * peak-preserving bucket downsampling, and correct RTL/LTR Cartesian alignment.
 */
@Composable
fun SpeedChart(
    stats: TrafficStats,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    rxHistoryList: List<Long> = emptyList(),
    txHistoryList: List<Long> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val rxBuffer = remember { mutableStateListOf<Long>() }
    val txBuffer = remember { mutableStateListOf<Long>() }
    var selectedRange by remember { mutableStateOf(ChartRange.M1) }
    var showRangeDropdown by remember { mutableStateOf(false) }

    val currentRx = if (rxHistoryList.isNotEmpty()) rxHistoryList else rxBuffer
    val currentTx = if (txHistoryList.isNotEmpty()) txHistoryList else txBuffer

    // Fallback buffer update if ViewModel history list is not provided
    LaunchedEffect(stats.sampleTimestamp, stats.rxRate, stats.txRate) {
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

    val peakTxRate by remember(visibleTx) {
        derivedStateOf { visibleTx.maxOrNull() ?: 0L }
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
            val downloadColor = LocalStatusPalette.current.connected
            val uploadColor = MaterialTheme.colorScheme.secondary

            // Header: Title & Legends & Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Strings.speedChartLiveTraffic(appLanguage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LegendItem(
                        color = downloadColor,
                        label = "${Strings.speedChartDlShort(appLanguage)}: ${Formatters.rate(stats.rxRate)}"
                    )
                    LegendItem(
                        color = uploadColor,
                        label = "${Strings.speedChartUlShort(appLanguage)}: ${Formatters.rate(stats.txRate)}"
                    )

                    // Range Selector Chip / Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showRangeDropdown = true }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
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

            Spacer(modifier = Modifier.height(4.dp))

            // Sub-header showing Peak Download rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Strings.speedChartPeakDl(appLanguage, Formatters.rate(peakRxRate)),
                    style = MaterialTheme.typography.bodySmall.merge(MonoNumberStyle),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Canvas
            val chartDesc = "Real-time Traffic Graph. Download rate: ${Formatters.rate(stats.rxRate)}, Upload rate: ${Formatters.rate(stats.txRate)}"
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .semantics { contentDescription = chartDesc }
            ) {
                val width = size.width
                val height = size.height

                val baselineY = height - 6.dp.toPx()
                val topPadding = 12.dp.toPx()
                val plotHeight = (baselineY - topPadding).coerceAtLeast(1f)

                // Grid background lines aligned with baseline and scale divisions
                val gridColor = uploadColor.copy(alpha = 0.08f)
                val midGridY = baselineY - (plotHeight * 0.5f)
                val topGridY = topPadding

                drawLine(gridColor, Offset(0f, topGridY), Offset(width, topGridY), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, midGridY), Offset(width, midGridY), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, baselineY), Offset(width, baselineY), strokeWidth = 1.2.dp.toPx())

                if (visibleRx.isEmpty()) return@Canvas

                val maxVal = peakRxRate
                    .coerceAtLeast(peakTxRate)
                    .coerceAtLeast(1024L)
                    .toFloat()

                val maxPointsToDraw = selectedRange.maxPoints.coerceAtMost(200)

                // Peak-preserving bucket downsampling to prevent missing spikes on 1h/2h/5h ranges
                val sampledRx = mutableListOf<Long>()
                val sampledTx = mutableListOf<Long>()
                val sampledIndices = mutableListOf<Int>()

                if (visibleRx.size <= maxPointsToDraw) {
                    for (i in visibleRx.indices) {
                        sampledRx.add(visibleRx[i])
                        sampledTx.add(visibleTx.getOrElse(i) { 0L })
                        sampledIndices.add(i)
                    }
                } else {
                    val bucketSize = visibleRx.size.toFloat() / maxPointsToDraw
                    for (b in 0 until maxPointsToDraw) {
                        val startIdx = (b * bucketSize).toInt().coerceIn(0, visibleRx.lastIndex)
                        val endIdx = ((b + 1) * bucketSize).toInt().coerceIn(startIdx + 1, visibleRx.size)

                        var maxRx = 0L
                        for (k in startIdx until endIdx) {
                            val v = visibleRx[k]
                            if (v > maxRx) maxRx = v
                        }
                        var maxTx = 0L
                        for (k in startIdx until endIdx) {
                            val v = visibleTx.getOrElse(k) { 0L }
                            if (v > maxTx) maxTx = v
                        }

                        sampledRx.add(maxRx)
                        sampledTx.add(maxTx)
                        sampledIndices.add(startIdx)
                    }
                }

                fun getPoints(data: List<Long>): List<Offset> {
                    val totalRangeSec = (selectedRange.maxPoints - 1).coerceAtLeast(1).toFloat()
                    val totalDataSize = visibleRx.size
                    return data.mapIndexed { i, valBps ->
                        val origIdx = sampledIndices.getOrElse(i) { i }
                        val ageInSec = (totalDataSize - 1 - origIdx).coerceAtLeast(0)
                        val fractionFromLeft = (totalRangeSec - ageInSec) / totalRangeSec
                        val x = (width * fractionFromLeft).coerceIn(0f, width)
                        val y = baselineY - (valBps.toFloat() / maxVal * plotHeight)
                        Offset(x, y.coerceIn(topPadding, baselineY))
                    }
                }

                val rxPoints = getPoints(sampledRx)
                val txPoints = getPoints(sampledTx)

                // Peak Download Horizontal Dashed Line
                if (peakRxRate > 0L) {
                    val yPeak = baselineY - (peakRxRate.toFloat() / maxVal * plotHeight)
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
                if (rxPoints.isNotEmpty()) {
                    val firstPt = rxPoints.first()
                    val lastPt = rxPoints.last()

                    if (rxPoints.size >= 2) {
                        val rxPath = Path().apply {
                            moveTo(firstPt.x, firstPt.y)
                            for (i in 1 until rxPoints.size) {
                                val prev = rxPoints[i - 1]
                                val current = rxPoints[i]
                                val controlX = (prev.x + current.x) / 2f
                                cubicTo(controlX, prev.y, controlX, current.y, current.x, current.y)
                            }
                        }
                        val rxFillPath = Path().apply {
                            addPath(rxPath)
                            lineTo(lastPt.x, baselineY)
                            lineTo(firstPt.x, baselineY)
                            close()
                        }
                        drawPath(
                            path = rxFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(downloadColor.copy(alpha = 0.24f), Color.Transparent),
                                startY = topPadding,
                                endY = baselineY,
                            )
                        )
                        drawPath(
                            path = rxPath,
                            color = downloadColor,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // Endpoint indicator dot for RX
                    drawCircle(
                        color = downloadColor.copy(alpha = 0.3f),
                        radius = 6.dp.toPx(),
                        center = lastPt
                    )
                    drawCircle(
                        color = downloadColor,
                        radius = 3.5.dp.toPx(),
                        center = lastPt
                    )
                }

                // Draw Upload (TX) Path with vertical gradient fill
                if (txPoints.isNotEmpty()) {
                    val firstPt = txPoints.first()
                    val lastPt = txPoints.last()

                    if (txPoints.size >= 2) {
                        val txPath = Path().apply {
                            moveTo(firstPt.x, firstPt.y)
                            for (i in 1 until txPoints.size) {
                                val prev = txPoints[i - 1]
                                val current = txPoints[i]
                                val controlX = (prev.x + current.x) / 2f
                                cubicTo(controlX, prev.y, controlX, current.y, current.x, current.y)
                            }
                        }
                        val txFillPath = Path().apply {
                            addPath(txPath)
                            lineTo(lastPt.x, baselineY)
                            lineTo(firstPt.x, baselineY)
                            close()
                        }
                        drawPath(
                            path = txFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(uploadColor.copy(alpha = 0.16f), Color.Transparent),
                                startY = topPadding,
                                endY = baselineY,
                            )
                        )
                        drawPath(
                            path = txPath,
                            color = uploadColor,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Endpoint indicator dot for TX
                    drawCircle(
                        color = uploadColor.copy(alpha = 0.3f),
                        radius = 5.dp.toPx(),
                        center = lastPt
                    )
                    drawCircle(
                        color = uploadColor,
                        radius = 3.dp.toPx(),
                        center = lastPt
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis Time Indicators:
            // Explicitly wrapped in LTR so that Cartesian X=0 (past) is always on the LEFT
            // and X=width (now) is always on the RIGHT, matching Canvas coordinates in both English and Persian.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "-${selectedRange.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                    Text(
                        text = Strings.speedChartNow(appLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
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
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.merge(MonoNumberStyle),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
