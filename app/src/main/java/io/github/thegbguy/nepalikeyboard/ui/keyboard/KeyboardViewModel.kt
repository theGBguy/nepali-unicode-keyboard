package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.thegbguy.nepalikeyboard.data.emoji.EmojiRecents
import io.github.thegbguy.nepalikeyboard.domain.Backspace
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.model.EnterAction
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import io.github.thegbguy.nepalikeyboard.domain.transliteration.NepaliTransliterationEngine
import io.github.thegbguy.nepalikeyboard.domain.transliteration.TransliterationEngine
import io.github.thegbguy.nepalikeyboard.domain.voice.SpeechRecognitionManager
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.EnglishLayout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.NepaliLayout
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.SymbolLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for managing keyboard state and handling key interactions.
 */
class KeyboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    /**
     * Supplies the focused field's InputConnection. Set by the keyboard view to read
     * [android.inputmethodservice.InputMethodService.getCurrentInputConnection] on every use:
     * the connection changes each time a new field gets focus, so it must never be cached.
     */
    var inputConnectionProvider: () -> InputConnection? = { null }
    private val inputConnection: InputConnection? get() = inputConnectionProvider()

    /** Stored settings (Nepali style). The IME service replaces this with the on-device store. */
    var prefs: KeyboardPrefs = InMemoryKeyboardPrefs()

    /** Asks for the microphone permission (opens a small activity). Set by the IME service. */
    var onRequestMicPermission: (() -> Unit)? = null

    /** Opens the app's settings. Set by the IME service. */
    var onOpenSettings: () -> Unit = {}

    fun openSettings() {
        commitTransliterationBuffer()
        onOpenSettings()
    }

    /** Opens Android's keyboard picker. Set by the IME service. */
    var onSwitchKeyboard: () -> Unit = {}

    // Job for handling backspace long-press repeating deletion
    private var backspaceJob: Job? = null

    // Track last shift press time for double-tap caps lock detection
    private var lastShiftPressTime: Long = 0
    private val DOUBLE_TAP_TIMEOUT_MS = 300L // Time window for double-tap detection

    // Transliteration engine for romanized to Devanagari conversion
    private val transliterationEngine: TransliterationEngine = NepaliTransliterationEngine()

    // Speech recognition manager (set by IME service)
    private var speechRecognitionManager: SpeechRecognitionManager? = null

    // Context for permission checking (set by IME service)
    private var appContext: Context? = null

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

        backspaceJob = viewModelScope.launch {
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
        if (!ic.getSelectedText(0).isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(32, 0) ?: ""
        ic.deleteSurroundingText(Backspace.length(before), 0)
    }

    // ==================== Emoji ====================

    /** Opens the emoji panel in place of the keys. */
    fun openEmoji() {
        commitTransliterationBuffer()
        if (_state.value.isVoiceListening) stopVoiceInput()
        _state.update {
            it.copy(
                showEmoji = true,
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
        inputConnection?.commitText(emoji, 1)
        val field = _state.value.field
        if (field.isPrivate || field.noLearning) return
        prefs.emojiRecents = EmojiRecents.add(prefs.emojiRecents, emoji)
    }

    /** Backspace in the emoji panel. */
    fun onEmojiBackspace() = deleteBackward()

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

    /** Menu choice: Android's keyboard picker. */
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
            inputConnection?.commitText(output, 1)

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
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION && endsRomanizedWord(output)) {
            // Punctuation ends the word: "huncha," must commit हुन्छ, not look up "huncha,".
            commitTransliterationBuffer()
            inputConnection?.commitText(if (output == ".") "।" else output, 1)
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
            inputConnection?.setComposingText(newBuffer.romanizedText, 1)

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
                    inputConnection?.commitText(output + " ", 1)

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
                    inputConnection?.commitText(" ", 1)
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
        if (combined != null && ic.getTextBeforeCursor(1, 0)?.toString() == "अ") {
            ic.beginBatchEdit()
            ic.deleteSurroundingText(1, 0)
            ic.commitText(combined, 1)
            ic.endBatchEdit()
        } else {
            ic.commitText(text, 1)
        }
    }

    /** Keep the Romanized word as typed (Latin), instead of converting it. */
    fun onKeepLatin() {
        val buffer = _state.value.transliterationBuffer
        if (buffer.isEmpty()) return
        inputConnection?.commitText(buffer.romanizedText + " ", 1)
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
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            } else {
                ic.commitText("\n", 1)
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

    /** Commits the Romanized word being typed as Devanagari, if there is one. */
    private fun commitTransliterationBuffer() {
        val buffer = _state.value.transliterationBuffer
        if (_state.value.currentLanguage != LanguageMode.TRANSLITERATION || buffer.isEmpty()) return
        inputConnection?.commitText(transliterationEngine.commitBuffer(buffer), 1)
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
                val currentTime = System.currentTimeMillis()
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
                        inputConnection?.finishComposingText()
                    } else {
                        // Show romanized text, not Devanagari
                        inputConnection?.setComposingText(newBuffer.romanizedText, 1)
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
        inputConnection?.commitText(suggestion + " ", 1)

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
            return
        }

        val language = when {
            field.prefersEnglish -> LanguageMode.ENGLISH
            prefs.typeNepali -> nepaliMode(field)
            else -> LanguageMode.ENGLISH
        }

        // Reset state when new input field is focused
        _state.update {
            it.copy(
                field = field,
                showLanguageMenu = false,
                showEmoji = false,
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
    }

    /**
     * Called by the IME service when the cursor or composing region changes. If the app or the
     * user moved the cursor away from the Romanized word, or the app removed it (e.g. cleared the
     * box after Send), drop the buffer so new letters don't get glued onto stale text.
     *
     * @param composingStart start of the composing region, or -1 if there is none
     */
    fun onSelectionChanged(selStart: Int, selEnd: Int, composingStart: Int, composingEnd: Int) {
        if (_state.value.transliterationBuffer.isEmpty()) return
        val stillComposing = composingStart >= 0 && selStart == selEnd && selEnd == composingEnd
        if (stillComposing) return
        if (composingStart >= 0) inputConnection?.finishComposingText()
        _state.update {
            it.copy(
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
    }

    /** Called by the IME service when the field loses focus. */
    fun onInputFinished() {
        commitTransliterationBuffer()
        if (_state.value.isVoiceListening) stopVoiceInput()
    }

    // ==================== Voice Typing Methods ====================

    /**
     * Sets the speech recognition manager and context.
     * Called by the IME service when it's ready.
     */
    fun setSpeechRecognitionManager(manager: SpeechRecognitionManager, context: Context) {
        speechRecognitionManager = manager
        appContext = context.applicationContext
        setupSpeechRecognitionListener()
    }

    private fun setupSpeechRecognitionListener() {
        speechRecognitionManager?.setListener(object : SpeechRecognitionManager.SpeechRecognitionListener {
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
                    inputConnection?.commitText(text, 1)
                }
                _state.update {
                    it.copy(
                        isVoiceListening = false,
                        partialVoiceResult = null
                    )
                }
            }

            override fun onError(messageRes: Int) {
                _state.update {
                    it.copy(
                        isVoiceListening = false,
                        voiceError = messageRes,
                        partialVoiceResult = null
                    )
                }
                // Clear error after 3 seconds
                voiceErrorClearJob?.cancel()
                voiceErrorClearJob = viewModelScope.launch {
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
    private fun hasMicrophonePermission(): Boolean {
        val context = appContext ?: return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
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
                it.copy(voiceError = R.string.voice_error_mic_permission)
            }
            // Clear error after 4 seconds
            voiceErrorClearJob?.cancel()
            voiceErrorClearJob = viewModelScope.launch {
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
            inputConnection?.commitText(output, 1)
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

    /**
     * Clean up voice resources.
     * Called when the ViewModel is cleared.
     */
    private companion object {
        /** Vowel sign → the vowel letter it forms after अ. */
        val INDEPENDENT_VOWELS = mapOf("ा" to "आ", "ो" to "ओ", "ौ" to "औ", "ै" to "ऐ")
    }

    override fun onCleared() {
        super.onCleared()
        voiceErrorClearJob?.cancel()
        speechRecognitionManager?.destroy()
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
    @StringRes val voiceError: Int? = null,
    val partialVoiceResult: String? = null
)
