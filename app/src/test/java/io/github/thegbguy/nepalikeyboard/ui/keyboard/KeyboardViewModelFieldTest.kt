package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyboardViewModelFieldTest {

    private lateinit var vm: KeyboardViewModel
    private lateinit var ic: InputConnection

    private val textField = FieldInfo.classify(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE)
    private val chatField = FieldInfo.classify(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION
    )
    private val sendField = FieldInfo.classify(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_SEND)
    private val passwordField = FieldInfo.classify(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.IME_ACTION_GO
    )

    private val enter = KeyData("↵", keyType = KeyType.ACTION)
    private val globe = KeyData("🌐", keyType = KeyType.FUNCTION)

    @Before
    fun setUp() {
        vm = KeyboardViewModel()
        ic = mockk(relaxed = true)
        vm.inputConnectionProvider = { ic }
    }

    private fun language() = vm.state.value.currentLanguage

    /** English → Nepali, which is Romanized by default. */
    private fun switchToRomanized() {
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.TRANSLITERATION, language())
    }

    @Test
    fun `password field opens in English and its language key gives traditional Nepali`() {
        vm.onInputStarted(textField)
        switchToRomanized()

        vm.onInputStarted(passwordField)
        assertEquals(LanguageMode.ENGLISH, language())
        assertFalse(vm.state.value.field.allowsVoice)

        vm.onKeyClick(globe)
        assertEquals(LanguageMode.NEPALI, language()) // Romanized isn't allowed here
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.ENGLISH, language())
    }

    @Test
    fun `language key toggles only between English and the chosen Nepali style`() {
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.TRANSLITERATION, language())
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.ENGLISH, language())

        vm.setNepaliStyle(LanguageMode.NEPALI)
        assertEquals(LanguageMode.NEPALI, language())
        assertEquals(LanguageMode.NEPALI, vm.prefs.nepaliStyle)
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.ENGLISH, language())
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.NEPALI, language())
    }

    @Test
    fun `stored style is used when the keyboard starts`() {
        vm.prefs = InMemoryKeyboardPrefs(LanguageMode.NEPALI)
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.NEPALI, language())
    }

    @Test
    fun `long-press menu opens, switch keyboard calls the picker and closes it`() {
        var pickerShown = false
        vm.onSwitchKeyboard = { pickerShown = true }
        vm.prefs = InMemoryKeyboardPrefs(LanguageMode.NEPALI)
        vm.onLanguageLongPress()
        assertTrue(vm.state.value.showLanguageMenu)
        assertEquals(LanguageMode.NEPALI, vm.state.value.nepaliStyle) // checked even while in English
        vm.switchKeyboard()
        assertTrue(pickerShown)
        assertFalse(vm.state.value.showLanguageMenu)
    }

    @Test
    fun `switching language commits the Romanized word first`() {
        vm.onInputStarted(textField)
        switchToRomanized()
        "ram".forEach { vm.onKeyClick(KeyData(it.toString())) }
        vm.onKeyClick(globe)
        verify { ic.commitText(match<CharSequence> { it.isNotEmpty() && it.first() in 'ऀ'..'ॿ' }, 1) }
        assertEquals(LanguageMode.ENGLISH, language())
    }

    @Test
    fun `leaving a password field restores the user's Nepali mode`() {
        vm.onInputStarted(textField)
        switchToRomanized()
        vm.onInputStarted(passwordField)
        vm.onKeyClick(globe) // a switch inside the password field is not remembered

        vm.onInputStarted(textField)
        assertEquals(LanguageMode.TRANSLITERATION, language())
    }

    @Test
    fun `typing in a password field never composes text`() {
        vm.onInputStarted(passwordField)
        vm.onKeyClick(KeyData("a"))
        verify { ic.commitText("a", 1) }
        verify(exactly = 0) { ic.setComposingText(any(), any()) }
    }

    @Test
    fun `enter inserts a new line in a multi-line chat field`() {
        vm.onInputStarted(chatField)
        vm.onKeyClick(enter)
        verify { ic.commitText("\n", 1) }
        verify(exactly = 0) { ic.performEditorAction(any()) }
    }

    @Test
    fun `enter runs the field action and commits the Romanized word first`() {
        vm.onInputStarted(sendField)
        switchToRomanized()
        "ram".forEach { vm.onKeyClick(KeyData(it.toString())) }

        vm.onKeyClick(enter)
        verifyOrder {
            ic.setComposingText("ram", 1)
            ic.commitText(match<CharSequence> { it.isNotEmpty() && it.first() in 'ऀ'..'ॿ' }, 1)
            ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
        }
        assertTrue(vm.state.value.transliterationBuffer.isEmpty())
    }

    @Test
    fun `number field opens on the numbers layout`() {
        vm.onInputStarted(FieldInfo.classify(InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_DONE))
        assertTrue(vm.state.value.showingNumbers)
        vm.onInputStarted(textField)
        assertFalse(vm.state.value.showingNumbers)
    }

    @Test
    fun `each key goes to the connection of the field focused now`() {
        val second: InputConnection = mockk(relaxed = true)
        vm.onInputStarted(textField)
        vm.onKeyClick(KeyData("a"))
        vm.inputConnectionProvider = { second }
        vm.onKeyClick(KeyData("b"))
        verify { ic.commitText("a", 1) }
        verify { second.commitText("b", 1) }
        verify(exactly = 0) { ic.commitText("b", any()) }
    }

    @Test
    fun `app clearing the box drops the Romanized word so new letters start fresh`() {
        vm.onInputStarted(textField)
        switchToRomanized()
        "ram".forEach { vm.onKeyClick(KeyData(it.toString())) }
        vm.onSelectionChanged(selStart = 3, selEnd = 3, composingStart = 0, composingEnd = 3)
        assertEquals("ram", vm.state.value.transliterationBuffer.romanizedText) // our own update: kept

        vm.onSelectionChanged(selStart = 0, selEnd = 0, composingStart = -1, composingEnd = -1)
        assertTrue(vm.state.value.transliterationBuffer.isEmpty())
        vm.onKeyClick(KeyData("n"))
        verify { ic.setComposingText("n", 1) }
    }

    @Test
    fun `moving the cursor away finishes the word where it is`() {
        vm.onInputStarted(textField)
        switchToRomanized()
        "ram".forEach { vm.onKeyClick(KeyData(it.toString())) }
        vm.onSelectionChanged(selStart = 0, selEnd = 0, composingStart = 5, composingEnd = 8)
        verify { ic.finishComposingText() }
        assertTrue(vm.state.value.transliterationBuffer.isEmpty())
    }

    @Test
    fun `long-press types the alternate and keep-Latin commits the typed letters`() {
        vm.onInputStarted(textField)
        switchToRomanized()
        "ram".forEach { vm.onKeyClick(KeyData(it.toString())) }
        vm.onKeepLatin()
        verify { ic.commitText("ram ", 1) }
        vm.onKeyLongPress("@")
        verify { ic.commitText("@", 1) }
    }

    @Test
    fun `the last language is remembered across keyboard restarts`() {
        val stored = InMemoryKeyboardPrefs(typeNepali = true)
        vm.prefs = stored
        vm.onInputStarted(textField)
        assertEquals(LanguageMode.TRANSLITERATION, language())

        vm.onKeyClick(globe) // to English
        assertEquals(false, stored.typeNepali)

        val next = KeyboardViewModel().apply { prefs = stored }
        next.onInputStarted(textField)
        assertEquals(LanguageMode.ENGLISH, next.state.value.currentLanguage)
    }

    @Test
    fun `a style picked in the app updates the open keyboard`() {
        val stored = InMemoryKeyboardPrefs(typeNepali = true)
        vm.prefs = stored
        vm.onInputStarted(textField)
        stored.nepaliStyle = LanguageMode.NEPALI
        vm.onPrefsChanged()
        assertEquals(LanguageMode.NEPALI, language())

        vm.onInputStarted(FieldInfo.classify(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, EditorInfo.IME_ACTION_DONE))
        vm.onPrefsChanged()
        assertEquals(LanguageMode.ENGLISH, language()) // email fields stay English
    }

    @Test
    fun `traditional layout joins अ and a vowel sign into one letter`() {
        vm.prefs = InMemoryKeyboardPrefs(LanguageMode.NEPALI)
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        io.mockk.every { ic.getTextBeforeCursor(1, 0) } returns "अ"
        vm.onKeyClick(KeyData("ा", "ँ"))
        verify { ic.deleteSurroundingText(1, 0) }
        verify { ic.commitText("आ", 1) }

        io.mockk.every { ic.getTextBeforeCursor(1, 0) } returns "क"
        vm.onKeyClick(KeyData("ो", "ौ"))
        verify { ic.commitText("ो", 1) }
    }

    @Test
    fun `leaving a field mid-word commits it as Devanagari`() {
        vm.onInputStarted(textField)
        switchToRomanized()
        "ram".forEach { vm.onKeyClick(KeyData(it.toString())) }
        vm.onInputFinished()
        verify { ic.commitText(match<CharSequence> { it.isNotEmpty() && it.first() in 'ऀ'..'ॿ' }, 1) }
        assertTrue(vm.state.value.transliterationBuffer.isEmpty())
    }
}
