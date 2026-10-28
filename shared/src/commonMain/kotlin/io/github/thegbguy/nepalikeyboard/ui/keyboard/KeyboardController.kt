package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.data.emoji.EmojiRecents
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipItem
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardHistory
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardSource
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardStorage
import io.github.thegbguy.nepalikeyboard.domain.clipboard.CopiedText
import io.github.thegbguy.nepalikeyboard.domain.clipboard.InMemoryClipboardStorage
import io.github.thegbguy.nepalikeyboard.domain.Backspace
import io.github.thegbguy.nepalikeyboard.domain.handwriting.HandwritingRecognizer
import io.github.thegbguy.nepalikeyboard.domain.handwriting.InkStroke
import io.github.thegbguy.nepalikeyboard.domain.model.EnterAction
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartAction
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartSuggester
import io.github.thegbguy.nepalikeyboard.domain.transliteration.NepaliTransliterationEngine
import io.github.thegbguy.nepalikeyboard.domain.transliteration.TransliterationEngine
import io.github.thegbguy.nepalikeyboard.domain.voice.VoiceError
import io.github.thegbguy.nepalikeyboard.domain.voice.VoiceInput
import io.github.thegbguy.nepalikeyboard.domain.voice.VoiceListener
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.EnglishLayout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.NepaliLayout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.SymbolLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * What the keyboard does: key presses, Romanized typing, the panels (emoji, clipboard,
 * handwriting), smart suggestions and voice. Shared by Android (held by `KeyboardViewModel`) and
 * the iOS keyboard extension. The platform supplies the field ([editor]), the settings and the
 * system services, and draws [state].
 *
 * Coroutines (backspace repeat, handwriting, voice) run in the scope from [scopeFactory], made on
 * first use; [close] cancels it.
 */
class KeyboardController(scopeFactory: () -> CoroutineScope = { MainScope() }) {

    private val scopeDelegate = lazy(scopeFactory)
    private val scope: CoroutineScope by scopeDelegate

    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    /**
     * Supplies the focused field, read on every use: it changes each time a new field gets focus,
     * so it must never be cached.
     */
    var editor: () -> EditorConnection? = { null }
    private val inputConnection: EditorConnection? get() = editor()

    /** Stored settings (Nepali style). The platform replaces this with the on-device store. */
    var prefs: KeyboardPrefs = InMemoryKeyboardPrefs()

    /** The system clipboard. Set by the platform; null in tests that don't need it. */
    var clipboardSource: ClipboardSource? = null

    /** Saved clipboard items. The platform replaces this with the on-device file. */
    var clipboardStorage: ClipboardStorage = InMemoryClipboardStorage()

    /** Handwriting recognition. Set by the platform; null in tests that don't need it. */
    var handwritingRecognizer: HandwritingRecognizer? = null

    /** Smart suggestions (amounts, dates, units); replaced in tests. */
    var smartSuggester = SmartSuggester()

    /** Wall-clock time for clipboard items; replaced in tests. */
    var clock: () -> Long = ::nowMillis

    /** The copy last pasted from the strip's Paste suggestion, so it isn't offered again. */
    private var usedCopyId: Long? = null

    /** Asks for the microphone permission (Android opens a small activity). Set by the platform. */
    var onRequestMicPermission: (() -> Unit)? = null

    /** Asks the system to show the keyboard again for the focused field. Set by the platform. */
    var onRequestShowKeyboard: () -> Unit = {}

    /** Opens the app's settings. Set by the platform. */
    var onOpenSettings: () -> Unit = {}

    fun openSettings() {
        commitTransliterationBuffer()
        onOpenSettings()
    }

    /** Opens the system keyboard picker. Set by the platform. */
    var onSwitchKeyboard: () -> Unit = {}

    // Job for handling backspace long-press repeating deletion
    private var backspaceJob: Job? = null

    // Track last shift press time for double-tap caps lock detection
    private var lastShiftPressTime: Long = 0
    private val DOUBLE_TAP_TIMEOUT_MS = 300L // Time window for double-tap detection

    // Transliteration engine for romanized to Devanagari conversion
    private val transliterationEngine: TransliterationEngine = NepaliTransliterationEngine()

    // Speech recognition (set by the platform)
    private var speechRecognitionManager: VoiceInput? = null

    // Job for clearing voice error after delay
    private var voiceErrorClearJob: Job? = null

    init {
        // Initialize with English layout
        updateLayout()
    }

    /**
     * Handles key click events.
     */
    fun onKeyClick(key: KeyData) {
        if (_state.value.pasteSuggestion != null) _state.update { it.copy(pasteSuggestion = null) }
        // Keys that change the text drop smart suggestions; the cursor update that follows makes new ones.
        // Shift and the layout keys leave them.
        val editsText = key.keyType == KeyType.CHARACTER || key.keyType == KeyType.ACTION || key.primary == "⌫"
        if (editsText && _state.value.smartActions.isNotEmpty()) _state.update { it.copy(smartActions = emptyList()) }
        when (key.keyType) {
            KeyType.CHARACTER -> handleCharacterKey(key)
            KeyType.ACTION -> handleActionKey(key)
            KeyType.MODIFIER -> handleModifierKey(key)
            KeyType.FUNCTION -> handleFunctionKey(key)
            KeyType.SPACER -> { /* Do nothing */ }
        }
    }

