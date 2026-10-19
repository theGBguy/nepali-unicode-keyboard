package io.github.thegbguy.nepalikeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.model.EnterAction
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.ui.keyboard.components.VoiceInputBar

/**
 * Main keyboard view composable.
 */
@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E) // Dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            // Language indicator bar
            LanguageIndicator(
                currentLanguage = stringResource(state.currentLanguage.labelRes()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Voice input bar with suggestions and mic button
            VoiceInputBar(
                suggestions = if (state.showSuggestionBar) state.suggestions else emptyList(),
                primarySuggestion = state.transliterationBuffer.primaryCandidate,
                isListening = state.isVoiceListening,
                partialResult = state.partialVoiceResult,
                errorMessage = state.voiceError,
                onSuggestionClick = { viewModel.onSuggestionClick(it) },
                onMicClick = { viewModel.toggleVoiceInput() },
                showMic = state.field.allowsVoice
            )

            val enterLabel = enterKeyLabel(state.field)

            // Keyboard rows
            state.currentLayout.rows.forEach { row ->
                KeyboardRow(
                    keys = row,
                    isShifted = state.isShifted,
                    isCapsLock = state.isCapsLock,
                    onKeyClick = { key -> viewModel.onKeyClick(key) },
                    onBackspaceLongPressStart = { viewModel.onBackspaceLongPressStart() },
                    onBackspaceLongPressEnd = { viewModel.onBackspaceLongPressEnd() },
                    enterLabel = enterLabel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Language indicator at the top of keyboard.
 */
@Composable
fun LanguageIndicator(
    currentLanguage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentLanguage,
            color = Color(0xFF9E9E9E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * A single row of keys.
 */
@Composable
fun KeyboardRow(
    keys: List<KeyData>,
    isShifted: Boolean,
    isCapsLock: Boolean,
    onKeyClick: (KeyData) -> Unit,
    onBackspaceLongPressStart: () -> Unit,
    onBackspaceLongPressEnd: () -> Unit,
    enterLabel: EnterKeyLabel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            Key(
                keyData = key,
                isShifted = isShifted,
                isCapsLock = isCapsLock,
                onClick = { onKeyClick(key) },
                onLongPressStart = if (key.primary == "⌫") onBackspaceLongPressStart else null,
                onLongPressEnd = if (key.primary == "⌫") onBackspaceLongPressEnd else null,
                label = if (key.primary == "↵") enterLabel.text else null,
                contentDescription = if (key.primary == "↵") enterLabel.description else null,
                modifier = Modifier.weight(key.flex)
            )
        }
    }
}

/**
 * Individual key composable.
 */
@Composable
fun Key(
    keyData: KeyData,
    isShifted: Boolean,
    isCapsLock: Boolean,
    onClick: () -> Unit,
    onLongPressStart: (() -> Unit)? = null,
    onLongPressEnd: (() -> Unit)? = null,
    label: String? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        // Shift key highlighting
        keyData.keyType == KeyType.MODIFIER && keyData.primary == "⇧" && isCapsLock -> {
            Color(0xFF0A84FF) // Blue for caps lock (same as action keys)
        }
        keyData.keyType == KeyType.MODIFIER && keyData.primary == "⇧" && isShifted -> {
            Color(0xFF5A5A5A) // Lighter gray for shift active
        }
        // Default colors for other keys
        keyData.keyType == KeyType.CHARACTER -> Color(0xFF3A3A3A)
        keyData.keyType == KeyType.ACTION -> Color(0xFF0A84FF) // iOS-style blue for action keys
        keyData.keyType == KeyType.MODIFIER -> Color(0xFF3A3A3A)
        keyData.keyType == KeyType.FUNCTION -> Color(0xFF2C2C2C)
        keyData.keyType == KeyType.SPACER -> Color.Transparent
        else -> Color(0xFF3A3A3A)
    }

    val textColor = when (keyData.keyType) {
        KeyType.ACTION -> Color.White
        else -> Color(0xFFE5E5E5)
    }

    Box(
        modifier = modifier
            .then(
                if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            )
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .then(
                if (keyData.keyType != KeyType.SPACER && onLongPressStart != null && onLongPressEnd != null) {
                    // Use pointerInput for backspace to handle long-press
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onClick()
                                val pressed = tryAwaitRelease()
                                if (pressed) {
                                    onLongPressEnd()
                                }
                            },
                            onLongPress = {
                                onLongPressStart()
                            }
                        )
                    }
                } else if (keyData.keyType != KeyType.SPACER) {
                    // Regular clickable for other keys
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (keyData.keyType != KeyType.SPACER) {
            val displayText = when {
                label != null -> label
                keyData.keyType == KeyType.MODIFIER && isShifted -> keyData.primary
                keyData.keyType == KeyType.CHARACTER && isShifted && keyData.shift != null -> keyData.shift
                else -> keyData.primary
            }

            Text(
                text = displayText,
                color = textColor,
                fontSize = when {
                    label != null && label.length > 2 -> 13.sp
                    else -> when (keyData.keyType) {
                    KeyType.CHARACTER -> 20.sp
                    KeyType.FUNCTION -> 16.sp
                    else -> 18.sp
                    }
                },
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}


/** What the Enter key shows for the focused field, plus its spoken label. */
data class EnterKeyLabel(val text: String, val description: String)

@Composable
fun enterKeyLabel(field: FieldInfo): EnterKeyLabel = when (field.enterAction) {
    EnterAction.NEWLINE -> EnterKeyLabel("↵", stringResource(R.string.key_enter_newline))
    EnterAction.DONE -> EnterKeyLabel("✓", stringResource(R.string.key_enter_done))
    EnterAction.GO -> EnterKeyLabel("→", stringResource(R.string.key_enter_go))
    EnterAction.SEARCH -> EnterKeyLabel("⌕", stringResource(R.string.key_enter_search))
    EnterAction.SEND -> EnterKeyLabel("➤", stringResource(R.string.key_enter_send))
    EnterAction.NEXT -> EnterKeyLabel("⇥", stringResource(R.string.key_enter_next))
    EnterAction.PREVIOUS -> EnterKeyLabel("⇤", stringResource(R.string.key_enter_previous))
    EnterAction.CUSTOM -> {
        val label = field.actionLabel.orEmpty()
        EnterKeyLabel(label, label)
    }
}
