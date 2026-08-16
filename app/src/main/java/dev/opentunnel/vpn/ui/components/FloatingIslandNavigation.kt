package dev.opentunnel.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import dev.opentunnel.vpn.util.Strings
import kotlin.math.roundToInt

/**
 * Centered, symmetrical, frosted-glass Floating Island Navigation Bar.
 *
 * Supports 4 primary destinations with connection-state aware ambient illumination,
 * continuous sliding pill indicator, and precise RTL geometry support.
 * Optimized for 120 FPS using lambda-based offset to avoid recomposition during scroll.
 */
@Composable
fun FloatingIslandNavigation(
    currentPage: Int,
    pagePositionProvider: () -> Float,
    stage: ConnectionStage,
    lang: AppLanguage,
    onNavigateToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val isRtl = Strings.isRtl(lang)
    val palette = LocalStatusPalette.current
    val density = LocalDensity.current

    // Dynamic connection-aware indicator glow
    val indicatorAccentColor by animateColorAsState(
        targetValue = when (stage) {
            ConnectionStage.CONNECTED -> palette.connected
            ConnectionStage.ERROR -> palette.error
            ConnectionStage.IDLE -> MaterialTheme.colorScheme.primary
            else -> palette.connecting
        },
        animationSpec = tween(450),
        label = "navIndicatorAccent",
    )

    val itemWidthDp = 60.dp
    val itemSpacingDp = 4.dp
    val indicatorSizeDp = 48.dp

    val itemWidthPx = with(density) { itemWidthDp.toPx() }
    val itemSpacingPx = with(density) { itemSpacingDp.toPx() }
    val indicatorSizePx = with(density) { indicatorSizeDp.toPx() }
    val totalSpanPx = itemWidthPx + itemSpacingPx
    val centeringOffsetPx = (itemWidthPx - indicatorSizePx) / 2f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Active sliding pill indicator (lambda offset executes in Layout/Draw phase - 120 FPS)
            Box(
                modifier = Modifier
                    .offset {
                        val pos = pagePositionProvider()
                        val effectivePos = if (isRtl) (3f - pos).coerceIn(0f, 3f) else pos.coerceIn(0f, 3f)
                        val targetX = totalSpanPx * effectivePos + centeringOffsetPx
                        IntOffset(x = targetX.roundToInt(), y = 0)
                    }
                    .size(indicatorSizeDp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                indicatorAccentColor.copy(alpha = if (stage == ConnectionStage.CONNECTED) 0.22f else 0.16f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f),
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                indicatorAccentColor.copy(alpha = if (stage == ConnectionStage.CONNECTED) 0.55f else 0.40f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            )
                        ),
                        CircleShape
                    )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacingDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val items = listOf(
                    Triple(Icons.Rounded.Home, Strings.navHome(lang), 0),
                    Triple(Icons.Rounded.DataUsage, Strings.navTraffic(lang), 1),
                    Triple(Icons.AutoMirrored.Rounded.Article, Strings.navLogs(lang), 2),
                    Triple(Icons.Rounded.Settings, Strings.navSettings(lang), 3),
                )

                items.forEach { (icon, label, index) ->
                    val isSelected = currentPage == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "navIconScale_$index",
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        },
                        animationSpec = tween(250),
                        label = "navColor_$index",
                    )

                    Column(
                        modifier = Modifier
                            .size(width = itemWidthDp, height = 48.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (currentPage != index) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNavigateToPage(index)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(scale),
                        )
                    }
                }
            }
        }
    }
}
