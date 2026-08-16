package dev.opentunnel.vpn.ui.screens

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.data.VpnProfile
import dev.opentunnel.vpn.ui.components.SectionCard
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import dev.opentunnel.vpn.ui.theme.MonoNumberStyle
import dev.opentunnel.vpn.util.HapticHelper
import dev.opentunnel.vpn.util.RememberLazyListHaptic
import dev.opentunnel.vpn.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    activeProfileId: String,
    profiles: List<VpnProfile>,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedbackEnabled: Boolean = true,
    onSelectProfile: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
    onDeleteProfile: (String) -> Unit,
    onExportProfiles: ((String) -> Unit) -> Unit,
    onImportProfiles: (String, (Int) -> Unit) -> Unit,
    onReorderProfiles: (List<VpnProfile>) -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val lang = appLanguage
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val palette = LocalStatusPalette.current

    RememberLazyListHaptic(listState, hapticFeedbackEnabled)

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonString by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var profileToDelete by remember { mutableStateOf<VpnProfile?>(null) }
    var profilePings by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isPinging by remember { mutableStateOf(false) }

    val handlePingAndSort = {
        if (!isPinging && profiles.isNotEmpty()) {
            HapticHelper.performClick(context, hapticFeedbackEnabled)
            scope.launch {
                isPinging = true
                val results = withContext(Dispatchers.IO) {
                    profiles.map { p ->
                        async { p.id to measureServerPing(p.server) }
                    }.awaitAll().toMap()
                }
                profilePings = results
                val sorted = profiles.sortedWith(
                    compareBy<VpnProfile> {
                        val ping = results[it.id] ?: -1L
                        if (ping >= 0) ping else Long.MAX_VALUE
                    }.thenBy { it.displayName }
                )
                onReorderProfiles(sorted)
                isPinging = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.profileManagementTitle(lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (profiles.isNotEmpty()) {
                        IconButton(
                            onClick = { handlePingAndSort() },
                            enabled = !isPinging,
                        ) {
                            if (isPinging) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = "Ping & Sort Profiles",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        onExportProfiles { json ->
                            exportJsonString = json
                            showExportDialog = true
                        }
                    }) {
                        Icon(Icons.Rounded.Upload, contentDescription = Strings.exportProfiles(lang))
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Rounded.Download, contentDescription = Strings.importProfiles(lang))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProfile,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = Strings.addProfile(lang))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            text = Strings.noProfilesYet(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = Strings.tapToAddProfile(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    items(profiles, key = { it.id }) { p ->
                        val isActive = p.id == activeProfileId || (activeProfileId.isBlank() && profiles.firstOrNull()?.id == p.id)

                        SectionCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectProfile(p.id) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isActive,
                                    onClick = { onSelectProfile(p.id) },
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = p.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                        )
                                        if (isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = MaterialTheme.shapes.extraSmall,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                            ) {
                                                Text(
                                                    text = Strings.activeProfileBadge(lang),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (p.server.isNotBlank()) p.server else "No server configured",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (p.username.isNotBlank()) {
                                        Text(
                                            text = "${p.username} · ${p.protocol}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        )
                                    }

                                    // Single-profile ping tester badge
                                    val pingMs = profilePings[p.id]
                                    if (pingMs != null) {
                                        Spacer(Modifier.height(6.dp))
                                        val (pingColor, pingLabel) = when {
                                            pingMs < 0 -> palette.error to "Timeout"
                                            pingMs < 100 -> palette.connected to "$pingMs ms"
                                            pingMs < 250 -> palette.connecting to "$pingMs ms"
                                            else -> palette.error to "$pingMs ms"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = pingColor.copy(alpha = 0.14f),
                                            modifier = Modifier.clickable {
                                                scope.launch {
                                                    val single = withContext(Dispatchers.IO) { measureServerPing(p.server) }
                                                    profilePings = profilePings + (p.id to single)
                                                }
                                            },
                                        ) {
                                            Text(
                                                text = "⚡ $pingLabel",
                                                style = MaterialTheme.typography.labelSmall.merge(MonoNumberStyle),
                                                color = pingColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { onEditProfile(p.id) }) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Edit profile",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                IconButton(onClick = { profileToDelete = p }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = Strings.delete(lang),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }

    // Delete confirmation dialog
    profileToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(Strings.delete(lang)) },
            text = { Text(Strings.deleteProfileConfirm(lang, p.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProfile(p.id)
                    profileToDelete = null
                }) {
                    Text(Strings.delete(lang), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text(Strings.cancel(lang))
                }
            },
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(Strings.exportProfiles(lang)) },
            text = {
                Column {
                    Text(
                        text = if (Strings.isRtl(lang)) "تنظیمات پروفایل‌ها در قالب JSON (بدون رمز عبور برای امنیت):"
                        else "Profile configuration JSON (passwords excluded for security):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportJsonString,
                        onValueChange = {},
                        readOnly = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(exportJsonString))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text("Copy JSON", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(Strings.cancel(lang))
                }
            },
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.importProfiles(lang)) },
            text = {
                Column {
                    Text(
                        text = if (Strings.isRtl(lang)) "رشته JSON شامل لیست پروفایل‌ها را وارد کنید:"
                        else "Paste JSON array of profiles below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("[{ \"name\": \"Work VPN\", \"server\": \"...\" }]") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onImportProfiles(importJsonInput) { count ->
                        Toast.makeText(context, "Imported $count profile(s)", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                        importJsonInput = ""
                    }
                }) {
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(Strings.cancel(lang))
                }
            },
        )
    }
}

private fun measureServerPing(server: String): Long {
    if (server.isBlank()) return -1L
    val clean = server.removePrefix("https://").removePrefix("http://").substringBefore("/")
    val hostPort = clean.split(":")
    val host = hostPort.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return -1L
    val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 443

    val start = SystemClock.elapsedRealtime()
    return runCatching {
        java.net.Socket().use { socket ->
            socket.connect(java.net.InetSocketAddress(host, port), 2500)
        }
        SystemClock.elapsedRealtime() - start
    }.getOrDefault(-1L)
}
