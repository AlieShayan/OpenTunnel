package dev.opentunnel.vpn.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

object HapticHelper {

    fun performTick(context: Context, enabled: Boolean) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12)
                }
            }
        } catch (_: Exception) {}
    }

    fun performClick(context: Context, enabled: Boolean) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            }
        } catch (_: Exception) {}
    }

    fun performConnect(context: Context, enabled: Boolean) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 35, 50, 50),
                            intArrayOf(0, 180, 0, 255),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(90)
                }
            }
        } catch (_: Exception) {}
    }

    fun performDisconnect(context: Context, enabled: Boolean) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 60, 40, 40),
                            intArrayOf(0, 220, 0, 120),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(110)
                }
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

@Composable
fun RememberLazyListHaptic(listState: LazyListState, enabled: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(listState, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { _ ->
                if (listState.isScrollInProgress) {
                    HapticHelper.performTick(context, enabled)
                }
            }
    }
}

@Composable
fun RememberScrollHaptic(scrollState: ScrollState, enabled: Boolean, stepDp: Int = 72) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val stepPx = with(density) { stepDp.dp.toPx() }
    LaunchedEffect(scrollState, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow { if (stepPx > 0) (scrollState.value / stepPx).toInt() else 0 }
            .distinctUntilChanged()
            .collect { _ ->
                if (scrollState.isScrollInProgress) {
                    HapticHelper.performTick(context, enabled)
                }
            }
    }
}
