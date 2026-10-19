package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.thegbguy.nepalikeyboard.domain.voice.SpeechRecognitionManager

class ComposeKeyboardView(
    context: Context,
    private val speechRecognitionManager: SpeechRecognitionManager? = null
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        val viewModel: KeyboardViewModel = viewModel()

        // Get InputConnection from the IME service
        val imeService = context as? InputMethodService

        // Read the current connection on every use; it changes with each focused field
        LaunchedEffect(imeService) {
            viewModel.inputConnectionProvider = { imeService?.currentInputConnection }
        }

        // Set up speech recognition manager with context for permission checking
        LaunchedEffect(speechRecognitionManager) {
            speechRecognitionManager?.let {
                viewModel.setSpeechRecognitionManager(it, context)
            }
        }

        KeyboardView(viewModel = viewModel)
    }
}
