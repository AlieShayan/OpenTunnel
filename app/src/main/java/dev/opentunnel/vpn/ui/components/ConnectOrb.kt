package dev.opentunnel.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import kotlin.math.min

/**
 * The single tap target of the whole app.
 *
 * Idle – calm outline with a faint glow.
 * Busy – sweeping arc plus two expanding rings.
 * Live – closed ring and a halo that breathes slowly.
 */
@Composable
fun ConnectOrb(
    stage: ConnectionStage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    diameter: Dp = 228.dp,
) {
    val palette = LocalStatusPalette.current
    val scheme = MaterialTheme.colorScheme

    val accent by animateColorAsState(
        targetValue = when (stage) {
            ConnectionStage.CONNECTED -> palette.connected
            ConnectionStage.ERROR -> palette.error
            ConnectionStage.IDLE -> palette.idle
            else -> palette.connecting
        },
        animationSpec = tween(450),
        label = "orb-accent",
    )

    val transition = rememberInfiniteTransition(label = "orb")

    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "orb-sweep",
    )
    val waveA by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "orb-wave-a",
    )
    val waveB by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, delayMillis = 1300, easing = FastOutSlowInEasing),
            RepeatMode.Restart,
        ),
        label = "orb-wave-b",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb-breathe",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = tween(160),
        label = "orb-press",
    )
    val glow by animateFloatAsState(
        targetValue = when (stage) {
            ConnectionStage.CONNECTED -> 1f
            ConnectionStage.IDLE -> 0.30f
            ConnectionStage.ERROR -> 0.55f
            else -> 0.70f
        },
        animationSpec = tween(600),
        label = "orb-glow",
    )

    val busy = stage.isBusy
    val live = stage == ConnectionStage.CONNECTED

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f
            val scale = pressScale * if (live) breathe else 1f

            // Ambient halo.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.30f * glow),
                        accent.copy(alpha = 0.10f * glow),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * scale,
                ),
                radius = radius * scale,
                center = center,
            )

            // Expanding rings while the tunnel comes up.
            if (busy) {
                for (progress in listOf(waveA, waveB)) {
                    drawCircle(
                        color = accent.copy(alpha = 0.35f * (1f - progress)),
                        radius = radius * (0.58f + 0.42f * progress),
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            val trackRadius = radius * 0.72f * scale
            drawCircle(
                color = accent.copy(alpha = if (live) 0.34f else 0.18f),
                radius = trackRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )

            if (busy || live) {
                drawArc(
                    color = accent,
                    startAngle = if (live) -90f else sweep,
                    sweepAngle = if (live) 360f else 84f,
                    useCenter = false,
                    topLeft = Offset(center.x - trackRadius, center.y - trackRadius),
                    size = Size(trackRadius * 2f, trackRadius * 2f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            val coreRadius = radius * 0.58f * scale
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(scheme.surfaceContainerHigh, scheme.surfaceContainer),
                    startY = center.y - coreRadius,
                    endY = center.y + coreRadius,
                ),
                radius = coreRadius,
                center = center,
            )
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = when {
                    live -> Icons.Rounded.Shield
                    busy -> Icons.Rounded.Bolt
                    else -> Icons.Rounded.PowerSettingsNew
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(38.dp),
            )
            Text(
                text = when (stage) {
                    ConnectionStage.CONNECTED -> "CONNECTED"
                    ConnectionStage.IDLE -> "CONNECT"
                    ConnectionStage.ERROR -> "RETRY"
                    ConnectionStage.DISCONNECTING -> "STOPPING"
                    ConnectionStage.RECONNECTING -> "RECONNECTING"
                    ConnectionStage.AUTHENTICATING -> "SIGNING IN"
                    else -> "CONNECTING"
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    letterSpacing = 2.2.sp,
                ),
                color = scheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
