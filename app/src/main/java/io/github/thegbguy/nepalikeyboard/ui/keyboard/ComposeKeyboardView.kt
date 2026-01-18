package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.viewmodel.compose.viewModel

class ComposeKeyboardView(context: Context) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        val viewModel: KeyboardViewModel = viewModel()

        // Get InputConnection from the IME service
        val imeService = context as? InputMethodService

        // Update InputConnection when composition happens
        LaunchedEffect(imeService) {
            viewModel.inputConnection = imeService?.currentInputConnection
        }

        KeyboardView(viewModel = viewModel)
    }
}
