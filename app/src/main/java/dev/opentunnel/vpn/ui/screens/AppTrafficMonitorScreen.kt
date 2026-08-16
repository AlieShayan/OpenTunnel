package dev.opentunnel.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.opentunnel.vpn.core.TrafficStats
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.data.AppTrafficEntry
import dev.opentunnel.vpn.data.AppTrafficSummary
import dev.opentunnel.vpn.data.InstalledApps
import dev.opentunnel.vpn.data.SortDirection
import dev.opentunnel.vpn.data.TrafficFilterMode
import dev.opentunnel.vpn.data.TrafficSortBy
import dev.opentunnel.vpn.ui.theme.MonoNumberStyle
import dev.opentunnel.vpn.util.Formatters
import dev.opentunnel.vpn.util.HapticHelper
import dev.opentunnel.vpn.util.RememberLazyListHaptic
import dev.opentunnel.vpn.util.Strings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTrafficMonitorScreen(
    vpnStats: TrafficStats = TrafficStats(),
    summary: AppTrafficSummary,
    entries: List<AppTrafficEntry>,
    sortBy: TrafficSortBy,
    sortDirection: SortDirection,
    filterMode: TrafficFilterMode,
    searchQuery: String,
    appLanguage: AppLanguage,
    hapticEnabled: Boolean,
    hasUsageAccessPermission: Boolean = true,
    onSortByChange: (TrafficSortBy) -> Unit,
    onToggleSortDirection: () -> Unit,
    onFilterModeChange: (TrafficFilterMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onResetStats: () -> Unit,
    onPauseMonitoring: () -> Unit,
    onResumeMonitoring: () -> Unit,
    onGrantUsageAccess: () -> Unit = {},
    onRefreshPermission: () -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val isRtl = Strings.isRtl(appLanguage)
    val listState = rememberLazyListState()
    RememberLazyListHaptic(listState, hapticEnabled)

    // Reset scroll position to top whenever sorting or filtering changes
    LaunchedEffect(sortBy, sortDirection, filterMode) {
        listState.scrollToItem(0)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = Strings.trafficMonitorTitle(appLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (summary.activeAppsCount > 0) {
                                Strings.trafficActiveApps(appLanguage, summary.activeAppsCount)
                            } else {
                                Strings.trafficTotalApps(appLanguage, summary.totalAppsCount)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hapticEnabled) HapticHelper.performClick(context, true)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = Strings.cancel(appLanguage),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (hapticEnabled) HapticHelper.performClick(context, true)
                            if (isPaused) {
                                onResumeMonitoring()
                                isPaused = false
                            } else {
                                onPauseMonitoring()
                                isPaused = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (isPaused) Strings.trafficResume(appLanguage) else Strings.trafficPause(appLanguage),
                            tint = if (isPaused) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = {
                            if (hapticEnabled) HapticHelper.performClick(context, true)
                            showResetDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = Strings.trafficResetStats(appLanguage),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (!hasUsageAccessPermission) {
                UsageAccessPermissionBanner(
                    lang = appLanguage,
                    onGrantPermission = {
                        if (hapticEnabled) HapticHelper.performClick(context, true)
                        onGrantUsageAccess()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            // ── Overall Summary Card ─────────────────────────────────────────
            SummaryHeaderCard(
                vpnStats = vpnStats,
                lang = appLanguage,
                isPaused = isPaused,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            // ── Search & Filter Row ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = Strings.trafficSearchPlaceholder(appLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(TrafficFilterMode.entries) { mode ->
                        val isSelected = filterMode == mode
                        val label = when (mode) {
                            TrafficFilterMode.ALL -> Strings.trafficFilterAll(appLanguage)
                            TrafficFilterMode.ACTIVE_ONLY -> Strings.trafficFilterActive(appLanguage)
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (hapticEnabled) HapticHelper.performClick(context, true)
                                onFilterModeChange(mode)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            shape = MaterialTheme.shapes.medium,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Sort Dimension & Direction Bar
                SortControlsBar(
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    lang = appLanguage,
                    hapticEnabled = hapticEnabled,
                    onSortByChange = onSortByChange,
                    onToggleSortDirection = onToggleSortDirection,
                )
            }

            // ── Applications Traffic List ────────────────────────────────────
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DataUsage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                Strings.trafficEmptyNoMatches(appLanguage, searchQuery)
                            } else {
                                Strings.trafficEmptyNoData(appLanguage)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = entries,
                        key = { it.packageName },
                    ) { entry ->
                        AppTrafficCard(
                            entry = entry,
                            lang = appLanguage,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    // ── Reset Confirmation Dialog ────────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = Strings.trafficResetConfirmTitle(appLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = Strings.trafficResetConfirmBody(appLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (hapticEnabled) HapticHelper.performClick(context, true)
                        showResetDialog = false
                        onResetStats()
                    }
                ) {
                    Text(
                        text = Strings.trafficResetStats(appLanguage),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text(text = Strings.cancel(appLanguage))
                }
            },
        )
    }
}

@Composable
private fun SummaryHeaderCard(
    vpnStats: TrafficStats,
    lang: AppLanguage,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    val totalTunnelBytes = vpnStats.rxBytes + vpnStats.txBytes
    val isLive = !isPaused && (vpnStats.rxRate > 0 || vpnStats.txRate > 0)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        tonalElevation = 4.dp,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header Row: Total & Live badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPaused) {
                                    scheme.tertiary
                                } else if (isLive) {
                                    scheme.primary.copy(alpha = pulseAlpha)
                                } else {
                                    scheme.outline
                                }
                            )
                    )
                    Text(
                        text = Strings.trafficTotal(lang),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Text(
                    text = Formatters.bytes(totalTunnelBytes),
                    style = MonoNumberStyle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                    ),
                )
            }

            Spacer(Modifier.height(14.dp))

            // 2-Column Live Rates & Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Download Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(24.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = null,
                                        tint = scheme.primary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Text(
                                text = Strings.downloaded(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = Formatters.bytes(vpnStats.rxBytes),
                            style = MonoNumberStyle.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "↓ ${Formatters.rate(vpnStats.rxRate)}",
                                style = MonoNumberStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (vpnStats.rxRate > 0) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.7f),
                                ),
                            )
                        }
                    }
                }

                // Upload Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.tertiary.copy(alpha = 0.14f),
                                modifier = Modifier.size(24.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = null,
                                        tint = scheme.tertiary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Text(
                                text = Strings.uploaded(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = Formatters.bytes(vpnStats.txBytes),
                            style = MonoNumberStyle.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "↑ ${Formatters.rate(vpnStats.txRate)}",
                                style = MonoNumberStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (vpnStats.txRate > 0) scheme.tertiary else scheme.onSurfaceVariant.copy(alpha = 0.7f),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortControlsBar(
    sortBy: TrafficSortBy,
    sortDirection: SortDirection,
    lang: AppLanguage,
    hapticEnabled: Boolean,
    onSortByChange: (TrafficSortBy) -> Unit,
    onToggleSortDirection: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(TrafficSortBy.entries) { option ->
                val isSelected = sortBy == option
                val label = when (option) {
                    TrafficSortBy.TOTAL_TRAFFIC -> Strings.trafficSortTotal(lang)
                    TrafficSortBy.DOWNLOAD -> Strings.trafficSortDownload(lang)
                    TrafficSortBy.UPLOAD -> Strings.trafficSortUpload(lang)
                    TrafficSortBy.DOWNLOAD_SPEED -> "↓ ${Strings.trafficSortDownloadSpeed(lang)}"
                    TrafficSortBy.UPLOAD_SPEED -> "↑ ${Strings.trafficSortUploadSpeed(lang)}"
                    TrafficSortBy.APP_NAME -> Strings.trafficSortName(lang)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (hapticEnabled) HapticHelper.performClick(context, true)
                            onSortByChange(option)
                        },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Direction Toggle Button
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    if (hapticEnabled) HapticHelper.performClick(context, true)
                    onToggleSortDirection()
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Icon(
                    imageVector = if (sortDirection == SortDirection.DESCENDING) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                    contentDescription = if (sortDirection == SortDirection.DESCENDING) Strings.trafficSortOrderDesc(lang) else Strings.trafficSortOrderAsc(lang),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (sortDirection == SortDirection.DESCENDING) Strings.trafficSortOrderDesc(lang) else Strings.trafficSortOrderAsc(lang),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AppTrafficCard(
    entry: AppTrafficEntry,
    lang: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val icon by produceState<ImageBitmap?>(null, entry.packageName) {
        value = InstalledApps.icon(context, entry.packageName)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (entry.isActive) {
            scheme.surfaceContainerHigh
        } else {
            scheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (entry.isActive) {
                scheme.primary.copy(alpha = 0.35f)
            } else {
                scheme.outlineVariant.copy(alpha = 0.3f)
            },
        ),
        tonalElevation = if (entry.isActive) 3.dp else 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Top Line: Icon + App Name + Package + Split Badge + Total Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap = icon
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Android,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // App Name & Package
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        if (entry.isActive) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = entry.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        // Split Tunnel Routing Tag
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (entry.isVpnRouted) scheme.primaryContainer.copy(alpha = 0.6f) else scheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = if (entry.isVpnRouted) Strings.trafficVpnRouted(lang) else Strings.trafficBypassed(lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                color = if (entry.isVpnRouted) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Total Data Amount
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = Formatters.bytes(entry.totalBytes),
                        style = MonoNumberStyle.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.totalBytes > 0) scheme.primary else scheme.onSurfaceVariant,
                        ),
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", entry.trafficSharePercent),
                        style = MonoNumberStyle.copy(
                            fontSize = 11.sp,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Linear Progress Bar for Traffic Share
            LinearProgressIndicator(
                progress = { (entry.trafficSharePercent / 100f).coerceIn(0f, 1f) },
                strokeCap = StrokeCap.Round,
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )

            Spacer(Modifier.height(10.dp))

            // Bottom Metrics: Downloaded vs Uploaded & Live Rates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surfaceContainerLowest.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Download Metric
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = Formatters.bytes(entry.rxBytes),
                        style = MonoNumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                    )
                    if (entry.rxRate > 0) {
                        Text(
                            text = "(${Formatters.rate(entry.rxRate)})",
                            style = MonoNumberStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = scheme.primary,
                        )
                    }
                }

                // Upload Metric
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = scheme.tertiary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = Formatters.bytes(entry.txBytes),
                        style = MonoNumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                    )
                    if (entry.txRate > 0) {
                        Text(
                            text = "(${Formatters.rate(entry.txRate)})",
                            style = MonoNumberStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = scheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageAccessPermissionBanner(
    lang: AppLanguage,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = Strings.trafficPermissionRequiredTitle(lang),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            Text(
                text = Strings.trafficPermissionRequiredBody(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                lineHeight = 18.sp,
            )

            Button(
                onClick = onGrantPermission,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = Strings.trafficGrantPermission(lang),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
