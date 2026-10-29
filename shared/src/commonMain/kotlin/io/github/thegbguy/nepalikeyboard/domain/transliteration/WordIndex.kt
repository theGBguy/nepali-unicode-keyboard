package io.github.thegbguy.nepalikeyboard.domain.transliteration

import kotlin.math.ln

/**
 * Nepali words with their frequencies, looked up by [SoundKey.looseKey] so that the many ways of
 * typing a word in Roman letters all find it.
 *
 * Input lines are `word<TAB>frequency[<TAB>alias,alias]`; aliases are extra Romanized spellings
 * (e.g. `kathmandu` for काठमाडौं) that the sound rules can't derive.
 *
 * Everything is packed into a few strings and int arrays rather than a map of lists of entry
 * objects: the iOS keyboard extension has a hard memory limit, and ~60k words held as objects took
 * about 46 MB there. Each kind of text (words, keys, spellings) is one string with an offsets
 * array; item `i` is `text[offsets[i] until offsets[i + 1]]`.
 *
 * - Words are sorted, so [frequency] is a binary search.
 * - Keys are sorted and each key's entries are stored together, so [lookup] is a binary search and
 *   [completions] walks the keys that follow.
 * - An entry is one way of typing a word: the word's id, the line's frequency and the spelling.
 *
 * Ties keep the file's order (the earlier line first) on every platform.
 */
