package com.openclaw.assistant.ui.cron

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openclaw.assistant.R
import com.openclaw.assistant.node.NodeRuntime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronScreen() {
    val viewModel: CronViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    var showAddEditDialog by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<NodeRuntime.CronJob?>(null) }
    var deleteTarget by remember { mutableStateOf<NodeRuntime.CronJob?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh when connection state changes to connected
    LaunchedEffect(isConnected) {
        if (isConnected) viewModel.refresh()
        else viewModel.refresh() // triggers Disconnected state
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cron_title)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cron_refresh))
                    }
                }
            )
        },
        floatingActionButton = {
            if (isConnected) {
                FloatingActionButton(onClick = {
                    editTarget = null
                    showAddEditDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cron_add))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is CronUiState.Disconnected -> CronDisconnectedContent()
                is CronUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CronUiState.Error -> {
                    CronErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
                is CronUiState.Success -> {
                    if (state.jobs.isEmpty()) {
                        CronEmptyContent(
                            onAdd = {
                                editTarget = null
                                showAddEditDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.jobs, key = { it.id }) { job ->
                                CronJobCard(
                                    job = job,
                                    onEdit = {
                                        editTarget = job
                                        showAddEditDialog = true
                                    },
                                    onDelete = { deleteTarget = job },
                                    onToggleEnabled = { enabled ->
                                        viewModel.update(
                                            id = job.id,
                                            name = job.name,
                                            schedule = job.schedule,
                                            command = job.command,
                                            enabled = enabled
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        CronAddEditDialog(
            initial = editTarget,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, schedule, command, enabled ->
                val target = editTarget
                if (target == null) {
                    viewModel.create(name, schedule, command, enabled)
                } else {
                    viewModel.update(target.id, name, schedule, command, enabled)
                }
                showAddEditDialog = false
            }
        )
    }

    deleteTarget?.let { job ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.cron_delete_confirm_title)) },
            text = { Text(stringResource(R.string.cron_delete_confirm_message, job.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(job.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.cron_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CronDisconnectedContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.cron_not_connected),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CronErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun CronEmptyContent(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.cron_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.cron_add))
            }
        }
    }
}

@Composable
private fun CronJobCard(
    job: NodeRuntime.CronJob,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = job.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.schedule,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            if (job.command.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = job.command,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            job.nextRun?.let { nextRun ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.cron_next_run, nextRun),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cron_edit),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cron_delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CronAddEditDialog(
    initial: NodeRuntime.CronJob?,
    onDismiss: () -> Unit,
    onSave: (name: String, schedule: String, command: String, enabled: Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var schedule by rememberSaveable { mutableStateOf(initial?.schedule ?: "") }
    var command by rememberSaveable { mutableStateOf(initial?.command ?: "") }
    var enabled by rememberSaveable { mutableStateOf(initial?.enabled ?: true) }

    val isValid = name.isNotBlank() && schedule.isNotBlank()
    val titleRes = if (initial == null) R.string.cron_add else R.string.cron_edit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.cron_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text(stringResource(R.string.cron_schedule)) },
                    placeholder = { Text("*/5 * * * *", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(stringResource(R.string.cron_command)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.cron_enabled), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), schedule.trim(), command.trim(), enabled) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.cron_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
