package dev.opentunnel.vpn.data

import androidx.compose.runtime.Immutable

/**
 * Domain model representing live and cumulative network statistics for a single application.
 */
@Immutable
data class AppTrafficEntry(
    val packageName: String,
    val uid: Int,
    val label: String,
    val isSystem: Boolean,
    val isVpnRouted: Boolean,
    val rxBytes: Long = 0L,              // Cumulative session download (bytes)
    val txBytes: Long = 0L,              // Cumulative session upload (bytes)
    val totalBytes: Long = 0L,           // rxBytes + txBytes
    val rxRate: Long = 0L,               // Smoothed download speed (bytes/sec)
    val txRate: Long = 0L,               // Smoothed upload speed (bytes/sec)
    val totalRate: Long = 0L,            // rxRate + txRate (bytes/sec)
    val trafficSharePercent: Float = 0f, // % of total traffic (0.0f - 100.0f)
    val firstSeenTime: Long = 0L,
    val lastActiveTime: Long = 0L,
    val isActive: Boolean = false,       // rxRate > 0 || txRate > 0
)

/**
 * Overall summary across all monitored applications.
 */
@Immutable
data class AppTrafficSummary(
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val currentRxRate: Long = 0L,
    val currentTxRate: Long = 0L,
    val activeAppsCount: Int = 0,
    val totalAppsCount: Int = 0,
    val sessionStartTime: Long = 0L,
)

/**
 * Available sorting dimensions for application traffic.
 */
enum class TrafficSortBy {
    TOTAL_TRAFFIC,
    DOWNLOAD,
    UPLOAD,
    DOWNLOAD_SPEED,
    UPLOAD_SPEED,
    APP_NAME,
}

/**
 * Sort direction.
 */
enum class SortDirection {
    DESCENDING,
    ASCENDING,
}

enum class TrafficFilterMode {
    ALL,
    ACTIVE_ONLY,
}
