package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.model.EnterAction
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.SymbolLayout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.components.ClipboardPanel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.components.EmojiPanel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.components.KeyboardStrip

private val KeyGap = 5.dp
private val RowGap = 7.dp
private val StripHeight = 40.dp

private fun isNumberRow(row: List<KeyData>) =
    row.all { it.keyType == KeyType.CHARACTER && it.primary.singleOrNull()?.isDigit() == true }

private fun rowHeight(row: List<KeyData>, keyHeight: Dp, numberRowKeyHeight: Dp) =
    if (isNumberRow(row)) numberRowKeyHeight else keyHeight
private val KeyShape = RoundedCornerShape(7.dp)
private val PreviewShape = RoundedCornerShape(10.dp)
private val PreviewWidth = 46.dp
private val PreviewHeight = 56.dp

/**
 * The keyboard: strip, then key rows. Colours follow the system light/dark setting.
 */
@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = keyboardColors()
    val enterLabel = enterKeyLabel(state.field)
    val spaceLabel = stringResource(
        when (state.currentLanguage) {
            LanguageMode.NEPALI -> R.string.space_traditional
            LanguageMode.TRANSLITERATION -> R.string.space_romanized
            LanguageMode.ENGLISH -> R.string.mode_english
        }
    )

    val keyHeight = state.feel.height.dp.dp
    val numberRowKeyHeight = keyHeight - 6.dp

    Box(modifier = Modifier.fillMaxWidth().background(colors.background)) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(keyboardBottomInsets)
            .padding(start = 3.dp, end = 3.dp, bottom = 4.dp)
    ) {
        if (state.showEmoji) {
            // Same height as the strip and keys it replaces
            val rowsHeight = state.currentLayout.rows.fold(0.dp) { sum, row -> sum + rowHeight(row, keyHeight, numberRowKeyHeight) + RowGap }
            EmojiPanel(
                colors = colors,
                height = StripHeight + rowsHeight - RowGap,
                bottomRowHeight = keyHeight,
                feel = state.feel,
                recents = state.emojiRecents,
                onEmoji = { viewModel.onEmojiClick(it) },
                onBackspace = { viewModel.onEmojiBackspace() },
                onBackspaceRepeatStart = { viewModel.onBackspaceLongPressStart() },
                onBackspaceRepeatEnd = { viewModel.onBackspaceLongPressEnd() },
                onClose = { viewModel.closeEmoji() }
            )
            Spacer(Modifier.height(RowGap))
        } else if (state.showClipboard) {
            val rowsHeight = state.currentLayout.rows.fold(0.dp) { sum, row -> sum + rowHeight(row, keyHeight, numberRowKeyHeight) + RowGap }
            ClipboardPanel(
                colors = colors,
                height = StripHeight + rowsHeight - RowGap,
                bottomRowHeight = keyHeight,
                feel = state.feel,
                copied = state.copied,
                items = state.clipItems,
                onPaste = { viewModel.onClipPaste(it) },
                onPin = { text, pinned -> viewModel.onClipPin(text, pinned) },
                onDelete = { viewModel.onClipDelete(it) },
                onClearAll = { viewModel.onClipClearAll() },
                onBackspace = { viewModel.onEmojiBackspace() },
                onBackspaceRepeatStart = { viewModel.onBackspaceLongPressStart() },
                onBackspaceRepeatEnd = { viewModel.onBackspaceLongPressEnd() },
                onClose = { viewModel.closeClipboard() }
            )
            Spacer(Modifier.height(RowGap))
        } else {
        val buffer = state.transliterationBuffer
        KeyboardStrip(
            colors = colors,
            suggestions = if (state.showSuggestionBar) state.suggestions else emptyList(),
            primarySuggestion = buffer.primaryCandidate,
            latinText = if (state.showSuggestionBar) buffer.romanizedText else "",
            isListening = state.isVoiceListening,
            partialResult = state.partialVoiceResult,
            errorMessage = state.voiceError,
            showMic = state.field.allowsVoice,
            showEmoji = !state.field.isPrivate && !state.field.prefersNumbers,
            showClipboard = !state.field.isPrivate,
            pasteSuggestion = state.pasteSuggestion?.let { if (it.sensitive) "" else it.text },
            onEmojiClick = { viewModel.openEmoji() },
            onClipboardClick = { viewModel.openClipboard() },
            onPasteClick = { viewModel.onPasteSuggestion() },
            onSettingsClick = { viewModel.openSettings() },
            onSuggestionClick = { viewModel.onSuggestionClick(it) },
            onKeepLatin = { viewModel.onKeepLatin() },
            onMicClick = { viewModel.toggleVoiceInput() }
        )

        state.currentLayout.rows.forEach { row ->
            val allCharacters = row.all { it.keyType == KeyType.CHARACTER }
            val isNumberRow = isNumberRow(row)
            // Indent short letter rows (the 9-key home row) by half a key on each side
            val indent = if (allCharacters) ((10f - row.sumOf { it.flex.toDouble() }.toFloat()) / 2f).coerceAtLeast(0f) else 0f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RowGap),
                horizontalArrangement = Arrangement.spacedBy(KeyGap)
            ) {
                if (indent > 0f) Box(Modifier.weight(indent))
                row.forEach { key ->
                    val alternate = keyAlternate(key, state.isShifted)
                    val isBackspace = key.primary == "⌫"
                    val isLanguageKey = key.primary == "🌐"
                    Key(
                        key = key,
                        visual = keyVisual(key, state, enterLabel, spaceLabel, colors),
                        colors = colors,
                        height = if (isNumberRow) numberRowKeyHeight else keyHeight,
                        feel = state.feel,
                        onTap = { viewModel.onKeyClick(key) },
                        onLongPress = when {
                            isLanguageKey -> ({ viewModel.onLanguageLongPress() })
                            alternate != null -> ({ viewModel.onKeyLongPress(alternate) })
                            else -> null
                        },
                        onRepeatStart = if (isBackspace) ({ viewModel.onBackspaceLongPressStart() }) else null,
                        onRepeatEnd = if (isBackspace) ({ viewModel.onBackspaceLongPressEnd() }) else null,
                        modifier = Modifier.weight(key.flex)
                    )
                }
                if (indent > 0f) Box(Modifier.weight(indent))
            }
        }
        }
    }
        if (state.showLanguageMenu) {
            LanguageMenu(
                colors = colors,
                bottomOffset = keyHeight + RowGap + 8.dp,
                style = state.nepaliStyle,
                romanizedAllowed = state.field.allowsTransliteration,
                onStyle = { viewModel.setNepaliStyle(it) },
                onSwitchKeyboard = { viewModel.switchKeyboard() },
                onDismiss = { viewModel.dismissLanguageMenu() }
            )
        }
    }
}

