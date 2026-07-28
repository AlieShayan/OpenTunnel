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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import dev.opentunnel.vpn.core.LogLine
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import dev.opentunnel.vpn.util.Strings
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    status: TunnelStatus,
    stats: TrafficStats,
    profile: VpnProfile,
    profiles: List<VpnProfile>,
    settings: AppSettings,
    rxHistoryList: List<Long> = emptyList(),
    txHistoryList: List<Long> = emptyList(),
    onToggleConnection: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProfileManagement: () -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onOpenSplitNetworks: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalStatusPalette.current
    val scheme = MaterialTheme.colorScheme
    val lang = settings.appLanguage

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

    val scrollState = rememberScrollState()
    dev.opentunnel.vpn.util.RememberScrollHaptic(scrollState, settings.hapticFeedbackEnabled)

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
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp),
        ) {
            HomeTopBar()

            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val haptic = LocalHapticFeedback.current
                ConnectOrb(
                    stage = status.stage,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleConnection()
                    },
                    lang = settings.appLanguage,
                    enabled = true,
                )
            }

            Spacer(Modifier.height(12.dp))

            StatusLine(status = status, lang = settings.appLanguage)

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = status.stage == ConnectionStage.CONNECTED &&
                    (status.info.locationName != null || status.info.pingMs >= 0),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                LocationBadge(
                    flag = status.info.locationFlag ?: "\uD83C\uDF10",
                    name = status.info.locationName ?: Strings.connected(settings.appLanguage),
                    pingMs = status.info.pingMs,
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = status.stage == ConnectionStage.CONNECTED ||
                    status.stage == ConnectionStage.RECONNECTING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    TrafficRow(stats, settings.appLanguage)
                    Spacer(Modifier.height(14.dp))
                    dev.opentunnel.vpn.ui.components.SpeedChart(
                        stats = stats,
                        appLanguage = settings.appLanguage,
                        rxHistoryList = rxHistoryList,
                        txHistoryList = txHistoryList,
                    )
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
                    lang = settings.appLanguage,
                    onSelectProfile = onSelectProfile,
                    onOpenProfile = onOpenProfile,
                    onOpenProfileManagement = onOpenProfileManagement,
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SectionCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SettingRow(
                        icon = Icons.Rounded.Apps,
                        title = Strings.splitTunnelTitle(lang),
                        subtitle = splitTunnelSummary(settings, lang),
                        iconTint = scheme.tertiary,
                        iconBackground = scheme.tertiary.copy(alpha = 0.14f),
                        onClick = onOpenSplitTunnel,
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .offset(y = (-6).dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp))
                        .clickable { onOpenProfileManagement() },
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(28.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = scheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Text(
                                text = Strings.manageProfilesAction(lang),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Icon(
                            imageVector = if (Strings.isRtl(lang)) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
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

            Spacer(Modifier.height(80.dp))
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPagerScreen(
    status: TunnelStatus,
    stats: TrafficStats,
    logs: List<LogLine>,
    profile: VpnProfile,
    profiles: List<VpnProfile>,
    settings: AppSettings,
    rxHistoryList: List<Long> = emptyList(),
    txHistoryList: List<Long> = emptyList(),
    onToggleConnection: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProfileManagement: () -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onOpenSplitNetworks: () -> Unit,
    onClearLogs: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAppLanguage: (AppLanguage) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onBypassLocalNetworks: (Boolean) -> Unit,
    onConnectOnBoot: (Boolean) -> Unit,
    onReconnectOnNetworkChange: (Boolean) -> Unit,
    onShowStatsInNotification: (Boolean) -> Unit,
    onVerboseLogging: (Boolean) -> Unit,
    onHapticFeedbackEnabled: (Boolean) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> LogScreen(
                    logs = logs,
                    appLanguage = settings.appLanguage,
                    hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                    onClear = onClearLogs,
                    onBack = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                )
                1 -> HomeScreen(
                    status = status,
                    stats = stats,
                    profile = profile,
                    profiles = profiles,
                    settings = settings,
                    rxHistoryList = rxHistoryList,
                    txHistoryList = txHistoryList,
                    onToggleConnection = onToggleConnection,
                    onSelectProfile = onSelectProfile,
                    onOpenProfile = onOpenProfile,
                    onOpenProfileManagement = onOpenProfileManagement,
                    onOpenSplitTunnel = onOpenSplitTunnel,
                    onOpenSplitNetworks = onOpenSplitNetworks,
                    onOpenLogs = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    onOpenSettings = {
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                )
                2 -> SettingsScreen(
                    settings = settings,
                    onThemeMode = onThemeMode,
                    onAppLanguage = onAppLanguage,
                    onDynamicColor = onDynamicColor,
                    onBypassLocalNetworks = onBypassLocalNetworks,
                    onConnectOnBoot = onConnectOnBoot,
                    onReconnectOnNetworkChange = onReconnectOnNetworkChange,
                    onShowStatsInNotification = onShowStatsInNotification,
                    onVerboseLogging = onVerboseLogging,
                    onHapticFeedbackEnabled = onHapticFeedbackEnabled,
                    onBack = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                )
            }
        }

        FloatingCapsuleNavigation(
            pagerState = pagerState,
            onNavigateToPage = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp),
        )
    }
}

@Composable
private fun FloatingCapsuleNavigation(
    pagerState: PagerState,
    onNavigateToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val position by remember(pagerState) {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val itemWidth = 48.dp
            val itemSpacing = 4.dp
            val indicatorSize = 42.dp

            val indicatorOffset = (itemWidth + itemSpacing) * position + (itemWidth - indicatorSize) / 2

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(indicatorSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.70f),
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f),
                            )
                        ),
                        CircleShape
                    )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val items = listOf(
                    Icons.Rounded.Article to "Connection Logs",
                    Icons.Rounded.Home to "Home",
                    Icons.Rounded.Settings to "Settings",
                )

                items.forEachIndexed { index, (icon, contentDesc) ->
                    val isSelected = pagerState.currentPage == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "iconScale",
                    )

                    Box(
                        modifier = Modifier
                            .size(itemWidth)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToPage(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = contentDesc,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            },
                            modifier = Modifier.scale(scale),
                        )
                    }
                }
            }
        }
    }
}

