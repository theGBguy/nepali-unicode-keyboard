package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.AbstractComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.thegbguy.nepalikeyboard.domain.voice.SpeechRecognitionManager

class ComposeKeyboardView(
    context: Context,
    private val speechRecognitionManager: SpeechRecognitionManager? = null
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        val keyboard = viewModel<KeyboardViewModel>().controller

        // Get InputConnection from the IME service
        val imeService = context as? InputMethodService

        // Read the current connection on every use; it changes with each focused field
        LaunchedEffect(imeService) {
            keyboard.editor = { imeService?.currentInputConnection?.let(::InputConnectionEditor) }
        }

        // Set up speech recognition, with the app context for permission checking
        LaunchedEffect(speechRecognitionManager) {
            speechRecognitionManager?.let {
                val app = context.applicationContext
                keyboard.setVoiceInput(it) {
                    ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                }
            }
        }

        KeyboardView(keyboard = keyboard, onInsetsChange = ::updateInsets)
    }

    /** Window y of the drawn keyboard's top; -1 until it has been laid out. */
    var keyboardTop = -1
        private set

    /** How far above [keyboardTop] the app should still treat as covered (mid-animation). */
    var extraTop = 0
        private set

    private fun updateInsets(top: Int, extra: Int) {
        if (top == keyboardTop && extra == extraTop) return
        keyboardTop = top
        extraTop = extra
        // The service recomputes insets on the next traversal
        post { requestLayout() }
    }
}
