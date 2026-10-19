package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.content.Context
import androidx.core.content.edit
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/** Keyboard settings the view model reads and writes. */
interface KeyboardPrefs {
    /** How the user types Nepali: [LanguageMode.TRANSLITERATION] (Romanized) or [LanguageMode.NEPALI] (traditional layout). */
    var nepaliStyle: LanguageMode
}

/** Used by tests and until the IME service supplies the stored settings. */
class InMemoryKeyboardPrefs(
    override var nepaliStyle: LanguageMode = LanguageMode.TRANSLITERATION
) : KeyboardPrefs

/** Settings stored on the device. Holds no typed text. */
class SharedKeyboardPrefs(context: Context) : KeyboardPrefs {
    private val prefs = context.applicationContext.getSharedPreferences("keyboard", Context.MODE_PRIVATE)

    override var nepaliStyle: LanguageMode
        get() = when (prefs.getString(KEY_STYLE, null)) {
            STYLE_TRADITIONAL -> LanguageMode.NEPALI
            else -> LanguageMode.TRANSLITERATION
        }
        set(value) = prefs.edit {
            putString(KEY_STYLE, if (value == LanguageMode.NEPALI) STYLE_TRADITIONAL else STYLE_ROMANIZED)
        }

    private companion object {
        const val KEY_STYLE = "nepali_style"
        const val STYLE_ROMANIZED = "romanized"
        const val STYLE_TRADITIONAL = "traditional"
    }
}
