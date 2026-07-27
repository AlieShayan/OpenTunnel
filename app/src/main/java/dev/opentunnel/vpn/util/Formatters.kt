package dev.opentunnel.vpn.util

import java.util.Locale
import kotlin.math.abs

object Formatters {

    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    /** 1_536 -> "1.5 KB". Uses 1024 steps, which is what people expect for traffic. */
    fun bytes(value: Long): String {
        if (value < 1024) return "$value B"
        var amount = value.toDouble()
        var unit = 0
        while (amount >= 1024 && unit < units.lastIndex) {
            amount /= 1024.0
            unit++
        }
        val decimals = if (amount >= 100) 0 else 1
        return String.format(Locale.US, "%.${decimals}f %s", amount, units[unit])
    }

    fun rate(bytesPerSecond: Long): String = "${bytes(abs(bytesPerSecond))}/s"

    /** Elapsed milliseconds -> "1:04:09" or "04:09". */
    fun duration(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** Turns "pin-sha256:AbCd…" into a readable, wrappable fingerprint. */
    fun fingerprint(value: String): String {
        val body = value.substringAfter(':', value)
        return body.chunked(16).joinToString("\n")
    }
}
