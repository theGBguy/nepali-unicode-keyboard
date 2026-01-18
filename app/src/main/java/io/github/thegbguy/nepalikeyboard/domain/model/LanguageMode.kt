package io.github.thegbguy.nepalikeyboard.domain.model

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
     * Returns the display name for this language mode.
     */
    fun displayName(): String = when (this) {
        NEPALI -> "नेपाली"
        ENGLISH -> "English"
        TRANSLITERATION -> "Transliteration"
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
