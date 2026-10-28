package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/** Key height presets. [dp] is the letter-key height (points on iOS); the digit row is 6 shorter. */
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