// ── Profile Picker Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePickerRow(
    profile: VpnProfile,
    profiles: List<VpnProfile>,
    lang: dev.opentunnel.vpn.data.AppLanguage,
    onSelectProfile: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProfileManagement: () -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box {
        SettingRow(
            icon = Icons.Rounded.Public,
            title = profile.displayName,
            subtitle = when {
                !profile.isComplete -> Strings.tapToSetupProfile(lang)
                profile.username.isNotBlank() -> "${profile.username} \u00B7 ${profile.protocol}"
                else -> profile.protocol
            },
            trailing = {
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = Strings.selectProfileTitle(lang),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onClick = onOpenProfile,
        )

        if (showSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = Strings.selectProfileTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    profiles.forEach { p ->
                        val isSelected = p.id == profile.id
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectProfile(p.id)
                                    showSheet = false
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectProfile(p.id)
                                        showSheet = false
                                    },
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = p.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    if (p.server.isNotBlank()) {
                                        Text(
                                            text = p.server,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ── Location Badge ─────────────────────────────────────────────────────────

@Composable
private fun LocationBadge(flag: String, name: String, pingMs: Long = -1L) {
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
                if (name.isNotBlank()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (pingMs >= 0) {
                    if (name.isNotBlank()) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "\u26A1 $pingMs ms",
                        style = MaterialTheme.typography.bodyMedium.merge(MonoNumberStyle),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "OpenTunnel",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "AnyConnect \u00B7 openconnect",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Status ─────────────────────────────────────────────────────────────────

@Composable
private fun StatusLine(status: TunnelStatus, lang: dev.opentunnel.vpn.data.AppLanguage) {
    val palette = LocalStatusPalette.current

    val label = when (status.stage) {
        ConnectionStage.CONNECTED -> status.info.server ?: Strings.connected(lang)
        ConnectionStage.IDLE -> Strings.notConnected(lang)
        ConnectionStage.ERROR -> Strings.connectionFailed(lang)
        ConnectionStage.AUTHENTICATING -> Strings.authenticating(lang)
        ConnectionStage.PREPARING -> Strings.preparing(lang)
        ConnectionStage.CONNECTING -> Strings.connecting(lang)
        ConnectionStage.DISCONNECTING -> Strings.disconnecting(lang)
        ConnectionStage.RECONNECTING -> Strings.reconnecting(lang)
        else -> status.detail ?: Strings.connecting(lang)
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
                    delay(1000)
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
            label = Strings.downloaded(lang),
            total = Formatters.bytes(stats.rxBytes),
            rate = Formatters.rate(stats.rxRate),
            tint = LocalStatusPalette.current.connected,
            up = false,
        )
        TrafficTile(
            modifier = Modifier.weight(1f),
            label = Strings.uploaded(lang),
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
    SectionCard(title = Strings.connectionDetails(lang)) {
        Column(Modifier.padding(vertical = 6.dp)) {
            info.ipv4?.takeIf { it.isNotBlank() }?.let { DetailRow(Strings.ipv4Address(lang), it) }
            info.ipv6?.takeIf { it.isNotBlank() }?.let { DetailRow(Strings.ipv6Address(lang), it) }
            if (info.dns.isNotEmpty()) DetailRow(Strings.dnsServers(lang), info.dns.joinToString("\n"))
            info.domain?.takeIf { it.isNotBlank() }?.let { DetailRow(Strings.searchDomain(lang), it) }
            if (info.mtu > 0) DetailRow(Strings.mtuLabel(lang), info.mtu.toString())
            info.cstpCipher?.takeIf { it.isNotBlank() }?.let { DetailRow(Strings.tlsChannel(lang), it) }
            val dtls = info.dtlsCipher?.takeIf { it.isNotBlank() }
            DetailRow(Strings.dtlsChannel(lang), dtls ?: Strings.dtlsNotEstablished(lang))
            if (info.serverRoutes.isNotEmpty()) {
                DetailRow(Strings.gatewayRoutes(lang), info.serverRoutes.joinToString("\n"))
            }
            if (info.excludedApps > 0) {
                DetailRow(Strings.appsOutsideTunnel(lang), info.excludedApps.toString())
            }
            info.locationName?.let { name ->
                val display = if (info.locationFlag != null) "${info.locationFlag} $name" else name
                DetailRow(Strings.locationLabel(lang), display)
            }
            if (info.pingMs >= 0) {
                DetailRow(Strings.pingLabel(lang), "${info.pingMs} ms")
            }
        }
    }
}

private fun splitTunnelSummary(settings: AppSettings, lang: dev.opentunnel.vpn.data.AppLanguage): String = when {
    !settings.splitTunnelEnabled -> Strings.splitTunnelOffSummary(lang)
    settings.selectedPackages.isEmpty() -> Strings.splitTunnelNoAppsSelectedSummary(lang)
    settings.splitTunnelMode == SplitTunnelMode.EXCLUDE_SELECTED ->
        "${settings.selectedPackages.size} app(s) bypass the VPN"
    else -> "Only ${settings.selectedPackages.size} app(s) use the VPN"
}

private fun splitTunnelNetworksSummary(settings: AppSettings, lang: dev.opentunnel.vpn.data.AppLanguage): String = when {
    !settings.splitTunnelNetworksEnabled -> Strings.splitTunnelNetworksOffSummary(lang)
    settings.splitTunnelNetworks.isEmpty() -> Strings.splitTunnelNetworksNoEntriesSummary(lang)
    settings.splitTunnelNetworksMode == SplitTunnelMode.EXCLUDE_SELECTED ->
        "${settings.splitTunnelNetworks.size} network(s)/site(s) bypass the VPN"
    else -> "Only ${settings.splitTunnelNetworks.size} network(s)/site(s) use the VPN"
}
