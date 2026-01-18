package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.view.inputmethod.InputConnection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
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
        _state.update { currentState ->
            val newMode = currentState.currentLanguage.next()
            currentState.copy(currentLanguage = newMode)
        }
        updateLayout()
    }

    /**
     * Toggles between main and numbers layout.
     */
    fun toggleNumbersLayout() {
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
        val output = key.getOutput(_state.value.isShifted)
        inputConnection?.commitText(output, 1)

        // Reset shift after character input (unless caps lock)
        if (_state.value.isShifted && !_state.value.isCapsLock) {
            _state.update { it.copy(isShifted = false) }
        }
    }

    private fun handleActionKey(key: KeyData) {
        when (key.primary) {
            " " -> {
                // Space key
                inputConnection?.commitText(" ", 1)
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
                // Shift key - toggle shift state
                val newShiftState = !_state.value.isShifted
                _state.update {
                    it.copy(
                        isShifted = newShiftState,
                        isCapsLock = false
                    )
                }
            }
        }
    }

    private fun handleFunctionKey(key: KeyData) {
        when (key.primary) {
            "⌫" -> {
                // Backspace key
                inputConnection?.deleteSurroundingText(1, 0)
            }
            "🌐" -> {
                // Language switch key
                switchLanguage()
            }
            "?123", "123" -> {
                // Switch to numbers layout
                toggleNumbersLayout()
            }
            "ABC" -> {
                // Switch back to main layout
                if (_state.value.showingSymbols) {
                    toggleSymbolsLayout()
                } else {
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
                    LanguageMode.NEPALI, LanguageMode.TRANSLITERATION -> NepaliLayout.getNumbersLayout()
                    LanguageMode.ENGLISH -> EnglishLayout.getNumbersLayout()
                }
            }
            else -> {
                when (currentState.currentLanguage) {
                    LanguageMode.NEPALI -> NepaliLayout.getLayout()
                    LanguageMode.TRANSLITERATION -> NepaliLayout.getLayout() // Will be replaced in Phase 2
                    LanguageMode.ENGLISH -> EnglishLayout.getLayout()
                }
            }
        }

        _state.update { it.copy(currentLayout = newLayout) }
    }

    /**
     * Updates the keyboard state when input field changes.
     */
    fun onInputStarted() {
        // Reset state when new input field is focused
        _state.update {
            it.copy(
                isShifted = false,
                isCapsLock = false,
                showingNumbers = false,
                showingSymbols = false
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
    val suggestions: List<String> = emptyList() // For Phase 2
)
