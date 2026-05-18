package com.openclaw.assistant.ui.backend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.openclaw.assistant.R
import com.openclaw.assistant.backend.AgentBackendConfig
import com.openclaw.assistant.backend.AgentClientFactory
import com.openclaw.assistant.backend.BackendManager
import com.openclaw.assistant.backend.BackendType
import com.openclaw.assistant.backend.ConnectionTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home overview for the two products Agent Voice can talk to. This deliberately
 * avoids exposing transport names as the primary concept: users choose between
 * OpenClaw and Hermes, while Gateway/HTTP/API details stay secondary.
 */
@Composable
fun PrimaryBackendCard(
    openClawConnected: Boolean = false,
    openClawStatusText: String = "",
) {
    val context = LocalContext.current
    val manager = remember { BackendManager.getInstance(context) }
    val backends by manager.backends.collectAsState()
    val openClaw = remember(backends) { backends.preferredOpenClawBackend() }
    val hermes = remember(backends) { backends.preferredHermesBackend() }
    var openClawTest by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var hermesTest by remember { mutableStateOf<ConnectionTestResult?>(null) }
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.av_home_connections_title),
                style = MaterialTheme.typography.titleMedium,
            )
            BackendProductRow(
                name = "OpenClaw",
                config = openClaw,
                connected = openClawConnected,
                statusText = when {
                    openClawConnected -> if (openClawStatusText.isBlank()) stringResource(R.string.av_home_connected) else openClawStatusText
                    openClaw != null -> stringResource(R.string.av_home_configured_not_connected)
                    else -> stringResource(R.string.av_home_not_configured)
                },
                testResult = openClawTest,
                onTest = openClaw?.let { config ->
                    {
                        scope.launch {
                            openClawTest = ConnectionTestResult(false, context.getString(R.string.av_connection_testing))
                            openClawTest = withContext(Dispatchers.IO) { AgentClientFactory.create(config).testConnection() }
                        }
                    }
                },
            )
            BackendProductRow(
                name = "Hermes",
                config = hermes,
                connected = hermesTest?.ok == true,
                statusText = when {
                    hermesTest?.ok == true -> stringResource(R.string.av_home_reachable)
                    hermes != null -> stringResource(R.string.av_home_configured)
                    else -> stringResource(R.string.av_home_not_configured)
                },
                testResult = hermesTest,
                onTest = hermes?.let { config ->
                    {
                        scope.launch {
                            hermesTest = ConnectionTestResult(false, context.getString(R.string.av_connection_testing))
                            hermesTest = withContext(Dispatchers.IO) { AgentClientFactory.create(config).testConnection() }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun BackendProductRow(
    name: String,
    config: AgentBackendConfig?,
    connected: Boolean,
    statusText: String,
    testResult: ConnectionTestResult?,
    onTest: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                StatusDot(connected = connected, configured = config != null)
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (config?.isPrimary == true) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.av_home_primary_chip)) })
                }
            }
            config?.let {
                Text(
                    text = stringResource(R.string.av_home_connection_detail, it.displayName, kindShort(it.type)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            testResult?.let {
                Text(
                    text = if (it.ok) {
                        stringResource(R.string.av_home_test_ok, it.message, it.latencyMs ?: 0)
                    } else {
                        stringResource(R.string.av_home_test_failed, it.message)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            if (onTest != null) {
                OutlinedButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.av_home_test_connection))
                }
            }
        }
    }
}

@Composable
private fun StatusDot(connected: Boolean, configured: Boolean) {
    val color = when {
        connected -> Color(0xFF34C759)
        configured -> Color(0xFFFFB020)
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(12.dp)) {
        Spacer(modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun kindShort(type: BackendType) = when (type) {
    BackendType.HERMES_API_SERVER -> stringResource(R.string.av_backend_kind_hermes)
    BackendType.OPENCLAW_GATEWAY -> stringResource(R.string.av_backend_kind_gateway)
    BackendType.OPENCLAW_HTTP -> stringResource(R.string.av_backend_kind_http)
}

private fun List<AgentBackendConfig>.preferredOpenClawBackend(): AgentBackendConfig? {
    val enabled = filter { it.enabled && (it.type == BackendType.OPENCLAW_GATEWAY || it.type == BackendType.OPENCLAW_HTTP) }
    return enabled.firstOrNull { it.isPrimary }
        ?: enabled.firstOrNull { it.type == BackendType.OPENCLAW_GATEWAY }
        ?: enabled.firstOrNull()
}

private fun List<AgentBackendConfig>.preferredHermesBackend(): AgentBackendConfig? {
    val enabled = filter { it.enabled && it.type == BackendType.HERMES_API_SERVER }
    return enabled.firstOrNull { it.isPrimary } ?: enabled.firstOrNull()
}
