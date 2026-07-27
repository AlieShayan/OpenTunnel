package dev.opentunnel.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import dev.opentunnel.vpn.core.PromptField
import dev.opentunnel.vpn.core.PromptFieldType
import dev.opentunnel.vpn.core.UserPrompt
import dev.opentunnel.vpn.util.Formatters

@Composable
fun PromptHost(
    prompt: UserPrompt?,
    onSubmit: (Map<String, String>) -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    when (prompt) {
        null -> Unit
        is UserPrompt.Auth -> AuthPromptDialog(prompt, onSubmit, onCancel)
        is UserPrompt.CertTrust -> CertTrustDialog(prompt, onAccept, onCancel)
    }
}

@Composable
private fun AuthPromptDialog(
    prompt: UserPrompt.Auth,
    onSubmit: (Map<String, String>) -> Unit,
    onCancel: () -> Unit,
) {
    val values = remember(prompt.id) {
        mutableStateMapOf<String, String>().apply {
            prompt.fields.forEach { field ->
                put(field.name, field.prefill.ifEmpty { field.choices.firstOrNull()?.value ?: "" })
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(prompt.title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                prompt.banner?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                prompt.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                prompt.error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                prompt.fields.forEachIndexed { index, field ->
                    PromptFieldEditor(
                        field = field,
                        value = values[field.name].orEmpty(),
                        isLast = index == prompt.fields.lastIndex,
                        onValueChange = { values[field.name] = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(values.toMap()) }) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun PromptFieldEditor(
    field: PromptField,
    value: String,
    isLast: Boolean,
    onValueChange: (String) -> Unit,
) {
    when (field.type) {
        PromptFieldType.SELECT -> {
            var expanded by remember { mutableStateOf(false) }
            val currentLabel = field.choices.firstOrNull { it.value == value }?.label ?: value

            Column(Modifier.fillMaxWidth()) {
                Text(
                    field.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { expanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(currentLabel, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    field.choices.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice.label) },
                            onClick = {
                                onValueChange(choice.value)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        PromptFieldType.PASSWORD -> {
            var visible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                singleLine = true,
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                ),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PromptFieldType.TEXT -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CertTrustDialog(
    prompt: UserPrompt.CertTrust,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    var showDetails by remember(prompt.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                Icons.Rounded.GppMaybe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
        },
        title = { Text("Untrusted server certificate") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "${prompt.host} presented a certificate Android does not trust.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    prompt.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "Only continue if you recognise this fingerprint. It will be pinned, " +
                        "and you will be asked again if it ever changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Formatters.fingerprint(prompt.fingerprint),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                )
                if (prompt.details.isNotBlank()) {
                    TextButton(onClick = { showDetails = !showDetails }) {
                        Text(if (showDetails) "Hide certificate" else "Show certificate")
                    }
                    if (showDetails) {
                        Text(
                            text = prompt.details,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Trust and connect") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
        shape = MaterialTheme.shapes.large,
    )
}
