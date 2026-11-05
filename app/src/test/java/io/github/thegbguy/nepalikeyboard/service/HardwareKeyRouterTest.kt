package io.github.thegbguy.nepalikeyboard.service

import android.view.KeyEvent
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.ui.keyboard.EditorConnection
import io.github.thegbguy.nepalikeyboard.ui.keyboard.InMemoryKeyboardPrefs
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Physical keys → the controller or the app, against a fake field. */
class HardwareKeyRouterTest {

    /** Committed text plus the composing word, cursor at the end. */
    private class Field : EditorConnection {
        var text = ""
        var composing = ""
        val shown get() = text + composing
        override fun commitText(text: String) { this.text += text; composing = "" }
        override fun setComposingText(text: String) { composing = text }
        override fun finishComposingText() { text += composing; composing = "" }
        override fun textBeforeCursor(length: Int) = shown.takeLast(length)
        override fun selectedText() = ""
        override fun deleteBeforeCursor(length: Int) { text = text.dropLast(length) }
        override fun beginBatchEdit() {}
        override fun endBatchEdit() {}
        override fun sendEnterKey() {}
        override fun performEditorAction(actionId: Int) {}
    }

    private lateinit var keyboard: KeyboardController
    private lateinit var field: Field
    private lateinit var router: HardwareKeyRouter

    @Before
    fun setUp() {
        field = Field()
        keyboard = KeyboardController().also {
            it.editor = { field }
            it.prefs = InMemoryKeyboardPrefs(typeNepali = true)
        }
        keyboard.onInputStarted(FieldInfo())
        router = HardwareKeyRouter { keyboard }
    }

    @After
    fun tearDown() = keyboard.close()

    private fun press(keyCode: Int, meta: Int = 0, char: Char? = null, repeat: Int = 0): Boolean {
        val down = router.onKeyDown(keyCode, meta, char?.code ?: 0, repeat)
        val up = router.onKeyUp(keyCode)
        assertEquals("key up must match key down for $keyCode", down, up)
        return down
    }

    private fun typeLatin(text: String) = text.forEach { c ->
        val code = if (c.isLetter()) KeyEvent.KEYCODE_A + (c.lowercaseChar() - 'a') else KeyEvent.KEYCODE_UNKNOWN
        assertTrue("$c", press(code, if (c.isUpperCase()) KeyEvent.META_SHIFT_ON else 0, c))
    }

    @Test
    fun `Romanized letters compose and space commits`() {
        typeLatin("namaste")
        assertEquals("namaste", field.composing)
        assertTrue(press(KeyEvent.KEYCODE_SPACE, char = ' '))
        assertEquals("नमस्ते ", field.shown)
    }

    @Test
    fun `a full stop gives a danda`() {
        typeLatin("ho")
        assertTrue(press(KeyEvent.KEYCODE_PERIOD, char = '.'))
        assertEquals("हो।", field.shown)
    }

    @Test
    fun `capitals typed with shift reach the engine`() {
        typeLatin("N")
        press(KeyEvent.KEYCODE_SPACE, char = ' ')
        assertEquals("ण ", field.shown)
    }

    @Test
    fun `backspace edits the word, then goes to the app`() {
        typeLatin("ab")
        assertTrue(press(KeyEvent.KEYCODE_DEL))
        assertEquals("a", field.composing)
        assertTrue(press(KeyEvent.KEYCODE_DEL))
        assertFalse(press(KeyEvent.KEYCODE_DEL))
    }

    @Test
    fun `enter, arrows and shortcuts go to the app after the word is committed`() {
        typeLatin("ghar")
        assertFalse(press(KeyEvent.KEYCODE_ENTER))
        assertEquals("घर", field.shown)
        typeLatin("ma")
        assertFalse(press(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON, 'v'))
        assertEquals("", field.composing)
        assertTrue(field.text, field.text.startsWith("घर") && field.text.length > 2)
        assertFalse(press(KeyEvent.KEYCODE_DPAD_LEFT))
    }

    @Test
    fun `shift alone doesn't end the word`() {
        typeLatin("gha")
        assertFalse(press(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON))
        assertEquals("gha", field.composing)
    }

    @Test
    fun `ctrl+space cycles the language, once per press`() {
        assertEquals(LanguageMode.TRANSLITERATION, keyboard.state.value.currentLanguage)
        val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        assertTrue(router.onKeyDown(KeyEvent.KEYCODE_SPACE, ctrl, ' '.code, 0))
        assertTrue(router.onKeyDown(KeyEvent.KEYCODE_SPACE, ctrl, ' '.code, 1)) // held: no repeat switch
        assertTrue(router.onKeyUp(KeyEvent.KEYCODE_SPACE))
        assertEquals(LanguageMode.ENGLISH, keyboard.state.value.currentLanguage)
        press(KeyEvent.KEYCODE_SPACE, ctrl, ' ')
        assertEquals(LanguageMode.TRANSLITERATION, keyboard.state.value.currentLanguage)
    }

    @Test
    fun `English mode leaves every key to the app`() {
        keyboard.switchLanguage()
        assertEquals(LanguageMode.ENGLISH, keyboard.state.value.currentLanguage)
        assertFalse(press(KeyEvent.KEYCODE_A, char = 'a'))
        assertFalse(press(KeyEvent.KEYCODE_SPACE, char = ' '))
        assertEquals("", field.shown)
    }

    @Test
    fun `traditional mode types the Windows Nepali layout by key position`() {
        keyboard.setNepaliStyle(LanguageMode.NEPALI)
        assertEquals(LanguageMode.NEPALI, keyboard.state.value.currentLanguage)
        // On a QWERTY keyboard: K (प) S (क) shift-; (स्) T (त) . (।)
        press(KeyEvent.KEYCODE_K, char = 'k')
        press(KeyEvent.KEYCODE_S, char = 's')
        press(KeyEvent.KEYCODE_SEMICOLON, KeyEvent.META_SHIFT_ON, ':')
        press(KeyEvent.KEYCODE_T, char = 't')
        press(KeyEvent.KEYCODE_PERIOD, char = '.')
        assertEquals("पकस्त।", field.shown)
        // C (अ) then F (ा) joins into आ; shift+1 is a Nepali digit
        press(KeyEvent.KEYCODE_C, char = 'c')
        press(KeyEvent.KEYCODE_F, char = 'f')
        press(KeyEvent.KEYCODE_1, KeyEvent.META_SHIFT_ON, '!')
        assertEquals("पकस्त।आ१", field.shown)
    }

    @Test
    fun `right alt is AltGr in traditional mode`() {
        keyboard.setNepaliStyle(LanguageMode.NEPALI)
        val altGr = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
        assertTrue(press(KeyEvent.KEYCODE_P, altGr))
        assertEquals("ऊ", field.shown)
        // Nothing on AltGr+W: the app gets it
        assertFalse(press(KeyEvent.KEYCODE_W, altGr))
    }
}
