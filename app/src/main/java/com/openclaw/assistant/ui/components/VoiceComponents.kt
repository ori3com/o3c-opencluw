package com.openclaw.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.openclaw.assistant.ui.voice.VoiceTurn

@Composable
fun VoiceTurnBubble(entry: VoiceTurn) {
    val alignment = if (entry.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (entry.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (entry.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (entry.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = alignment) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 300.dp)
        ) {
            Text(
                text = entry.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun VoiceThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Thinking...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun VoiceMicButton(
    enabled: Boolean,
    inputLevel: Float,
    cooldown: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // inputLevel is typically dB. Let's normalize it roughly for visualization.
    // rmsdB is usually in range 0..10.
    val extraRingSize = (inputLevel.coerceAtLeast(0f) * 2.2f).dp

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Animated background ring
        if (enabled) {
            Box(
                modifier = Modifier
                    .size(68.dp + extraRingSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }

        FilledIconButton(
            onClick = onClick,
            enabled = !cooldown,
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Microphone",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
