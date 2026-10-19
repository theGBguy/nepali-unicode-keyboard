package io.github.thegbguy.nepalikeyboard.domain.model

/**
 * The keyboard's typing mode. The language key switches between [ENGLISH] and the user's Nepali
 * style, which is [TRANSLITERATION] (Romanized) or [NEPALI] (traditional layout).
 */
enum class LanguageMode {
    /** Traditional Nepali layout: keys type Devanagari directly. */
    NEPALI,

    /** English QWERTY. */
    ENGLISH,

    /** Romanized Nepali: Latin letters become Devanagari, e.g. "namaste" → "नमस्ते". */
    TRANSLITERATION
}
