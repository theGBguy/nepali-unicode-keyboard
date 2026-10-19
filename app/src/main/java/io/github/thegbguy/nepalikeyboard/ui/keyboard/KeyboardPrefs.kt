package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/** Keyboard settings the view model reads and writes. */
interface KeyboardPrefs {
    /** How the user types Nepali: [LanguageMode.TRANSLITERATION] (Romanized) or [LanguageMode.NEPALI] (traditional layout). */
    var nepaliStyle: LanguageMode

    /** Whether the user last chose Nepali with the language key; restored on the next normal field. */
    var typeNepali: Boolean
}

/** Used by tests and until the IME service supplies the stored settings. */
class InMemoryKeyboardPrefs(
    override var nepaliStyle: LanguageMode = LanguageMode.TRANSLITERATION,
    override var typeNepali: Boolean = false
) : KeyboardPrefs

/** Settings stored on the device. Holds no typed text. */
class SharedKeyboardPrefs(context: Context) : KeyboardPrefs {
    private val prefs = context.applicationContext.getSharedPreferences("keyboard", Context.MODE_PRIVATE)

    // Held strongly: SharedPreferences keeps listeners in a weak map
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    /** Calls [onChange] when a setting is changed elsewhere, e.g. the style picked in the app. */
    fun observe(onChange: () -> Unit) {
        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
            .also(prefs::registerOnSharedPreferenceChangeListener)
    }

    fun stopObserving() {
        listener?.let(prefs::unregisterOnSharedPreferenceChangeListener)
        listener = null
    }

    override var nepaliStyle: LanguageMode
        get() = when (prefs.getString(KEY_STYLE, null)) {
            STYLE_TRADITIONAL -> LanguageMode.NEPALI
            else -> LanguageMode.TRANSLITERATION
        }
        set(value) = prefs.edit {
            putString(KEY_STYLE, if (value == LanguageMode.NEPALI) STYLE_TRADITIONAL else STYLE_ROMANIZED)
        }

    /** Nepali by default: that's why someone installs this keyboard. */
    override var typeNepali: Boolean
        get() = prefs.getBoolean(KEY_TYPE_NEPALI, true)
        set(value) = prefs.edit { putBoolean(KEY_TYPE_NEPALI, value) }

    private companion object {
        const val KEY_TYPE_NEPALI = "type_nepali"
        const val KEY_STYLE = "nepali_style"
        const val STYLE_ROMANIZED = "romanized"
        const val STYLE_TRADITIONAL = "traditional"
    }
}
