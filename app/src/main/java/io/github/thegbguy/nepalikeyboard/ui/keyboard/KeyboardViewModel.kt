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

    /** The mode the user picked with the language key; restored when a field doesn't force English. */
    private var preferredLanguage: LanguageMode = LanguageMode.ENGLISH

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
                inputConnection?.deleteSurroundingText(1, 0)
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

    /**
     * Switches to the next language mode.
     */
    fun switchLanguage() {
        // If switching away from transliteration with uncommitted text, auto-commit
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION
            && _state.value.transliterationBuffer.isNotEmpty()
        ) {
            val buffer = _state.value.transliterationBuffer
            val output = transliterationEngine.commitBuffer(buffer)
            // commitText replaces composing text
            inputConnection?.commitText(output, 1)
        }

        _state.update { currentState ->
            var newMode = currentState.currentLanguage.next()
            if (newMode == LanguageMode.TRANSLITERATION && !currentState.field.allowsTransliteration) {
                newMode = newMode.next()
            }
            // A choice made in an email/URL/password field applies to that field only
            if (!currentState.field.prefersEnglish) preferredLanguage = newMode
            currentState.copy(
                currentLanguage = newMode,
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

    /**
     * Toggles between numbers and symbols layout (for English).
     */
    fun toggleSymbolsLayout() {
        _state.update { it.copy(showingSymbols = !it.showingSymbols) }
        updateLayout()
    }

    private fun handleCharacterKey(key: KeyData) {
        if (_state.value.currentLanguage == LanguageMode.TRANSLITERATION) {
            // Transliteration mode - buffer input and show romanized text
            val output = key.getOutput(_state.value.isShifted)
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
            // Direct input mode (existing behavior for English and Nepali)
            val output = key.getOutput(_state.value.isShifted)
            inputConnection?.commitText(output, 1)
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
        inputConnection?.commitText(alternate, 1)
        if (_state.value.isShifted && !_state.value.isCapsLock) {
            _state.update { it.copy(isShifted = false) }
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
                    // Normal backspace
                    inputConnection?.deleteSurroundingText(1, 0)
                }
            }
            "🌐" -> {
                // Language switch key
                switchLanguage()
            }
            "?123" -> {
                // Switch to numbers layout from main layout
                toggleNumbersLayout()
            }
            "123" -> {
                // Switch back to numbers from symbols layout
                if (_state.value.showingSymbols) {
                    toggleSymbolsLayout()
                }
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
            "=+/" -> {
                // Switch to symbols layout
                toggleSymbolsLayout()
            }
        }
    }

    private fun updateLayout() {
        val currentState = _state.value
        val newLayout = when {
            currentState.showingSymbols && currentState.currentLanguage == LanguageMode.ENGLISH -> {
                EnglishLayout.getSymbolsLayout()
            }
            currentState.showingNumbers -> {
                when (currentState.currentLanguage) {
                    LanguageMode.NEPALI -> NepaliLayout.getNumbersLayout()
                    LanguageMode.TRANSLITERATION, LanguageMode.ENGLISH -> EnglishLayout.getNumbersLayout()
                }
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
            else -> preferredLanguage
        }

        // Reset state when new input field is focused
        _state.update {
            it.copy(
                field = field,
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
