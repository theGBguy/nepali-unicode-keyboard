package io.github.thegbguy.nepalikeyboard.domain.model

/**
 * Represents the current transliteration buffer state.
 *
 * @property romanizedText Current romanized input text being buffered
 * @property primaryCandidate Top suggestion (preview) to show to user
 * @property candidates All available transliteration suggestions
 * @property cursorPosition Cursor position within the buffer
 */
data class TransliterationBuffer(
    val romanizedText: String = "",
    val primaryCandidate: String = "",
    val candidates: List<TransliterationCandidate> = emptyList(),
    val cursorPosition: Int = 0
) {
    /**
     * Returns true if the buffer is empty (no text).
     */
    fun isEmpty(): Boolean = romanizedText.isEmpty()

    /**
     * Returns true if the buffer contains text.
     */
    fun isNotEmpty(): Boolean = romanizedText.isNotEmpty()
}