/**
 * Long-press menu on the language key, drawn inside the keyboard above the bottom row: choose
 * how to type Nepali, or open Android's keyboard picker. Tapping anywhere else closes it.
 */
@Composable
private fun BoxScope.LanguageMenu(
    colors: KeyboardColors,
    bottomOffset: Dp,
    style: LanguageMode,
    romanizedAllowed: Boolean,
    onStyle: (LanguageMode) -> Unit,
    onSwitchKeyboard: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .matchParentSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .windowInsetsPadding(keyboardBottomInsets)
            .padding(start = 8.dp, bottom = bottomOffset)
            .width(232.dp)
            .shadow(8.dp, PreviewShape)
            .clip(PreviewShape)
            .background(colors.preview)
            .padding(vertical = 6.dp)
    ) {
        MenuRow(colors, stringResource(R.string.mode_romanized), checked = style == LanguageMode.TRANSLITERATION, enabled = romanizedAllowed) {
            onStyle(LanguageMode.TRANSLITERATION)
        }
        MenuRow(colors, stringResource(R.string.menu_traditional), checked = style == LanguageMode.NEPALI) {
            onStyle(LanguageMode.NEPALI)
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(colors.divider))
        MenuRow(colors, stringResource(R.string.menu_switch_keyboard), icon = R.drawable.ic_ui_keyboard, onClick = onSwitchKeyboard)
    }
}