class WordIndex private constructor(
    private val words: String,
    private val wordOffsets: IntArray,
    private val wordFrequencies: IntArray,
    private val keys: String,
    private val keyOffsets: IntArray,
    /** The entries of key `k` are `keyEntries[k] until keyEntries[k + 1]`. */
    private val keyEntries: IntArray,
    private val entryWords: IntArray,
    private val entryFrequencies: IntArray,
    private val spellings: String,
    private val spellingOffsets: IntArray
) {
    data class Match(val word: String, val score: Double)

    val size: Int get() = wordFrequencies.size

    private val keyCount: Int get() = keyOffsets.size - 1

    fun frequency(word: String): Int? {
        val id = search(words, wordOffsets, word)
        return if (id >= 0) wordFrequencies[id] else null
    }

    /**
     * Words whose sound key equals the typed text's, best first. Score = log frequency minus a
     * penalty per letter of difference between what was typed and how the word is spelled, so a
     * common word typed loosely beats a rare word typed exactly only when it's much more common.
     */
    fun lookup(typed: String, limit: Int = 4): List<Match> {
        val normalized = SoundKey.normalize(typed)
        val key = search(keys, keyOffsets, SoundKey.looseKey(normalized))
        if (key < 0) return emptyList()
        val best = LinkedHashMap<Int, Double>()
        for (e in keyEntries[key] until keyEntries[key + 1]) {
            val spelling = spellings.substring(spellingOffsets[e], spellingOffsets[e + 1])
            val score = ln(entryFrequencies[e] + 1.0) - DISTANCE_PENALTY * SoundKey.distance(normalized, spelling)
            val word = entryWords[e]
            if (score > (best[word] ?: Double.NEGATIVE_INFINITY)) best[word] = score
        }
        return best.entries.sortedByDescending { it.value }.take(limit).map { Match(word(it.key), it.value) }
    }

    /** Common longer words that start with what's been typed, for the suggestion strip. */
    fun completions(typed: String, limit: Int = 2): List<String> {
        val key = SoundKey.looseKey(SoundKey.normalize(typed))
        if (key.length < 3) return emptyList()
        val found = LinkedHashMap<Int, Int>()
        var visited = 0
        // Keys after [key] itself that start with it, in order
        var k = search(keys, keyOffsets, key).let { if (it >= 0) it + 1 else -(it + 1) }
        while (k < keyCount && startsWith(keys, keyOffsets[k], keyOffsets[k + 1], key)) {
            for (e in keyEntries[k] until keyEntries[k + 1]) {
                val word = entryWords[e]
                if (entryFrequencies[e] > (found[word] ?: -1)) found[word] = entryFrequencies[e]
            }
            k++
            if (++visited >= MAX_COMPLETION_KEYS) break
        }
        return found.entries.sortedByDescending { it.value }.take(limit).map { word(it.key) }
    }

    private fun word(id: Int): String = words.substring(wordOffsets[id], wordOffsets[id + 1])

    companion object {
        const val DISTANCE_PENALTY = 2.0
        private const val MAX_COMPLETION_KEYS = 400

        /** [build] from the whole file's text (for Swift, which can't make a Kotlin Sequence). */
        fun parse(text: String): WordIndex = build(text.lineSequence())

        /**
         * Builds straight into packed buffers and sorts index arrays, so no object per word or
         * spelling is kept along the way: on Kotlin/Native those intermediate objects alone took
         * over 30 MB, and the load peak counts against the iOS extension's limit as much as the
         * finished index does.
         */
        fun build(lines: Sequence<String>): WordIndex {
            // Per line: its word and frequency. Per row (one way of typing a word, file order):
            // its line, sound key and spelling.
            val lineWords = PackedBuilder()
            val lineFrequencies = IntList()
            val rowLines = IntList()
            val rowKeys = PackedBuilder()
            val rowSpellings = PackedBuilder()
            fun addRow(spelling: String) {
                rowLines.add(lineFrequencies.size - 1)
                rowKeys.add(SoundKey.looseKey(spelling))
                rowSpellings.add(spelling)
            }
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#")) continue
                val parts = line.split('\t')
                val word = parts[0].trim()
                val frequency = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: continue
                if (word.isEmpty()) continue
                lineWords.add(word)
                lineFrequencies.add(frequency)
                for (spelling in SoundKey.spellingsForWord(word)) addRow(spelling)
                parts.getOrNull(2)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { alias ->
                    addRow(SoundKey.normalize(alias))
                }
            }

            // Words sorted and unique; a word listed twice keeps its highest frequency
            val (lineText, lineOffsets) = lineWords.finish()
            val lineCount = lineFrequencies.size
            val byWord = stableOrder(lineCount) { a, b -> compare(lineText, lineOffsets, a, b) }
            val lineWordIds = IntArray(lineCount)
            val words = PackedBuilder()
            val wordFrequencies = IntList()
            for (i in 0 until lineCount) {
                val line = byWord[i]
                if (i == 0 || compare(lineText, lineOffsets, byWord[i - 1], line) != 0) {
                    words.add(lineText, lineOffsets[line], lineOffsets[line + 1])
                    wordFrequencies.add(0)
                }
                val id = wordFrequencies.size - 1
                lineWordIds[line] = id
                wordFrequencies[id] = maxOf(wordFrequencies[id], lineFrequencies[line])
            }

            // Rows grouped by key; the sort is stable, so each key's entries stay in file order
            val (rowKeyText, rowKeyOffsets) = rowKeys.finish()
            val (rowSpellingText, rowSpellingOffsets) = rowSpellings.finish()
            val rowCount = rowLines.size
            val byKey = stableOrder(rowCount) { a, b -> compare(rowKeyText, rowKeyOffsets, a, b) }
            val keys = PackedBuilder()
            val keyEntries = IntList()
            val spellings = PackedBuilder()
            for (i in 0 until rowCount) {
                val row = byKey[i]
                if (i == 0 || compare(rowKeyText, rowKeyOffsets, byKey[i - 1], row) != 0) {
                    keys.add(rowKeyText, rowKeyOffsets[row], rowKeyOffsets[row + 1])
                    keyEntries.add(i)
                }
                spellings.add(rowSpellingText, rowSpellingOffsets[row], rowSpellingOffsets[row + 1])
            }
            keyEntries.add(rowCount)

            val (wordText, wordOffsets) = words.finish()
            val (keyText, keyOffsets) = keys.finish()
            val (spellingText, spellingOffsets) = spellings.finish()
            return WordIndex(
                words = wordText,
                wordOffsets = wordOffsets,
                wordFrequencies = wordFrequencies.toIntArray(),
                keys = keyText,
                keyOffsets = keyOffsets,
                keyEntries = keyEntries.toIntArray(),
                entryWords = IntArray(rowCount) { lineWordIds[rowLines[byKey[it]]] },
                entryFrequencies = IntArray(rowCount) { lineFrequencies[rowLines[byKey[it]]] },
                spellings = spellingText,
                spellingOffsets = spellingOffsets
            )
        }

        /** Strings appended back to back, and where each starts. */
        private class PackedBuilder {
            private val text = StringBuilder()
            private val offsets = IntList()

            fun add(s: CharSequence, start: Int = 0, end: Int = s.length) {
                offsets.add(text.length)
                text.appendRange(s, start, end)
            }

            /** The text, and offsets with the total length appended. */
            fun finish(): Pair<String, IntArray> {
                offsets.add(text.length)
                return text.toString() to offsets.toIntArray()
            }
        }

        /** A growable IntArray (an ArrayList<Int> boxes every element). */
        private class IntList {
            private var values = IntArray(1024)
            var size = 0
                private set

            fun add(value: Int) {
                if (size == values.size) values = values.copyOf(size * 2)
                values[size++] = value
            }

            operator fun get(i: Int): Int = values[i]
            operator fun set(i: Int, value: Int) {
                values[i] = value
            }

            fun toIntArray(): IntArray = values.copyOf(size)
        }

        /** `0 until count` stably sorted by [compare] (a merge sort over plain ints). */
        private inline fun stableOrder(count: Int, compare: (Int, Int) -> Int): IntArray {
            var from = IntArray(count) { it }
            var to = IntArray(count)
            var width = 1
            while (width < count) {
                var low = 0
                while (low < count) {
                    val mid = minOf(low + width, count)
                    val high = minOf(low + 2 * width, count)
                    var i = low
                    var j = mid
                    var k = low
                    while (i < mid && j < high) to[k++] = if (compare(from[j], from[i]) < 0) from[j++] else from[i++]
                    while (i < mid) to[k++] = from[i++]
                    while (j < high) to[k++] = from[j++]
                    low = high
                }
                val t = from; from = to; to = t
                width *= 2
            }
            return from
        }

        /** Compares packed items [a] and [b] like [String.compareTo]. */
        private fun compare(text: String, offsets: IntArray, a: Int, b: Int): Int {
            val aStart = offsets[a]
            val bStart = offsets[b]
            val aLength = offsets[a + 1] - aStart
            val bLength = offsets[b + 1] - bStart
            for (i in 0 until minOf(aLength, bLength)) {
                val diff = text[aStart + i] - text[bStart + i]
                if (diff != 0) return diff
            }
            return aLength - bLength
        }

        /**
         * [target]'s index among the sorted strings packed in [text], or `-(insertion point) - 1`,
         * like [List.binarySearch]. Orders like [String.compareTo].
         */
        private fun search(text: String, offsets: IntArray, target: String): Int {
            var low = 0
            var high = offsets.size - 2
            while (low <= high) {
                val mid = (low + high) ushr 1
                val cmp = compare(text, offsets[mid], offsets[mid + 1], target)
                when {
                    cmp < 0 -> low = mid + 1
                    cmp > 0 -> high = mid - 1
                    else -> return mid
                }
            }
            return -(low + 1)
        }

        private fun compare(text: String, start: Int, end: Int, other: String): Int {
            val length = end - start
            for (i in 0 until minOf(length, other.length)) {
                val diff = text[start + i] - other[i]
                if (diff != 0) return diff
            }
            return length - other.length
        }

        private fun startsWith(text: String, start: Int, end: Int, prefix: String): Boolean =
            end - start >= prefix.length && text.regionMatches(start, prefix, 0, prefix.length)
    }
}
