package dev.opentunnel.vpn.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.opentunnel.vpn.core.LogLevel
import dev.opentunnel.vpn.core.LogLine
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import dev.opentunnel.vpn.util.HapticHelper
import dev.opentunnel.vpn.util.RememberLazyListHaptic
import dev.opentunnel.vpn.util.Strings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LogFilter {
    ALL, ERROR, INFO, APP;

    fun getLabel(lang: AppLanguage): String = when (this) {
        ALL -> Strings.logLevelAll(lang)
        ERROR -> Strings.logLevelError(lang)
        INFO -> Strings.logLevelInfo(lang)
        APP -> Strings.logLevelApp(lang)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    logs: List<LogLine>,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedbackEnabled: Boolean = true,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    RememberLazyListHaptic(listState, hapticFeedbackEnabled)

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    var filter by remember { mutableStateOf(LogFilter.ALL) }
    var autoScroll by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, filter, searchQuery) {
        val needle = searchQuery.trim().lowercase()
        logs.filter { line ->
            val matchesLevel = when (filter) {
                LogFilter.ALL -> true
                LogFilter.ERROR -> line.level == LogLevel.ERROR
                LogFilter.INFO -> line.level == LogLevel.INFO || line.level == LogLevel.DEBUG
                LogFilter.APP -> line.level == LogLevel.APP
            }
            val matchesQuery = needle.isEmpty() || line.text.lowercase().contains(needle)
            matchesLevel && matchesQuery
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.lastIndex)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.logsTitle(appLanguage)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "Search logs",
                            tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {
                        copyToClipboard(context, filteredLogs.toPlainText(formatter))
                        scope.launch { snackbar.showSnackbar("Log copied") }
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy log")
                    }
                    IconButton(onClick = { shareText(context, filteredLogs.toPlainText(formatter)) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share log")
                    }
                    IconButton(onClick = {
                        onClear()
                        scope.launch { snackbar.showSnackbar(Strings.logsCleared(appLanguage)) }
                    }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear log")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Strings.searchLogsPlaceholder(appLanguage)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LogFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = {
                            HapticHelper.performClick(context, hapticFeedbackEnabled)
                            filter = option
                        },
                        label = { Text(option.getLabel(appLanguage)) },
                        shape = MaterialTheme.shapes.small,
                    )
                }
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = autoScroll,
                    onClick = {
                        HapticHelper.performClick(context, hapticFeedbackEnabled)
                        autoScroll = !autoScroll
                    },
                    label = { Text(Strings.autoScrollLabel(appLanguage)) },
                    shape = MaterialTheme.shapes.small,
                )
            }

            // Atmospheric translucent container for high readability and world atmosphere
            val logBoxShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(logBoxShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        logBoxShape,
                    ),
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching log entries found." else "Nothing logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        items(filteredLogs) { line ->
                            LogRow(line, formatter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(line: LogLine, formatter: SimpleDateFormat) {
    val palette = LocalStatusPalette.current
    val color = when (line.level) {
        LogLevel.ERROR -> palette.error
        LogLevel.APP -> MaterialTheme.colorScheme.primary
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(Modifier.fillMaxWidth()) {
        Text(
            text = formatter.format(Date(line.timestamp)),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
            ),
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun List<LogLine>.toPlainText(formatter: SimpleDateFormat): String {
    val raw = joinToString("\n") { "${formatter.format(Date(it.timestamp))}  ${it.level.name.padEnd(5)}  ${it.text}" }
    return sanitizeLog(raw)
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("OpenTunnel log", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share OpenTunnel log"))
}

private fun sanitizeLog(raw: String): String {
    var s = raw
    val pwRegex = Regex("""(?i)(password|secret|pass|token)(\s*[:=]\s*)([^\s\r\n]+)""")
    s = pwRegex.replace(s) { m -> "${m.groupValues[1]}${m.groupValues[2]}***REDACTED***" }
    return s
}
