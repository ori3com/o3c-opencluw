package com.openclaw.assistant.ui.cron

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.openclaw.assistant.R
import com.openclaw.assistant.backend.AgentBackendConfig
import com.openclaw.assistant.backend.BackendRepository
import com.openclaw.assistant.backend.BackendType
import com.openclaw.assistant.backend.HermesConfigApi
import com.openclaw.assistant.backend.HermesCronJob
import kotlinx.coroutines.launch

private enum class CronOwner {
    HermesAgent,
    OpenClaw,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val backendRepository = remember { BackendRepository.getInstance(context) }
    val backends by backendRepository.backends.collectAsState()
    var selectedOwner by rememberSaveable { mutableStateOf(CronOwner.HermesAgent) }

    val hermesBackend = remember(backends) {
        backends.firstOrNull { it.enabled && it.type == BackendType.HERMES_API_SERVER }
    }
    val openClawBackends = remember(backends) {
        backends.filter {
            it.enabled && (it.type == BackendType.OPENCLAW_GATEWAY || it.type == BackendType.OPENCLAW_HTTP)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cron_title)) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CronOwnerSelector(
                    selectedOwner = selectedOwner,
                    onOwnerSelected = { selectedOwner = it },
                )
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedOwner) {
                        CronOwner.HermesAgent -> {
                            if (hermesBackend == null) {
                                EmptyCronState(
                                    title = stringResource(R.string.cron_hermes_not_configured),
                                    body = stringResource(R.string.cron_hermes_not_configured_desc),
                                )
                            } else {
                                CronJobList(backend = hermesBackend)
                            }
                        }
                        CronOwner.OpenClaw -> {
                            EmptyCronState(
                                title = stringResource(R.string.cron_openclaw_title),
                                body = if (openClawBackends.isEmpty()) {
                                    stringResource(R.string.cron_openclaw_not_configured_desc)
                                } else {
                                    stringResource(
                                        R.string.cron_openclaw_not_supported_desc,
                                        openClawBackends.joinToString { it.displayName },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CronOwnerSelector(
    selectedOwner: CronOwner,
    onOwnerSelected: (CronOwner) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedOwner == CronOwner.HermesAgent,
            onClick = { onOwnerSelected(CronOwner.HermesAgent) },
            leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
            label = { Text(stringResource(R.string.cron_owner_hermes_agent)) },
        )
        FilterChip(
            selected = selectedOwner == CronOwner.OpenClaw,
            onClick = { onOwnerSelected(CronOwner.OpenClaw) },
            leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
            label = { Text(stringResource(R.string.cron_owner_openclaw)) },
        )
    }
}

@Composable
private fun EmptyCronState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CronJobList(backend: AgentBackendConfig) {
    val context = LocalContext.current
    val api = remember { HermesConfigApi() }
    val scope = rememberCoroutineScope()

    var jobs by remember { mutableStateOf<List<HermesCronJob>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingJob by remember { mutableStateOf<HermesCronJob?>(null) }
    var jobToDelete by remember { mutableStateOf<HermesCronJob?>(null) }

    fun refresh() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                jobs = api.fetchJobs(backend)
            } catch (e: Exception) {
                errorMessage = e.message ?: context.getString(R.string.cron_error_fetch)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(backend) {
        refresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_try_again))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_try_again))
                }
            }
        } else if (jobs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.cron_no_jobs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_try_again))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_try_again))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.cron_hermes_backend_header, backend.displayName),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = backend.baseUrl.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(jobs, key = { it.id }) { job ->
                    CronJobCard(
                        job = job,
                        backendName = backend.displayName,
                        onToggle = { enabled ->
                            scope.launch {
                                try {
                                    api.updateJob(backend, job.id, enabled = enabled)
                                    refresh()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: context.getString(R.string.cron_error_save)
                                }
                            }
                        },
                        onEdit = { editingJob = job },
                        onDelete = { jobToDelete = job }
                    )
                }
            }
        }

        // Add Job FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cron_add_job))
        }
    }

    // Add Job Dialog
    if (showAddDialog) {
        JobEditDialog(
            title = stringResource(R.string.cron_add_job),
            onDismiss = { showAddDialog = false },
            onSave = { name, expr, prompt, repeat ->
                scope.launch {
                    isLoading = true
                    try {
                        api.createJob(backend, name, expr, prompt, repeat = repeat)
                        showAddDialog = false
                        refresh()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: context.getString(R.string.cron_error_save)
                        isLoading = false
                    }
                }
            }
        )
    }

    // Edit Job Dialog
    editingJob?.let { job ->
        JobEditDialog(
            title = stringResource(R.string.cron_edit_job),
            initialName = job.name,
            initialSchedule = job.schedule.expr,
            initialPrompt = job.prompt,
            initialRepeat = job.repeat?.times,
            onDismiss = { editingJob = null },
            onSave = { name, expr, prompt, repeat ->
                scope.launch {
                    isLoading = true
                    try {
                        api.updateJob(
                            backend,
                            jobId = job.id,
                            name = name,
                            schedule = expr,
                            prompt = prompt,
                            repeat = repeat
                        )
                        editingJob = null
                        refresh()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: context.getString(R.string.cron_error_save)
                        isLoading = false
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    jobToDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            title = { Text(stringResource(R.string.cron_delete_job)) },
            text = { Text(stringResource(R.string.cron_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                api.deleteJob(backend, job.id)
                                jobToDelete = null
                                refresh()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: context.getString(R.string.cron_error_delete)
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.cron_delete_job), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { jobToDelete = null }) {
                    Text(stringResource(R.string.cron_cancel))
                }
            }
        )
    }
}

@Composable
fun CronJobCard(
    job: HermesCronJob,
    backendName: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = job.schedule.expr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.cron_owner_hermes_agent)) },
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(backendName) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = job.enabled,
                        onCheckedChange = onToggle
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cron_edit_job))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cron_delete_job), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cron_job_prompt_value, job.prompt),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.cron_job_deliver_value, cronDeliverLabel(job.deliver)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            job.repeat?.times?.let { repeatTimes ->
                Spacer(modifier = Modifier.height(4.dp))
                val completed = job.repeat.completed ?: 0
                Text(
                    text = stringResource(R.string.cron_job_repeat_value, completed, repeatTimes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun cronDeliverLabel(deliver: String): String {
    return when (deliver.trim().lowercase()) {
        "local" -> stringResource(R.string.cron_deliver_hermes_local)
        "origin" -> stringResource(R.string.cron_deliver_origin)
        "all" -> stringResource(R.string.cron_deliver_all)
        else -> deliver.ifBlank { stringResource(R.string.cron_deliver_unknown) }
    }
}

@Composable
fun JobEditDialog(
    title: String,
    initialName: String = "",
    initialSchedule: String = "",
    initialPrompt: String = "",
    initialRepeat: Int? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, expr: String, prompt: String, repeat: Int?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var expr by remember { mutableStateOf(initialSchedule) }
    var prompt by remember { mutableStateOf(initialPrompt) }
    var repeatStr by remember { mutableStateOf(initialRepeat?.toString() ?: "") }

    var errorText by remember { mutableStateOf<String?>(null) }
    val emptyFieldsMessage = stringResource(R.string.cron_empty_fields)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error)
                }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.cron_job_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = expr,
                    onValueChange = { expr = it },
                    label = { Text(stringResource(R.string.cron_job_schedule)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.cron_job_prompt)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                TextField(
                    value = repeatStr,
                    onValueChange = { repeatStr = it },
                    label = { Text(stringResource(R.string.cron_job_repeat)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || expr.isBlank()) {
                        errorText = emptyFieldsMessage
                    } else {
                        val repeatVal = repeatStr.toIntOrNull()
                        onSave(name, expr, prompt, repeatVal)
                    }
                }
            ) {
                Text(stringResource(R.string.cron_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cron_cancel))
            }
        }
    )
}
