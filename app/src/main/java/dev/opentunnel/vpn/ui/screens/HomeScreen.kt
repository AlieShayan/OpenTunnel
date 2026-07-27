package dev.opentunnel.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.core.TrafficStats
import dev.opentunnel.vpn.core.TunnelStatus
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.data.SplitTunnelMode
import dev.opentunnel.vpn.data.VpnProfile
import dev.opentunnel.vpn.ui.components.ConnectOrb
import dev.opentunnel.vpn.ui.components.DetailRow
import dev.opentunnel.vpn.ui.components.SectionCard
import dev.opentunnel.vpn.ui.components.SettingRow
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import dev.opentunnel.vpn.ui.theme.MonoNumberStyle
import dev.opentunnel.vpn.util.Formatters
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    status: TunnelStatus,
    stats: TrafficStats,
    profile: VpnProfile,
    profiles: List<VpnProfile>,
    settings: AppSettings,
    onToggleConnection: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProfileManagement: () -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalStatusPalette.current
    val scheme = MaterialTheme.colorScheme

    val ambient by animateColorAsState(
        targetValue = when (status.stage) {
            ConnectionStage.CONNECTED -> palette.connected.copy(alpha = 0.16f)
            ConnectionStage.ERROR -> palette.error.copy(alpha = 0.14f)
            ConnectionStage.IDLE -> palette.idle.copy(alpha = 0.07f)
            else -> palette.connecting.copy(alpha = 0.13f)
        },
        animationSpec = tween(700),
        label = "ambient",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to ambient,
                    0.55f to Color.Transparent,
                    1f to Color.Transparent,
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            HomeTopBar(onOpenSettings = onOpenSettings, onOpenLogs = onOpenLogs)

            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ConnectOrb(
                    stage = status.stage,
                    // Always enabled — user can tap to cancel even while connecting
                    enabled = true,
                    onClick = onToggleConnection,
                )
            }

            Spacer(Modifier.height(18.dp))

            StatusLine(status = status, language = settings.appLanguage)

            // Location badge — shown when connected and location is resolved
            AnimatedVisibility(
                visible = status.stage == ConnectionStage.CONNECTED &&
                    status.info.locationName != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    LocationBadge(
                        flag = status.info.locationFlag.orEmpty(),
                        name = status.info.locationName.orEmpty(),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            AnimatedVisibility(
                visible = status.stage == ConnectionStage.CONNECTED ||
                    status.stage == ConnectionStage.RECONNECTING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    TrafficRow(stats, settings.appLanguage)
                    Spacer(Modifier.height(18.dp))
                }
            }

            AnimatedVisibility(
                visible = status.stage == ConnectionStage.ERROR && status.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    ErrorBanner(status.error.orEmpty())
                    Spacer(Modifier.height(18.dp))
                }
            }

            SectionCard {
                ProfilePickerRow(
                    profile = profile,
                    profiles = profiles,
                    onSelectProfile = onSelectProfile,
                    onOpenProfile = onOpenProfile,
                    onOpenProfileManagement = onOpenProfileManagement,
                )
                SettingRow(
                    icon = Icons.Rounded.Apps,
                    title = dev.opentunnel.vpn.util.Strings.splitTunnelTitle(settings.appLanguage),
                    subtitle = splitTunnelSummary(settings),
                    iconTint = scheme.tertiary,
                    iconBackground = scheme.tertiary.copy(alpha = 0.14f),
                    onClick = onOpenSplitTunnel,
                )
            }

            AnimatedVisibility(
                visible = status.stage == ConnectionStage.CONNECTED,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    ConnectionDetails(status, settings.appLanguage)
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionCard {
                SettingRow(
                    icon = Icons.Rounded.Article,
                    title = dev.opentunnel.vpn.util.Strings.logsTitle(settings.appLanguage),
                    subtitle = "Everything openconnect reports, live",
                    iconTint = scheme.secondary,
                    iconBackground = scheme.secondary.copy(alpha = 0.14f),
                    onClick = onOpenLogs,
                )
                SettingRow(
                    icon = Icons.Rounded.Tune,
                    title = dev.opentunnel.vpn.util.Strings.settingsTitle(settings.appLanguage),
                    subtitle = "Appearance, reconnection, diagnostics",
                    iconTint = scheme.onSurfaceVariant,
                    iconBackground = scheme.onSurfaceVariant.copy(alpha = 0.12f),
                    onClick = onOpenSettings,
                )
            }

            Spacer(Modifier.height(20.dp))

            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            uriHandler.openUri("https://github.com/AlieShayan/OpenTunnel")
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Github@AlieShayan",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Profile Picker ─────────────────────────────────────────────────────────

/**
 * Shows the active profile name with a dropdown arrow. Tapping opens a menu
 * listing all saved profiles. A secondary tap (on the title text) opens the
 * profile editor for the currently active profile.
 */
@Composable
private fun ProfilePickerRow(
    profile: VpnProfile,
    profiles: List<VpnProfile>,
    onSelectProfile: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProfileManagement: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        SettingRow(
            icon = Icons.Rounded.Public,
            title = profile.displayName,
            subtitle = when {
                !profile.isComplete -> "Tap to add your server, username and password"
                profile.username.isNotBlank() -> "${profile.username} · ${profile.protocol}"
                else -> profile.protocol
            },
            trailing = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = "Switch profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onClick = onOpenProfile,
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            profiles.forEach { p ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = p.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (p.id == profile.id) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (p.server.isNotBlank()) {
                                Text(
                                    text = p.server,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectProfile(p.id)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = "⚙ Manage profiles…",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                onClick = {
                    expanded = false
                    onOpenProfileManagement()
                },
            )
        }
    }
}

// ── Location Badge ─────────────────────────────────────────────────────────

@Composable
private fun LocationBadge(flag: String, name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (flag.isNotBlank()) {
                    Text(
                        text = flag,
                        fontSize = 20.sp,
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit, onOpenLogs: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "OpenTunnel",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "AnyConnect · openconnect",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenLogs) {
            Icon(Icons.Rounded.Article, contentDescription = "Connection log")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

// ── Status ─────────────────────────────────────────────────────────────────

@Composable
private fun StatusLine(status: TunnelStatus, lang: dev.opentunnel.vpn.data.AppLanguage) {
    val palette = LocalStatusPalette.current

    val label = when (status.stage) {
        ConnectionStage.CONNECTED -> status.info.server ?: dev.opentunnel.vpn.util.Strings.connected(lang)
        ConnectionStage.IDLE -> dev.opentunnel.vpn.util.Strings.notConnected(lang)
        ConnectionStage.ERROR -> dev.opentunnel.vpn.util.Strings.connectionFailed(lang)
        else -> status.detail ?: "Working…"
    }

    val tint = when (status.stage) {
        ConnectionStage.CONNECTED -> palette.connected
        ConnectionStage.ERROR -> palette.error
        ConnectionStage.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> palette.connecting
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            textAlign = TextAlign.Center,
        )

        if (status.stage == ConnectionStage.CONNECTED && status.connectedAtElapsed > 0L) {
            val elapsed by produceState(0L, status.connectedAtElapsed) {
                while (true) {
                    value = SystemClock.elapsedRealtime() - status.connectedAtElapsed
                    delay(500)
                }
            }
            Text(
                text = Formatters.duration(elapsed),
                style = MaterialTheme.typography.displaySmall.merge(MonoNumberStyle),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ── Traffic ────────────────────────────────────────────────────────────────

@Composable
private fun TrafficRow(stats: TrafficStats, lang: dev.opentunnel.vpn.data.AppLanguage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrafficTile(
            modifier = Modifier.weight(1f),
            label = dev.opentunnel.vpn.util.Strings.downloaded(lang),
            total = Formatters.bytes(stats.rxBytes),
            rate = Formatters.rate(stats.rxRate),
            tint = LocalStatusPalette.current.connected,
            up = false,
        )
        TrafficTile(
            modifier = Modifier.weight(1f),
            label = dev.opentunnel.vpn.util.Strings.uploaded(lang),
            total = Formatters.bytes(stats.txBytes),
            rate = Formatters.rate(stats.txRate),
            tint = MaterialTheme.colorScheme.secondary,
            up = true,
        )
    }
}

@Composable
private fun TrafficTile(
    modifier: Modifier,
    label: String,
    total: String,
    rate: String,
    tint: Color,
    up: Boolean,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = total,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = rate,
                style = MaterialTheme.typography.bodySmall.merge(MonoNumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Error ──────────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

// ── Connection details ─────────────────────────────────────────────────────

@Composable
private fun ConnectionDetails(status: TunnelStatus, lang: dev.opentunnel.vpn.data.AppLanguage) {
    val info = status.info
    SectionCard(title = dev.opentunnel.vpn.util.Strings.connectionDetails(lang)) {
        Column(Modifier.padding(vertical = 6.dp)) {
            info.ipv4?.takeIf { it.isNotBlank() }?.let { DetailRow(dev.opentunnel.vpn.util.Strings.ipv4Address(lang), it) }
            info.ipv6?.takeIf { it.isNotBlank() }?.let { DetailRow(dev.opentunnel.vpn.util.Strings.ipv6Address(lang), it) }
            if (info.dns.isNotEmpty()) DetailRow(dev.opentunnel.vpn.util.Strings.dnsServers(lang), info.dns.joinToString("\n"))
            info.domain?.takeIf { it.isNotBlank() }?.let { DetailRow(dev.opentunnel.vpn.util.Strings.searchDomain(lang), it) }
            if (info.mtu > 0) DetailRow(dev.opentunnel.vpn.util.Strings.mtuLabel(lang), info.mtu.toString())
            info.cstpCipher?.takeIf { it.isNotBlank() }?.let { DetailRow(dev.opentunnel.vpn.util.Strings.tlsChannel(lang), it) }
            val dtls = info.dtlsCipher?.takeIf { it.isNotBlank() }
            DetailRow(dev.opentunnel.vpn.util.Strings.dtlsChannel(lang), dtls ?: "not established (TLS only)")
            if (info.serverRoutes.isNotEmpty()) {
                DetailRow("Gateway routes", info.serverRoutes.joinToString("\n"))
            }
            if (info.excludedApps > 0) {
                DetailRow("Apps outside the tunnel", info.excludedApps.toString())
            }
            info.locationName?.let { name ->
                val display = if (info.locationFlag != null) "${info.locationFlag} $name" else name
                DetailRow(dev.opentunnel.vpn.util.Strings.locationLabel(lang), display)
            }
        }
    }
}

private fun splitTunnelSummary(settings: AppSettings): String = when {
    !settings.splitTunnelEnabled -> "Off — every app uses the VPN"
    settings.selectedPackages.isEmpty() -> "On, but no apps selected yet"
    settings.splitTunnelMode == SplitTunnelMode.EXCLUDE_SELECTED ->
        "${settings.selectedPackages.size} app(s) bypass the VPN"
    else -> "Only ${settings.selectedPackages.size} app(s) use the VPN"
}
