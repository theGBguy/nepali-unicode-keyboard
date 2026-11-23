package io.github.thegbguy.nepalikeyboard.domain.voice

/**
 * Turns punctuation said aloud into marks, because the Nepali speech recognizer writes "अल्पविराम"
 * as a word: "म आएँ अल्पविराम तिमी" → "म आएँ, तिमी". A mark joins the word before it; "नयाँ लाइन"
 * starts a new line. Only whole words match, so "प्रश्न" on its own stays a word.
 */
object SpokenPunctuation {

    private const val NEW_LINE = "\n"

    /** Spoken phrases (as the recognizer writes them) and their marks, longest phrase first. */
    private val PHRASES: List<Pair<List<String>, String>> = listOf(
        "," to listOf("अल्पविराम", "अल्प विराम", "कमा", "कम्मा"),
        "?" to listOf(
            "प्रश्नचिन्ह", "प्रश्न चिन्ह", "प्रश्नचिह्न", "प्रश्न चिह्न",
            "प्रश्नवाचक चिन्ह", "प्रश्नवाचक चिह्न", "प्रश्नवाचक", "क्वेश्चन मार्क", "क्वेस्चन मार्क"
        ),
        "।" to listOf("पूर्णविराम", "पूर्ण विराम", "पुर्णविराम", "पुर्ण विराम", "फुलस्टप", "फुल स्टप"),
        "!" to listOf("विस्मयादिबोधक चिन्ह", "विस्मयादिबोधक चिह्न", "विस्मयादिबोधक"),
        NEW_LINE to listOf("नयाँ लाइन", "नयाँ लाईन", "नया लाइन", "नया लाईन")
    ).flatMap { (mark, phrases) -> phrases.map { it.split(' ') to mark } }
        .sortedByDescending { it.first.size }

    /** Marks a dictation may start with; no space goes before them. */
    private val MARKS = setOf(",", "?", "।", "!", NEW_LINE)

    fun apply(text: String): String {
        val words = text.split(' ', '\t').filter { it.isNotEmpty() }
        val out = StringBuilder()
        var i = 0
        while (i < words.size) {
            val match = PHRASES.firstOrNull { (phrase, _) ->
                i + phrase.size <= words.size && phrase.indices.all { words[i + it] == phrase[it] }
            }
            if (match != null) {
                while (out.endsWith(" ")) out.setLength(out.length - 1)
                out.append(match.second)
                i += match.first.size
            } else {
                if (out.isNotEmpty() && !out.endsWith(NEW_LINE)) out.append(' ')
                out.append(words[i])
                i++
            }
        }
        return out.toString()
    }

    /**
     * What to type for a dictation [text] (already through [apply]) after [before], the character
     * before the cursor: a space first unless the field is empty, already ends in a space or
     * line break, or the dictation starts with a mark.
     */
    fun withLeadingSpace(text: String, before: String?): String {
        if (text.isEmpty() || before.isNullOrEmpty() || before.last().isWhitespace()) return text
        if (MARKS.any { text.startsWith(it) }) return text
        return " $text"
    }
}
