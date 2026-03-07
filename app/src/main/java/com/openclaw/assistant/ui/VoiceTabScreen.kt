package com.openclaw.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openclaw.assistant.ui.components.VoiceMicButton
import com.openclaw.assistant.ui.components.VoiceThinkingBubble
import com.openclaw.assistant.ui.components.VoiceTurnBubble
import com.openclaw.assistant.ui.voice.VoiceViewModel
import kotlinx.coroutines.launch

@Composable
fun VoiceTabScreen(
    viewModel: VoiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val micLiveTranscript by viewModel.micLiveTranscript.collectAsState()
    val micConversation by viewModel.micConversation.collectAsState()
    val micInputLevel by viewModel.micInputLevel.collectAsState()
    val micIsSending by viewModel.micIsSending.collectAsState()
    val micEnabled by viewModel.micEnabled.collectAsState()
    val micCooldown by viewModel.micCooldown.collectAsState()
    val micStatusText by viewModel.micStatusText.collectAsState()
    val speakerEnabled by viewModel.speakerEnabled.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasMicPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.setVoiceScreenActive(true)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setVoiceScreenActive(false)
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(micConversation.size, micIsSending) {
        if (micConversation.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(micConversation.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Conversation History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(micConversation) { entry ->
                VoiceTurnBubble(entry = entry)
            }

            if (micIsSending && micConversation.none { it.isStreaming }) {
                item {
                    VoiceThinkingBubble()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Transcript
        if (!micLiveTranscript.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            ) {
                Text(
                    text = micLiveTranscript!!.trim(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Status Text
        if (micStatusText.isNotBlank()) {
            Text(
                text = micStatusText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speaker Toggle
            IconButton(
                onClick = { viewModel.setSpeakerEnabled(!speakerEnabled) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp
                                   else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (speakerEnabled) "Mute speaker" else "Unmute speaker",
                    tint = if (speakerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            // Mic Button
            VoiceMicButton(
                enabled = micEnabled,
                inputLevel = micInputLevel,
                cooldown = micCooldown,
                onClick = {
                    if (hasMicPermission) {
                        viewModel.toggleMic()
                    } else {
                        // In a real app we'd request permission here
                        // For now we assume MainActivity handled it or user will be prompted by OS
                        viewModel.toggleMic()
                    }
                }
            )

            // Space for symmetry or additional buttons
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
