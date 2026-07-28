package dev.opentunnel.vpn.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.data.VpnProfile
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.util.Strings
import dev.opentunnel.vpn.ui.components.SectionCard

import dev.opentunnel.vpn.util.HapticHelper
import dev.opentunnel.vpn.util.RememberScrollHaptic

private val REPORTED_OS = listOf(
    "android" to "Android",
    "linux-64" to "Linux 64-bit",
    "linux" to "Linux 32-bit",
    "win" to "Windows",
    "mac-intel" to "macOS",
    "apple-ios" to "iOS",
)

private val PROTOCOLS = listOf(
    "anyconnect" to "Cisco AnyConnect",
    "nc" to "Juniper Network Connect",
    "pulse" to "Ivanti / Pulse Connect Secure",
    "gp" to "Palo Alto GlobalProtect",
    "f5" to "F5 BIG-IP",
    "fortinet" to "Fortinet FortiGate",
    "array" to "Array Networks",
)

private val SOFTWARE_TOKENS = listOf(
    "0" to "Disabled",
    "1" to "RSA SecurID (stoken)",
    "2" to "TOTP (Google Auth, etc.)",
    "3" to "HOTP (Counter-based)",
)

private val SPLIT_TUNNEL_MODES = listOf(
    "auto" to "Auto",
    "exclude" to "Exclude selected apps",
    "include" to "Only include selected apps",
)

