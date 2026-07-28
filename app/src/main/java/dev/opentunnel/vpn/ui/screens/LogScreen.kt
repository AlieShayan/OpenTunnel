package dev.opentunnel.vpn.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.opentunnel.vpn.core.LogLevel
import dev.opentunnel.vpn.core.LogLine
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.util.RememberLazyListHaptic
import dev.opentunnel.vpn.util.Strings

import androidx.compose.material3.FilterChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    val filteredLogs = remember(logs, filter) {
        when (filter) {
            LogFilter.ALL -> logs
            LogFilter.ERROR -> logs.filter { it.level == LogLevel.ERROR }
            LogFilter.INFO -> logs.filter { it.level == LogLevel.INFO || it.level == LogLevel.DEBUG }
            LogFilter.APP -> logs.filter { it.level == LogLevel.APP }
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.logsTitle(appLanguage)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        copyToClipboard(context, filteredLogs.toPlainText(formatter))
                        scope.launch { snackbar.showSnackbar("Log copied") }
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy log")
                    }
                    IconButton(onClick = { shareText(context, filteredLogs.toPlainText(formatter)) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share log")
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear log")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                        onClick = { filter = option },
                        label = { Text(option.getLabel(appLanguage)) },
                        shape = MaterialTheme.shapes.small,
                    )
                }
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = autoScroll,
                    onClick = { autoScroll = !autoScroll },
                    label = { Text(Strings.autoScrollLabel(appLanguage)) },
                    shape = MaterialTheme.shapes.small,
                )
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Nothing logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(filteredLogs) { line ->
                        LogRow(line, formatter)
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

private fun sanitizeLog(log: String): String {
    var clean = log
    clean = clean.replace(Regex("""(?i)(password|passwd|pass|token|secret|key|authorization|bearer)\s*[:=]\s*([^\s,;]+)"""), "$1: [REDACTED]")
    clean = clean.replace(Regex("""(?i)(pin-sha256:)[A-Za-z0-9+/=]+"""), "$1[REDACTED_FINGERPRINT]")
    return clean
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("OpenTunnel log", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share log"))
    }
}