@Composable
private fun MenuRow(
    colors: KeyboardColors,
    label: String,
    checked: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            val iconRes = icon ?: if (checked) R.drawable.ic_ui_check else null
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (checked) colors.accent else colors.keyText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = if (enabled) colors.keyText else colors.hint,
            fontSize = 15.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/** How a key is drawn: a label or an icon, an optional corner hint, and its colours. */
data class KeyVisual(
    val text: String? = null,
    @param:DrawableRes val icon: Int? = null,
    val hint: String? = null,
    val background: Color,
    val content: Color,
    val fontSize: TextUnit = 21.sp,
    val description: String? = null,
    val showsPreview: Boolean = false
)

/**
 * The character a long-press types, shown as the key's corner hint: the Nepali shift character,
 * or the first long-press symbol. Uppercase Latin letters don't count (shift already gives them).
 */
private fun keyAlternate(key: KeyData, isShifted: Boolean): String? {
    if (key.keyType != KeyType.CHARACTER || isShifted) return null
    val shift = key.shift
    if (shift != null && shift != key.primary && shift != key.primary.uppercase()) return shift
    return key.longPress?.firstOrNull { it != key.primary }
}

@Composable
private fun keyVisual(
    key: KeyData,
    state: KeyboardState,
    enter: EnterKeyLabel,
    spaceLabel: String,
    colors: KeyboardColors
): KeyVisual {
    val nepaliKeys = state.currentLanguage == LanguageMode.NEPALI
    return when (key.primary) {
        "↵" -> KeyVisual(
            text = enter.text, icon = enter.icon, background = colors.accent, content = colors.onAccent,
            fontSize = 13.sp, description = enter.description
        )
        "⌫" -> KeyVisual(
            icon = R.drawable.ic_ui_backspace, background = colors.functionKey, content = colors.keyText,
            description = stringResource(R.string.key_delete)
        )
        "⇧" -> KeyVisual(
            icon = if (state.isCapsLock) R.drawable.ic_ui_shift_lock else R.drawable.ic_ui_shift,
            background = colors.functionKey,
            content = if (state.isShifted) colors.accent else colors.keyText,
            description = stringResource(if (state.isCapsLock) R.string.key_caps_lock else R.string.key_shift)
        )
        "🌐" -> KeyVisual(
            text = "अ/A", background = colors.functionKey, content = colors.keyText, fontSize = 15.sp,
            description = stringResource(R.string.key_language)
        )
        " " -> KeyVisual(
            text = spaceLabel, background = colors.key, content = colors.hint, fontSize = 12.sp,
            description = stringResource(R.string.key_space)
        )
        "?123", SymbolLayout.PAGE_ONE, SymbolLayout.PAGE_TWO -> KeyVisual(
            text = if (key.primary == "?123" && nepaliKeys) "?१२३" else key.primary,
            background = colors.functionKey, content = colors.keyText, fontSize = 14.sp,
            description = stringResource(R.string.key_numbers)
        )
        "ABC" -> KeyVisual(
            text = if (nepaliKeys) "कखग" else "ABC",
            background = colors.functionKey, content = colors.keyText, fontSize = 14.sp,
            description = stringResource(R.string.key_letters)
        )
        else -> KeyVisual(
            text = key.getOutput(state.isShifted),
            hint = keyAlternate(key, state.isShifted),
            background = if (key.keyType == KeyType.CHARACTER) colors.key else colors.functionKey,
            content = colors.keyText,
            fontSize = if (key.primary.length > 2) 14.sp else 21.sp,
            showsPreview = key.keyType == KeyType.CHARACTER
        )
    }
}

/**
 * One key. Taps fire on release; a long-press types the corner hint; backspace deletes on
 * touch-down and repeats while held. Each press gives a light haptic tick, and character keys
 * show a preview bubble above the finger.
 */
@Composable
private fun Key(
    key: KeyData,
    visual: KeyVisual,
    colors: KeyboardColors,
    height: Dp,
    feel: KeyFeel,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
    onRepeatStart: (() -> Unit)?,
    onRepeatEnd: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    val repeatStart by rememberUpdatedState(onRepeatStart)
    val repeatEnd by rememberUpdatedState(onRepeatEnd)
    val repeating = onRepeatStart != null
    val hasLongPress = onLongPress != null

    Box(
        modifier = modifier
            .height(height)
            .then(if (visual.description != null) Modifier.semantics { contentDescription = visual.description } else Modifier)
            .pointerInput(key.primary, repeating, hasLongPress) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        if (feel.vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (feel.sound) playKeySound(view.context, key)
                        if (repeating) tap()
                        tryAwaitRelease()
                        pressed = false
                        // Stop repeating on release or when the gesture is cancelled
                        if (repeating) repeatEnd?.invoke()
                    },
                    onTap = { if (!repeating) tap() },
                    onLongPress = when {
                        repeating -> { _ -> repeatStart?.invoke() }
                        hasLongPress -> { _ -> longPress?.invoke() }
                        else -> null
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (colors.keyShadow == Color.Transparent) 0.dp else 0.5.dp, KeyShape)
                .clip(KeyShape)
                .background(if (pressed && !visual.showsPreview) colors.functionKey else visual.background),
            contentAlignment = Alignment.Center
        ) {
            KeyContent(visual)
            visual.hint?.let {
                Text(
                    text = it,
                    color = colors.hint,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }
        }
        if (pressed && visual.showsPreview) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -PreviewHeight - 4.dp)
                    .requiredSize(PreviewWidth, PreviewHeight)
                    .shadow(6.dp, PreviewShape)
                    .clip(PreviewShape)
                    .background(colors.preview),
                contentAlignment = Alignment.Center
            ) {
                Text(text = visual.text.orEmpty(), color = colors.keyText, fontSize = 28.sp, maxLines = 1)
            }
        }
    }
}

