package io.github.thegbguy.nepalikeyboard.domain.model

import androidx.annotation.StringRes
import io.github.thegbguy.nepalikeyboard.R

/**
 * Represents the different language modes supported by the keyboard.
 */
enum class LanguageMode {
    /**
     * Pure Nepali/Devanagari typing mode.
     * Keys directly output Devanagari characters.
     */
    NEPALI,

    /**
     * English QWERTY typing mode.
     * Standard English keyboard layout.
     */
    ENGLISH,

    /**
     * Nepali transliteration mode.
     * Romanized input is converted to Devanagari in real-time.
     * Example: "namaste" → "नमस्ते"
     */
    TRANSLITERATION;

    /**
     * String resource for this mode's label.
     */
    @StringRes
    fun labelRes(): Int = when (this) {
        NEPALI -> R.string.mode_nepali
        ENGLISH -> R.string.mode_english
        TRANSLITERATION -> R.string.mode_romanized
    }

    /**
     * Returns the next language mode in the cycle.
     * Used for language switching button.
     */
    fun next(): LanguageMode = when (this) {
        NEPALI -> TRANSLITERATION
        TRANSLITERATION -> ENGLISH
        ENGLISH -> NEPALI
    }
}
