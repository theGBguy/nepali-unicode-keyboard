package io.github.thegbguy.nepalikeyboard.domain.smart

import android.text.InputType
import android.view.inputmethod.EditorInfo
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.model.classify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSuggesterTest {

    private val text = FieldInfo()
    private val mode = LanguageMode.TRANSLITERATION

    /** Offers "<X>" for whatever the last [consumed] characters are. */
    private fun source(consumed: Int, vararg outputs: String) = SmartSource { input, _ ->
        outputs.map { SmartMatch("<$it>", consumed.coerceAtMost(input.length), SmartAction.Kind.AMOUNT) }
    }

    @Test
    fun `replace counts only the text before the composing word`() {
        val suggester = SmartSuggester(listOf(source(5, "a")))
        val actions = suggester.suggest(before = "rs ", composing = "50", mode = mode, field = text)
        assertEquals(listOf(SmartAction("<a>", "<a>", 3, SmartAction.Kind.AMOUNT)), actions)
    }

    @Test
    fun `a match that doesn't cover the whole composing word is dropped`() {
        val suggester = SmartSuggester(listOf(source(2, "a")))
        assertTrue(suggester.suggest("", "abc12", mode, text).isEmpty())
    }

    @Test
    fun `at most two suggestions, duplicates removed, sources in order`() {
        val suggester = SmartSuggester(listOf(source(1, "a", "a"), source(1, "b", "c")))
        assertEquals(listOf("<a>", "<b>"), suggester.suggest("x", "", mode, text).map { it.text })
    }

    @Test
    fun `nothing in password, number or phone fields, but incognito is fine`() {
        val suggester = SmartSuggester(listOf(source(1, "a")))
        val password = FieldInfo.classify(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, 0)
        val number = FieldInfo.classify(InputType.TYPE_CLASS_NUMBER, 0)
        val phone = FieldInfo.classify(InputType.TYPE_CLASS_PHONE, 0)
        val incognito = FieldInfo.classify(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        listOf(password, number, phone).forEach { assertTrue(suggester.suggest("x", "", mode, it).isEmpty()) }
        assertEquals(1, suggester.suggest("x", "", mode, incognito).size)
    }

    @Test
    fun `the real amount source is on by default`() {
        val actions = SmartSuggester().suggest("", "1500000", mode, text)
        assertEquals(listOf("१५,००,०००", "पन्ध्र लाख"), actions.map { it.label })
    }
}