/** Android's own key-click sounds, at the touch-sound volume. */
private fun playKeySound(context: Context, key: KeyData) {
    val effect = when (key.primary) {
        "⌫" -> AudioManager.FX_KEYPRESS_DELETE
        " " -> AudioManager.FX_KEYPRESS_SPACEBAR
        "↵" -> AudioManager.FX_KEYPRESS_RETURN
        else -> AudioManager.FX_KEYPRESS_STANDARD
    }
    context.getSystemService(AudioManager::class.java)?.playSoundEffect(effect, -1f)
}

@Composable
private fun KeyContent(visual: KeyVisual) {
    if (visual.icon != null) {
        Icon(
            painter = painterResource(visual.icon),
            contentDescription = null,
            tint = visual.content,
            modifier = Modifier.size(22.dp)
        )
    } else {
        Text(
            text = visual.text.orEmpty(),
            color = visual.content,
            fontSize = visual.fontSize,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

/** What the Enter key shows for the focused field (icon, or the app's own label), plus its spoken label. */
data class EnterKeyLabel(val text: String?, @param:DrawableRes val icon: Int?, val description: String)

@Composable
fun enterKeyLabel(field: FieldInfo): EnterKeyLabel = when (field.enterAction) {
    EnterAction.NEWLINE -> EnterKeyLabel(null, R.drawable.ic_ui_keyboard_return, stringResource(R.string.key_enter_newline))
    EnterAction.DONE -> EnterKeyLabel(null, R.drawable.ic_ui_check, stringResource(R.string.key_enter_done))
    EnterAction.GO -> EnterKeyLabel(null, R.drawable.ic_ui_arrow_forward, stringResource(R.string.key_enter_go))
    EnterAction.SEARCH -> EnterKeyLabel(null, R.drawable.ic_ui_search, stringResource(R.string.key_enter_search))
    EnterAction.SEND -> EnterKeyLabel(null, R.drawable.ic_ui_send, stringResource(R.string.key_enter_send))
    EnterAction.NEXT -> EnterKeyLabel(null, R.drawable.ic_ui_keyboard_tab, stringResource(R.string.key_enter_next))
    EnterAction.PREVIOUS -> EnterKeyLabel(null, R.drawable.ic_ui_keyboard_tab_rtl, stringResource(R.string.key_enter_previous))
    EnterAction.CUSTOM -> {
        val label = field.actionLabel.orEmpty()
        EnterKeyLabel(label, null, label)
    }
}

/**
 * Space to keep clear at the bottom: the navigation bar, and with gesture navigation the system's
 * keyboard bar (hide and switch-keyboard buttons). Android draws that bar inside the keyboard's own
 * window and reports it as a caption-bar inset; the navigation-bar inset alone only covers the
 * gesture handle, so the bottom row would sit under those buttons.
 */
private val keyboardBottomInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.union(WindowInsets.captionBar)
