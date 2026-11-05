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
        // A consonant typed on its own is a half letter (b → ब्, yn → ञ्). A capital one is the
        // full letter first (N → ण, S → श), as capitals are typed for the letter itself.
        val half = RomanizedConverter.halfLetter(romanizedText)
        // A letter or syllable typed on its own means the letter itself: its exact spelling goes
        // first (i → इ, ka → क, tra → त्र, M → ं), then the words it could stand for.
        val special = SPECIAL_SPELLINGS[romanizedText.lowercase()]
        val literal = when {
            half == null -> listOfNotNull(special, converted)
            romanizedText.any { it.isUpperCase() } -> listOf(converted, half)
            else -> listOf(half, converted)
        }
        val syllable = half != null || special != null || RomanizedConverter.isSyllable(romanizedText)
        val words = index() ?: return candidates(romanizedText, literal, emptyList())

        val scores = LinkedHashMap<String, Double>()
        val letters = if (isStrict(romanizedText)) converted.filter { it in SPECIAL_LETTERS } else ""
        // A typed chh is always छ: the sound key folds it with ch, so drop words with fewer छ (chhanda → छन्द, not चन्द)
        val chh = CHH.findAll(romanizedText.lowercase()).count()
        val keeps = { word: String -> letters.all(word::contains) && word.count { it == 'छ' } >= chh }
        // Typed capitals and yn/gy ask for those letters: drop words that don't have them (N → ण, not न)
        words.lookup(romanizedText).forEach { if (keeps(it.word)) scores[it.word] = it.score }
        // The literal conversion competes like a word typed exactly, with a small frequency floor
        val convertedScore = ln((words.frequency(converted) ?: UNKNOWN_WORD_FREQUENCY) + 1.0)
        if (convertedScore > (scores[converted] ?: Double.NEGATIVE_INFINITY)) scores[converted] = convertedScore

        val ranked = scores.entries.sortedByDescending { it.value }.map { it.key }
        // A word: the likely word first, and the exact spelling never lower than second, so it
        // can't drop out of the strip (shra → श्र, Sha → ष, ramro → राम्रो then रम्रो).
        val exact = when {
            syllable -> literal + ranked
            ranked.indexOf(converted) <= 1 -> ranked
            else -> (ranked - converted).let { it.take(1) + converted + it.drop(1) }
        }
        val completions = words.completions(romanizedText).filter { it !in scores && keeps(it) }
        return candidates(romanizedText, exact, completions)
    }

    private fun candidates(
        romanized: String,
        exact: List<String>,
        completions: List<String>
    ): List<TransliterationCandidate> {
        val fixedCompletions = completions.map(::eyelashRa)
        return (exact.map(::eyelashRa).distinct().take(MAX_EXACT) + fixedCompletions)
            .distinct().take(MAX_CANDIDATES).mapIndexed { i, word ->
                TransliterationCandidate(
                    devanagari = word,
                    romanized = romanized,
                    confidence = 1f - i * 0.1f,
                    isComplete = word !in fixedCompletions
                )
            }
    }

    /**
     * Input that spells its letters out: a capital (`ghaDi`, `N`) or `yn`. A capital that only
     * starts a longer word is sentence case (`Nepal`, `Sita`), not a request for ण or श.
     * (`gy` isn't strict: bhagya is भाग्य, not a word with ज्ञ.)
     */
    private fun isStrict(roman: String): Boolean {
        if ("yn" in roman) return true
        val rest = if (roman.length >= 3) roman.drop(1) else roman
        return rest.any { it.isUpperCase() }
    }

    /**
     * Verb endings -र्यो / -र्यौ are written with the eyelash ra (ZWJ): गर्‍यो, पार्‍यो, हेर्‍यौ.
     * Without the joiner they render as a reph over य, which is how कार्य or सूर्य are spelled.
     */
    private fun eyelashRa(word: String): String = EYELASH_ENDING.replace(word, "र्\u200Dय$1")

    private companion object {
        /** Frequency given to a converted word that isn't in the list (names, slang, typos). */
        const val UNKNOWN_WORD_FREQUENCY = 10
        /** Letters the converter has no spelling for, typed the way people write them. */
        val SPECIAL_SPELLINGS = mapOf("om" to "ॐ")
        /** Letters only an explicit spelling gives: retroflex, श/ष and ञ (also in ज्ञ). */
        const val SPECIAL_LETTERS = "टठडढणशषञ"
        val EYELASH_ENDING = Regex("र्य([ोौ][ंँ]?)$")
        val POLITE_ENDING = Regex("(nu|ho)s$")
        val CHH = Regex("chh")
        const val MAX_EXACT = 4
        const val MAX_CANDIDATES = 6
    }
}
