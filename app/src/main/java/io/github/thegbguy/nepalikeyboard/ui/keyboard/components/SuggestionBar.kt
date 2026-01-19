package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Suggestion bar component for displaying transliteration suggestions.
 * Shows up to 3 suggestion chips above the keyboard.
 *
 * @param suggestions List of suggestions to display
 * @param primarySuggestion The primary (first) suggestion to highlight
 * @param onSuggestionClick Callback when a suggestion is clicked
 * @param modifier Modifier for the suggestion bar
 */
@Composable
fun SuggestionBar(
    suggestions: List<String>,
    primarySuggestion: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary suggestion (highlighted)
                if (primarySuggestion.isNotEmpty()) {
                    SuggestionChip(
                        text = primarySuggestion,
                        isPrimary = true,
                        onClick = { onSuggestionClick(primarySuggestion) },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Additional suggestions (if any)
                suggestions.drop(1).take(2).forEach { suggestion ->
                    SuggestionChip(
                        text = suggestion,
                        isPrimary = false,
                        onClick = { onSuggestionClick(suggestion) },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

/**
 * Individual suggestion chip.
 *
 * @param text The suggestion text to display
 * @param isPrimary Whether this is the primary (highlighted) suggestion
 * @param onClick Callback when this chip is clicked
 * @param modifier Modifier for the chip
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
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPrimary) Color(0xFF0A84FF) else Color(0xFF3A3A3A))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