    /**
     * Handles backspace long-press start.
     * Starts a coroutine that deletes characters repeatedly with increasing speed.
     */
    fun onBackspaceLongPressStart() {
        // Clear transliteration buffer immediately on long press
        if (_state.value.transliterationBuffer.isNotEmpty()) {
            inputConnection?.finishComposingText()
            _state.update {
                it.copy(
                    transliterationBuffer = TransliterationBuffer(),
                    suggestions = emptyList(),
                    showSuggestionBar = false
                )
            }
        }

        // Cancel any existing job
        backspaceJob?.cancel()

        backspaceJob = scope.launch {
            // Initial delay before repeating starts
            delay(500)

            // Start with slower deletion
            var delayTime = 100L

            while (isActive) {
                deleteBackward()
                delay(delayTime)

                // Speed up deletion over time (minimum 30ms delay)
                if (delayTime > 30) {
                    delayTime = (delayTime * 0.9).toLong()
                }
            }
        }
    }

    /**
     * Handles backspace long-press end.
     * Stops the repeating deletion.
     */
    fun onBackspaceLongPressEnd() {
        backspaceJob?.cancel()
        backspaceJob = null
    }

    /** Deletes one character, or a whole emoji (see [Backspace]). A selection is deleted as a whole. */
    private fun deleteBackward() {
        val ic = inputConnection ?: return
        if (!ic.selectedText().isNullOrEmpty()) {
            ic.commitText("")
            return
        }
        val before = ic.textBeforeCursor(32) ?: ""
        ic.deleteBeforeCursor(Backspace.length(before))
    }

    // ==================== Handwriting ====================

    private var handwritingJob: Job? = null
    private var recognizeJob: Job? = null

    /**
     * Opens the handwriting panel in place of the keys. Until the user has agreed, it shows what
     * turning handwriting on means (Google model download and ML Kit diagnostics) instead.
     */
    fun openHandwriting() {
        commitTransliterationBuffer()
        if (_state.value.isVoiceListening) stopVoiceInput()
        _state.update {
            it.copy(
                handwriting = if (prefs.handwriting) HandwritingPhase.CHECKING else HandwritingPhase.CONSENT,
                inkStrokes = emptyList(),
                inkCandidates = emptyList(),
                showEmoji = false,
                showClipboard = false,
                showLanguageMenu = false,
                pasteSuggestion = null,
                smartActions = emptyList()
            )
        }
        if (prefs.handwriting) prepareModel()
    }

    /** The user agreed on the consent card: remember it and download the model. */
    fun onHandwritingAccept() {
        prefs.handwriting = true
        prepareModel()
    }

    /** Download again after a failure. */
    fun onHandwritingRetry() = prepareModel()

    private fun prepareModel() {
        val recognizer = handwritingRecognizer ?: return
        handwritingJob?.cancel()
        handwritingJob = scope.launch {
            val ready = runCatching { recognizer.isModelReady() }.getOrDefault(false)
            if (!ready) {
                setHandwritingPhase(HandwritingPhase.DOWNLOADING)
                val ok = runCatching { recognizer.download() }.isSuccess
                if (!ok) {
                    setHandwritingPhase(HandwritingPhase.FAILED)
                    return@launch
                }
            }
            setHandwritingPhase(HandwritingPhase.READY)
        }
    }

    private fun setHandwritingPhase(phase: HandwritingPhase) {
        if (_state.value.handwriting != null) _state.update { it.copy(handwriting = phase) }
    }

    /** A finished stroke on the writing area ([width] × [height] pixels): recognise all strokes so far. */
    fun onInkStroke(stroke: InkStroke, width: Float, height: Float) {
        if (stroke.isEmpty() || _state.value.handwriting != HandwritingPhase.READY) return
        _state.update { it.copy(inkStrokes = it.inkStrokes + listOf(stroke)) }
        recognize(width, height)
    }

    private var inkArea = 0f to 0f

    private fun recognize(width: Float = inkArea.first, height: Float = inkArea.second) {
        inkArea = width to height
        val recognizer = handwritingRecognizer ?: return
        val strokes = _state.value.inkStrokes
        recognizeJob?.cancel()
        if (strokes.isEmpty()) {
            _state.update { it.copy(inkCandidates = emptyList()) }
            return
        }
        val preContext = inputConnection?.textBeforeCursor(PRE_CONTEXT_CHARS) ?: ""
        recognizeJob = scope.launch {
            val candidates = runCatching { recognizer.recognize(strokes, width, height, preContext) }.getOrDefault(emptyList())
            // Ignore a late answer for strokes that have since changed
            if (_state.value.inkStrokes == strokes) _state.update { it.copy(inkCandidates = candidates) }
        }
    }

    /** Types [text] (a candidate) and clears the writing area. */
    fun onInkCandidate(text: String) {
        inputConnection?.commitText(text)
        clearInk()
    }

    /** Commits the best guess for what's written, if anything is. */
    private fun commitInk() {
        _state.value.inkCandidates.firstOrNull()?.let { inputConnection?.commitText(it) }
        clearInk()
    }

    private fun clearInk() {
        recognizeJob?.cancel()
        _state.update { it.copy(inkStrokes = emptyList(), inkCandidates = emptyList()) }
    }

    fun onInkSpace() {
        commitInk()
        inputConnection?.commitText(" ")
    }

    fun onInkEnter() {
        commitInk()
        handleEnter()
    }

    /** Typed punctuation from the panel's bottom row. */
    fun onInkText(text: String) {
        commitInk()
        inputConnection?.commitText(text)
    }

    /** Backspace removes the last stroke while something is written, else deletes text. */
    fun onInkBackspace() {
        val strokes = _state.value.inkStrokes
        if (strokes.isEmpty()) {
            deleteBackward()
            return
        }
        _state.update { it.copy(inkStrokes = strokes.dropLast(1)) }
        recognize()
    }

