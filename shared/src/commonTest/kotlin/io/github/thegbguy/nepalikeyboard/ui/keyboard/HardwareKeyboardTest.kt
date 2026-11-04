package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.FieldKind
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.TraditionalHardwareLayout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Typing on a physical keyboard: the controller's hardware-key API and the traditional table. */
class HardwareKeyboardTest {

    private lateinit var keyboard: KeyboardController
    private lateinit var field: FakeEditor

    @BeforeTest
    fun setUp() {
        field = FakeEditor()
        keyboard = KeyboardController().also {
            it.editor = { field }
            it.prefs = InMemoryKeyboardPrefs(typeNepali = true)
        }
        keyboard.onInputStarted(FieldInfo())
    }

    @AfterTest
    fun tearDown() = keyboard.close()

    private fun type(text: String) = text.forEach { keyboard.onHardwareText(it.toString()) }

    @Test
    fun `Romanized words typed on a physical keyboard commit on space`() {
        assertEquals(LanguageMode.TRANSLITERATION, keyboard.state.value.currentLanguage)
        type("namaste")
        assertEquals("namaste", field.composing)
        keyboard.onHardwareSpace()
        assertEquals("नमस्ते ", field.shown)
    }

    @Test
    fun `backspace edits the Romanized word then leaves the field to the app`() {
        type("nam")
        assertTrue(keyboard.onHardwareBackspace())
        assertEquals("na", field.composing)
        keyboard.onHardwareSpace()
        assertFalse(keyboard.onHardwareBackspace())
    }

    @Test
    fun `a full stop is a danda and after a digit a decimal point`() {
        type("ho.")
        assertEquals("हो।", field.shown)
        field.text = ""
        type("2.5")
        keyboard.onHardwareSpace()
        assertEquals("२.५ ", field.shown)
    }

    @Test
    fun `keys the app handles finish the Romanized word first`() {
        type("garyo")
        keyboard.onHardwarePassThrough()
        assertEquals("गर्\u200Dयो", field.shown)
        assertTrue(keyboard.state.value.transliterationBuffer.isEmpty())
    }

    @Test
    fun `a decimal point left before a key the app handles becomes a danda`() {
        type("5.")
        keyboard.onHardwarePassThrough()
        assertEquals("५।", field.shown)
    }

    @Test
    fun `traditional keys type the standard layout and join vowels after अ`() {
        keyboard.prefs.nepaliStyle = LanguageMode.NEPALI
        keyboard.onInputStarted(FieldInfo())
        assertEquals(LanguageMode.NEPALI, keyboard.state.value.currentLanguage)
        // C (अ) then F (ा): आ, as typed on Windows
        keyboard.onHardwareText(TraditionalHardwareLayout.output("B03", shift = false)!!)
        keyboard.onHardwareText(TraditionalHardwareLayout.output("C04", shift = false)!!)
        assertEquals("आ", field.shown)
    }

    @Test
    fun `a field focused without the keyboard on screen still opens in English for passwords`() {
        keyboard.onHiddenInputStarted(FieldInfo(kind = FieldKind.PASSWORD), restarting = false)
        assertEquals(LanguageMode.ENGLISH, keyboard.state.value.currentLanguage)
    }

    @Test
    fun `a physical keyboard shows only the strip until the keys are opened`() {
        keyboard.onHardwareKeyboardChanged(true)
        assertTrue(keyboard.state.value.hardwareKeyboard)
        assertFalse(keyboard.state.value.keysWithHardwareKeyboard)
        keyboard.toggleKeysWithHardwareKeyboard()
        assertTrue(keyboard.state.value.keysWithHardwareKeyboard)
        // Reconnecting starts with the strip again
        keyboard.onHardwareKeyboardChanged(false)
        keyboard.onHardwareKeyboardChanged(true)
        assertFalse(keyboard.state.value.keysWithHardwareKeyboard)
    }

    @Test
    fun `the traditional table follows the Windows Nepali driver`() {
        val t = TraditionalHardwareLayout
        assertEquals("त्र", t.output("D01", shift = false))
        assertEquals("ो", t.output("D01", shift = true))
        assertEquals("।", t.output("B09", shift = false))
        assertEquals("१", t.output("E01", shift = true))
        assertEquals("ञ्", t.output("E00", shift = true))
        assertEquals("ऊ", t.output("D10", shift = false, altGr = true))
        assertNull(t.output("D02", shift = false, altGr = true))
        assertNull(t.output("A03", shift = false))
    }

    @Test
    fun `every consonant and vowel sign and independent vowel can be typed traditionally`() {
        val all = TraditionalHardwareLayout.allOutputs.toSet()
        val needed = "कखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसह".map { it.toString() } +
            listOf("ा", "ि", "ी", "ु", "ू", "ृ", "े", "ै", "ो", "्", "ं", "ँ", "ः", "अ", "आ", "इ", "उ", "ऊ", "ए", "ऋ", "औ", "।")
        assertEquals(emptyList(), needed.filter { it !in all })
    }
}
