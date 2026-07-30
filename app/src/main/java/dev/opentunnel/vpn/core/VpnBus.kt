package dev.opentunnel.vpn.core

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for tunnel state, shared between the VpnService (which
 * writes) and the UI (which reads). Deliberately a process-wide singleton: the
 * service and the activity live in the same process, and this saves the UI from
 * having to bind to the service just to render a status dot.
 */
object VpnBus {

    private const val TAG = "OpenTunnel"
    private const val MAX_LOG_LINES = 800

    private val _status = MutableStateFlow(TunnelStatus())
    val status: StateFlow<TunnelStatus> = _status.asStateFlow()

    private val _stats = MutableStateFlow(TrafficStats())
    val stats: StateFlow<TrafficStats> = _stats.asStateFlow()

    private val logDeque = ArrayDeque<LogLine>(MAX_LOG_LINES)
    private val _logs = MutableStateFlow<List<LogLine>>(emptyList())
    val logs: StateFlow<List<LogLine>> = _logs.asStateFlow()

    // ── status ──────────────────────────────────────────────────────────────

    fun setStage(stage: ConnectionStage, detail: String? = null) {
        _status.update { current ->
            val clearLocation = stage == ConnectionStage.PREPARING ||
                stage == ConnectionStage.CONNECTING ||
                stage == ConnectionStage.RECONNECTING
            current.copy(
                stage = stage,
                detail = detail ?: defaultDetail(stage),
                error = if (stage == ConnectionStage.ERROR) current.error else null,
                info = if (clearLocation) {
                    current.info.copy(
                        locationName = null,
                        locationFlag = null,
                        outboundIp = null,
                        pingMs = -1L,
                    )
                } else current.info,
            )
        }
    }

    fun setConnected(connectedAtElapsed: Long, info: TunnelInfo) {
        _status.update {
            it.copy(
                stage = ConnectionStage.CONNECTED,
                detail = null,
                error = null,
                connectedAtElapsed = connectedAtElapsed,
                info = info,
            )
        }
    }

    fun updateInfo(transform: (TunnelInfo) -> TunnelInfo) {
        _status.update { it.copy(info = transform(it.info)) }
    }

    fun setError(message: String) {
        _status.update {
            it.copy(
                stage = ConnectionStage.ERROR,
                detail = null,
                error = message,
                connectedAtElapsed = 0L,
            )
        }
    }

    fun reset() {
        _status.value = TunnelStatus()
        _stats.value = TrafficStats()
    }

    private fun defaultDetail(stage: ConnectionStage): String? = when (stage) {
        ConnectionStage.PREPARING -> "Preparing tunnel"
        ConnectionStage.AUTHENTICATING -> "Authenticating"
        ConnectionStage.CONNECTING -> "Negotiating connection"
        ConnectionStage.RECONNECTING -> "Reconnecting"
        ConnectionStage.DISCONNECTING -> "Disconnecting"
        else -> null
    }

    // ── traffic ─────────────────────────────────────────────────────────────

    private var lastSampleAt = 0L

    fun updateStats(rxBytes: Long, txBytes: Long, rxPackets: Long, txPackets: Long, nowElapsed: Long) {
        _stats.update { prev ->
            val dtMs = if (lastSampleAt == 0L) 0L else (nowElapsed - lastSampleAt)
            val rxRate = if (dtMs > 250) (rxBytes - prev.rxBytes) * 1000 / dtMs else prev.rxRate
            val txRate = if (dtMs > 250) (txBytes - prev.txBytes) * 1000 / dtMs else prev.txRate
            lastSampleAt = nowElapsed
            TrafficStats(
                rxBytes = rxBytes,
                txBytes = txBytes,
                rxPackets = rxPackets,
                txPackets = txPackets,
                rxRate = rxRate.coerceAtLeast(0),
                txRate = txRate.coerceAtLeast(0),
            )
        }
    }

    fun resetStatsWindow() {
        lastSampleAt = 0L
    }

    // ── logs ────────────────────────────────────────────────────────────────

    fun log(level: LogLevel, message: String) {
        if (message.isBlank()) return
        val line = LogLine(System.currentTimeMillis(), level, message.trimEnd())
        synchronized(logDeque) {
            if (logDeque.size >= MAX_LOG_LINES) {
                logDeque.removeFirst()
            }
            logDeque.addLast(line)
            _logs.value = logDeque.toList()
        }
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, message)
            LogLevel.DEBUG, LogLevel.TRACE -> Log.d(TAG, message)
            else -> Log.i(TAG, message)
        }
    }

    fun info(message: String) = log(LogLevel.APP, message)
    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun error(message: String) = log(LogLevel.ERROR, message)

    fun clearLogs() {
        synchronized(logDeque) {
            logDeque.clear()
            _logs.value = emptyList()
        }
    }
}
