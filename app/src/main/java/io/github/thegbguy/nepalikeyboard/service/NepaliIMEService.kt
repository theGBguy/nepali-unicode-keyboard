package io.github.thegbguy.nepalikeyboard.service

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.transliteration.NepaliWordList
import io.github.thegbguy.nepalikeyboard.domain.voice.SpeechRecognitionManager
import io.github.thegbguy.nepalikeyboard.locale.AppLanguage
import io.github.thegbguy.nepalikeyboard.ui.keyboard.ComposeKeyboardView
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardViewModel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.SharedKeyboardPrefs

/**
 * Core InputMethodService for Nepali Keyboard.
 * Implements necessary lifecycle owners to support Jetpack Compose.
 */
class NepaliIMEService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val savedStateRegistryController: SavedStateRegistryController by lazy(
        LazyThreadSafetyMode.NONE
    ) {
        SavedStateRegistryController.create(this)
    }

    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()

    // Speech recognition manager for voice typing
    private var speechRecognitionManager: SpeechRecognitionManager? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private fun handleLifecycleEvent(event: Lifecycle.Event) =
        lifecycleRegistry.handleLifecycleEvent(event)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Load the Romanized-typing word list in the background
        NepaliWordList.loadAsync(this)

        keyboardViewModel.prefs = SharedKeyboardPrefs(this)
        keyboardViewModel.onSwitchKeyboard = {
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        }

        // Initialize speech recognition manager
        speechRecognitionManager = SpeechRecognitionManager(this)
    }

    override fun onCreateInputView(): View {
        val view = ComposeKeyboardView(this, speechRecognitionManager)

        // Configure window to not overlap with system navigation bar
        window?.window?.let { window ->
            // Draw the keyboard behind the navigation bar so its background fills that strip;
            // KeyboardView pads its keys by the navigation-bar inset.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // No grey scrim behind 3-button navigation
                window.isNavigationBarContrastEnforced = false
            }
            window.decorView.let { decorView ->
                decorView.setViewTreeLifecycleOwner(this)
                decorView.setViewTreeViewModelStoreOwner(this)
                decorView.setViewTreeSavedStateRegistryOwner(this)
            }
        }

        return view
    }

    private val keyboardViewModel: KeyboardViewModel
        get() = ViewModelProvider(this)[KeyboardViewModel::class.java]

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardViewModel.inputConnectionProvider = { currentInputConnection }
        keyboardViewModel.onInputStarted(FieldInfo.from(info), restarting)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        keyboardViewModel.onSelectionChanged(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        keyboardViewModel.onInputFinished()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        speechRecognitionManager?.destroy()
        speechRecognitionManager = null
        super.onDestroy()
    }
}
