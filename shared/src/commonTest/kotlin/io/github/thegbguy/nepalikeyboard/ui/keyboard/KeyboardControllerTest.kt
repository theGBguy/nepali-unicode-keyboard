package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipItem
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardSource
import io.github.thegbguy.nepalikeyboard.domain.clipboard.CopiedText
import io.github.thegbguy.nepalikeyboard.domain.clipboard.InMemoryClipboardStorage
import io.github.thegbguy.nepalikeyboard.domain.model.EnterAction
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.FieldKind
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shared controller against a fake field, so the typing logic is also checked on the iOS
 * simulator. The Android view-model tests in the app cover the same controller in more detail
 * through the InputConnection adapter.
 */
class KeyboardControllerTest {

    private lateinit var keyboard: KeyboardController
    private lateinit var field: FakeEditor

    private val space = KeyData(" ", keyType = KeyType.ACTION)
    private val enter = KeyData("↵", keyType = KeyType.ACTION)
    private val backspace = KeyData("⌫", keyType = KeyType.FUNCTION)
    private val globe = KeyData("🌐", keyType = KeyType.FUNCTION)

    @BeforeTest
    fun setUp() {
        field = FakeEditor()
        keyboard = KeyboardController().also {
            it.editor = { field }
            it.prefs = InMemoryKeyboardPrefs(typeNepali = false)
        }
        keyboard.onInputStarted(FieldInfo())
    }

    @AfterTest
    fun tearDown() = keyboard.close()

    private fun type(text: String) = text.forEach { keyboard.onKeyClick(KeyData(it.toString())) }

    private fun language() = keyboard.state.value.currentLanguage

    @Test
    fun `English letters are typed as they are`() {
        type("hi")
        keyboard.onKeyClick(space)
        assertEquals("hi ", field.shown)
    }

    @Test
    fun `a Romanized word is composed in Latin and committed as Devanagari on space`() {
        keyboard.onKeyClick(globe)
        assertEquals(LanguageMode.TRANSLITERATION, language())
        type("namaste")
        assertEquals("", field.text)
        assertEquals("namaste", field.composing)
        assertTrue(keyboard.state.value.showSuggestionBar)

        keyboard.onKeyClick(space)
        assertEquals("नमस्ते ", field.shown)
        assertTrue(keyboard.state.value.transliterationBuffer.isEmpty())
    }

    @Test
    fun `backspace edits the Romanized word before touching the text`() {
        field.text = "a "
        keyboard.onKeyClick(globe)
        type("ghar")
        keyboard.onKeyClick(backspace)
        assertEquals("gha", field.composing)
        assertEquals("a ", field.text)
    }

    @Test
    fun `backspace on the only Romanized letter removes it`() {
        field.text = "a "
        keyboard.onKeyClick(globe)
        type("y")
        keyboard.onKeyClick(backspace)
        assertEquals("a ", field.shown)
        assertTrue(keyboard.state.value.transliterationBuffer.isEmpty())
        keyboard.onKeyClick(backspace)
        assertEquals("a", field.shown)
    }

    @Test
    fun `a full stop ends the Romanized word as a danda`() {
        keyboard.onKeyClick(globe)
        type("namaste.")
        assertEquals("नमस्ते।", field.shown)
    }

    @Test
    fun `a vowel sign after अ becomes the vowel letter`() {
        keyboard.prefs.nepaliStyle = LanguageMode.NEPALI
        keyboard.onKeyClick(globe)
        assertEquals(LanguageMode.NEPALI, language())
        type("अा")
        assertEquals("आ", field.shown)
    }

    @Test
    fun `backspace removes a whole emoji`() {
        field.text = "ok 👍🏽"
        keyboard.onKeyClick(backspace)
        assertEquals("ok ", field.shown)
    }

    @Test
    fun `backspace deletes a selection as a whole`() {
        field.selection = "selected"
        keyboard.onKeyClick(backspace)
        assertEquals("", field.selection)
    }

    @Test
    fun `enter runs the field's action or inserts a line or sends a key`() {
        keyboard.onInputStarted(FieldInfo(enterAction = EnterAction.SEND, imeActionId = 4))
        keyboard.onKeyClick(enter)
        assertEquals(listOf(4), field.actions)

        keyboard.onInputStarted(FieldInfo())
        keyboard.onKeyClick(enter)
        assertEquals("\n", field.text)

        keyboard.onInputStarted(FieldInfo(sendsKeyEvents = true))
        keyboard.onKeyClick(enter)
        assertEquals(1, field.enterKeys)
    }

    @Test
    fun `enter commits the Romanized word first`() {
        keyboard.onKeyClick(globe)
        type("ghar")
        keyboard.onKeyClick(enter)
        assertEquals("घर\n", field.shown)
    }

    @Test
    fun `password fields open in English`() {
        keyboard.prefs.typeNepali = true
        keyboard.onInputStarted(FieldInfo(kind = FieldKind.PASSWORD, isPrivate = true, noLearning = true))
        assertEquals(LanguageMode.ENGLISH, language())
    }

    @Test
    fun `clear all clears the phone's current copy unless it's pinned`() {
        val clipboard = object : ClipboardSource {
            var copied: CopiedText? = CopiedText("copied", sensitive = false)
            override fun read() = copied
            override fun observe(onCopy: (() -> Unit)?) {}
            override fun recentCopyId(withinMillis: Long): Long? = null
            override fun clear() {
                copied = null
            }
        }
        val storage = InMemoryClipboardStorage(listOf(ClipItem("older", time = 1)))
        keyboard.clipboardSource = clipboard
        keyboard.clipboardStorage = storage

        keyboard.openClipboard()
        keyboard.onClipClearAll()
        assertNull(clipboard.copied)
        assertEquals(emptyList(), storage.load())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `holding backspace keeps deleting until it's released`() = runTest {
        val repeating = KeyboardController { backgroundScope }.also { it.editor = { field } }
        field.text = "abcdefghij"
        repeating.onBackspaceLongPressStart()
        advanceTimeBy(499)
        runCurrent()
        assertEquals("abcdefghij", field.text) // nothing before the initial delay
        advanceTimeBy(1)
        runCurrent()
        assertEquals("abcdefghi", field.text)
        advanceTimeBy(100)
        runCurrent()
        assertEquals("abcdefgh", field.text)
        repeating.onBackspaceLongPressEnd()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("abcdefgh", field.text)
    }
}
