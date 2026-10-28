package io.github.thegbguy.nepalikeyboard.domain.transliteration

import kotlin.math.ln

/**
 * Nepali words with their frequencies, looked up by [SoundKey.looseKey] so that the many ways of
 * typing a word in Roman letters all find it.
 *
 * Input lines are `word<TAB>frequency[<TAB>alias,alias]`; aliases are extra Romanized spellings
 * (e.g. `kathmandu` for काठमाडौं) that the sound rules can't derive.
 */
class WordIndex private constructor(
    private val byKey: HashMap<String, MutableList<Entry>>,
    private val frequencies: HashMap<String, Int>
) {
    /** The keys of [byKey] in order, for prefix search. */
    private val sortedKeys: List<String> = byKey.keys.sorted()

    private class Entry(val word: String, val frequency: Int, val spelling: String)

    data class Match(val word: String, val score: Double)

    val size: Int get() = frequencies.size

    fun frequency(word: String): Int? = frequencies[word]

    /**
     * Words whose sound key equals the typed text's, best first. Score = log frequency minus a
     * penalty per letter of difference between what was typed and how the word is spelled, so a
     * common word typed loosely beats a rare word typed exactly only when it's much more common.
     */
    fun lookup(typed: String, limit: Int = 4): List<Match> {
        val normalized = SoundKey.normalize(typed)
        val entries = byKey[SoundKey.looseKey(normalized)] ?: return emptyList()
        val best = HashMap<String, Double>()
        for (e in entries) {
            val score = ln(e.frequency + 1.0) - DISTANCE_PENALTY * SoundKey.distance(normalized, e.spelling)
            if (score > (best[e.word] ?: Double.NEGATIVE_INFINITY)) best[e.word] = score
        }
        return best.entries.sortedByDescending { it.value }.take(limit).map { Match(it.key, it.value) }
    }

    /** Common longer words that start with what's been typed, for the suggestion strip. */
    fun completions(typed: String, limit: Int = 2): List<String> {
        val key = SoundKey.looseKey(SoundKey.normalize(typed))
        if (key.length < 3) return emptyList()
        val found = HashMap<String, Int>()
        var visited = 0
        // Keys after [key] itself that start with it, in order
        var i = sortedKeys.binarySearch(key).let { if (it >= 0) it + 1 else -(it + 1) }
        while (i < sortedKeys.size && sortedKeys[i].startsWith(key)) {
            val entries = byKey.getValue(sortedKeys[i++])
            entries.forEach { if (it.frequency > (found[it.word] ?: -1)) found[it.word] = it.frequency }
            if (++visited >= MAX_COMPLETION_KEYS) break
        }
        return found.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    companion object {
        const val DISTANCE_PENALTY = 2.0
        private const val MAX_COMPLETION_KEYS = 400

        /** [build] from the whole file's text (for Swift, which can't make a Kotlin Sequence). */
        fun parse(text: String): WordIndex = build(text.lineSequence())

        fun build(lines: Sequence<String>): WordIndex {
            val byKey = HashMap<String, MutableList<Entry>>()
            val frequencies = HashMap<String, Int>()
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#")) continue
                val parts = line.split('\t')
                val word = parts[0].trim()
                val frequency = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: continue
                if (word.isEmpty()) continue
                frequencies[word] = maxOf(frequency, frequencies[word] ?: 0)
                for (spelling in SoundKey.spellingsForWord(word)) {
                    byKey.getOrPut(SoundKey.looseKey(spelling)) { mutableListOf() } += Entry(word, frequency, spelling)
                }
                parts.getOrNull(2)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { alias ->
                    val aliasSpelling = SoundKey.normalize(alias)
                    byKey.getOrPut(SoundKey.looseKey(aliasSpelling)) { mutableListOf() } +=
                        Entry(word, frequency, aliasSpelling)
                }
            }
            return WordIndex(byKey, frequencies)
        }
    }
}
