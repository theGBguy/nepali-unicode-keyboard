package io.github.thegbguy.nepalikeyboard.ui.keyboard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.layout.layout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.components.withNepaliSpeech
import androidx.compose.ui.semantics.text
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import kotlin.math.roundToInt
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.EnglishLayout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.NepaliLayout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.thegbguy.nepalikeyboard.domain.voice.messageRes
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.model.EnterAction
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.SymbolLayout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.components.ClipboardPanel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.components.HandwritingPanel
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
private const val PressScale = 0.95f
private const val PressTint = 0.12f
private val PreviewWidth = 46.dp
private val PreviewHeight = 56.dp

/**
 * The keyboard: strip, then key rows. Colours follow the system light/dark setting.
 */
@Composable
fun KeyboardView(
    keyboard: KeyboardController = viewModel<KeyboardViewModel>().controller,
    onInsetsChange: (visibleTop: Int, extraTop: Int) -> Unit = { _, _ -> }
) {
    val state by keyboard.state.collectAsState()
    val colors = keyboardColors()
    val enterLabel = enterKeyLabel(state.field)
    val spaceLabel = stringResource(
        when (state.currentLanguage) {
            LanguageMode.NEPALI -> R.string.space_traditional
            LanguageMode.TRANSLITERATION -> R.string.space_romanized
            LanguageMode.ENGLISH -> R.string.mode_english
        }
    )

    // Height changes (key height setting, a layout with another row, a panel) animate instead of
    // jumping: keys grow or shrink smoothly, and the keyboard's outline follows with a spring.
    val keyHeight by animateDpAsState(state.feel.height.dp.dp, tween(220, easing = FastOutSlowInEasing), label = "keyHeight")
    val numberRowKeyHeight = keyHeight - 6.dp

    // A layout with another row (traditional Nepali has six, English five) changes the height. The
    // window never resizes for it (that flickers): a transparent spacer reserves room for the
    // tallest layout and the keys area eases inside it. The service reports the keyboard's top to
    // the app as content insets: right away when it grows, once the animation ends when it shrinks,
    // so the app resizes once and never shows a gap.
    val targetKeyHeight = state.feel.height.dp.dp
    val rowsTarget = state.currentLayout.rows.fold(0.dp) { sum, row -> sum + rowHeight(row, targetKeyHeight, targetKeyHeight - 6.dp) + RowGap }
    var settledRows by remember { mutableStateOf(rowsTarget) }
    val rowsHeight by animateDpAsState(
        rowsTarget,
        tween(220, easing = FastOutSlowInEasing),
        label = "rowsHeight",
        finishedListener = { settledRows = it }
    )
    val reservedRows = remember(targetKeyHeight) {
        listOf(EnglishLayout.getLayout(), NepaliLayout.getLayout()).maxOf { layout ->
            layout.rows.fold(0.dp) { sum, row -> sum + rowHeight(row, targetKeyHeight, targetKeyHeight - 6.dp) + RowGap }
        }
    }.coerceAtLeast(rowsTarget)
    val reportedRows = maxOf(rowsTarget, settledRows)
    val density = LocalDensity.current
    val extraTop = with(density) { (reportedRows - rowsHeight).coerceAtLeast(0.dp).roundToPx() }
    var visibleTop by remember { mutableIntStateOf(-1) }
    LaunchedEffect(visibleTop, extraTop) { if (visibleTop >= 0) onInsetsChange(visibleTop, extraTop) }

    Column(Modifier.fillMaxWidth()) {
    Spacer(Modifier.height((reservedRows - rowsHeight).coerceAtLeast(0.dp)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .onGloballyPositioned { visibleTop = it.positionInWindow().y.roundToInt() }
    ) {
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
                onEmoji = { keyboard.onEmojiClick(it) },
                onBackspace = { keyboard.onEmojiBackspace() },
                onBackspaceRepeatStart = { keyboard.onBackspaceLongPressStart() },
                onBackspaceRepeatEnd = { keyboard.onBackspaceLongPressEnd() },
                onClose = { keyboard.closeEmoji() }
            )
            Spacer(Modifier.height(RowGap))
        } else if (state.handwriting != null) {
            val rowsHeight = state.currentLayout.rows.fold(0.dp) { sum, row -> sum + rowHeight(row, keyHeight, numberRowKeyHeight) + RowGap }
            HandwritingPanel(
                colors = colors,
                height = StripHeight + rowsHeight - RowGap,
                bottomRowHeight = keyHeight,
                feel = state.feel,
                enter = enterLabel,
                phase = state.handwriting!!,
                strokes = state.inkStrokes,
                candidates = state.inkCandidates,
                onAccept = { keyboard.onHandwritingAccept() },
                onRetry = { keyboard.onHandwritingRetry() },
                onStroke = { stroke, w, h -> keyboard.onInkStroke(stroke, w, h) },
                onCandidate = { keyboard.onInkCandidate(it) },
                onText = { keyboard.onInkText(it) },
                onSpace = { keyboard.onInkSpace() },
                onEnter = { keyboard.onInkEnter() },
                onBackspace = { keyboard.onInkBackspace() },
                onBackspaceRepeatStart = { keyboard.onBackspaceLongPressStart() },
                onBackspaceRepeatEnd = { keyboard.onBackspaceLongPressEnd() },
                onClose = { keyboard.closeHandwriting() }
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
                onPaste = { keyboard.onClipPaste(it) },
                onPin = { text, pinned -> keyboard.onClipPin(text, pinned) },
                onDelete = { keyboard.onClipDelete(it) },
                onClearAll = { keyboard.onClipClearAll() },
                onBackspace = { keyboard.onEmojiBackspace() },
                onBackspaceRepeatStart = { keyboard.onBackspaceLongPressStart() },
                onBackspaceRepeatEnd = { keyboard.onBackspaceLongPressEnd() },
                onClose = { keyboard.closeClipboard() }
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
            errorMessage = state.voiceError?.messageRes,
            showMic = state.field.allowsVoice,
            showEmoji = !state.field.isPrivate && !state.field.prefersNumbers,
            showClipboard = !state.field.isPrivate,
            pasteSuggestion = state.pasteSuggestion?.let { if (it.sensitive) "" else it.text },
            smartActions = state.smartActions,
            onSmartActionClick = { keyboard.onSmartActionClick(it) },
            onEmojiClick = { keyboard.openEmoji() },
            onClipboardClick = { keyboard.openClipboard() },
            showHandwriting = !state.field.isPrivate,
            onHandwritingClick = { keyboard.openHandwriting() },
            onPasteClick = { keyboard.onPasteSuggestion() },
            onSettingsClick = { keyboard.openSettings() },
            onSuggestionClick = { keyboard.onSuggestionClick(it) },
            onKeepLatin = { keyboard.onKeepLatin() },
            onMicClick = { keyboard.toggleVoiceInput() }
        )

        // The keys area eases to the new layout's height and clips while it does
        Box(Modifier.fillMaxWidth().height(rowsHeight).clipToBounds()) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                // Keys are cells that draw half a gap in from each side, so the rows reach half a gap
                // past the side padding to keep the outer keys where they were
                .layout { measurable, constraints ->
                    val extra = KeyGap.roundToPx()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
                    )
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
                }
        ) {
        state.currentLayout.rows.forEach { row ->
            val allCharacters = row.all { it.keyType == KeyType.CHARACTER }
            val isNumberRow = isNumberRow(row)
            // Indent short letter rows (the 9-key home row) by half a key on each side
            val indent = if (allCharacters) ((10f - row.sumOf { it.flex.toDouble() }.toFloat()) / 2f).coerceAtLeast(0f) else 0f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RowGap),
            ) {
                if (indent > 0f) Box(Modifier.weight(indent))
                row.forEach { key ->
                    val alternate = keyAlternate(key, state.isShifted)
                    val isBackspace = key.primary == "⌫"
                    val isLanguageKey = key.primary == "🌐"
                    val visual = keyVisual(key, state, enterLabel, spaceLabel, colors)
                    val alternateLabel = alternate?.let { stringResource(R.string.key_type_alternate, spokenCharacter(it)) }
                    val languageOptions = stringResource(R.string.key_language_options)
                    Key(
                        key = key,
                        visual = visual,
                        a11y = KeyA11y(
                            label = visual.description ?: spokenCharacter(visual.text ?: key.primary),
                            state = if (isLanguageKey) languageName(state.currentLanguage) else null,
                            selected = key.primary == "⇧" && state.isShifted,
                            actions = when {
                                isLanguageKey -> listOf(CustomAccessibilityAction(languageOptions) { keyboard.onLanguageLongPress(); true })
                                alternate != null && alternateLabel != null -> listOf(CustomAccessibilityAction(alternateLabel) { keyboard.onKeyLongPress(alternate); true })
                                else -> emptyList()
                            }
                        ),
                        colors = colors,
                        height = if (isNumberRow) numberRowKeyHeight else keyHeight,
                        feel = state.feel,
                        onTap = { keyboard.onKeyClick(key) },
                        onLongPress = when {
                            isLanguageKey -> ({ keyboard.onLanguageLongPress() })
                            alternate != null -> ({ keyboard.onKeyLongPress(alternate) })
                            else -> null
                        },
                        onRepeatStart = if (isBackspace) ({ keyboard.onBackspaceLongPressStart() }) else null,
                        onRepeatEnd = if (isBackspace) ({ keyboard.onBackspaceLongPressEnd() }) else null,
                        modifier = Modifier.weight(key.flex)
                    )
                }
                if (indent > 0f) Box(Modifier.weight(indent))
            }
        }
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
                onStyle = { keyboard.setNepaliStyle(it) },
                onSwitchKeyboard = { keyboard.switchKeyboard() },
                onDismiss = { keyboard.dismissLanguageMenu() }
            )
        }
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
            .clearAndSetSemantics {}
    )
    val title = stringResource(R.string.key_language)
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .windowInsetsPadding(keyboardBottomInsets)
            .padding(start = 8.dp, bottom = bottomOffset)
            // TalkBack announces the menu when it opens and offers Dismiss to close it
            .semantics {
                paneTitle = title
                dismiss { onDismiss(); true }
            }
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
            .semantics { if (checked) selected = true }
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
            description = stringResource(if (key.primary == "?123") R.string.key_numbers else R.string.key_more_symbols)
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