    /** Closes the panel; what's written is typed first so it isn't lost. */
    fun closeHandwriting() {
        if (_state.value.handwriting == HandwritingPhase.READY) commitInk()
        handwritingJob?.cancel()
        recognizeJob?.cancel()
        _state.update { it.copy(handwriting = null, inkStrokes = emptyList(), inkCandidates = emptyList()) }
    }

    // ==================== Emoji ====================

    /** Opens the emoji panel in place of the keys. */
    fun openEmoji() {
        commitTransliterationBuffer()
        if (_state.value.isVoiceListening) stopVoiceInput()
        _state.update {
            it.copy(
                showEmoji = true,
                showClipboard = false,
                handwriting = null,
                pasteSuggestion = null,
                smartActions = emptyList(),
                showLanguageMenu = false,
                emojiRecents = prefs.emojiRecents,
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
    }

    fun closeEmoji() {
        _state.update { it.copy(showEmoji = false) }
    }

    /**
     * Types [emoji] and remembers it in recents, unless the field asks for no learning. The panel
     * keeps showing the recents it opened with, so nothing moves under the finger.
     */
    fun onEmojiClick(emoji: String) {
        inputConnection?.commitText(emoji)
        val field = _state.value.field
        if (field.isPrivate || field.noLearning) return
        prefs.emojiRecents = EmojiRecents.add(prefs.emojiRecents, emoji)
    }

    /** Backspace in the emoji panel. */
    fun onEmojiBackspace() = deleteBackward()

    /**
     * Saving is allowed when clipboard history is on and the field isn't a password or incognito
     * field. Reading saved items is always fine.
     */
    private fun mayRecordClipboard(): Boolean {
        val field = _state.value.field
        return prefs.clipboardHistory && !field.isPrivate && !field.noLearning
    }

    /**
     * Something was copied somewhere on the phone: save it (unless it's marked sensitive or
     * history is off), offer it as Paste in the strip, and refresh the panel if it's open.
     */
    fun onSystemCopy() {
        val source = clipboardSource ?: return
        val copied = source.read() ?: return
        var items = clipboardStorage.load()
        if (!copied.sensitive && mayRecordClipboard()) {
            items = ClipboardHistory.add(items, copied.text, clock())
            clipboardStorage.save(items)
        }
        val offer = !_state.value.field.isPrivate
        _state.update {
            it.copy(
                pasteSuggestion = if (offer) copied else null,
                copied = if (it.showClipboard) copied else it.copied,
                clipItems = items
            )
        }
    }

    /**
     * Opens the clipboard panel in place of the keys. The current copy is read again (it may
     * predate the keyboard starting) and added to the history unless it's marked sensitive.
     */
    fun openClipboard() {
        commitTransliterationBuffer()
        if (_state.value.isVoiceListening) stopVoiceInput()
        val copied = clipboardSource?.read()
        var items = clipboardStorage.load()
        if (copied != null && !copied.sensitive && mayRecordClipboard()) {
            items = ClipboardHistory.add(items, copied.text, clock())
            clipboardStorage.save(items)
        }
        _state.update {
            it.copy(
                showClipboard = true,
                showEmoji = false,
                handwriting = null,
                showLanguageMenu = false,
                pasteSuggestion = null,
                smartActions = emptyList(),
                copied = copied,
                clipItems = items,
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
    }

    fun closeClipboard() {
        _state.update { it.copy(showClipboard = false, copied = null) }
    }

    /** Pastes [text] from the panel, moves it to the top of the history, and goes back to the keys. */
    fun onClipPaste(text: String) {
        inputConnection?.commitText(text)
        val sensitiveCopy = _state.value.copied?.let { it.sensitive && it.text == text } == true
        if (!sensitiveCopy && mayRecordClipboard()) {
            clipboardStorage.save(ClipboardHistory.add(clipboardStorage.load(), text, clock()))
        }
        closeClipboard()
    }

    fun onClipPin(text: String, pinned: Boolean) =
        updateClipItems { ClipboardHistory.setPinned(it, text, pinned) }

    /** Deletes [text] from the history. If it's the phone's current copy, that's cleared too. */
    fun onClipDelete(text: String) {
        if (_state.value.copied?.text == text) {
            clipboardSource?.clear()
            _state.update { it.copy(copied = null) }
        }
        updateClipItems { ClipboardHistory.delete(it, text) }
    }

    /**
     * The panel's "Clear all": pinned items stay. The phone's current copy is cleared too unless
     * it's pinned, or it would stay on screen and be saved again the next time the panel opens.
     */
    fun onClipClearAll() {
        val copied = _state.value.copied
        if (copied != null && clipboardStorage.load().none { it.text == copied.text && it.pinned }) {
            clipboardSource?.clear()
            _state.update { it.copy(copied = null, pasteSuggestion = null) }
        }
        updateClipItems(ClipboardHistory::clearUnpinned)
    }

    private fun updateClipItems(change: (List<ClipItem>) -> List<ClipItem>) {
        val items = change(clipboardStorage.load())
        clipboardStorage.save(items)
        _state.update { it.copy(clipItems = items) }
    }

    /** The strip's Paste suggestion, offered when something was copied in the last two minutes. */
    fun onPasteSuggestion() {
        val copied = _state.value.pasteSuggestion ?: return
        usedCopyId = clipboardSource?.recentCopyId(PASTE_SUGGESTION_WINDOW_MS)
        _state.update { it.copy(pasteSuggestion = null) }
        commitTransliterationBuffer()
        inputConnection?.commitText(copied.text)
        if (!copied.sensitive && mayRecordClipboard()) {
            clipboardStorage.save(ClipboardHistory.add(clipboardStorage.load(), copied.text, clock()))
        }
    }

    /** The copy to offer as Paste when a field opens: made in the last two minutes and not yet pasted from the strip. */
    private fun recentCopyToOffer(field: FieldInfo): CopiedText? {
        if (field.isPrivate) return null
        val source = clipboardSource ?: return null
        val id = source.recentCopyId(PASTE_SUGGESTION_WINDOW_MS) ?: return null
        if (id == usedCopyId) return null
        return source.read()
    }

    /**
     * The language key cycles English → the user's Nepali style → the other Nepali style → English.
     * The saved style (settings, long-press menu) only sets which Nepali mode comes first; cycling
     * doesn't change it. Where Romanized isn't allowed it's English ⇄ traditional.
     */
    fun switchLanguage() {
        val field = _state.value.field
        val preferred = nepaliMode(field)
        val other = if (preferred == LanguageMode.NEPALI) LanguageMode.TRANSLITERATION else LanguageMode.NEPALI
        val otherAllowed = other != LanguageMode.TRANSLITERATION || field.allowsTransliteration
        val next = when (_state.value.currentLanguage) {
            LanguageMode.ENGLISH -> preferred
            preferred -> if (otherAllowed) other else LanguageMode.ENGLISH
            else -> LanguageMode.ENGLISH
        }
        setLanguage(next)
    }

    /** Long-press on the language key. */
    fun onLanguageLongPress() {
        _state.update { it.copy(showLanguageMenu = true, nepaliStyle = prefs.nepaliStyle) }
    }

    fun dismissLanguageMenu() {
        _state.update { it.copy(showLanguageMenu = false) }
    }

    /** Menu choice: type Nepali Romanized ([LanguageMode.TRANSLITERATION]) or with the traditional layout ([LanguageMode.NEPALI]). */
    fun setNepaliStyle(style: LanguageMode) {
        prefs.nepaliStyle = style
        dismissLanguageMenu()
        setLanguage(nepaliMode(_state.value.field))
    }

    /**
     * Settings changed outside the keyboard (the app's style choice): bring the open keyboard in
     * line. Does nothing when it already matches, so the keyboard's own writes don't loop.
     */
    fun onPrefsChanged() {
        _state.update { it.copy(feel = prefs.feel) }
        val field = _state.value.field
        val target = when {
            field.prefersEnglish -> return
            prefs.typeNepali -> nepaliMode(field)
            else -> LanguageMode.ENGLISH
        }
        if (target != _state.value.currentLanguage) setLanguage(target)
    }

    /** Menu choice: the system keyboard picker. */
    fun switchKeyboard() {
        dismissLanguageMenu()
        onSwitchKeyboard()
    }

    /** The Nepali mode for this field: the user's style, or traditional where Romanized isn't allowed. */
    private fun nepaliMode(field: FieldInfo): LanguageMode {
        val style = prefs.nepaliStyle
        return if (style == LanguageMode.TRANSLITERATION && !field.allowsTransliteration) LanguageMode.NEPALI else style
    }

    private fun setLanguage(mode: LanguageMode) {
        commitTransliterationBuffer()
        val field = _state.value.field
        // A choice made in an email/URL/password field applies to that field only
        if (!field.prefersEnglish) prefs.typeNepali = mode != LanguageMode.ENGLISH
        _state.update {
            it.copy(
                currentLanguage = mode,
                smartActions = emptyList(),
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
        updateLayout()
    }

    /**
     * Toggles between main and numbers layout.
     */
    fun toggleNumbersLayout() {
        // Auto-commit buffer if switching away from transliteration
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION
            && _state.value.transliterationBuffer.isNotEmpty()
        ) {
            val buffer = _state.value.transliterationBuffer
            val output = transliterationEngine.commitBuffer(buffer)
            // commitText replaces composing text
            inputConnection?.commitText(output)

            _state.update {
                it.copy(
                    transliterationBuffer = TransliterationBuffer(),
                    suggestions = emptyList(),
                    showSuggestionBar = false
                )
            }
        }

        _state.update { it.copy(showingNumbers = !it.showingNumbers) }
        updateLayout()
    }

    /** Flips between symbol pages 1/2 and 2/2. */
    fun toggleSymbolsLayout() {
        _state.update { it.copy(showingSymbols = !it.showingSymbols) }
        updateLayout()
    }

    private fun handleCharacterKey(key: KeyData) {
        val output = key.getOutput(_state.value.isShifted)
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION) settlePendingPoint(output)
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION && endsRomanizedWord(output)) {
            // Punctuation ends the word: "huncha," must commit हुन्छ, not look up "huncha,".
            val afterDigit = output == "." && endsWithDigit()
            commitTransliterationBuffer()
            inputConnection?.commitText(if (output == "." && !afterDigit) "।" else output)
            pendingPoint = afterDigit
        } else if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION) {
            // Transliteration mode - buffer input and show romanized text
            val currentBuffer = _state.value.transliterationBuffer

            val newBuffer = transliterationEngine.processInput(currentBuffer, output)

            _state.update {
                it.copy(
                    transliterationBuffer = newBuffer,
                    suggestions = newBuffer.candidates.map { candidate -> candidate.devanagari },
                    showSuggestionBar = newBuffer.isNotEmpty()
                )
            }

            // Show romanized text as composing text (underlined in input field)
            // User will see "namaste" not "नमस्ते" until they press space
            inputConnection?.setComposingText(newBuffer.romanizedText)

        } else {
            // Direct input mode (English and traditional Nepali)
            commitDirect(output)
        }

        // Reset shift after character input (unless caps lock)
        if (_state.value.isShifted && !_state.value.isCapsLock) {
            _state.update { it.copy(isShifted = false) }
        }
    }

    private fun handleActionKey(key: KeyData) {
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION) settlePendingPoint(key.primary)
        when (key.primary) {
            " " -> {
                // Space key
                if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION
                    && _state.value.transliterationBuffer.isNotEmpty()
                ) {
                    // Commit transliteration buffer
                    val buffer = _state.value.transliterationBuffer
                    val output = transliterationEngine.commitBuffer(buffer)

                    // commitText automatically replaces the composing text (romanized)
                    // with the Devanagari output, so no need to finishComposingText first
                    inputConnection?.commitText(output + " ")

                    // Clear buffer
                    _state.update {
                        it.copy(
                            transliterationBuffer = TransliterationBuffer(),
                            suggestions = emptyList(),
                            showSuggestionBar = false
                        )
                    }
                } else {
                    // Normal space
                    inputConnection?.commitText(" ")
                }
            }
            "↵" -> handleEnter()
        }
    }

