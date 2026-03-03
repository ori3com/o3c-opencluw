package com.openclaw.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openclaw.assistant.ui.theme.OpenClawOrange

/**
 * Animated visual feedback overlay for voice conversations.
 * Displays an animated pulsing orb and status text.
 */
@Composable
fun TalkOrbOverlay(
    modifier: Modifier = Modifier,
    statusText: String = "",
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    seamColor: Color = OpenClawOrange,
    audioLevel: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Dynamic animation duration and scale based on state
    val duration = when {
        isListening -> 1000
        isSpeaking -> 1200
        else -> 3000 // Thinking/Idle/Preparing
    }

    val targetScale = when {
        isListening -> 1.3f
        isSpeaking -> 1.2f
        else -> 1.05f
    }

    // Audio level animation (ported from OpenClawSession)
    val normalizedLevel = ((audioLevel + 2f) / 10f).coerceIn(0f, 1f)
    val targetLevelScale = 1f + (normalizedLevel * 0.5f)
    val animatedLevelScale by animateFloatAsState(
        targetValue = if (isListening) targetLevelScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "audio_level_scale"
    )

    val basePulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = targetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val finalScale = if (isListening) maxOf(basePulseScale, animatedLevelScale) else basePulseScale

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            // Outer soft glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(finalScale * 1.15f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                seamColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Middle glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(finalScale * 1.1f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                seamColor.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Main Orb
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(finalScale)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                seamColor,
                                seamColor.copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        if (statusText.isNotEmpty() && statusText != "Off") {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.Black
            )
        }
    }
}