/** What TalkBack reads and offers for a key. */
private data class KeyA11y(
    val label: String,
    val state: String? = null,
    val selected: Boolean = false,
    val actions: List<CustomAccessibilityAction> = emptyList()
)

private val vowelForSign = mapOf(
    "ा" to "आ", "ि" to "इ", "ी" to "ई", "ु" to "उ", "ू" to "ऊ", "ृ" to "ऋ",
    "े" to "ए", "ै" to "ऐ", "ो" to "ओ", "ौ" to "औ"
)

/**
 * What TalkBack says for a character key. Devanagari vowel signs and marks alone are silent or
 * garbled in speech, so they get names; capital Latin letters say so.
 */
@Composable
private fun spokenCharacter(text: String): String = when (text) {
    "्" -> stringResource(R.string.key_halanta)
    "ं" -> stringResource(R.string.key_anusvara)
    "ँ" -> stringResource(R.string.key_chandrabindu)
    "ः" -> stringResource(R.string.key_visarga)
    else -> vowelForSign[text]?.let { stringResource(R.string.key_vowel_sign, it) }
        ?: if (text.length == 1 && text != text.lowercase()) stringResource(R.string.key_capital, text) else text
}

/** The current input mode, the language key's TalkBack state. */
@Composable
private fun languageName(mode: LanguageMode): String = stringResource(
    when (mode) {
        LanguageMode.NEPALI -> R.string.key_mode_traditional
        LanguageMode.TRANSLITERATION -> R.string.key_mode_romanized
        else -> R.string.key_mode_english
    }
)

