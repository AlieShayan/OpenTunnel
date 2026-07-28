package dev.opentunnel.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.data.SplitTunnelMode
import dev.opentunnel.vpn.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelNetworksScreen(
    settings: AppSettings,
    onToggleEnabled: (Boolean) -> Unit,
    onChangeMode: (SplitTunnelMode) -> Unit,
    onAddNetwork: (String) -> Unit,
    onRemoveNetwork: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
) {
    val lang = settings.appLanguage
    val isPersian = lang == dev.opentunnel.vpn.data.AppLanguage.PERSIAN
    val context = LocalContext.current

    var newEntryText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isPersian) "تونل‌سازی شبکه‌ها و سایت‌ها" else "Network & Site Split Tunneling",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        dev.opentunnel.vpn.util.HapticHelper.performClick(context, settings.hapticFeedbackEnabled)
                        onBack()
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (settings.splitTunnelNetworks.isNotEmpty()) {
                        TextButton(onClick = {
                            dev.opentunnel.vpn.util.HapticHelper.performClick(context, settings.hapticFeedbackEnabled)
                            onClearAll()
                        }) {
                            Text(
                                text = if (isPersian) "پاکسازی همه" else "Clear All",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) "فعال‌سازی تونل‌سازی شبکه" else "Enable Network Split Tunneling",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (isPersian)
                                    "مدیریت ترافیک بر اساس رنج آدرس IP یا پسوند دامنه‌ها"
                                else
                                    "Route traffic by IP subnet ranges or domain suffixes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.splitTunnelNetworksEnabled,
                            onCheckedChange = {
                                dev.opentunnel.vpn.util.HapticHelper.performClick(context, settings.hapticFeedbackEnabled)
                                onToggleEnabled(it)
                            },
                        )
                    }
                }
            }

            if (settings.splitTunnelNetworksEnabled) {
                item {
                    SectionCard(title = if (isPersian) "حالت تونل‌سازی" else "Split Tunnel Mode") {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = settings.splitTunnelNetworksMode == SplitTunnelMode.EXCLUDE_SELECTED,
                                    onClick = {
                                        dev.opentunnel.vpn.util.HapticHelper.performClick(context, settings.hapticFeedbackEnabled)
                                        onChangeMode(SplitTunnelMode.EXCLUDE_SELECTED)
                                    },
                                    label = { Text(if (isPersian) "استثناء کردن شبکه‌های انتخابی" else "Bypass selected networks") },
                                )
                                FilterChip(
                                    selected = settings.splitTunnelNetworksMode == SplitTunnelMode.INCLUDE_SELECTED,
                                    onClick = {
                                        dev.opentunnel.vpn.util.HapticHelper.performClick(context, settings.hapticFeedbackEnabled)
                                        onChangeMode(SplitTunnelMode.INCLUDE_SELECTED)
                                    },
                                    label = { Text(if (isPersian) "فقط شبکه‌های انتخابی" else "Only include selected") },
                                )
                            }
                            Text(
                                text = if (settings.splitTunnelNetworksMode == SplitTunnelMode.EXCLUDE_SELECTED) {
                                    if (isPersian) "همه ترافیک از VPN می‌گذرد به جز شبکه‌ها/سایت‌های لیست شده."
                                    else "All traffic routes through VPN except listed networks/domains."
                                } else {
                                    if (isPersian) "فقط شبکه‌ها/سایت‌های لیست شده از VPN می‌گذرند."
                                    else "Only listed networks/domains route through VPN."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    SectionCard(title = if (isPersian) "افزودن شبکه یا دامنه" else "Add Network or Domain") {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = newEntryText,
                                    onValueChange = { newEntryText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text(if (isPersian) "مثال: *.ir یا 192.168.1.0/24" else "e.g. *.ir or 192.168.1.0/24")
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                Button(
                                    onClick = {
                                        if (newEntryText.isNotBlank()) {
                                            dev.opentunnel.vpn.util.HapticHelper.performClick(context, settings.hapticFeedbackEnabled)
                                            onAddNetwork(newEntryText)
                                            newEntryText = ""
                                        }
                                    },
                                    enabled = newEntryText.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                                }
                            }
                            Text(
                                text = if (isPersian)
                                    "راهنما: برای استثناء کردن تمام سایت‌های یک پسوند، از * استفاده کنید (مثلا *.ir شامل تمامی دامنه و زیردامنه‌های .ir می‌شود)."
                                else
                                    "Tip: Use wildcard * to match domain suffixes (e.g. *.ir matches all .ir websites and subdomains).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = if (isPersian) "شبکه‌ها و دامنه‌های اضافه شده (${settings.splitTunnelNetworks.size})"
                        else "Added Networks & Domains (${settings.splitTunnelNetworks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (settings.splitTunnelNetworks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isPersian) "هیچ شبکه یا دامنه‌ای اضافه نشده است" else "No networks or domains added yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(settings.splitTunnelNetworks.toList()) { net ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (net.contains("/")) Icons.Rounded.Public else Icons.Rounded.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = net,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                IconButton(onClick = {
                                    dev.opentunnel.vpn.util.HapticHelper.performClick(context, settings.hapticFeedbackEnabled)
                                    onRemoveNetwork(net)
                                }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
