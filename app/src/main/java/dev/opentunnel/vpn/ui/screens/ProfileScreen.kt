package dev.opentunnel.vpn.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.data.VpnProfile
import dev.opentunnel.vpn.ui.components.SectionCard
import dev.opentunnel.vpn.util.HapticHelper
import dev.opentunnel.vpn.util.RememberScrollHaptic
import dev.opentunnel.vpn.util.Strings

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
    val context = LocalContext.current
    var draft by remember(profile) { mutableStateOf(profile) }
    var showPassword by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var triedSaving by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    RememberScrollHaptic(scrollState, hapticFeedbackEnabled)

    // Form Validation Rules
    val nameError = draft.name.trim().isBlank()
    val serverError = draft.server.trim().isBlank()
    val mtuError = draft.mtu > 0 && draft.mtu !in 576..1500
    val dpdError = draft.overrideDpdTimeout && draft.dpdSeconds <= 0

    val isFormValid = !nameError && !serverError && !mtuError && !dpdError

    val handleSaveAndExit = {
        triedSaving = true
        if (isFormValid) {
            HapticHelper.performClick(context, hapticFeedbackEnabled)
            onSave(draft.copy(name = draft.name.trim(), server = draft.server.trim()))
            onBack()
        }
    }

    val handleBackNavigation = {
        if (draft == profile) {
            onBack()
        } else if (isFormValid) {
            onSave(draft.copy(name = draft.name.trim(), server = draft.server.trim()))
            onBack()
        } else {
            showDiscardConfirm = true
        }
    }

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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.editProfileTitle(lang, draft.name)) },
                navigationIcon = {
                    IconButton(onClick = handleBackNavigation) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
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
                    TextButton(
                        onClick = handleSaveAndExit,
                        enabled = isFormValid || !triedSaving,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(Strings.save(lang), fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Section 1: Essential Connection Information ──────────────────
            SectionCard(title = if (Strings.isRtl(lang)) "مشخصات سرور و پروتکل" else "Essential Connection") {
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
                        isError = triedSaving && nameError,
                        supportingText = if (triedSaving && nameError) {
                            { Text(Strings.fieldRequired(lang), color = MaterialTheme.colorScheme.error) }
                        } else null,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.server,
                        onValueChange = { draft = draft.copy(server = it) },
                        label = { Text(Strings.serverAddress(lang)) },
                        placeholder = { Text("vpn.example.com") },
                        singleLine = true,
                        isError = triedSaving && serverError,
                        supportingText = if (triedSaving && serverError) {
                            { Text(Strings.fieldRequired(lang), color = MaterialTheme.colorScheme.error) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LabelledDropdown(
                        label = Strings.vpnProtocol(lang),
                        options = PROTOCOLS,
                        selected = draft.protocol,
                        onSelected = { draft = draft.copy(protocol = it) },
                    )
                }
            }

            // ── Section 2: Credentials & Authentication ─────────────────────
            SectionCard(title = if (Strings.isRtl(lang)) "احراز هویت و گواهی‌ها" else "Credentials & Security") {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = draft.username,
                        onValueChange = { draft = draft.copy(username = it) },
                        label = { Text(Strings.username(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = draft.password,
                        onValueChange = { draft = draft.copy(password = it) },
                        label = { Text(Strings.password(lang)) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (draft.password.isNotEmpty()) {
                                    IconButton(onClick = { draft = draft.copy(password = "") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Clear password",
                                        )
                                    }
                                }
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password",
                                    )
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LabelledDropdown(
                        label = Strings.softwareToken(lang),
                        options = SOFTWARE_TOKENS,
                        selected = draft.softwareTokenMode.toString(),
                        onSelected = { draft = draft.copy(softwareTokenMode = it.toIntOrNull() ?: 0) },
                    )

                    if (draft.softwareTokenMode > 0) {
                        OutlinedTextField(
                            value = draft.tokenString,
                            onValueChange = { draft = draft.copy(tokenString = it) },
                            label = { Text(Strings.tokenString(lang)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    OutlinedTextField(
                        value = draft.caCertPath,
                        onValueChange = { draft = draft.copy(caCertPath = it) },
                        label = { Text(Strings.caCertificate(lang)) },
                        placeholder = { Text("Select CA certificate file") },
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
                        placeholder = { Text("Select user client certificate") },
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
                        placeholder = { Text("Select private key file") },
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

                    CheckboxLine(
                        title = Strings.disableCredentialCaching(lang),
                        subtitle = Strings.disableCredentialCachingSub(lang),
                        checked = draft.disableCredentialCaching,
                        hapticEnabled = hapticFeedbackEnabled,
                        onCheckedChange = { draft = draft.copy(disableCredentialCaching = it) },
                    )
                }
            }

            // ── Section 3: Advanced Network & Transport (Collapsible) ────────
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    if (showAdvanced) Strings.hideAdvancedOptions(lang) else Strings.advancedOptions(lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionCard(title = if (Strings.isRtl(lang)) "تنظیمات کانال انتقال داده" else "Transport & Network") {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            ToggleLine(
                                title = "Use DTLS (UDP)",
                                subtitle = "Fast UDP data channel when network allows",
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
                                title = "WiFi compatibility mode",
                                subtitle = "Enable when connecting through mobile hotspot or captive portal",
                                checked = draft.wifiCompatMode,
                                onCheckedChange = { draft = draft.copy(wifiCompatMode = it) },
                            )
                            ToggleLine(
                                title = "Allow legacy ciphers",
                                subtitle = "Only for very old gateways with outdated crypto",
                                checked = draft.allowInsecureCrypto,
                                onCheckedChange = { draft = draft.copy(allowInsecureCrypto = it) },
                            )

                            Column(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                NumberField(
                                    label = "MTU override (576 - 1500)",
                                    value = draft.mtu,
                                    placeholder = "Automatic",
                                    isError = mtuError,
                                    supportingText = if (mtuError) Strings.invalidMtuRange(lang) else null,
                                    onValueChange = { draft = draft.copy(mtu = it) },
                                )
                            }
                        }
                    }

                    SectionCard(title = if (Strings.isRtl(lang)) "پارامترهای پیشرفته هندشیک" else "Handshake & Protocol Tuning") {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            LabelledDropdown(
                                label = Strings.batchMode(lang),
                                options = BATCH_MODES,
                                selected = if (draft.batchMode) "enabled" else "disabled",
                                onSelected = { draft = draft.copy(batchMode = (it == "enabled")) },
                            )

                            LabelledDropdown(
                                label = Strings.reportedOs(lang),
                                options = REPORTED_OS,
                                selected = draft.reportedOs,
                                onSelected = { draft = draft.copy(reportedOs = it) },
                            )

                            OutlinedTextField(
                                value = draft.csdWrapper,
                                onValueChange = { draft = draft.copy(csdWrapper = it) },
                                label = { Text(Strings.csdWrapper(lang)) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            CheckboxLine(
                                title = Strings.disableXmlPost(lang),
                                subtitle = Strings.disableXmlPostSub(lang),
                                checked = draft.disableXmlPost,
                                hapticEnabled = hapticFeedbackEnabled,
                                onCheckedChange = { draft = draft.copy(disableXmlPost = it) },
                            )

                            CheckboxLine(
                                title = Strings.requirePfs(lang),
                                subtitle = Strings.requirePfsSub(lang),
                                checked = draft.requirePfs,
                                hapticEnabled = hapticFeedbackEnabled,
                                onCheckedChange = { draft = draft.copy(requirePfs = it) },
                            )

                            CheckboxLine(
                                title = Strings.overrideDpdTimeout(lang),
                                subtitle = Strings.overrideDpdTimeoutSub(lang),
                                checked = draft.overrideDpdTimeout,
                                hapticEnabled = hapticFeedbackEnabled,
                                onCheckedChange = { draft = draft.copy(overrideDpdTimeout = it) },
                            )

                            if (draft.overrideDpdTimeout) {
                                NumberField(
                                    label = Strings.dpdSeconds(lang),
                                    value = draft.dpdSeconds,
                                    placeholder = "30",
                                    isError = dpdError,
                                    supportingText = if (dpdError) "Must be greater than 0" else null,
                                    onValueChange = { draft = draft.copy(dpdSeconds = it) },
                                )
                            }

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

                    if (draft.trustedCertificate.isNotBlank()) {
                        SectionCard(title = "Pinned Certificate") {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = draft.trustedCertificate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        draft = draft.copy(trustedCertificate = "")
                                        onForgetCertificate()
                                    },
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text("Forget this certificate")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(Strings.delete(lang)) },
            text = { Text(Strings.deleteProfileConfirm(lang, draft.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(draft.id)
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text(Strings.delete(lang), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Strings.cancel(lang))
                }
            },
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(if (Strings.isRtl(lang)) "تغییرات ذخیره نشده" else "Unsaved Changes") },
            text = {
                Text(
                    if (Strings.isRtl(lang)) "پروفایل دارای خطای اعتبارسنجی است. آیا مایل به لغو تغییرات و بازگشت هستید؟"
                    else "The profile has incomplete or invalid fields. Discard changes and leave?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onBack()
                }) {
                    Text(
                        if (Strings.isRtl(lang)) "لغو تغییرات" else "Discard",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(if (Strings.isRtl(lang)) "ادامه ویرایش" else "Keep Editing")
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
    hapticEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticHelper.performClick(context, hapticEnabled)
                onCheckedChange(!checked)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                HapticHelper.performClick(context, hapticEnabled)
                onCheckedChange(it)
            },
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticHelper.performClick(context, true)
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                HapticHelper.performClick(context, true)
                onCheckedChange(it)
            },
        )
    }
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
    val display = options.firstOrNull { it.first == selected }?.second ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    placeholder: String,
    isError: Boolean = false,
    supportingText: String? = null,
    onValueChange: (Int) -> Unit,
) {
    OutlinedTextField(
        value = if (value > 0) value.toString() else "",
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(5)
            onValueChange(digits.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && supportingText != null) {
            { Text(supportingText, color = MaterialTheme.colorScheme.error) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
}