    /**
     * Long-press on a key: type its alternate character (the Nepali shift character or a symbol)
     * directly. A pending Romanized word is committed first.
     */
    fun onKeyLongPress(alternate: String) {
        commitTransliterationBuffer()
        commitDirect(alternate)
        if (_state.value.isShifted && !_state.value.isCapsLock) {
            _state.update { it.copy(isShifted = false) }
        }
    }

    /**
     * Types [text] directly. A vowel sign typed right after अ becomes the single vowel letter
     * (अ + ा → आ, not अा), which is how Nepali is written and what search and spell-check expect.
     */
    private fun commitDirect(text: String) {
        val ic = inputConnection ?: return
        val combined = INDEPENDENT_VOWELS[text]
        if (combined != null && ic.textBeforeCursor(1) == "अ") {
            ic.beginBatchEdit()
            ic.deleteBeforeCursor(1)
            ic.commitText(combined)
            ic.endBatchEdit()
        } else {
            ic.commitText(text)
        }
    }

    /** Keep the Romanized word as typed (Latin), instead of converting it. */
    fun onKeepLatin() {
        val buffer = _state.value.transliterationBuffer
        if (buffer.isEmpty()) return
        inputConnection?.commitText(buffer.romanizedText + " ")
        _state.update {
            it.copy(
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
    }

    /**
     * Enter: run the field's action (Send, Search, Go, Next...) or insert a new line.
     * Any pending Romanized word is committed first so it isn't lost.
     */
    private fun handleEnter() {
        commitTransliterationBuffer()
        val field = _state.value.field
        val ic = inputConnection ?: return
        if (field.enterAction == EnterAction.NEWLINE) {
            if (field.sendsKeyEvents) {
                ic.sendEnterKey()
            } else {
                ic.commitText("\n")
            }
        } else {
            ic.performEditorAction(field.imeActionId)
        }
    }

    /**
     * A single punctuation mark (not a letter, digit, or the ~ ^ chandrabindu marks typed inside
     * words) ends the Romanized word instead of joining its spelling.
     */
    private fun endsRomanizedWord(output: String): Boolean =
        output.length == 1 && !output[0].isLetterOrDigit() && output[0] !in "~^" && !output[0].isWhitespace()

    /**
     * A "." typed right after a digit in Romanized mode stays a decimal point (रु ०.५०) instead of
     * becoming ।. If the next key isn't a digit, it was a full stop after all: turn it into ।.
     */
    private var pendingPoint = false

    private fun endsWithDigit(): Boolean {
        val buffered = _state.value.transliterationBuffer.romanizedText
        if (buffered.isNotEmpty()) return buffered.last().isDigit()
        val before = inputConnection?.textBeforeCursor(1)?.lastOrNull() ?: return false
        return before in '0'..'9' || before in '०'..'९'
    }

    private fun settlePendingPoint(next: String) {
        if (!pendingPoint) return
        pendingPoint = false
        if (next.length == 1 && (next[0] in '0'..'9')) return
        val ic = inputConnection ?: return
        if (ic.textBeforeCursor(1) != ".") return
        ic.beginBatchEdit()
        ic.deleteBeforeCursor(1)
        ic.commitText("।")
        ic.endBatchEdit()
    }

    /** Commits the Romanized word being typed as Devanagari, if there is one. */
    private fun commitTransliterationBuffer() {
        val buffer = _state.value.transliterationBuffer
        if (_state.value.currentLanguage != LanguageMode.TRANSLITERATION || buffer.isEmpty()) return
        inputConnection?.commitText(transliterationEngine.commitBuffer(buffer))
        _state.update {
            it.copy(
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
    }

    private fun handleModifierKey(key: KeyData) {
        when (key.primary) {
            "⇧" -> {
                // Shift key - handle single tap, double tap (caps lock), and caps lock toggle
                val currentTime = nowMillis()
                val currentState = _state.value

                when {
                    // If caps lock is already enabled, turn it off
                    currentState.isCapsLock -> {
                        _state.update {
                            it.copy(
                                isShifted = false,
                                isCapsLock = false
                            )
                        }
                        lastShiftPressTime = 0 // Reset double-tap detection
                    }
                    // If shift was pressed recently (double-tap), enable caps lock
                    currentState.isShifted && (currentTime - lastShiftPressTime) <= DOUBLE_TAP_TIMEOUT_MS -> {
                        _state.update {
                            it.copy(
                                isShifted = true,
                                isCapsLock = true
                            )
                        }
                        lastShiftPressTime = currentTime
                    }
                    // Otherwise, just toggle shift (single tap)
                    else -> {
                        _state.update {
                            it.copy(
                                isShifted = !currentState.isShifted,
                                isCapsLock = false
                            )
                        }
                        lastShiftPressTime = currentTime
                    }
                }
            }
        }
    }

    private fun handleFunctionKey(key: KeyData) {
        when (key.primary) {
            "⌫" -> {
                // Backspace key
                if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION
                    && _state.value.transliterationBuffer.isNotEmpty()
                ) {
                    // Delete from buffer
                    val newBuffer = transliterationEngine.handleBackspace(
                        _state.value.transliterationBuffer
                    )

                    _state.update {
                        it.copy(
                            transliterationBuffer = newBuffer,
                            suggestions = newBuffer.candidates.map { candidate -> candidate.devanagari },
                            showSuggestionBar = newBuffer.isNotEmpty()
                        )
                    }

                    if (newBuffer.isEmpty()) {
                        // Remove the last letter: finishing composing would keep it as typed
                        inputConnection?.setComposingText("")
                        inputConnection?.finishComposingText()
                    } else {
                        // Show romanized text, not Devanagari
                        inputConnection?.setComposingText(newBuffer.romanizedText)
                    }
                } else {
                    deleteBackward()
                }
            }
            "🌐" -> {
                // Language switch key
                switchLanguage()
            }
            "?123" -> {
                // Letters to symbol page 1
                _state.update { it.copy(showingSymbols = false) }
                toggleNumbersLayout()
            }
            SymbolLayout.PAGE_ONE, SymbolLayout.PAGE_TWO -> {
                // Flip between the two symbol pages
                toggleSymbolsLayout()
            }
            "ABC" -> {
                // Switch back to main layout
                if (_state.value.showingSymbols) {
                    toggleSymbolsLayout()
                }
                if (_state.value.showingNumbers) {
                    toggleNumbersLayout()
                }
            }
        }
    }

    private fun updateLayout() {
        val currentState = _state.value
        val newLayout = when {
            currentState.showingNumbers -> {
                SymbolLayout.getLayout(
                    page = if (currentState.showingSymbols) 2 else 1,
                    mode = currentState.currentLanguage
                )
            }
            else -> {
                when (currentState.currentLanguage) {
                    LanguageMode.NEPALI -> NepaliLayout.getLayout()
                    LanguageMode.TRANSLITERATION -> EnglishLayout.getLayout() // Use English keys for transliteration
                    LanguageMode.ENGLISH -> EnglishLayout.getLayout()
                }
            }
        }

        _state.update { it.copy(currentLayout = newLayout) }
    }

    /**
     * Handles suggestion click in transliteration mode.
     * Commits the selected suggestion and clears the buffer.
     */
    fun onSuggestionClick(suggestion: String) {
        if (_state.value.currentLanguage != LanguageMode.TRANSLITERATION) return

        // Commit the selected suggestion (replaces composing text)
        inputConnection?.commitText(suggestion + " ")

        // Clear buffer
        _state.update {
            it.copy(
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
    }

    /**
     * Called by the IME service each time a field gets focus.
     *
     * @param restarting The same field restarted input (e.g. the app replaced its text): keep the
     *   current mode and layout.
     */
    fun onInputStarted(field: FieldInfo, restarting: Boolean = false) {
        inputShown = true
        // Stop voice input if active
        if (_state.value.isVoiceListening) {
            stopVoiceInput()
        }

        if (restarting) {
            _state.update { it.copy(field = field) }
            if (!field.allowsTransliteration && _state.value.currentLanguage == LanguageMode.TRANSLITERATION) {
                _state.update { it.copy(currentLanguage = LanguageMode.ENGLISH) }
                updateLayout()
            }
            startPendingVoice()
            return
        }

        val language = when {
            field.prefersEnglish -> LanguageMode.ENGLISH
            prefs.typeNepali -> nepaliMode(field)
            else -> LanguageMode.ENGLISH
        }

        pendingPoint = false
        // Reset state when new input field is focused
        _state.update {
            it.copy(
                field = field,
                showLanguageMenu = false,
                showEmoji = false,
                showClipboard = false,
                handwriting = null,
                inkStrokes = emptyList(),
                inkCandidates = emptyList(),
                copied = null,
                pasteSuggestion = recentCopyToOffer(field),
                smartActions = emptyList(),
                feel = prefs.feel,
                currentLanguage = language,
                isShifted = false,
                isCapsLock = false,
                showingNumbers = field.prefersNumbers,
                showingSymbols = false,
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false,
                isVoiceListening = false,
                voiceError = null,
                partialVoiceResult = null
            )
        }
        updateLayout()
        startPendingVoice()
    }

    /**
     * Called by the IME service when the cursor or composing region changes. If the app or the
     * user moved the cursor away from the Romanized word, or the app removed it (e.g. cleared the
     * box after Send), drop the buffer so new letters don't get glued onto stale text.
     *
     * @param composingStart start of the composing region, or -1 if there is none
     */
    fun onSelectionChanged(selStart: Int, selEnd: Int, composingStart: Int, composingEnd: Int) {
        if (_state.value.transliterationBuffer.isNotEmpty()) {
            val stillComposing = composingStart >= 0 && selStart == selEnd && selEnd == composingEnd
            if (!stillComposing) {
                if (composingStart >= 0) inputConnection?.finishComposingText()
                _state.update {
                    it.copy(
                        transliterationBuffer = TransliterationBuffer(),
                        suggestions = emptyList(),
                        showSuggestionBar = false
                    )
                }
            }
        }
        refreshSmartActions(hasSelection = selStart != selEnd)
    }

    /**
     * Looks at the text before the cursor and offers conversions of it in the strip. Runs on
     * every cursor update, which also follows each key press, so a suggestion never outlives
     * the text it was made from.
     */
    private fun refreshSmartActions(hasSelection: Boolean) {
        val st = _state.value
        val off = hasSelection || !prefs.smartSuggestions || st.showEmoji || st.showClipboard || st.handwriting != null ||
            !smartSuggester.allows(st.field)
        val actions = if (off) emptyList() else {
            val composing = if (st.currentLanguage == LanguageMode.TRANSLITERATION) st.transliterationBuffer.romanizedText else ""
            val text = inputConnection
                ?.textBeforeCursor(SmartSuggester.CONTEXT_CHARS + composing.length)
                ?.toString()
            // The field holds the Romanized word as composing text; if it doesn't end with it, this update is stale.
            if (text == null || !text.endsWith(composing)) emptyList()
            else smartSuggester.suggest(text.dropLast(composing.length), composing, st.currentLanguage, st.field)
        }
        if (actions != st.smartActions) _state.update { it.copy(smartActions = actions) }
    }

    /**
     * Applies a smart suggestion in one batch edit: drop the Romanized word being typed, delete
     * the characters the suggestion replaces, then type it.
     */
    fun onSmartActionClick(action: SmartAction) {
        val ic = inputConnection ?: return
        val composing = _state.value.currentLanguage == LanguageMode.TRANSLITERATION &&
            _state.value.transliterationBuffer.isNotEmpty()
        ic.beginBatchEdit()
        if (composing) {
            ic.setComposingText("")
            ic.finishComposingText()
        }
        if (action.replace > 0) ic.deleteBeforeCursor(action.replace)
        ic.commitText(action.text)
        ic.endBatchEdit()
        _state.update {
            it.copy(
                smartActions = emptyList(),
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
    }

    /** Called by the IME service when the field loses focus. */
    fun onInputFinished() {
        inputShown = false
        commitTransliterationBuffer()
        if (_state.value.handwriting == HandwritingPhase.READY) commitInk()
        if (_state.value.isVoiceListening) stopVoiceInput()
    }

    // ==================== Voice Typing Methods ====================

    /**
     * Sets speech recognition and how to check the microphone permission.
     * Called by the platform when it's ready.
     */
    fun setVoiceInput(voice: VoiceInput, hasMicPermission: () -> Boolean) {
        speechRecognitionManager = voice
        this.hasMicPermission = hasMicPermission
        setupSpeechRecognitionListener()
    }

    /** Whether the app may record audio. */
    private var hasMicPermission: () -> Boolean = { false }

    private fun setupSpeechRecognitionListener() {
        speechRecognitionManager?.setListener(object : VoiceListener {
            override fun onReadyForSpeech() {
                _state.update {
                    it.copy(
                        isVoiceListening = true,
                        voiceError = null,
                        partialVoiceResult = null
                    )
                }
            }

            override fun onBeginningOfSpeech() {
                // Speech input has begun
            }

            override fun onEndOfSpeech() {
                _state.update {
                    it.copy(isVoiceListening = false)
                }
            }

            override fun onResults(results: List<String>) {
                // Commit the first (best) result
                val text = results.firstOrNull()
                if (!text.isNullOrEmpty()) {
                    inputConnection?.commitText(text)
                }
                _state.update {
                    it.copy(
                        isVoiceListening = false,
                        partialVoiceResult = null
                    )
                }
            }

            override fun onError(error: VoiceError) {
                _state.update {
                    it.copy(
                        isVoiceListening = false,
                        voiceError = error,
                        partialVoiceResult = null
                    )
                }
                // Clear error after 3 seconds
                voiceErrorClearJob?.cancel()
                voiceErrorClearJob = scope.launch {
                    delay(3000)
                    _state.update { it.copy(voiceError = null) }
                }
            }

            override fun onPartialResults(partialResults: List<String>) {
                val partial = partialResults.firstOrNull()
                if (!partial.isNullOrEmpty()) {
                    _state.update {
                        it.copy(partialVoiceResult = partial)
                    }
                }
            }
        })
    }

    /**
     * Check if speech recognition is available.
     */
    fun isVoiceInputAvailable(): Boolean {
        return speechRecognitionManager?.isRecognitionAvailable() == true
    }

    /**
     * Check if microphone permission is granted.
     */
    private fun hasMicrophonePermission(): Boolean = hasMicPermission()

    /** The mic was allowed from the keyboard's first tap: start listening once the field is back. */
    private var startVoiceWhenBack = false
    private var micGrantedAt = 0L

    /** Whether the keyboard is on screen (between onInputStarted and onInputFinished). Voice only starts then. */
    private var inputShown = false

    /**
     * The permission screen closed. If the mic was allowed, start listening without a second tap:
     * when the field gets focus back (onInputStarted), or shortly after if the keyboard stayed up.
     */
    fun onMicPermissionResult(granted: Boolean) {
        startVoiceWhenBack = granted
        micGrantedAt = clock()
        if (!granted) return
        scope.launch {
            delay(MIC_RESUME_DELAY_MS)
            // Many apps don't bring the keyboard back after another activity was on top: ask for it.
            if (inputShown) startPendingVoice() else onRequestShowKeyboard()
        }
    }

    /**
     * Never listens with the keyboard hidden: if the app didn't bring the keyboard back after the
     * permission screen, the pending start waits for it, but only for a few seconds, so reopening the
     * keyboard later doesn't start recording by surprise.
     */
    private fun startPendingVoice() {
        if (!startVoiceWhenBack || !inputShown) return
        startVoiceWhenBack = false
        if (clock() - micGrantedAt > MIC_PENDING_MS) return
        if (_state.value.field.allowsVoice && !_state.value.isVoiceListening) startVoiceInput()
    }

    /**
     * Start voice input.
     */
    fun startVoiceInput() {
        if (!_state.value.field.allowsVoice) return

        // Check microphone permission first
        if (!hasMicrophonePermission()) {
            onRequestMicPermission?.let { request ->
                request()
                return
            }
            _state.update {
                it.copy(voiceError = VoiceError.MIC_PERMISSION)
            }
            // Clear error after 4 seconds
            voiceErrorClearJob?.cancel()
            voiceErrorClearJob = scope.launch {
                delay(4000)
                _state.update { it.copy(voiceError = null) }
            }
            return
        }

        // Auto-commit any pending transliteration buffer
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION
            && _state.value.transliterationBuffer.isNotEmpty()
        ) {
            val buffer = _state.value.transliterationBuffer
            val output = transliterationEngine.commitBuffer(buffer)
            inputConnection?.commitText(output)
            _state.update {
                it.copy(
                    transliterationBuffer = TransliterationBuffer(),
                    suggestions = emptyList(),
                    showSuggestionBar = false
                )
            }
        }

        speechRecognitionManager?.startListening()
    }

    /**
     * Stop voice input.
     */
    fun stopVoiceInput() {
        speechRecognitionManager?.stopListening()
        _state.update {
            it.copy(
                isVoiceListening = false,
                partialVoiceResult = null
            )
        }
    }

    /**
     * Toggle voice input on/off.
     */
    fun toggleVoiceInput() {
        if (_state.value.isVoiceListening) {
            stopVoiceInput()
        } else {
            startVoiceInput()
        }
    }

    /** Stops everything this controller started and releases speech recognition. */
    fun close() {
        voiceErrorClearJob?.cancel()
        speechRecognitionManager?.destroy()
        if (scopeDelegate.isInitialized()) scope.cancel()
    }

    private companion object {
        /** Vowel sign → the vowel letter it forms after अ. */
        val INDEPENDENT_VOWELS = mapOf("ा" to "आ", "ो" to "ओ", "ौ" to "औ", "ै" to "ऐ")

        /** Wait after the permission screen closes, so the app's field has focus again. */
        const val MIC_RESUME_DELAY_MS = 400L

        /** How long after allowing the mic a reopened keyboard still starts listening by itself. */
        const val MIC_PENDING_MS = 5_000L

        /** Text before the cursor given to handwriting recognition as context. */
        const val PRE_CONTEXT_CHARS = 20

        /** How long after a copy the strip offers Paste. */
        const val PASTE_SUGGESTION_WINDOW_MS = 2 * 60 * 1000L
    }
}

/**
 * State data class for the keyboard.
 */
data class KeyboardState(
    val field: FieldInfo = FieldInfo(),
    val showLanguageMenu: Boolean = false,
    /** The emoji panel replaces the keys. */
    val showEmoji: Boolean = false,
    /** Recents as they were when the panel opened. */
    val emojiRecents: List<String> = emptyList(),
    /** The clipboard panel replaces the keys. */
    val showClipboard: Boolean = false,
    /** The phone's current copy, read when the clipboard panel opened. */
    val copied: CopiedText? = null,
    /** Saved clipboard items, most recent first. */
    val clipItems: List<ClipItem> = emptyList(),
    /** The idle strip offers this recent copy as Paste (copied in the last two minutes). */
    val pasteSuggestion: CopiedText? = null,
    /** The handwriting panel replaces the keys while this is set. */
    val handwriting: HandwritingPhase? = null,
    /** Finished strokes on the writing area, and what they're recognised as (best first). */
    val inkStrokes: List<InkStroke> = emptyList(),
    val inkCandidates: List<String> = emptyList(),
    /** Conversions of the text before the cursor (amounts, dates, units), at most two. */
    val smartActions: List<SmartAction> = emptyList(),
    /** The saved Nepali style, shown checked in the language menu. */
    val nepaliStyle: LanguageMode = LanguageMode.TRANSLITERATION,
    /** Key height, vibration and sound from Settings. */
    val feel: KeyFeel = KeyFeel(),
    val currentLanguage: LanguageMode = LanguageMode.ENGLISH,
    val currentLayout: KeyboardLayout = EnglishLayout.getLayout(),
    val isShifted: Boolean = false,
    val isCapsLock: Boolean = false,
    val showingNumbers: Boolean = false,
    val showingSymbols: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val transliterationBuffer: TransliterationBuffer = TransliterationBuffer(),
    val showSuggestionBar: Boolean = false,
    // Voice typing state
    val isVoiceListening: Boolean = false,
    val voiceError: VoiceError? = null,
    val partialVoiceResult: String? = null
)

/** What the handwriting panel shows. */
enum class HandwritingPhase {
    /** What turning it on means, with Download / Not now. */
    CONSENT,
    /** Checking whether the model is on the phone. */
    CHECKING,
    DOWNLOADING,
    /** The download failed; offers Try again. */
    FAILED,
    /** The writing area. */
    READY
}
