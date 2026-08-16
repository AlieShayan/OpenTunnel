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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette

/**
 * WorldLighting renders the global luminary and ambient lighting of the OpenTunnel world.
 *
 * The ConnectOrb on the Home viewport acts as the primary luminary anchor in world coordinates.
 * Its real measured layout coordinates are provided via [orbCenterProvider].
 *
 * World Coordinates vs Viewport Coordinates:
 *   visibleLightX = orbWorldX + (pagerPosition * viewportWidth * dirMultiplier)
 *
 * Performance is guaranteed at 120 FPS by evaluating camera offsets and luminary geometry
 * solely inside the Draw phase (via [Modifier.drawBehind]), eliminating all recompositions during swipe gestures.
 */
@Composable
fun WorldLighting(
    stage: ConnectionStage,
    pagePositionProvider: () -> Float,
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    orbCenterProvider: () -> Offset = { Offset.Unspecified },
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

    // Breathing pulse transitions based on connection state
    val infiniteTransition = rememberInfiniteTransition(label = "worldLightingBreathe")
    val busyBreathe by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "busyBreathe",
    )

    val liveBreathe by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveBreathe",
    )

    val breatheScale = when {
        stage.isBusy -> busyBreathe
        stage == ConnectionStage.CONNECTED -> liveBreathe
        else -> 1f
    }

    // Base intensity scaling per state
    val targetIntensity = when (stage) {
        ConnectionStage.CONNECTED -> 0.24f
        ConnectionStage.ERROR -> 0.22f
        ConnectionStage.IDLE -> 0.11f
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

                val dirMultiplier = if (isRtl) 1f else -1f
                val currentPagePosition = pagePositionProvider()

                // Resolve real measured anchor position or safe fallback
                val measuredCenter = orbCenterProvider()
                val orbWorldX = if (measuredCenter.isSpecified && measuredCenter.x > 0f) {
                    measuredCenter.x
                } else {
                    width / 2f
                }
                val orbWorldY = if (measuredCenter.isSpecified && measuredCenter.y > 0f) {
                    measuredCenter.y
                } else {
                    height * 0.30f
                }

                // World-space light translation relative to camera viewport position
                val cameraOffsetX = currentPagePosition * width * dirMultiplier
                val visibleOrbCenterX = orbWorldX + cameraOffsetX
                val visibleOrbCenterY = orbWorldY
                val lightCenter = Offset(visibleOrbCenterX, visibleOrbCenterY)

                val effectiveAlpha = ambientIntensity.coerceIn(0f, 0.45f)
                if (effectiveAlpha <= 0.005f) return@drawBehind

                // Layer A: Extended Ambient Aura (wide spatial field spanning across viewports)
                val ambientRadius = width * 1.60f * breatheScale
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientColor.copy(alpha = effectiveAlpha * 0.35f),
                            ambientColor.copy(alpha = effectiveAlpha * 0.12f),
                            ambientColor.copy(alpha = effectiveAlpha * 0.03f),
                            Color.Transparent,
                        ),
                        center = lightCenter,
                        radius = ambientRadius,
                    )
                )

                // Layer B: Primary Luminary Core (focused radiance centered on ConnectOrb)
                val luminaryRadius = width * 0.85f * breatheScale
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientColor.copy(alpha = effectiveAlpha),
                            ambientColor.copy(alpha = effectiveAlpha * 0.50f),
                            ambientColor.copy(alpha = effectiveAlpha * 0.15f),
                            Color.Transparent,
                        ),
                        center = lightCenter,
                        radius = luminaryRadius,
                    )
                )
            }
    )
}

