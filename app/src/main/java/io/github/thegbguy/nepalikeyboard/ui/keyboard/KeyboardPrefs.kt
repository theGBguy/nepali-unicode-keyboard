package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.thegbguy.nepalikeyboard.data.emoji.EmojiRecents
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/** Key height presets. [dp] is the letter-key height; the digit row is 6dp shorter. */
enum class KeyHeight(val dp: Int) { SHORT(36), NORMAL(42), TALL(48) }

/** How the keyboard looks and feels; read by the keyboard view. */
data class KeyFeel(
    val height: KeyHeight = KeyHeight.NORMAL,
    val vibrate: Boolean = true,
    val sound: Boolean = false
)

/** Keyboard settings the view model reads and writes. */
interface KeyboardPrefs {
    /** How the user types Nepali: [LanguageMode.TRANSLITERATION] (Romanized) or [LanguageMode.NEPALI] (traditional layout). */
    var nepaliStyle: LanguageMode

    /** Whether the user last chose Nepali with the language key; restored on the next normal field. */
    var typeNepali: Boolean

    var keyHeight: KeyHeight
    var vibrate: Boolean
    var sound: Boolean

    /** Recently used emoji, most recent first. */
    var emojiRecents: List<String>

    /** Whether the clipboard panel keeps copied and pasted text on the phone. */
    var clipboardHistory: Boolean
    /** Whether the strip offers conversions of what's typed (amounts, dates, units). */
    var smartSuggestions: Boolean
    /** The user agreed to handwriting (Google model download and ML Kit diagnostics). */
    var handwriting: Boolean

    val feel: KeyFeel get() = KeyFeel(keyHeight, vibrate, sound)
}

/** Used by tests and until the IME service supplies the stored settings. */
class InMemoryKeyboardPrefs(
    override var nepaliStyle: LanguageMode = LanguageMode.TRANSLITERATION,
    override var typeNepali: Boolean = false,
    override var keyHeight: KeyHeight = KeyHeight.NORMAL,
    override var vibrate: Boolean = true,
    override var sound: Boolean = false,
    override var emojiRecents: List<String> = emptyList(),
    override var clipboardHistory: Boolean = true,
    override var smartSuggestions: Boolean = true,
    override var handwriting: Boolean = false
) : KeyboardPrefs

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
