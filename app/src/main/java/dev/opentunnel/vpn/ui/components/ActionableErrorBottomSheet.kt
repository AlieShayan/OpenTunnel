package dev.opentunnel.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.ui.theme.LocalStatusPalette
import dev.opentunnel.vpn.util.Strings

/**
 * Enterprise Actionable Error Bottom Sheet that translates raw technical errors
 * into structured, understandable explanations with direct 1-tap recovery actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionableErrorBottomSheet(
    errorMessage: String,
    appLanguage: AppLanguage,
    onRetry: () -> Unit,
    onEditProfile: () -> Unit,
    onViewLogs: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isRtl = Strings.isRtl(appLanguage)
    val palette = LocalStatusPalette.current
    val scheme = MaterialTheme.colorScheme

    val isAuthError = errorMessage.contains("auth", ignoreCase = true) ||
        errorMessage.contains("password", ignoreCase = true) ||
        errorMessage.contains("credential", ignoreCase = true) ||
        errorMessage.contains("401", ignoreCase = true) ||
        errorMessage.contains("403", ignoreCase = true)

    val isDnsOrNetworkError = errorMessage.contains("dns", ignoreCase = true) ||
        errorMessage.contains("resolve", ignoreCase = true) ||
        errorMessage.contains("unreachable", ignoreCase = true) ||
        errorMessage.contains("timeout", ignoreCase = true) ||
        errorMessage.contains("route", ignoreCase = true)

    val isCertError = errorMessage.contains("cert", ignoreCase = true) ||
        errorMessage.contains("ssl", ignoreCase = true) ||
        errorMessage.contains("tls", ignoreCase = true) ||
        errorMessage.contains("fingerprint", ignoreCase = true)

    val explanation = when {
        isAuthError -> if (isRtl) {
            "سرور اطلاعات ورود شما (نام کاربری، رمز عبور یا توکن) را نپذیرفت. لطفاً مشخصات ذخیره‌شده را بررسی فرمایید."
        } else {
            "The server rejected your credentials (username, password, or security token). Please verify your saved login details."
        }
        isDnsOrNetworkError -> if (isRtl) {
            "امکان برقراری ارتباط با آدرس سرور وجود ندارد. اتصال اینترنت خود یا آدرس دامنه سرور را بررسی کنید."
        } else {
            "Unable to reach the server gateway. Check your internet connection or verify the gateway hostname/IP."
        }
        isCertError -> if (isRtl) {
            "مشکلی در اعتبارسنجی گواهی امنیتی یا پروتکل رمزنگاری سرور رخ داده است."
        } else {
            "A TLS certificate verification or cryptographic handshake issue occurred."
        }
        else -> if (isRtl) {
            "ارتباط با درگاه VPN به دلیل خطای غیرمنتظره متوقف شد."
        } else {
            "The tunnel could not be established due to an unexpected connection error."
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Error icon in red pill
            Surface(
                shape = CircleShape,
                color = palette.error.copy(alpha = 0.16f),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = palette.error,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = Strings.actionableErrorTitle(appLanguage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(14.dp))

            // Raw technical detail container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = scheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = if (isRtl) "پیام فنی هسته ارتباطی:" else "Technical details:",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                        ),
                        color = palette.error,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onRetry()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.retryConnection(appLanguage), fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onEditProfile()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.editCredentials(appLanguage), fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = {
                        onDismiss()
                        onViewLogs()
                    }) {
                        Icon(Icons.Rounded.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.viewErrorLogs(appLanguage))
                    }

                    TextButton(onClick = onDismiss) {
                        Text(Strings.dismiss(appLanguage), color = scheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
