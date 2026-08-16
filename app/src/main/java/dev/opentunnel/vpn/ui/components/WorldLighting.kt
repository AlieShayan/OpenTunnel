package dev.opentunnel.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette

/**
 * WorldLighting renders the global ambient lighting of the OpenTunnel world.
 *
 * The ConnectOrb on the Home viewport acts as the primary luminary anchor in world coordinates.
 * Performance is optimized to 120 FPS by reading [pagePositionProvider] solely inside the Draw
 * phase (via [Modifier.drawBehind]), eliminating all Recomposition passes during scrolling.
 */
@Composable
fun WorldLighting(
    stage: ConnectionStage,
    pagePositionProvider: () -> Float,
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    orbOffsetY: Dp = 190.dp,
) {
    val palette = LocalStatusPalette.current

    // State-aware ambient color with smooth temporal interpolation
    val targetColor = when (stage) {
        ConnectionStage.CONNECTED -> palette.connected
        ConnectionStage.ERROR -> palette.error
        ConnectionStage.IDLE -> palette.idle
        else -> palette.connecting
    }

    val ambientColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "worldAmbientColor",
    )

    // Breathing pulse active only when connecting (saves battery and GPU during idle/connected)
    val infiniteTransition = rememberInfiniteTransition(label = "worldLightingBreathe")
    val busyBreathe by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "busyBreathe",
    )

    val breatheScale = if (stage.isBusy) busyBreathe else 1f

    // Base intensity scaling per state
    val targetIntensity = when (stage) {
        ConnectionStage.CONNECTED -> 0.26f
        ConnectionStage.ERROR -> 0.22f
        ConnectionStage.IDLE -> 0.12f
        else -> 0.28f
    }

    val ambientIntensity by animateFloatAsState(
        targetValue = targetIntensity,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "ambientIntensity",
    )

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height
                if (width <= 0f || height <= 0f) return@drawBehind

                val dirMultiplier = if (isRtl) -1f else 1f
                val orbCenterY = orbOffsetY.toPx()
                val currentPagePosition = pagePositionProvider()

                // World-space light translation relative to camera viewport position
                val orbWorldCenterX = width / 2f
                val cameraOffsetX = -currentPagePosition * width * dirMultiplier
                val visibleOrbCenterX = orbWorldCenterX + cameraOffsetX

                // Primary Luminary Anchor (organic radial glow centered around ConnectOrb)
                val primaryRadius = width * 0.80f * breatheScale
                val pageFalloff = (1f - (currentPagePosition * 0.35f)).coerceIn(0.15f, 1f)
                val effectiveAlpha = (ambientIntensity * pageFalloff).coerceIn(0f, 0.40f)

                if (effectiveAlpha > 0.01f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientColor.copy(alpha = effectiveAlpha),
                                ambientColor.copy(alpha = effectiveAlpha * 0.45f),
                                ambientColor.copy(alpha = effectiveAlpha * 0.10f),
                                Color.Transparent,
                            ),
                            center = Offset(visibleOrbCenterX, orbCenterY),
                            radius = primaryRadius,
                        )
                    )
                }
            }
    )
}
