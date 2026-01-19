package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Voice input bar component that shows suggestions and a microphone button.
 * When listening, shows a recording indicator with partial results.
 *
 * @param suggestions List of transliteration suggestions to display
 * @param primarySuggestion The primary (first) suggestion to highlight
 * @param isListening Whether voice recognition is currently active
 * @param partialResult Partial speech recognition result to display
 * @param errorMessage Error message to display (if any)
 * @param onSuggestionClick Callback when a suggestion is clicked
 * @param onMicClick Callback when the microphone button is clicked
 * @param modifier Modifier for the bar
 */
@Composable
fun VoiceInputBar(
    suggestions: List<String>,
    primarySuggestion: String,
    isListening: Boolean,
    partialResult: String?,
    errorMessage: String?,
    onSuggestionClick: (String) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        color = Color(0xFF2A2A2A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: suggestions or listening indicator
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                when {
                    isListening -> {
                        // Show listening indicator with partial result
                        ListeningIndicator(
                            partialResult = partialResult,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    errorMessage != null -> {
                        // Show error message
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    suggestions.isNotEmpty() -> {
                        // Show transliteration suggestions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Primary suggestion
                            if (primarySuggestion.isNotEmpty()) {
                                SuggestionChip(
                                    text = primarySuggestion,
                                    isPrimary = true,
                                    onClick = { onSuggestionClick(primarySuggestion) }
                                )
                            }
                            // Additional suggestions
                            suggestions.drop(1).take(2).forEach { suggestion ->
                                SuggestionChip(
                                    text = suggestion,
                                    isPrimary = false,
                                    onClick = { onSuggestionClick(suggestion) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Microphone button
            MicrophoneButton(
                isListening = isListening,
                onClick = onMicClick
            )
        }
    }
}

/**
 * Microphone button with animated recording indicator.
 */
@Composable
private fun MicrophoneButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isListening) Color(0xFFFF3B30) else Color(0xFF3A3A3A),
        animationSpec = tween(300),
        label = "micBackground"
    )

    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .size(32.dp)
            .scale(if (isListening) scale else 1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isListening) "⏹" else "🎤",
            fontSize = 16.sp
        )
    }
}

/**
 * Listening indicator with animated dots and partial result.
 */
@Composable
private fun ListeningIndicator(
    partialResult: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated recording dot
        val infiniteTransition = rememberInfiniteTransition(label = "recordingDot")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotAlpha"
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B30).copy(alpha = alpha))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = partialResult ?: "Listening...",
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Suggestion chip for transliteration.
 */
@Composable
private fun SuggestionChip(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPrimary) Color(0xFF0A84FF) else Color(0xFF3A3A3A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
