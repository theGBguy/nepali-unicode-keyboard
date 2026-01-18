package io.github.thegbguy.nepalikeyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.thegbguy.nepalikeyboard.ui.keyboard.ComposeKeyboardView

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

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private fun handleLifecycleEvent(event: Lifecycle.Event) =
        lifecycleRegistry.handleLifecycleEvent(event)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onCreateInputView(): View {
        val view = ComposeKeyboardView(this)

        // Configure window to not overlap with system navigation bar
        window?.window?.let { window ->
            window.decorView.let { decorView ->
                decorView.setViewTreeLifecycleOwner(this)
                decorView.setViewTreeViewModelStoreOwner(this)
                decorView.setViewTreeSavedStateRegistryOwner(this)
            }
        }

        return view
    }

    override fun onDestroy() {
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
