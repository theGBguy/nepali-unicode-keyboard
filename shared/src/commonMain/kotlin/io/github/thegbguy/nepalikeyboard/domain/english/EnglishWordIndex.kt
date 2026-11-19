package io.github.thegbguy.nepalikeyboard.domain.english

import kotlin.concurrent.Volatile
import kotlin.math.ln

/**
 * The bundled English word list (`words_en.tsv`), loaded once per process by the platform
 * (Android: `EnglishWordListLoader.kt` in the app, iOS: `WordListLoader.swift`) on a background
 * thread. Until it's ready, English typing has no word suggestions.
 *
 * Built by `tools/wordlist/build_english_wordlist.py` from OpenSubtitles word counts
 * (FrequencyWords, CC BY-SA 4.0) filtered through SCOWL.
 */
object EnglishWordList {
    const val ASSET = "words_en.tsv"

    @Volatile
    var index: EnglishWordIndex? = null
}

/**
 * English words by frequency, for the strip's suggestions: completions of the word being typed
 * and corrections one typo away (a letter missed, doubled, swapped or mistyped, or a missing
 * apostrophe: `dont` → don't, `im` → I'm).
 *
 * Words are kept in one array sorted by their lowercase spelling, so completions are a range
 * and exact lookups a binary search; no map or object per spelling is built (the iOS extension
 * has a tight memory limit).
 */
