package com.openclaw.assistant.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.assistant.ChatActivity
import com.openclaw.assistant.PermissionInfo
import com.openclaw.assistant.PermissionStatusInfo
import com.openclaw.assistant.R
import com.openclaw.assistant.service.OpenClawAssistantService
import com.openclaw.assistant.speech.diagnostics.DiagnosticStatus
import com.openclaw.assistant.speech.diagnostics.VoiceDiagnostic

@Composable
fun OperatorOfflineCard(deviceId: String) {
    val context = LocalContext.current
    val command = context.getString(R.string.operator_offline_command, deviceId)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.operator_offline_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.operator_offline_message),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Command", command)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.operator_offline_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.operator_offline_copy), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SystemStatusCard(
    connected: Boolean,
    statusText: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val backgroundColor = if (connected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (connected) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    val statusDotColor = if (connected) Color(0xFF4CAF50) else Color(0xFFF44336)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = contentColor.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = contentColor,
                            containerColor = Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.5f))
                    ) {
                        Text(stringResource(R.string.disconnect))
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CapabilityCard(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (statusText != null) {
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
fun DiagnosticPanel(diagnostic: VoiceDiagnostic, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.diagnostic_engines), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DiagnosticItem(label = "In: ${diagnostic.sttEngine?.take(10) ?: "Def"}", status = diagnostic.sttStatus, modifier = Modifier.weight(1f))
                DiagnosticItem(label = "Out: ${diagnostic.ttsEngine?.split('.')?.lastOrNull() ?: "null"}", status = diagnostic.ttsStatus, modifier = Modifier.weight(1f))
            }
            if (diagnostic.suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                diagnostic.suggestions.forEach { SuggestionItem(it) }
            }
        }
    }
}

@Composable
fun PermissionDiagnosticsPanel(allPermissionsStatus: List<PermissionStatusInfo>, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.diagnostic_app_permissions), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            allPermissionsStatus.forEach { perm ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = if (perm.isGranted) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val icon = if (perm.isGranted) Icons.Default.Check else Icons.Default.Close
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(perm.permissionName, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(
                        if (perm.isGranted) stringResource(R.string.permission_status_granted)
                        else stringResource(R.string.permission_status_denied),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticItem(label: String, status: DiagnosticStatus, modifier: Modifier = Modifier) {
    val color = when (status) { DiagnosticStatus.READY -> Color(0xFF4CAF50); DiagnosticStatus.WARNING -> Color(0xFFFFC107); DiagnosticStatus.ERROR -> Color(0xFFF44336) }
    val icon = when (status) { DiagnosticStatus.READY -> Icons.Default.Check; DiagnosticStatus.WARNING -> Icons.Default.Info; DiagnosticStatus.ERROR -> Icons.Default.Error }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SuggestionItem(suggestion: com.openclaw.assistant.speech.diagnostics.DiagnosticSuggestion) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = suggestion.message, modifier = Modifier.weight(1f), fontSize = 12.sp, lineHeight = 16.sp)
            if (suggestion.actionLabel != null && suggestion.intent != null) {
                TextButton(onClick = { try { context.startActivity(suggestion.intent) } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.state_error), Toast.LENGTH_SHORT).show() } }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(suggestion.actionLabel, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun CompactActionCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, description: String, isActive: Boolean = false, onClick: (() -> Unit)? = null, showSwitch: Boolean = false, switchValue: Boolean = false, onSwitchChange: ((Boolean) -> Unit)? = null, showInfoIcon: Boolean = false, onInfoClick: (() -> Unit)? = null) {
    Card(modifier = modifier, onClick = { onClick?.invoke() }, enabled = onClick != null && !showSwitch, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    if (showInfoIcon) Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).clickable { onInfoClick?.invoke() })
                    if (showSwitch) Switch(checked = switchValue, onCheckedChange = onSwitchChange, modifier = Modifier.scale(0.8f).offset(y = (-8).dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Text(text = description, fontSize = 12.sp, color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun MissingScopeCard(error: String, onClick: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permission_error_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.permission_error_desc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                    if (!expanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.permission_error_action),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                // Fix Request Section
                Text(
                    text = stringResource(R.string.fix_request_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                val fixMessage = stringResource(R.string.fix_request_message)
                Text(
                    text = fixMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(context, ChatActivity::class.java).apply {
                            putExtra("EXTRA_PREFILL_TEXT", fixMessage)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_ask_ai))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.technical_details),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Error", error)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.error_copied, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_copy_error))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    missingPermissions: List<PermissionInfo>,
    onRequestPermissions: (List<String>) -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.permissions_missing_title),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            missingPermissions.forEach { perm ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(perm.nameResId),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = stringResource(perm.descResId),
                            fontSize = 12.sp,
                            color = Color(0xFF795548)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpenAppSettings) {
                    Text(stringResource(R.string.open_settings), color = Color(0xFFE65100))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onRequestPermissions(missingPermissions.map { it.permission }) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text(stringResource(R.string.permission_grant))
                }
            }
        }
    }
}

@Composable
fun WarningCard(message: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, color = Color(0xFFE65100), modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFFF9800))
        }
    }
}

@Composable
fun UsageStep(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Text(text = number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp)
    }
}

@Composable
fun HowToUseDialog(displayWakeWord: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.how_to_use)) },
        text = {
            Column {
                (1..4).forEach { i ->
                    val resId = context.resources.getIdentifier("step_$i", "string", context.packageName)
                    val text = if (i == 1) {
                        stringResource(resId, displayWakeWord)
                    } else {
                        stringResource(resId)
                    }
                    UsageStep(i.toString(), text)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.got_it)) } }
    )
}

@Composable
fun TroubleshootingDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.assist_gesture_not_working)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.troubleshooting_intro), fontSize = 14.sp)
            listOf("circle_to_search", "gesture_navigation", "google_app_setting", "refresh_binding").forEach { key ->
                val titleId = context.resources.getIdentifier("${key}_title", "string", context.packageName)
                val descId = context.resources.getIdentifier("${key}_desc", "string", context.packageName)
                BulletPoint(stringResource(titleId), stringResource(descId))
            }
            Spacer(modifier = Modifier.height(8.dp)); HorizontalDivider(); Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { context.startService(Intent(context, OpenClawAssistantService::class.java).apply { action = OpenClawAssistantService.ACTION_SHOW_ASSISTANT }) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text(stringResource(R.string.debug_force_start)) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.got_it)) } })
}

@Composable
fun BulletPoint(title: String, desc: String) {
    Column { Text("• $title", fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(desc, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp)) }
}
