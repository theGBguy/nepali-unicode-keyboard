package io.github.thegbguy.nepalikeyboard.ui.keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/** Keeps the shared [KeyboardController] for the IME's lifetime and runs its coroutines in [viewModelScope]. */
class KeyboardViewModel : ViewModel() {
    val controller = KeyboardController { viewModelScope }

    override fun onCleared() {
        controller.close()
    }
}
