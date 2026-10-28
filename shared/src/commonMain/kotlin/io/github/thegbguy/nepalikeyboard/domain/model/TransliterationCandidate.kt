package io.github.thegbguy.nepalikeyboard.domain.model

/**
 * Represents a single transliteration suggestion.
 *
 * @property devanagari The Devanagari output text
 * @property romanized The romanized input text that produced this candidate
 * @property confidence Confidence score (0-1) for this suggestion
 * @property isComplete Whether this is a complete word or partial match
 */
data class TransliterationCandidate(
    val devanagari: String,
    val romanized: String,
    val confidence: Float = 1.0f,
    val isComplete: Boolean = true
)