/**
 * One key. Taps fire on release; a long-press types the corner hint; backspace deletes on
 * touch-down and repeats while held. Each press gives a light haptic tick, and character keys
 * show a preview bubble above the finger.
 *
 * TalkBack activates it through its click action and reaches the long-press through custom actions.
 */
@Composable
private fun Key(
    key: KeyData,
    visual: KeyVisual,
    a11y: KeyA11y,
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
    // The gesture block below only restarts when its keys change, so read the settings and the
    // key through updated state; otherwise keys that never restart (space, ⌫, ↵, digits) keep
    // the sound/vibrate values from when they were first drawn.
    val currentFeel by rememberUpdatedState(feel)
    val currentKey by rememberUpdatedState(key)
    val repeating = onRepeatStart != null
    val hasLongPress = onLongPress != null
    // Press feedback: a quick tint (in fast, out slower) and a slight shrink that springs back.
    // The tint mixes in the text colour, so it darkens keys in light mode and lightens them in dark.
    val background by animateColorAsState(
        targetValue = if (pressed) lerp(visual.background, colors.keyText, PressTint) else visual.background,
        animationSpec = tween(if (pressed) 60 else 150),
        label = "keyBackground"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "keyScale"
    )

    // No minimum touch target: Compose would otherwise widen these narrow keys to 48 dp for
    // touches and TalkBack, and the widened bounds overlap, so TalkBack's focus box sat off the key.
    // The keys tile the row instead (each draws half a gap in), so touches in the gaps still land.
    val viewConfiguration = LocalViewConfiguration.current
    val cellConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val minimumTouchTargetSize = DpSize.Zero
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides cellConfiguration) {
    Box(
        modifier = modifier
            .height(height)
            // TalkBack's double-tap never reaches the gesture below: give it a click action, and
            // what a long-press does as custom actions. The corner hint is part of this one node.
            .clearAndSetSemantics {
                // Text with a locale span, so Devanagari is spoken in a voice that can read it
                text = withNepaliSpeech(a11y.label)
                a11y.state?.let { stateDescription = it }
                if (a11y.selected) selected = true
                onClick { tap(); true }
                if (a11y.actions.isNotEmpty()) customActions = a11y.actions
            }
            .pointerInput(key.primary, repeating, hasLongPress) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        if (currentFeel.vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (currentFeel.sound) playKeySound(view.context, currentKey)
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
                .padding(horizontal = KeyGap / 2)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(if (colors.keyShadow == Color.Transparent) 0.dp else 0.5.dp, KeyShape)
                .clip(KeyShape)
                .background(background),
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
