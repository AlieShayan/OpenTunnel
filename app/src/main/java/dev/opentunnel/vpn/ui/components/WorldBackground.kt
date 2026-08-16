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
 * WorldBackground renders the persistent, continuous 4-viewport atmospheric foundation for OpenTunnel.
 *
 * Architecture:
 * - The background is modeled as a unified continuous environment spanning across all 4 destinations:
 *   Home (Viewport 0), Traffic Monitor (Viewport 1), Logs (Viewport 2), Settings (Viewport 3).
 * - Spatial Camera Tracking:
 *   - Camera X: Translates across the continuous 4-viewport world coordinate space.
 *   - Camera Y: Implements subtle celestial vertical parallax linked to [homeScrollProvider].
 *
 * Evaluated strictly in the Draw phase ([Modifier.drawBehind]) for 120 FPS performance.
 */
@Composable
fun WorldBackground(
    pagePositionProvider: () -> Float,
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    homeScrollProvider: () -> Float = { 0f },
) {
    val backgroundBase = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val verticalColors = remember(surfaceColor, backgroundBase) {
        listOf(
            surfaceColor.copy(alpha = 0.28f),
            backgroundBase.copy(alpha = 0.12f),
            MidnightBase.copy(alpha = 0.48f),
        )
    }

    val continuousWorldStops = remember(primaryColor, tertiaryColor, containerColor) {
        arrayOf(
            // Viewport 0: Home (Origin / Luminous Atmosphere)
            0.00f to primaryColor.copy(alpha = 0.08f),
            0.15f to Color.Transparent,
            // Viewport 1: Traffic Monitor (Teal / Data Horizon)
            0.35f to tertiaryColor.copy(alpha = 0.07f),
            0.50f to Color.Transparent,
            // Viewport 2: Logs (Monastic Slate Depth)
            0.65f to containerColor.copy(alpha = 0.09f),
            0.78f to Color.Transparent,
            // Viewport 3: Settings (Deep Indigo Architecture)
            0.90f to primaryColor.copy(alpha = 0.06f),
            1.00f to Color.Transparent,
        )
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height
                if (width <= 0f || height <= 0f) return@drawBehind

                val dirMultiplier = if (isRtl) 1f else -1f
                val currentPagePosition = pagePositionProvider()
                val currentHomeScroll = homeScrollProvider()

                // Camera offsets
                val cameraOffsetX = currentPagePosition * width * dirMultiplier
                val parallaxOffsetY = currentHomeScroll * 0.18f

                // Layer 1: Solid Base
                drawRect(color = backgroundBase)

                // Layer 2: Subtle Vertical Spatial Gradient with Parallax
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = verticalColors,
                        startY = -parallaxOffsetY,
                        endY = height + (height * 0.2f) - parallaxOffsetY,
                    )
                )

                // Layer 3: Continuous 4-Viewport World-Space Atmospheric Field
                // The world span is 4 viewports wide; the camera viewport views the respective slice
                val worldWidth = width * 4f
                val worldStartX = if (isRtl) cameraOffsetX - (width * 3f) else cameraOffsetX
                val worldEndX = worldStartX + worldWidth

                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = continuousWorldStops,
                        start = Offset(worldStartX, 0f),
                        end = Offset(worldEndX, height * 0.90f),
                    )
                )

                // Layer 4: Deep Celestial Nebula Landmarks (World-space landmarks for regional distinctiveness)
                // Landmark A: Traffic Flow Nebula (World X = 1.35 * width)
                val trafficLandmarkX = (width * 1.35f * (if (isRtl) -1f else 1f)) + cameraOffsetX
                if (trafficLandmarkX in (-width)..width * 2f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tertiaryColor.copy(alpha = 0.05f),
                                Color.Transparent,
                            ),
                            center = Offset(trafficLandmarkX, height * 0.25f - (parallaxOffsetY * 0.5f)),
                            radius = width * 1.2f,
                        )
                    )
                }

                // Landmark B: Settings Crystalline Aura (World X = 3.20 * width)
                val settingsLandmarkX = (width * 3.20f * (if (isRtl) -1f else 1f)) + cameraOffsetX
                if (settingsLandmarkX in (-width)..width * 2f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.04f),
                                Color.Transparent,
                            ),
                            center = Offset(settingsLandmarkX, height * 0.70f - (parallaxOffsetY * 0.5f)),
                            radius = width * 1.3f,
                        )
                    )
                }
            }
    )
}

