package io.github.thegbguy.nepalikeyboard.ui.keyboard.hardware

import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.ui.keyboard.FakeEditor
import io.github.thegbguy.nepalikeyboard.ui.keyboard.InMemoryKeyboardPrefs
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardController
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The platform-neutral key routing that Android and the Mac share. The Android key-code mapping is
 * tested on top of it in the app (`AndroidHardwareKeysTest`).
 */
class HardwareKeyRouterTest {

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

    private fun router(ownsLanguageKey: Boolean = true) = HardwareKeyRouter({ keyboard }, ownsLanguageKey)

    private fun HardwareKeyRouter.type(text: String) = text.forEach {
        assertTrue(onKeyDown(HardKeyEvent(HardKey.CHARACTER, text = it.toString(), shift = it.isUpperCase())), "$it")
    }

    private val space = HardKeyEvent(HardKey.SPACE, text = " ")

    @Test
    fun `Romanized letters compose and space commits`() {
        val router = router()
        router.type("namaste")
        assertEquals("namaste", field.composing)
        assertTrue(router.onKeyDown(space))
        assertEquals("नमस्ते ", field.shown)
    }

    @Test
    fun `traditional mode types by ISO position and ignores the typed text`() {
        keyboard.setNepaliStyle(LanguageMode.NEPALI)
        val router = router()
        // K S shift-; T on a QWERTY keyboard, whatever the Mac's own layout would type there
        listOf("C08" to false, "C02" to false, "C10" to true, "D05" to false).forEach { (iso, shift) ->
            assertTrue(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, iso = iso, text = "x", shift = shift)))
        }
        assertEquals("पकस्त", field.shown)
        // A key with no ISO position types nothing and goes to the app
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, text = "x")))
    }

    @Test
    fun `AltGr types the third level in traditional mode only when no other modifier is held`() {
        keyboard.setNepaliStyle(LanguageMode.NEPALI)
        val router = router()
        assertTrue(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, iso = "D10", altGr = true)))
        assertEquals("ऊ", field.shown)
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, iso = "D10", altGr = true, meta = true)))
        assertEquals("ऊ", field.shown)
    }

    @Test
    fun `ctrl and space cycles the language once per press where the router owns it`() {
        val router = router()
        val ctrlSpace = space.copy(ctrl = true)
        assertTrue(router.onKeyDown(ctrlSpace))
        assertTrue(router.onKeyDown(ctrlSpace.copy(repeat = true)))
        assertEquals(LanguageMode.ENGLISH, keyboard.state.value.currentLanguage)
        assertTrue(router.onKeyDown(HardKeyEvent(HardKey.LANGUAGE_SWITCH)))
        assertEquals(LanguageMode.TRANSLITERATION, keyboard.state.value.currentLanguage)
    }

    @Test
    fun `on the Mac ctrl and space is left to the system and commits the word`() {
        val router = router(ownsLanguageKey = false)
        router.type("ghar")
        assertFalse(router.onKeyDown(space.copy(ctrl = true)))
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.LANGUAGE_SWITCH)))
        assertEquals(LanguageMode.TRANSLITERATION, keyboard.state.value.currentLanguage)
        assertEquals("घर", field.text)
    }

    @Test
    fun `modifiers alone are ignored and shortcuts go to the app`() {
        val router = router()
        router.type("gha")
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.MODIFIER, shift = true)))
        assertEquals("gha", field.composing)
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, iso = "B04", text = "v", meta = true)))
        assertEquals("", field.composing)
    }

    @Test
    fun `control characters and unnamed keys without text go to the app`() {
        val router = router()
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, text = "\u001B")))
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER)))
        assertFalse(router.onKeyDown(HardKeyEvent(HardKey.ENTER, text = "\r")))
        assertEquals("", field.shown)
    }

    @Test
    fun `shift and the full-stop key give the English full stop in Romanized`() {
        val router = router()
        router.type("ghar")
        assertTrue(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, iso = "B09", text = ">", shift = true)))
        assertEquals("घर.", field.shown)
        assertTrue(router.onKeyDown(HardKeyEvent(HardKey.CHARACTER, iso = "B09", text = ".")))
        assertEquals("घर.।", field.shown)
    }

    @Test
    fun `tab picks the next suggestion and space commits it`() {
        val router = router()
        router.type("k")
        assertTrue(router.onKeyDown(HardKeyEvent(HardKey.TAB)))
        assertEquals(1, keyboard.state.value.highlightedSuggestion)
        router.onKeyDown(space)
        assertEquals("क ", field.shown)
    }
}
