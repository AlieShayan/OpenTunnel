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
import dev.opentunnel.vpn.ui.theme.MonoNumberStyle
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

    RememberLazyListHaptic(listState, hapticFeedbackEnabled)

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonString by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var profileToDelete by remember { mutableStateOf<VpnProfile?>(null) }
    var profilePings by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isPinging by remember { mutableStateOf(false) }

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
                    IconButton(onClick = {
                        onExportProfiles { json ->
                            exportJsonString = json
                            showExportDialog = true
                        }
                    }) {
                        Icon(Icons.Rounded.Upload, contentDescription = "Export profiles")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Rounded.Download, contentDescription = "Import profiles")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = {
                        if (!isPinging && profiles.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    },
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            text = if (Strings.isRtl(lang)) "پینگ و مرتب‌سازی" else "Ping & Sort",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                FloatingActionButton(
                    onClick = onAddProfile,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = Strings.addProfile(lang))
                }
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Strings.noProfilesYet(lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add a VPN profile with any gateway",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                    item { Spacer(Modifier.height(6.dp)) }

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
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        if (isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = MaterialTheme.shapes.extraSmall,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (p.server.isNotBlank()) p.server else "No server set",
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
                                    val pingMs = profilePings[p.id]
                                    if (pingMs != null) {
                                        Spacer(Modifier.height(4.dp))
                                        if (pingMs >= 0) {
                                            Text(
                                                text = "⚡ $pingMs ms",
                                                style = MaterialTheme.typography.bodySmall.merge(MonoNumberStyle),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        } else {
                                            Text(
                                                text = "⚡ Timeout",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
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
                                        contentDescription = "Delete profile",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Delete confirmation dialog
    profileToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete profile “${p.displayName}”?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProfile(p.id)
                    profileToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Profiles") },
            text = {
                Column {
                    Text("Profile configuration JSON (passwords excluded for security):")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportJsonString,
                        onValueChange = {},
                        readOnly = true,
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
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            },
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Profiles") },
            text = {
                Column {
                    Text("Paste JSON array of profiles below:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("[{ \"name\": \"My VPN\", \"server\": \"...\" }]") },
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
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
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
