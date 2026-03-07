package com.openclaw.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.openclaw.assistant.MainActivity
import com.openclaw.assistant.OpenClawApplication
import com.openclaw.assistant.R
import com.openclaw.assistant.LocationMode
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.speech.diagnostics.VoiceDiagnostic
import com.openclaw.assistant.ui.components.CapabilityCard
import com.openclaw.assistant.ui.components.CollapsibleSection
import com.openclaw.assistant.ui.components.CompactActionCard
import com.openclaw.assistant.ui.components.DiagnosticItem
import com.openclaw.assistant.ui.components.DiagnosticPanel
import com.openclaw.assistant.ui.components.HowToUseDialog
import com.openclaw.assistant.ui.components.MissingScopeCard
import com.openclaw.assistant.ui.components.OperatorOfflineCard
import com.openclaw.assistant.ui.components.PairingRequiredCard
import com.openclaw.assistant.ui.components.PermissionDiagnosticsPanel
import com.openclaw.assistant.ui.components.PermissionStatusCard
import com.openclaw.assistant.ui.components.SuggestionItem
import com.openclaw.assistant.ui.components.SystemStatusCard
import com.openclaw.assistant.ui.components.TroubleshootingDialog
import com.openclaw.assistant.ui.components.WarningCard
import com.openclaw.assistant.ChatActivity
import com.openclaw.assistant.SessionListActivity
import com.openclaw.assistant.PermissionInfo
import com.openclaw.assistant.PermissionStatusInfo
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ConnectTabScreen(
    settings: SettingsRepository,
    diagnostic: VoiceDiagnostic?,
    missingPermissions: List<PermissionInfo> = emptyList(),
    allPermissionsStatus: List<PermissionStatusInfo> = emptyList(),
    onRefreshDiagnostics: () -> Unit,
    onRequestPermissions: (List<String>) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val runtime = remember(context.applicationContext) {
        (context.applicationContext as OpenClawApplication).nodeRuntime
    }

    var isConfigured by remember { mutableStateOf(settings.isConfigured()) }
    var hotwordEnabled by remember { mutableStateOf(settings.hotwordEnabled) }
    val displayWakeWord = settings.getWakeWordDisplayName()
    var isAssistantSet by remember { mutableStateOf((context as? MainActivity)?.isAssistantActive() ?: false) }
    val nodeConnected by runtime.isConnected.collectAsState()
    val nodeStatusText by runtime.statusText.collectAsState()
    var showTroubleshooting by rememberSaveable { mutableStateOf(false) }
    var showHowToUse by rememberSaveable { mutableStateOf(false) }
    var showLocationInfo by rememberSaveable { mutableStateOf(false) }

    var showScreenCaptureDialog by remember { mutableStateOf(false) }
    val smsEnabled by runtime.smsEnabled.collectAsState()
    val screenRecordActive by runtime.screenRecordActive.collectAsState()

    val isPairingRequired by runtime.isPairingRequired.collectAsState()
    val isOperatorOffline by runtime.isOperatorOffline.collectAsState()
    val deviceId = runtime.deviceId
    val displayName by runtime.displayName.collectAsState()
    val pendingGatewayTrust by runtime.pendingGatewayTrust.collectAsState()
    val missingScopeError by runtime.missingScopeError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pendingGatewayTrust != null) {
            GatewayTrustDialog(
                prompt = pendingGatewayTrust!!,
                onAccept = { runtime.acceptGatewayTrustPrompt() },
                onDecline = { runtime.declineGatewayTrustPrompt() }
            )
        }

        if (missingScopeError != null) {
            MissingScopeCard(error = missingScopeError!!, onClick = onOpenSettings)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isPairingRequired && deviceId != null) {
            PairingRequiredCard(deviceId = deviceId, displayName = displayName)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isOperatorOffline && deviceId != null) {
            OperatorOfflineCard(deviceId = deviceId)
            Spacer(modifier = Modifier.height(16.dp))
        }

        val displayStatusText = when (nodeStatusText) {
            "Operator Online (Node Offline)" -> stringResource(R.string.status_operator_online_node_offline)
            "Node Online (Operator Offline)" -> stringResource(R.string.status_node_online_operator_offline)
            "Offline" -> stringResource(R.string.status_offline)
            else -> nodeStatusText
        }

        SystemStatusCard(
            connected = nodeConnected,
            statusText = displayStatusText,
            onConnect = { runtime.connectManual() },
            onDisconnect = { runtime.disconnect() },
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.permissions_for_openclaw_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val cameraEnabled by runtime.cameraEnabled.collectAsState()
        val locationMode by runtime.locationMode.collectAsState()
        val locationPrecise by runtime.locationPreciseEnabled.collectAsState()

        val hasTelephony = remember { runtime.sms.hasTelephonyFeature() }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CapabilityCard(
                icon = Icons.Default.PhotoCamera,
                label = stringResource(R.string.capability_camera),
                isActive = cameraEnabled,
                onClick = {
                    if (!cameraEnabled) {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (granted) runtime.setCameraEnabled(true)
                        else onRequestPermissions(listOf(Manifest.permission.CAMERA))
                    } else {
                        runtime.setCameraEnabled(false)
                    }
                },
                modifier = Modifier.weight(1f)
            )

            val locationStatusText = when {
                locationMode == LocationMode.Off -> stringResource(R.string.location_off)
                locationPrecise -> stringResource(R.string.location_precise)
                else -> stringResource(R.string.location_coarse)
            }
            CapabilityCard(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.capability_location),
                isActive = locationMode != LocationMode.Off,
                statusText = locationStatusText,
                onClick = {
                    if (locationMode == LocationMode.Off) {
                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (coarseGranted || fineGranted) {
                            runtime.setLocationMode(LocationMode.WhileUsing)
                            runtime.setLocationPreciseEnabled(fineGranted)
                        } else {
                            onRequestPermissions(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    } else {
                        runtime.setLocationMode(LocationMode.Off)
                    }
                },
                onLongClick = { showLocationInfo = true },
                modifier = Modifier.weight(1f)
            )

            if (hasTelephony) {
                CapabilityCard(
                    icon = Icons.Default.Sms,
                    label = stringResource(R.string.capability_sms),
                    isActive = smsEnabled,
                    onClick = {
                        if (smsEnabled) {
                            runtime.setSmsEnabled(false)
                        } else {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                            if (granted) runtime.setSmsEnabled(true)
                            else onRequestPermissions(listOf(Manifest.permission.SEND_SMS))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            CapabilityCard(
                icon = if (screenRecordActive) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
                label = stringResource(R.string.capability_screen),
                isActive = screenRecordActive,
                onClick = { showScreenCaptureDialog = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (missingPermissions.isNotEmpty()) {
            PermissionStatusCard(
                missingPermissions = missingPermissions,
                onRequestPermissions = onRequestPermissions,
                onOpenAppSettings = onOpenAppSettings
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (diagnostic != null || allPermissionsStatus.isNotEmpty()) {
            CollapsibleSection(
                title = stringResource(R.string.diagnostics_title),
                initiallyExpanded = diagnostic?.let { it.sttStatus != com.openclaw.assistant.speech.diagnostics.DiagnosticStatus.READY || it.ttsStatus != com.openclaw.assistant.speech.diagnostics.DiagnosticStatus.READY } ?: false
            ) {
                if (diagnostic != null) {
                    DiagnosticPanel(diagnostic, onRefreshDiagnostics)
                }
                if (diagnostic != null && allPermissionsStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (allPermissionsStatus.isNotEmpty()) {
                    PermissionDiagnosticsPanel(allPermissionsStatus, onRefreshDiagnostics)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(text = stringResource(R.string.activation_methods), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.Home,
                title = stringResource(R.string.home_button),
                description = if (isAssistantSet) "ON" else "OFF",
                isActive = isAssistantSet,
                onClick = onOpenAssistantSettings,
                showInfoIcon = true,
                onInfoClick = { showTroubleshooting = true }
            )
            CompactActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = if (hotwordEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                title = stringResource(R.string.wake_word),
                description = "$displayWakeWord (${if (hotwordEnabled) "ON" else "OFF"})",
                showSwitch = true,
                switchValue = hotwordEnabled,
                onSwitchChange = { enabled ->
                    (context as? MainActivity)?.toggleHotwordService(enabled)
                    hotwordEnabled = enabled
                },
                isActive = hotwordEnabled,
                showInfoIcon = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { context.startActivity(Intent(context, SessionListActivity::class.java)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.ChatBubble, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.open_chat), fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        if (!isConfigured && !nodeConnected) {
            Spacer(modifier = Modifier.height(24.dp))
            WarningCard(message = stringResource(R.string.setup_required_hint), onClick = onOpenSettings)
        }
    }

    if (showScreenCaptureDialog) {
        AlertDialog(
            onDismissRequest = { showScreenCaptureDialog = false },
            title = { Text(stringResource(R.string.screen_capture_title)) },
            text = { Text(stringResource(R.string.screen_capture_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showScreenCaptureDialog = false
                    runtime.setScreenRecordActive(!screenRecordActive)
                }) {
                    Text(stringResource(if (screenRecordActive) R.string.stop else R.string.start))
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreenCaptureDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTroubleshooting) TroubleshootingDialog(onDismiss = { showTroubleshooting = false })
    if (showHowToUse) HowToUseDialog(displayWakeWord = displayWakeWord, onDismiss = { showHowToUse = false })
    if (showLocationInfo) {
        AlertDialog(
            onDismissRequest = { showLocationInfo = false },
            title = { Text(stringResource(R.string.location_info_title)) },
            text = { Text(stringResource(R.string.location_info_message)) },
            confirmButton = {
                TextButton(onClick = { showLocationInfo = false }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }
}
