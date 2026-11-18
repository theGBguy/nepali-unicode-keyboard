package io.github.thegbguy.nepalikeyboard.data.emoji

/**
 * Search words for each emoji, in English and Nepali, from the bundled `emoji_keywords.tsv`
 * (built by tools/emoji/build_emoji_keywords.py from Unicode CLDR). Loading the file is up to
 * each platform; [parse] drops emoji the phone's font can't draw, like [EmojiList].
 */
class EmojiKeywords private constructor(private val entries: List<Entry>) {

    private class Entry(val emoji: String, val words: List<String>)

    val size: Int get() = entries.size

    /**
     * Emoji whose words match any of [queries], best first, at most [limit]. A query matches when
     * each of its words starts one of the emoji's words; whole-word matches and matches on the
     * emoji's name (its first words) rank higher, then the panel's order.
     */
    fun search(queries: List<String>, limit: Int = 60): List<String> {
        val tokenLists = queries.map { q -> normalise(q).split(' ').filter { it.isNotEmpty() } }
            .filter { it.isNotEmpty() }
            .distinct()
        if (tokenLists.isEmpty()) return emptyList()
        return entries.withIndex()
            .mapNotNull { (index, entry) ->
                val score = tokenLists.maxOf { tokens -> score(entry, tokens) }
                if (score > 0) Triple(entry.emoji, score, index) else null
            }
            .sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenBy { it.third })
            .take(limit)
            .map { it.first }
    }

    /** 0 when a token matches nothing; otherwise 2 per whole word and 1 per prefix, +1 if a word of the name matches. */
    private fun score(entry: Entry, tokens: List<String>): Int {
        var total = 0
        for (token in tokens) {
            val exact = entry.words.indexOf(token)
            total += when {
                exact >= 0 -> if (exact < NAME_WORDS) 3 else 2
                entry.words.any { it.startsWith(token) } -> 1
                else -> return 0
            }
        }
        return total
    }

    companion object {
        const val ASSET = "emoji_keywords.tsv"

        /** The first words of each line are the emoji's name (CLDR's text-to-speech name). */
        private const val NAME_WORDS = 3

        fun parse(text: String, canDraw: (String) -> Boolean): EmojiKeywords =
            EmojiKeywords(
                text.lineSequence()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .mapNotNull { line ->
                        val parts = line.split('\t')
                        val emoji = parts[0]
                        if (parts.size < 2 || !canDraw(emoji)) return@mapNotNull null
                        val words = parts.drop(1).flatMap { normalise(it).split(' ') }.filter { it.isNotEmpty() }
                        Entry(emoji, words.distinct())
                    }
                    .toList()
            )

        fun parse(text: String): EmojiKeywords = parse(text) { true }

        /**
         * Lowercase, and Devanagari folded where Romanized spellings differ: nasals (ँ, ं, or a
         * half न/म/ङ/ञ/ण before a consonant) and ा dropped, long ी/ू as short ि/ु, श/ष as स,
         * joiners removed. So हाँसो and हन्सो (from "hanso") meet, as do खुशी and खुसी.
         */
        internal fun normalise(text: String): String = buildString {
            val s = text.lowercase()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c in NASALS && s.getOrNull(i + 1) == '्' && s.getOrNull(i + 2)?.let { it in '\u0915'..'\u0939' } == true) {
                    i += 2
                    continue
                }
                when (c) {
                    'ँ', 'ं', 'ा', '\u200c', '\u200d' -> {}
                    'आ' -> append('अ')
                    'ी' -> append('ि')
                    'ू' -> append('ु')
                    'ई' -> append('इ')
                    'ऊ' -> append('उ')
                    'श', 'ष' -> append('स')
                    else -> append(if (c.isWhitespace()) ' ' else c)
                }
                i++
            }
        }.trim()

        private const val NASALS = "नमङञण"
    }
}