private val BATCH_MODES = listOf(
    "disabled" to "Disabled",
    "enabled" to "Enabled",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: VpnProfile,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedbackEnabled: Boolean = true,
    onSave: (VpnProfile) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onForgetCertificate: () -> Unit,
    onBack: () -> Unit,
) {
    val lang = appLanguage
    var draft by remember(profile) { mutableStateOf(profile) }
    var showPassword by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    RememberScrollHaptic(scrollState, hapticFeedbackEnabled)

    val caCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { draft = draft.copy(caCertPath = it.toString()) }
    }
    val userCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { draft = draft.copy(userCertPath = it.toString()) }
    }
    val privateKeyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { draft = draft.copy(privateKeyPath = it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.editProfileTitle(lang, draft.name)) },
                navigationIcon = {
                    IconButton(onClick = { onSave(draft); onBack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (draft.id.isNotBlank() && onDelete != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = Strings.delete(lang),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(onClick = { onSave(draft); onBack() }) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(Strings.save(lang))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionCard(title = Strings.serverSection(lang)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        label = { Text(Strings.profileName(lang)) },
                        placeholder = { Text("e.g. Work VPN") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.server,
                        onValueChange = { draft = draft.copy(server = it) },
                        label = { Text(Strings.serverAddress(lang)) },
                        placeholder = { Text("vpn.example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.caCertPath,
                        onValueChange = { draft = draft.copy(caCertPath = it) },
                        label = { Text(Strings.caCertificate(lang)) },
                        placeholder = { Text("Select or enter CA cert file path") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { caCertLauncher.launch("*/*") }) {
                                Icon(
                                    Icons.Rounded.FolderOpen,
                                    contentDescription = Strings.selectFile(lang),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.userCertPath,
                        onValueChange = { draft = draft.copy(userCertPath = it) },
                        label = { Text(Strings.userCertificate(lang)) },
                        placeholder = { Text("Client certificate path") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { userCertLauncher.launch("*/*") }) {
                                Icon(
                                    Icons.Rounded.FolderOpen,
                                    contentDescription = Strings.selectFile(lang),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.privateKeyPath,
                        onValueChange = { draft = draft.copy(privateKeyPath = it) },
                        label = { Text(Strings.privateKey(lang)) },
                        placeholder = { Text("Private key path") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { privateKeyLauncher.launch("*/*") }) {
                                Icon(
                                    Icons.Rounded.FolderOpen,
                                    contentDescription = Strings.selectFile(lang),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LabelledDropdown(
                        label = "Software token",
                        options = SOFTWARE_TOKENS,
                        selected = draft.softwareTokenMode.toString(),
                        onSelected = { draft = draft.copy(softwareTokenMode = it.toIntOrNull() ?: 0) },
                    )

                    if (draft.softwareTokenMode > 0) {
                        OutlinedTextField(
                            value = draft.tokenString,
                            onValueChange = { draft = draft.copy(tokenString = it) },
                            label = { Text("Token string") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    OutlinedTextField(
                        value = draft.username,
                        onValueChange = { draft = draft.copy(username = it) },
                        label = { Text("Username") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.password,
                        onValueChange = { draft = draft.copy(password = it) },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CheckboxLine(
                        title = "Disable credential caching",
                        subtitle = "Never cache login names, user groups, or passwords",
                        checked = draft.disableCredentialCaching,
                        hapticEnabled = hapticFeedbackEnabled,
                        onCheckedChange = { draft = draft.copy(disableCredentialCaching = it) },
                    )

                    TextButton(onClick = { draft = draft.copy(password = "") }) {
                        Text("Clear saved passwords", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(if (showAdvanced) "Hide advanced options" else "Advanced options")
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SectionCard(title = "Advanced") {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            LabelledDropdown(
                                label = "Batch mode",
                                options = BATCH_MODES,
                                selected = if (draft.batchMode) "enabled" else "disabled",
                                onSelected = { draft = draft.copy(batchMode = (it == "enabled")) },
                            )

                            LabelledDropdown(
                                label = "Reported OS",
                                options = REPORTED_OS,
                                selected = draft.reportedOs,
                                onSelected = { draft = draft.copy(reportedOs = it) },
                            )

                            OutlinedTextField(
                                value = draft.csdWrapper,
                                onValueChange = { draft = draft.copy(csdWrapper = it) },
                                label = { Text("Custom CSD wrapper") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            LabelledDropdown(
                                label = "Split tunnel mode",
                                options = SPLIT_TUNNEL_MODES,
                                selected = draft.profileSplitTunnelMode,
                                onSelected = { draft = draft.copy(profileSplitTunnelMode = it) },
                            )

                            OutlinedTextField(
                                value = draft.splitTunnelNetworks,
                                onValueChange = { draft = draft.copy(splitTunnelNetworks = it) },
                                label = { Text("Split tunnel networks") },
                                placeholder = { Text("e.g. 192.168.1.0/24, 10.0.0.0/8") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            CheckboxLine(
                                title = "Disable XML POST",
                                subtitle = "Use the old authentication handshake; may fail on newer servers",
                                checked = draft.disableXmlPost,
                                hapticEnabled = hapticFeedbackEnabled,
                                onCheckedChange = { draft = draft.copy(disableXmlPost = it) },
                            )

                            CheckboxLine(
                                title = "Require PFS",
                                subtitle = "Only negotiate cipher suites with Perfect Forward Secrecy",
                                checked = draft.requirePfs,
                                hapticEnabled = hapticFeedbackEnabled,
                                onCheckedChange = { draft = draft.copy(requirePfs = it) },
                            )

                            CheckboxLine(
                                title = "Override DPD timeout",
                                subtitle = "Use a custom Dead Peer Detection timeout instead of the server default",
                                checked = draft.overrideDpdTimeout,
                                hapticEnabled = hapticFeedbackEnabled,
                                onCheckedChange = { draft = draft.copy(overrideDpdTimeout = it) },
                            )

                            if (draft.overrideDpdTimeout) {
                                NumberField(
                                    label = "DPD timeout (seconds)",
                                    value = draft.dpdSeconds,
                                    placeholder = "30",
                                    onValueChange = { draft = draft.copy(dpdSeconds = it) },
                                )
                            }

                            LabelledDropdown(
                                label = "VPN protocol",
                                options = PROTOCOLS,
                                selected = draft.protocol,
                                onSelected = { draft = draft.copy(protocol = it) },
                            )

                            OutlinedTextField(
                                value = draft.userAgent,
                                onValueChange = { draft = draft.copy(userAgent = it) },
                                label = { Text("User agent override (optional)") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    SectionCard(title = "Transport") {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            ToggleLine(
                                title = "Use DTLS",
                                subtitle = "UDP data channel — much faster when the network allows it",
                                checked = draft.enableDtls,
                                onCheckedChange = { draft = draft.copy(enableDtls = it) },
                            )
                            ToggleLine(
                                title = "Enable IPv6",
                                subtitle = "Turn off if your gateway advertises broken IPv6",
                                checked = draft.enableIpv6,
                                onCheckedChange = { draft = draft.copy(enableIpv6 = it) },
                            )
                            ToggleLine(
                                title = "Allow legacy ciphers",
                                subtitle = "Only for very old gateways. Weakens the connection.",
                                checked = draft.allowInsecureCrypto,
                                onCheckedChange = { draft = draft.copy(allowInsecureCrypto = it) },
                            )
                            ToggleLine(
                                title = "WiFi compatibility mode",
                                subtitle = "Enable when connecting through a mobile hotspot or public WiFi that blocks VPN connections",
                                checked = draft.wifiCompatMode,
                                onCheckedChange = { draft = draft.copy(wifiCompatMode = it) },
                            )
                            Column(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                NumberField(
                                    label = "MTU override",
                                    value = draft.mtu,
                                    placeholder = "Automatic",
                                    onValueChange = { draft = draft.copy(mtu = it) },
                                )
                            }
                        }
                    }

                    if (draft.trustedCertificate.isNotBlank()) {
                        SectionCard(title = "Pinned certificate") {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    draft.trustedCertificate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = {
                                    draft = draft.copy(trustedCertificate = "")
                                    onForgetCertificate()
                                }) {
                                    Text("Forget this certificate")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete profile “${draft.displayName}”?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(draft.id)
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CheckboxLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    hapticEnabled: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val toggleAction: (Boolean) -> Unit = { newChecked ->
        if (hapticEnabled) {
            HapticHelper.performClick(context, true)
        }
        onCheckedChange(newChecked)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { toggleAction(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = toggleAction)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    hapticEnabled: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val toggleAction: (Boolean) -> Unit = { newChecked ->
        if (hapticEnabled) {
            HapticHelper.performClick(context, true)
        }
        onCheckedChange(newChecked)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { toggleAction(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = toggleAction)
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    placeholder: String,
    onValueChange: (Int) -> Unit,
) {
    OutlinedTextField(
        value = if (value > 0) value.toString() else "",
        onValueChange = { text ->
            onValueChange(text.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelledDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
