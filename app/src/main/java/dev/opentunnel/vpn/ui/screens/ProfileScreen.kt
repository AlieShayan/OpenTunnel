package dev.opentunnel.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.data.VpnProfile
import dev.opentunnel.vpn.ui.components.SectionCard

private val REPORTED_OS = listOf(
    "android" to "Android (honest)",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: VpnProfile,
    onSave: (VpnProfile) -> Unit,
    onForgetCertificate: () -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember(profile) { mutableStateOf(profile) }
    var showPassword by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VPN profile") },
                navigationIcon = {
                    IconButton(onClick = { onSave(draft); onBack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(draft); onBack() }) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Save")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionCard(title = "Gateway") {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = draft.server,
                        onValueChange = { draft = draft.copy(server = it) },
                        label = { Text("Server address") },
                        placeholder = { Text("vpn.company.com") },
                        supportingText = {
                            Text("Hostname, host:port, or a full https:// URL with the group path")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        label = { Text("Display name (optional)") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SectionCard(title = "Credentials") {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
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
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.authGroup,
                        onValueChange = { draft = draft.copy(authGroup = it) },
                        label = { Text("Auth group (optional)") },
                        supportingText = { Text("Only needed when the gateway shows a group dropdown") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Remember password", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Stored encrypted with a hardware-backed key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = draft.savePassword,
                            onCheckedChange = { draft = draft.copy(savePassword = it) },
                        )
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
                    SectionCard(title = "Protocol") {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            LabelledDropdown(
                                label = "VPN protocol",
                                options = PROTOCOLS,
                                selected = draft.protocol,
                                onSelected = { draft = draft.copy(protocol = it) },
                            )
                            LabelledDropdown(
                                label = "Report this OS to the gateway",
                                options = REPORTED_OS,
                                selected = draft.reportedOs,
                                onSelected = { draft = draft.copy(reportedOs = it) },
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
                                title = "Disable XML POST",
                                subtitle = "Compatibility shim for some non-Cisco gateways",
                                checked = draft.disableXmlPost,
                                onCheckedChange = { draft = draft.copy(disableXmlPost = it) },
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
                                NumberField(
                                    label = "Dead peer detection (seconds)",
                                    value = draft.dpdSeconds,
                                    placeholder = "Gateway default",
                                    onValueChange = { draft = draft.copy(dpdSeconds = it) },
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
}

@Composable
private fun ToggleLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
