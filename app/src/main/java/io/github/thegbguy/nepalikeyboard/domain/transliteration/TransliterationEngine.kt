package io.github.thegbguy.nepalikeyboard.domain.transliteration

import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationCandidate

/**
 * Interface for transliteration engine.
 * Defines operations for converting romanized text to Devanagari script.
 */
interface TransliterationEngine {

    /**
     * Processes a character input and updates the transliteration buffer.
     *
     * @param currentBuffer The current buffer state
     * @param input The new input character/string to process
     * @return Updated buffer with new input processed
     */
    fun processInput(
        currentBuffer: TransliterationBuffer,
        input: String
    ): TransliterationBuffer

    /**
     * Commits the current buffer and returns the final Devanagari output.
     * This is called when the user presses spacebar or selects a suggestion.
     *
     * @param buffer The buffer to commit
     * @return The final Devanagari text to be inserted into the input field
     */
    fun commitBuffer(buffer: TransliterationBuffer): String

    /**
     * Handles backspace in the transliteration buffer.
     * Removes the last character from the romanized text and regenerates suggestions.
     *
     * @param buffer The current buffer state
     * @return Updated buffer with last character removed
     */
    fun handleBackspace(buffer: TransliterationBuffer): TransliterationBuffer

    /**
     * Generates transliteration suggestions for the given romanized text.
     * The first candidate in the list is the primary suggestion.
     *
     * @param romanizedText The romanized input text
     * @return List of transliteration candidates (primary first)
     */
    fun generateSuggestions(romanizedText: String): List<TransliterationCandidate>
}
