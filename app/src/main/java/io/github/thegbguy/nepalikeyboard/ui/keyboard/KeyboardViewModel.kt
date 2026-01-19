package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.view.inputmethod.InputConnection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import io.github.thegbguy.nepalikeyboard.domain.transliteration.NepaliTransliterationEngine
import io.github.thegbguy.nepalikeyboard.domain.transliteration.TransliterationEngine
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

    // Reference to InputConnection (set by IME service)
    var inputConnection: InputConnection? = null

    // Job for handling backspace long-press repeating deletion
    private var backspaceJob: Job? = null

    // Track last shift press time for double-tap caps lock detection
    private var lastShiftPressTime: Long = 0
    private val DOUBLE_TAP_TIMEOUT_MS = 300L // Time window for double-tap detection

    // Transliteration engine for romanized to Devanagari conversion
    private val transliterationEngine: TransliterationEngine = NepaliTransliterationEngine()

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
            val newMode = currentState.currentLanguage.next()
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
            "↵" -> {
                // Enter key
                inputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            }
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
     * Updates the keyboard state when input field changes.
     */
    fun onInputStarted() {
        // Clear any composing text from transliteration
        inputConnection?.finishComposingText()

        // Reset state when new input field is focused
        _state.update {
            it.copy(
                isShifted = false,
                isCapsLock = false,
                showingNumbers = false,
                showingSymbols = false,
                transliterationBuffer = TransliterationBuffer(),
                suggestions = emptyList(),
                showSuggestionBar = false
            )
        }
        updateLayout()
    }
}

/**
 * State data class for the keyboard.
 */
data class KeyboardState(
    val currentLanguage: LanguageMode = LanguageMode.ENGLISH,
    val currentLayout: KeyboardLayout = EnglishLayout.getLayout(),
    val isShifted: Boolean = false,
    val isCapsLock: Boolean = false,
    val showingNumbers: Boolean = false,
    val showingSymbols: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val transliterationBuffer: TransliterationBuffer = TransliterationBuffer(),
    val showSuggestionBar: Boolean = false
)
