package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.domain.model.CapsMode

/** When English typing starts the next letter as a capital, from the text before the cursor. */
object AutoCaps {

    /** Characters of text before the cursor that [wants] needs to look at. */
    const val CONTEXT = 20

    private const val SENTENCE_ENDS = ".!?।"
    private const val CLOSERS = "\"')]”’"

    /**
     * Whether the next letter should be a capital in a field asking for [mode], with [before] typed
     * before the cursor. Sentences start at the beginning of the field, on a new line, and after
     * `.`, `!` or `?` (closing quotes and brackets allowed) followed by a space. [before] null means
     * the field can't say, so nothing is capitalised except in [CapsMode.CHARACTERS].
     */
    fun wants(mode: CapsMode, before: String?): Boolean = when (mode) {
        CapsMode.NONE -> false
        CapsMode.CHARACTERS -> true
        CapsMode.WORDS -> before != null && (before.isEmpty() || before.last().isWhitespace())
        CapsMode.SENTENCES -> before != null && startsSentence(before)
    }

    private fun startsSentence(before: String): Boolean {
        val text = before.trimEnd { it == ' ' || it == '\t' }
        if (text.isEmpty() || text.last() == '\n') return true
        if (text.length == before.length) return false // still inside a word
        val end = text.trimEnd { it in CLOSERS }
        return end.isNotEmpty() && end.last() in SENTENCE_ENDS
    }
}
