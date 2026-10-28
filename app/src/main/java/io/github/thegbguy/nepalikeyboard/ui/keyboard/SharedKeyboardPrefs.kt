package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.thegbguy.nepalikeyboard.data.emoji.EmojiRecents
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/** Settings stored on the device, plus recently used emoji. Holds no typed text (clipboard items live in their own file). */
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

    override var keyHeight: KeyHeight
        get() = KeyHeight.entries.firstOrNull { it.name == prefs.getString(KEY_HEIGHT, null) } ?: KeyHeight.NORMAL
        set(value) = prefs.edit { putString(KEY_HEIGHT, value.name) }

    /** Light tick on each key, on by default. Android's own touch-feedback setting still applies. */
    override var vibrate: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit { putBoolean(KEY_VIBRATE, value) }

    /** Key click sound, off by default. Plays at the phone's touch-sound volume. */
    override var sound: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) = prefs.edit { putBoolean(KEY_SOUND, value) }

    override var emojiRecents: List<String>
        get() = EmojiRecents.decode(prefs.getString(KEY_EMOJI_RECENTS, null))
        set(value) = prefs.edit { putString(KEY_EMOJI_RECENTS, EmojiRecents.encode(value)) }

    /** On by default. Turning it off in Settings also deletes the saved items. */
    override var clipboardHistory: Boolean
        get() = prefs.getBoolean(KEY_CLIPBOARD_HISTORY, true)
        set(value) = prefs.edit { putBoolean(KEY_CLIPBOARD_HISTORY, value) }

    /** On by default. */
    override var smartSuggestions: Boolean
        get() = prefs.getBoolean(KEY_SMART_SUGGESTIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_SMART_SUGGESTIONS, value) }

    /** Off until the user agrees in the keyboard or in Settings. */
    override var handwriting: Boolean
        get() = prefs.getBoolean(KEY_HANDWRITING, false)
        set(value) = prefs.edit { putBoolean(KEY_HANDWRITING, value) }

    private companion object {
        const val KEY_CLIPBOARD_HISTORY = "clipboard_history"
        const val KEY_SMART_SUGGESTIONS = "smart_suggestions"
        const val KEY_HANDWRITING = "handwriting"
        const val KEY_EMOJI_RECENTS = "emoji_recents"
        const val KEY_HEIGHT = "key_height"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_SOUND = "sound"
        const val KEY_TYPE_NEPALI = "type_nepali"
        const val KEY_STYLE = "nepali_style"
        const val STYLE_ROMANIZED = "romanized"
        const val STYLE_TRADITIONAL = "traditional"
    }
}
