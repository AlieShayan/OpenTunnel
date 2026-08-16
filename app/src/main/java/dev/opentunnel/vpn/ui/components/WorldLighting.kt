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
 * WorldLighting renders the continuous 3-layer global luminary and ambient lighting field of OpenTunnel.
 *
 * Architecture:
 * 1. World Space Coordinate System:
 *    The ConnectOrb on the Home viewport acts as the primary physical luminary anchor in world coordinates.
 *    Its invariant un-scrolled layout coordinates are provided via [orbAnchorProvider].
 *
 * 2. 2D Camera Transformations:
 *    - Camera X: Driven by [pagePositionProvider] (Pager horizontal movement across the 4 viewports)
 *    - Camera Y: Driven by [homeScrollProvider] (Home vertical scroll)
 *    - Viewport Luminary Center:
 *        visibleLightX = orbWorldX + (pagerPosition * viewportWidth * dirMultiplier)
 *        visibleLightY = orbWorldY - homeScrollOffset
 *
 * 3. 3-Layer Spatial Light Field:
 *    - Layer 1 (Local Aura): Focused, rich radiance centered on the ConnectOrb ($R \approx 0.85 \times W$)
 *    - Layer 2 (Regional Field): Expansive ambient glow spanning adjacent viewports ($R \approx 2.0 \times W$)
 *    - Layer 3 (Global Field): Ultra-soft celestial illumination reaching all 4 viewports ($R \approx 4.4 \times W$)
 *
 * Performance is guaranteed at 120 FPS by evaluating camera offsets and luminary geometry
 * solely inside the Draw phase (via [Modifier.drawBehind]), eliminating all recompositions during gestures.
 */
@Composable
fun WorldLighting(
    stage: ConnectionStage,
    pagePositionProvider: () -> Float,
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    homeScrollProvider: () -> Float = { 0f },
    orbAnchorProvider: () -> Offset = { Offset.Unspecified },
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

    // Decoupled slow celestial breathing transitions for the environment
    val infiniteTransition = rememberInfiniteTransition(label = "worldLightingBreathe")
    val busyBreathe by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "busyBreathe",
    )

    val liveBreathe by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
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
        ConnectionStage.CONNECTED -> 0.28f
        ConnectionStage.ERROR -> 0.22f
        ConnectionStage.IDLE -> 0.10f
        else -> 0.24f
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
                val currentHomeScroll = homeScrollProvider()

                // Resolve real measured anchor position (in un-scrolled world coordinates) or safe fallback
                val measuredAnchor = orbAnchorProvider()
                val orbWorldX = if (measuredAnchor.isSpecified && measuredAnchor.x > 0f) {
                    measuredAnchor.x
                } else {
                    width / 2f
                }
                val orbWorldY = if (measuredAnchor.isSpecified && measuredAnchor.y > 0f) {
                    measuredAnchor.y
                } else {
                    height * 0.28f
                }

                // 2D Camera Transformations: Pager X + Home Scroll Y applied simultaneously
                val cameraOffsetX = currentPagePosition * width * dirMultiplier
                val cameraOffsetY = currentHomeScroll
                val visibleOrbCenterX = orbWorldX + cameraOffsetX
                val visibleOrbCenterY = orbWorldY - cameraOffsetY
                val lightCenter = Offset(visibleOrbCenterX, visibleOrbCenterY)

                val effectiveAlpha = ambientIntensity.coerceIn(0f, 0.45f)
                if (effectiveAlpha <= 0.005f) return@drawBehind

                // ── Layer 3: Global World Illumination Field ────────────────────────
                // Vast, ultra-soft celestial field reaching all 4 viewports (Settings ~15-30% perception)
                val globalRadius = width * 4.4f * breatheScale
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to ambientColor.copy(alpha = effectiveAlpha * 0.40f),
                            0.20f to ambientColor.copy(alpha = effectiveAlpha * 0.28f),
                            0.45f to ambientColor.copy(alpha = effectiveAlpha * 0.16f),
                            0.70f to ambientColor.copy(alpha = effectiveAlpha * 0.07f),
                            0.90f to ambientColor.copy(alpha = effectiveAlpha * 0.02f),
                            1.00f to Color.Transparent,
                        ),
                        center = lightCenter,
                        radius = globalRadius,
                    )
                )

                // ── Layer 2: Regional Light Field ───────────────────────────────────
                // Spans multiple viewports (Traffic ~50-70% perception, Logs edge ~30-50%)
                val regionalRadius = width * 2.0f * breatheScale
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to ambientColor.copy(alpha = effectiveAlpha * 0.55f),
                            0.30f to ambientColor.copy(alpha = effectiveAlpha * 0.32f),
                            0.60f to ambientColor.copy(alpha = effectiveAlpha * 0.12f),
                            0.85f to ambientColor.copy(alpha = effectiveAlpha * 0.03f),
                            1.00f to Color.Transparent,
                        ),
                        center = lightCenter,
                        radius = regionalRadius,
                    )
                )

                // ── Layer 1: Local Orb Aura Core ───────────────────────────────────
                // Focused, high-radiance core centered on ConnectOrb on the Home viewport
                val localRadius = width * 0.85f * breatheScale
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to ambientColor.copy(alpha = effectiveAlpha * 0.85f),
                            0.35f to ambientColor.copy(alpha = effectiveAlpha * 0.45f),
                            0.70f to ambientColor.copy(alpha = effectiveAlpha * 0.15f),
                            1.00f to Color.Transparent,
                        ),
                        center = lightCenter,
                        radius = localRadius,
                    )
                )
            }
    )
}

