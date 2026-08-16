package dev.opentunnel.vpn.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.opentunnel.vpn.ui.theme.MidnightBase

/**
 * WorldBackground renders the persistent multi-layer foundation for the entire application.
 *
 * Reads [pagePositionProvider] only in the Draw phase to guarantee smooth 120 FPS performance.
 */
@Composable
fun WorldBackground(
    pagePositionProvider: () -> Float,
    isRtl: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundBase = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    // Static color list remembered across frames
    val verticalColors = remember(surfaceColor, backgroundBase) {
        listOf(
            surfaceColor.copy(alpha = 0.35f),
            backgroundBase.copy(alpha = 0.20f),
            MidnightBase.copy(alpha = 0.50f),
        )
    }

    val parallaxColors = remember(containerColor, surfaceColor) {
        listOf(
            containerColor.copy(alpha = 0.10f),
            Color.Transparent,
            surfaceColor.copy(alpha = 0.08f),
        )
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height
                if (width <= 0f || height <= 0f) return@drawBehind

                val dirMultiplier = if (isRtl) -1f else 1f

                // Layer 1: Solid Base
                drawRect(color = backgroundBase)

                // Layer 2: Subtle Vertical Spatial Gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = verticalColors,
                        startY = 0f,
                        endY = height,
                    )
                )

                // Layer 3: Soft Parallax Spatial Gradient
                val currentPagePosition = pagePositionProvider()
                val parallaxOffsetX = -currentPagePosition * width * 0.04f * dirMultiplier
                drawRect(
                    brush = Brush.linearGradient(
                        colors = parallaxColors,
                        start = Offset(parallaxOffsetX, 0f),
                        end = Offset(width + parallaxOffsetX, height * 0.8f),
                    )
                )
            }
    )
}
