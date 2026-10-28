package io.github.thegbguy.nepalikeyboard.domain.transliteration

import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationCandidate
import kotlin.math.ln

/**
 * Romanized → Nepali. Candidates come from two places:
 * - the word list ([WordIndex]): real words that sound like what was typed, ranked by how common
 *   they are and how closely their spelling matches the letters typed;
 * - the letter-by-letter [RomanizedConverter], so names and words missing from the list still work.
 *
 * The best-scoring candidate is committed on space; the others and a couple of longer completions
 * fill the suggestion strip.
 *
 * @param index Supplies the loaded word list, or null while it's still loading.
 */
class NepaliTransliterationEngine(
    private val index: () -> WordIndex? = { NepaliWordList.index }
) : TransliterationEngine {

    override fun processInput(currentBuffer: TransliterationBuffer, input: String): TransliterationBuffer =
        bufferFor(currentBuffer.romanizedText + input)

    override fun commitBuffer(buffer: TransliterationBuffer): String =
        buffer.primaryCandidate.ifEmpty { buffer.romanizedText }

    override fun handleBackspace(buffer: TransliterationBuffer): TransliterationBuffer =
        if (buffer.isEmpty()) buffer else bufferFor(buffer.romanizedText.dropLast(1))

    private fun bufferFor(romanized: String): TransliterationBuffer {
        val candidates = generateSuggestions(romanized)
        return TransliterationBuffer(
            romanizedText = romanized,
            primaryCandidate = candidates.firstOrNull()?.devanagari ?: romanized,
            candidates = candidates,
            cursorPosition = romanized.length
        )
    }

    override fun generateSuggestions(romanizedText: String): List<TransliterationCandidate> {
        if (romanizedText.isEmpty()) return emptyList()
        val converted = RomanizedConverter.convert(romanizedText).let {
            // Polite imperatives end in a halant: garnus → गर्नुस्, garnuhos → गर्नुहोस्
            if (POLITE_ENDING.containsMatchIn(romanizedText)) it + "्" else it
        }
        val words = index() ?: return listOf(TransliterationCandidate(converted, romanizedText))

        val scores = LinkedHashMap<String, Double>()
        words.lookup(romanizedText).forEach { scores[it.word] = it.score }
        // The literal conversion competes like a word typed exactly, with a small frequency floor
        val convertedScore = ln((words.frequency(converted) ?: UNKNOWN_WORD_FREQUENCY) + 1.0)
        if (convertedScore > (scores[converted] ?: Double.NEGATIVE_INFINITY)) scores[converted] = convertedScore

        val ranked = scores.entries.sortedByDescending { it.value }.map { it.key }
        val completions = words.completions(romanizedText).filter { it !in scores }
        return (ranked.take(MAX_EXACT) + completions).take(MAX_CANDIDATES).mapIndexed { i, word ->
            TransliterationCandidate(
                devanagari = word,
                romanized = romanizedText,
                confidence = 1f - i * 0.1f,
                isComplete = word !in completions
            )
        }
    }

    private companion object {
        /** Frequency given to a converted word that isn't in the list (names, slang, typos). */
        const val UNKNOWN_WORD_FREQUENCY = 10
        val POLITE_ENDING = Regex("(nu|ho)s$")
        const val MAX_EXACT = 4
        const val MAX_CANDIDATES = 6
    }
}
