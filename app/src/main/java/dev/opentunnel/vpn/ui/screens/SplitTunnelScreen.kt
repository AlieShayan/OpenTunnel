package dev.opentunnel.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.data.InstalledApp
import dev.opentunnel.vpn.data.InstalledApps
import dev.opentunnel.vpn.data.SplitTunnelMode

private enum class AppFilter {
    ALL,
    SELECTED,
    USER,
    SYSTEM;

    fun getLabel(lang: dev.opentunnel.vpn.data.AppLanguage): String = when (this) {
        ALL -> dev.opentunnel.vpn.util.Strings.splitTunnelFilterAll(lang)
        SELECTED -> dev.opentunnel.vpn.util.Strings.splitTunnelFilterSelected(lang)
        USER -> dev.opentunnel.vpn.util.Strings.splitTunnelFilterInstalled(lang)
        SYSTEM -> dev.opentunnel.vpn.util.Strings.splitTunnelFilterSystem(lang)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelScreen(
    settings: AppSettings,
    apps: List<InstalledApp>?,
    onLoadApps: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onChangeMode: (SplitTunnelMode) -> Unit,
    onTogglePackage: (String, Boolean) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
) {
    val lang = settings.appLanguage
    LaunchedEffect(Unit) { onLoadApps() }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }

    val selected = settings.selectedPackages
    val visible = remember(apps, query, filter, selected) {
        val needle = query.trim().lowercase()
        apps.orEmpty().filter { app ->
            val matchesQuery = needle.isEmpty() ||
                app.label.lowercase().contains(needle) ||
                app.packageName.lowercase().contains(needle)
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.SELECTED -> app.packageName in selected
                AppFilter.USER -> !app.isSystem
                AppFilter.SYSTEM -> app.isSystem
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dev.opentunnel.vpn.util.Strings.splitTunnelTitle(lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = dev.opentunnel.vpn.util.Strings.cancel(lang))
                    }
                },
                actions = {
                    if (selected.isNotEmpty()) {
                        TextButton(onClick = onClearAll) { Text(dev.opentunnel.vpn.util.Strings.splitTunnelClear(lang)) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HeaderCard(
                enabled = settings.splitTunnelEnabled,
                mode = settings.splitTunnelMode,
                selectedCount = selected.size,
                lang = lang,
                onToggleEnabled = onToggleEnabled,
                onChangeMode = onChangeMode,
            )

            AnimatedVisibility(visible = settings.splitTunnelEnabled) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(dev.opentunnel.vpn.util.Strings.splitTunnelSearchPlaceholder(lang)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppFilter.entries.forEach { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filter = option },
                                label = { Text(option.getLabel(lang)) },
                                shape = MaterialTheme.shapes.small,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            if (!settings.splitTunnelEnabled) {
                EmptyHint(dev.opentunnel.vpn.util.Strings.splitTunnelEmptyDisabled(lang))
                return@Column
            }

            when {
                apps == null -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                visible.isEmpty() -> EmptyHint(dev.opentunnel.vpn.util.Strings.splitTunnelNoMatches(lang, query))

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    items(visible, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in selected,
                            onCheckedChange = { onTogglePackage(app.packageName, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    enabled: Boolean,
    mode: SplitTunnelMode,
    selectedCount: Int,
    lang: dev.opentunnel.vpn.data.AppLanguage,
    onToggleEnabled: (Boolean) -> Unit,
    onChangeMode: (SplitTunnelMode) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        dev.opentunnel.vpn.util.Strings.splitTunnelHeaderTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        when {
                            !enabled -> dev.opentunnel.vpn.util.Strings.splitTunnelDisabledSub(lang)
                            selectedCount == 0 -> dev.opentunnel.vpn.util.Strings.splitTunnelNoAppsSub(lang)
                            mode == SplitTunnelMode.EXCLUDE_SELECTED ->
                                dev.opentunnel.vpn.util.Strings.splitTunnelExcludeCountSub(lang, selectedCount)
                            else -> dev.opentunnel.vpn.util.Strings.splitTunnelIncludeCountSub(lang, selectedCount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = enabled, onCheckedChange = onToggleEnabled)
            }

            AnimatedVisibility(visible = enabled) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    ModeSelector(mode = mode, lang = lang, onChangeMode = onChangeMode)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        dev.opentunnel.vpn.util.Strings.splitTunnelNotice(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(
    mode: SplitTunnelMode,
    lang: dev.opentunnel.vpn.data.AppLanguage,
    onChangeMode: (SplitTunnelMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeOption(
            text = dev.opentunnel.vpn.util.Strings.splitTunnelBypassMode(lang),
            selected = mode == SplitTunnelMode.EXCLUDE_SELECTED,
            modifier = Modifier.weight(1f),
            onClick = { onChangeMode(SplitTunnelMode.EXCLUDE_SELECTED) },
        )
        ModeOption(
            text = dev.opentunnel.vpn.util.Strings.splitTunnelOnlyMode(lang),
            selected = mode == SplitTunnelMode.INCLUDE_SELECTED,
            modifier = Modifier.weight(1f),
            onClick = { onChangeMode(SplitTunnelMode.INCLUDE_SELECTED) },
        )
    }
}

@Composable
private fun ModeOption(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, app.packageName) {
        value = InstalledApps.icon(context, app.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = icon
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            } else {
                Icon(
                    Icons.Rounded.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
