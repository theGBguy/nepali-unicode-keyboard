package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.domain.english.EnglishWordIndex
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.FieldKind
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** English word suggestions in the strip, through the shared controller. */
class EnglishSuggestionsTest {

    private lateinit var keyboard: KeyboardController
    private lateinit var field: FakeEditor
    private lateinit var prefs: InMemoryKeyboardPrefs

    private val words = EnglishWordIndex.parse("the\t100\nthey\t50\nhello\t40\nhelp\t60\nI'm\t70\nin\t80")
    private val space = KeyData(" ", keyType = KeyType.ACTION)

    @BeforeTest
    fun setUp() {
        field = FakeEditor()
        prefs = InMemoryKeyboardPrefs(typeNepali = false, autoCapitalise = false)
        keyboard = KeyboardController().also {
            it.editor = { field }
            it.prefs = prefs
            it.englishWords = { words }
        }
        keyboard.onInputStarted(FieldInfo())
    }

    @AfterTest
    fun tearDown() = keyboard.close()

    /** Types like the platforms do: each key is followed by a cursor update. */
    private fun type(text: String) = text.forEach {
        keyboard.onKeyClick(if (it == ' ') space else KeyData(it.toString()))
        keyboard.onCursorMoved(stillComposing = false, hasComposing = false, hasSelection = false)
    }

    private fun shown() = keyboard.state.value.englishSuggestions

    @Test
    fun `typing a word offers completions`() {
        type("he")
        assertEquals(listOf("help", "hello"), shown().take(2))
    }

    @Test
    fun `tapping a suggestion replaces the word and adds a space`() {
        type("say he")
        keyboard.onSuggestionClick("hello")
        assertEquals("say hello ", field.text)
        assertEquals(emptyList(), shown())
    }

    @Test
    fun `space keeps the word as typed`() {
        type("teh ")
        assertEquals("teh ", field.text)
        assertEquals(emptyList(), shown())
    }

    @Test
    fun `a correction replaces the whole typo`() {
        type("im")
        assertEquals("I'm", shown().first())
        keyboard.onSuggestionClick("I'm")
        assertEquals("I'm ", field.text)
    }

    @Test
    fun `nothing is offered with the setting off`() {
        prefs.wordSuggestions = false
        type("he")
        assertEquals(emptyList(), shown())
    }

    @Test
    fun `nothing is offered in email or private fields`() {
        keyboard.onInputStarted(FieldInfo(kind = FieldKind.EMAIL))
        type("he")
        assertEquals(emptyList(), shown())
        keyboard.onInputStarted(FieldInfo(kind = FieldKind.PASSWORD, isPrivate = true))
        type("he")
        assertEquals(emptyList(), shown())
    }

    @Test
    fun `nothing is offered in the middle of a word`() {
        field.after = "lo"
        type("hel")
        assertEquals(emptyList(), shown())
    }

    @Test
    fun `Nepali modes get no English suggestions`() {
        keyboard.setNepaliStyle(LanguageMode.TRANSLITERATION)
        type("he")
        assertTrue(shown().isEmpty())
    }

    @Test
    fun `switching to English offers words for the word already there`() {
        keyboard.setNepaliStyle(LanguageMode.NEPALI)
        field.commitText("he")
        keyboard.switchLanguage()
        assertEquals(LanguageMode.ENGLISH, keyboard.state.value.currentLanguage)
        assertEquals(listOf("help", "hello"), shown().take(2))
    }
}
