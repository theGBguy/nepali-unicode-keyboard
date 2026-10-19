package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardColors

/**
 * The 40dp row above the keys. Shows, in order of priority: the listening state, a voice error,
 * Romanized suggestions (best guess in bold, other guesses, then the Latin text as typed), or
 * nothing. The mic sits on the right when the field allows voice. It keeps its height when empty
 * so the keyboard never jumps.
 */
@Composable
fun KeyboardStrip(
    colors: KeyboardColors,
    suggestions: List<String>,
    primarySuggestion: String,
    latinText: String,
    isListening: Boolean,
    partialResult: String?,
    @StringRes errorMessage: Int?,
    showMic: Boolean,
    onSuggestionClick: (String) -> Unit,
    onKeepLatin: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            when {
                isListening -> Listening(colors, partialResult)
                errorMessage != null -> Text(
                    text = stringResource(errorMessage),
                    color = colors.error,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                latinText.isNotEmpty() -> Suggestions(
                    colors = colors,
                    words = buildList {
                        if (primarySuggestion.isNotEmpty()) add(primarySuggestion)
                        suggestions.filter { it != primarySuggestion }.take(1).forEach { add(it) }
                    },
                    latinText = latinText,
                    onSuggestionClick = onSuggestionClick,
                    onKeepLatin = onKeepLatin
                )
            }
        }
        if (showMic) {
            MicButton(colors = colors, isListening = isListening, onClick = onMicClick)
        }
    }
}

@Composable
private fun Suggestions(
    colors: KeyboardColors,
    words: List<String>,
    latinText: String,
    onSuggestionClick: (String) -> Unit,
    onKeepLatin: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        words.forEachIndexed { index, word ->
            if (index > 0) Divider(colors)
            Slot(
                text = word,
                color = colors.keyText,
                bold = index == 0,
                fontSize = 17.sp,
                onClick = { onSuggestionClick(word) }
            )
        }
        Divider(colors)
        Slot(text = latinText, color = colors.hint, bold = false, fontSize = 15.sp, onClick = onKeepLatin)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Slot(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    bold: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun Divider(colors: KeyboardColors) {
    Box(Modifier.width(1.dp).height(18.dp).background(colors.divider))
}

@Composable
private fun Listening(colors: KeyboardColors, partialResult: String?) {
    val transition = rememberInfiniteTransition(label = "listening")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dotAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
        Box(Modifier.size(8.dp).alpha(dotAlpha).clip(CircleShape).background(colors.accent))
        Spacer(Modifier.width(10.dp))
        Text(
            text = partialResult ?: stringResource(R.string.voice_listening),
            color = if (partialResult != null) colors.keyText else colors.hint,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MicButton(colors: KeyboardColors, isListening: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isListening) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(if (isListening) R.drawable.ic_ui_stop else R.drawable.ic_ui_mic),
            contentDescription = stringResource(if (isListening) R.string.voice_stop else R.string.voice_start),
            tint = if (isListening) colors.onAccent else colors.hint,
            modifier = Modifier.size(20.dp)
        )
    }
}