class EnglishWordIndex private constructor(
    /** Lowercase spellings, sorted. */
    private val keys: Array<String>,
    /** The word as written where it has capitals (I, I'm, London), else null. */
    private val forms: Array<String?>,
    private val scores: DoubleArray
) {
    val size: Int get() = keys.size

    /**
     * Up to [limit] words for [typed], best first: the word itself when it's a known word, then
     * completions and corrections by frequency. Each follows the capitals typed (`Hel` → Hello,
     * `HEL` → HELLO) unless the word has its own (`london` → London).
     */
    fun suggest(typed: String, limit: Int = 6): List<String> {
        val key = typed.replace('\u2019', '\'').lowercase()
        if (key.isEmpty() || key.length > MAX_LENGTH) return emptyList()
        val found = HashMap<Int, Double>()
        fun offer(index: Int, score: Double) {
            if (score > (found[index] ?: Double.NEGATIVE_INFINITY)) found[index] = score
        }

        val exact = find(key)
        if (exact >= 0) offer(exact, scores[exact] + EXACT_BONUS)

        // Completions: every word starting with what was typed, longer ones a little behind
        var i = lowerBound(key)
        while (i < keys.size && keys[i].startsWith(key)) {
            if (i != exact) offer(i, scores[i] - COMPLETION_COST - LETTER_COST * (keys[i].length - key.length))
            i++
        }

        // Corrections: known words one edit away. A missing apostrophe isn't a typo but the same
        // word (dont, cant, wont), so it competes with the typed word on frequency alone.
        if (key.length >= 2) {
            edits(key) { variant, cost ->
                val at = find(variant)
                if (at >= 0 && at != exact) {
                    offer(at, scores[at] - cost + if (cost == APOSTROPHE_COST) EXACT_BONUS else 0.0)
                }
            }
        }

        return found.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Double>> { it.value }.thenBy { keys[it.key] })
            .asSequence()
            .map { withCase(forms[it.key] ?: keys[it.key], typed) }
            .distinct()
            .take(limit)
            .toList()
    }

    /** Index of [key], or -1. */
    private fun find(key: String): Int {
        val at = lowerBound(key)
        return if (at < keys.size && keys[at] == key) at else -1
    }

    /** First index whose key is >= [key]. */
    private fun lowerBound(key: String): Int {
        var lo = 0
        var hi = keys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[mid] < key) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        /** Longer words aren't looked up (nobody types them as one word). */
        const val MAX_LENGTH = 24

        private const val EXACT_BONUS = 6.0
        private const val COMPLETION_COST = 0.5
        private const val LETTER_COST = 0.1
        private const val TYPO_COST = 4.5
        private const val NEAR_KEY_COST = 3.0
        private const val SWAP_COST = 3.0
        private const val APOSTROPHE_COST = 0.5

        private const val LETTERS = "abcdefghijklmnopqrstuvwxyz"
        private val ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

        fun parse(text: String): EnglishWordIndex = build(text.lineSequence())

        /** Lines are `word<TAB>count`; `#` lines are comments. */
        fun build(lines: Sequence<String>): EnglishWordIndex {
            val words = ArrayList<String>()
            val counts = ArrayList<Int>()
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#")) continue
                val tab = line.indexOf('\t')
                if (tab <= 0) continue
                val count = line.substring(tab + 1).trim().toIntOrNull() ?: continue
                words += line.substring(0, tab).trim()
                counts += count
            }
            val order = words.indices.sortedBy { words[it].lowercase() }
            val keys = Array(order.size) { words[order[it]].lowercase() }
            val forms = Array(order.size) { i -> words[order[i]].takeIf { it != keys[i] } }
            val scores = DoubleArray(order.size) { ln(counts[order[it]].coerceAtLeast(1).toDouble()) }
            return EnglishWordIndex(keys, forms, scores)
        }

        /** Applies the capitals the user typed to [word]. */
        internal fun withCase(word: String, typed: String): String {
            val letters = typed.filter { it.isLetter() }
            return when {
                letters.length > 1 && letters.all { it.isUpperCase() } -> word.uppercase()
                typed.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
                else -> word
            }
        }

        /** Every spelling one edit from [key], with what that typo costs. */
        private inline fun edits(key: String, visit: (String, Double) -> Unit) {
            for (i in key.indices) {
                // A letter too many (doubled letters are the usual slip)
                val extra = if (i > 0 && key[i] == key[i - 1]) NEAR_KEY_COST else TYPO_COST
                visit(key.removeRange(i, i + 1), extra)
                // Two letters swapped
                if (i + 1 < key.length && key[i] != key[i + 1]) {
                    visit(key.substring(0, i) + key[i + 1] + key[i] + key.substring(i + 2), SWAP_COST)
                }
                // A mistyped letter, cheaper when the keys are neighbours
                for (c in LETTERS) {
                    if (c != key[i]) {
                        visit(key.substring(0, i) + c + key.substring(i + 1), if (near(key[i], c)) NEAR_KEY_COST else TYPO_COST)
                    }
                }
            }
            // A missing letter or apostrophe
            for (i in 0..key.length) {
                val before = key.substring(0, i)
                val after = key.substring(i)
                if (i in 1 until key.length) visit("$before'$after", APOSTROPHE_COST)
                for (c in LETTERS) visit(before + c + after, TYPO_COST)
            }
        }

        /** Whether [a] and [b] sit next to each other on a QWERTY keyboard. */
        private fun near(a: Char, b: Char): Boolean {
            val ra = ROWS.indexOfFirst { a in it }
            val rb = ROWS.indexOfFirst { b in it }
            if (ra < 0 || rb < 0 || kotlin.math.abs(ra - rb) > 1) return false
            val ca = ROWS[ra].indexOf(a) + ra * 0.5
            val cb = ROWS[rb].indexOf(b) + rb * 0.5
            return kotlin.math.abs(ca - cb) <= 1.0
        }

        /**
         * The word the cursor is at the end of, or null: letters and apostrophes after a space
         * (or the start, or an opening bracket or quote). Not inside a word ([after] starts with
         * a letter), nor after `@`, `#`, `/`, a digit or other symbols, where it's a handle, tag,
         * link or code rather than an English word.
         */
        fun currentWord(before: String, after: String?): String? {
            if (after != null && after.firstOrNull()?.let { it.isLetterOrDigit() || it == '\'' || it == '\u2019' } == true) return null
            var start = before.length
            while (start > 0 && before[start - 1].let { it in 'a'..'z' || it in 'A'..'Z' || it == '\'' || it == '\u2019' }) start--
            val word = before.substring(start).trimStart('\'', '\u2019')
            if (word.isEmpty() || word.length > MAX_LENGTH) return null
            val previous = before.getOrNull(before.length - word.length - 1)
            if (previous != null && !previous.isWhitespace() && previous !in WORD_OPENERS) return null
            return word
        }

        private const val WORD_OPENERS = "\"'(\u201C\u2018\u2019[-"
    }
}
