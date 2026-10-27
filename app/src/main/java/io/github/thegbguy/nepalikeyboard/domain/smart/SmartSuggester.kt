package io.github.thegbguy.nepalikeyboard.domain.smart

import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.calendar.DateSource
import io.github.thegbguy.nepalikeyboard.domain.numbers.AmountSource
import io.github.thegbguy.nepalikeyboard.domain.units.UnitSource

/**
 * One kind of smart suggestion (amounts, dates, units). Looks at the end of [text], which is the
 * text before the cursor followed by the Romanized word being typed, and returns what it would
 * offer there.
 */
fun interface SmartSource {
    fun suggest(text: String, mode: LanguageMode): List<SmartMatch>
}

/**
 * A source's suggestion: [text] replaces the last [consumed] characters of the text it was given.
 */
data class SmartMatch(
    val text: String,
    val consumed: Int,
    val kind: SmartAction.Kind,
    val label: String = text.trimEnd()
)

/**
 * Asks each [SmartSource] in turn and keeps the first [MAX] suggestions. Nothing is remembered
 * between calls.
 */
class SmartSuggester(private val sources: List<SmartSource> = listOf(DateSource(), UnitSource, AmountSource)) {

    /** No smart suggestions in password, number or phone fields. Incognito fields still get them: nothing is learned. */
    fun allows(field: FieldInfo): Boolean = !field.isPrivate && !field.prefersNumbers

    /**
     * @param before text before the cursor, without the Romanized word being typed
     * @param composing the Romanized word being typed (Latin), or ""
     */
    fun suggest(before: String, composing: String, mode: LanguageMode, field: FieldInfo): List<SmartAction> {
        if (!allows(field)) return emptyList()
        val text = before + composing
        if (text.isBlank()) return emptyList()
        return sources.asSequence()
            .flatMap { it.suggest(text, mode) }
            // Tapping replaces the whole Romanized word, so a match must cover all of it.
            .filter { it.consumed >= composing.length && it.consumed <= text.length }
            .map { SmartAction(it.label, it.text, it.consumed - composing.length, it.kind) }
            .distinctBy { it.text }
            .take(MAX)
            .toList()
    }

    companion object {
        /** At most two suggestions share the strip with the emoji, clipboard, settings and mic buttons. */
        const val MAX = 2

        /** How much text before the cursor the sources see. */
        const val CONTEXT_CHARS = 40
    }
}
