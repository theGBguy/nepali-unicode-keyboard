package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartAction
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardColors

/**
 * The 40dp row above the keys. Shows, in order of priority: the listening state, a voice error,
 * Romanized suggestions (best guess in bold, a smart suggestion, the other guesses scrolling
 * sideways, then the Latin text as typed), smart suggestions, or the emoji and clipboard buttons (plus Paste right
 * after a copy). Settings sits on the right, and the mic too when the field allows voice.
 * It keeps its height when empty so the keyboard never jumps.
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
    showEmoji: Boolean,
    showClipboard: Boolean,
    /** A recent copy to offer: its text, "" for a sensitive copy (no preview), or null. */
    pasteSuggestion: String?,
    /** Conversions of the text before the cursor (amounts, dates, units). */
    smartActions: List<SmartAction>,
    onSmartActionClick: (SmartAction) -> Unit,
    onEmojiClick: () -> Unit,
    onClipboardClick: () -> Unit,
    showHandwriting: Boolean,
    onHandwritingClick: () -> Unit,
    onPasteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onKeepLatin: () -> Unit,
    onMicClick: () -> Unit,
    /** The typing mode, shown while a physical keyboard hides the keys (and the space bar); null otherwise. */
    modeLabel: String? = null,
    onModeClick: () -> Unit = {},
    /** With a physical keyboard: whether the keys are shown (the button hides them), else null. */
    keysShown: Boolean? = null,
    onToggleKeys: () -> Unit = {},
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
                        suggestions.forEach { if (it !in this) add(it) }
                    }.take(MAX_WORDS),
                    smartAction = smartActions.firstOrNull(),
                    latinText = latinText,
                    onSuggestionClick = onSuggestionClick,
                    onSmartActionClick = onSmartActionClick,
                    onKeepLatin = onKeepLatin
                )
                // Smart suggestions take the emoji and clipboard buttons' room; they come back on the next key.
                smartActions.isNotEmpty() -> androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // Each takes its natural width; if they don't both fit, the longer one may use 60%.
                    val longest = smartActions.maxOf { it.label.length }
                    val total = maxWidth
                    Row(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        smartActions.forEachIndexed { index, action ->
                            if (index > 0) Divider(colors)
                            val share = when {
                                smartActions.size == 1 -> 1f
                                action.label.length == longest && index == smartActions.indexOfFirst { it.label.length == longest } -> 0.6f
                                else -> 0.4f
                            }
                            SmartSlot(colors, action, Modifier.widthIn(max = total * share)) { onSmartActionClick(action) }
                        }
                    }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showEmoji) StripIcon(colors, R.drawable.ic_ui_mood, R.string.emoji_open, onEmojiClick)
                    if (showClipboard) StripIcon(colors, R.drawable.ic_ui_content_paste, R.string.clipboard_open, onClipboardClick)
                    if (showHandwriting) StripIcon(colors, R.drawable.ic_ui_draw, R.string.handwriting_open, onHandwritingClick)
                    if (pasteSuggestion != null) PasteSuggestion(colors, pasteSuggestion, onPasteClick)
                }
            }
        }
        if (!isListening && latinText.isEmpty()) {
            if (modeLabel != null) ModeLabel(colors, modeLabel, onModeClick)
            if (keysShown != null) {
                StripIcon(
                    colors,
                    R.drawable.ic_ui_keyboard,
                    if (keysShown) R.string.keyboard_hide_keys else R.string.keyboard_show_keys,
                    onToggleKeys
                )
            }
            StripIcon(colors, R.drawable.ic_ui_settings, R.string.keyboard_settings, onSettingsClick)
        }
        if (showMic) {
            MicButton(colors = colors, isListening = isListening, onClick = onMicClick)
        }
    }
}

/** Word guesses the strip shows while typing; the rest scroll in from the right. */
private const val MAX_WORDS = 6

/**
 * The best guess first (bold), then a smart suggestion, then the other guesses. Words take their
 * natural width and scroll sideways; the next one is cut off at the edge and fades out, which
 * says there's more. The Latin text stays pinned on the right so "keep English" is always one tap.
 */
@Composable
private fun Suggestions(
    colors: KeyboardColors,
    words: List<String>,
    smartAction: SmartAction?,
    latinText: String,
    onSuggestionClick: (String) -> Unit,
    onSmartActionClick: (SmartAction) -> Unit,
    onKeepLatin: () -> Unit
) {
    val scroll = rememberScrollState()
    // A new key gives new guesses: show them from the best one again.
    LaunchedEffect(words, smartAction) { scroll.scrollTo(0) }
    val fade = with(LocalDensity.current) { 28.dp.toPx() }
    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Fade the right edge while more words are off screen.
                    if (scroll.canScrollForward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Black, Color.Transparent),
                                startX = size.width - fade,
                                endX = size.width
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                .horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically
        ) {
            words.forEachIndexed { index, word ->
                if (index > 0) Divider(colors)
                WordSlot(word, colors.keyText, bold = index == 0) { onSuggestionClick(word) }
                if (index == 0 && smartAction != null) {
                    Divider(colors)
                    // Up to 2.5 word widths so dates and amounts in words stay readable.
                    SmartSlot(colors, smartAction, Modifier.widthIn(max = 200.dp)) { onSmartActionClick(smartAction) }
                }
            }
            if (words.isEmpty() && smartAction != null) {
                SmartSlot(colors, smartAction, Modifier.widthIn(max = 200.dp)) { onSmartActionClick(smartAction) }
            }
        }
        Divider(colors)
        Box(
            modifier = Modifier
                .widthIn(min = 56.dp, max = 120.dp)
                .fillMaxHeight()
                .clickable(onClick = onKeepLatin),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = latinText,
                color = colors.hint,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

/** A word guess at its natural width (at least a comfortable tap target). */
@Composable
private fun WordSlot(text: String, color: Color, bold: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = withNepaliSpeech(text),
            color = color,
            fontSize = 17.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}

/**
 * A smart suggestion: plain text like a word guess, after a small hint-coloured icon so it reads
 * as a conversion.
 */
@Composable
private fun SmartSlot(
    colors: KeyboardColors,
    action: SmartAction,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            painter = painterResource(smartIcon(action.kind)),
            contentDescription = null,
            tint = colors.hint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = action.label,
            color = colors.keyText,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun smartIcon(kind: SmartAction.Kind): Int = when (kind) {
    SmartAction.Kind.AMOUNT -> R.drawable.ic_ui_123
    SmartAction.Kind.DATE -> R.drawable.ic_ui_calendar_today
    SmartAction.Kind.UNIT -> R.drawable.ic_ui_straighten
}

/** A fresh copy offered for pasting: a paste icon and the start of the text on one line. */
@Composable
private fun PasteSuggestion(colors: KeyboardColors, preview: String, onClick: () -> Unit) {
    val label = preview.replace('\n', ' ').trim().ifEmpty { stringResource(R.string.clipboard_paste) }
    val description = stringResource(R.string.clipboard_paste)
    Row(
        modifier = Modifier
            .padding(start = 4.dp, end = 8.dp)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClickLabel = description, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ui_content_paste),
            contentDescription = null,
            tint = colors.hint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = colors.keyText,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
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

/** The current mode as plain hint text; tapping it switches language like the language key. */
@Composable
private fun ModeLabel(colors: KeyboardColors, label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = colors.hint,
        fontSize = 13.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

@Composable
private fun StripIcon(colors: KeyboardColors, icon: Int, @StringRes description: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = colors.hint,
            modifier = Modifier.size(20.dp)
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
