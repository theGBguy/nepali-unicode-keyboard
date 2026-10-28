package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.model.classify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyboardViewModelSmartTest {

    private lateinit var vm: KeyboardController
    private lateinit var ic: InputConnection
    private lateinit var prefs: InMemoryKeyboardPrefs

    /** What the field holds before the cursor; the mock returns its tail. */
    private var fieldText = ""

    private val textField = FieldInfo.classify(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE)
    private val passwordField = FieldInfo.classify(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.IME_ACTION_GO
    )
    private val globe = KeyData("🌐", keyType = KeyType.FUNCTION)

    @Before
    fun setUp() {
        prefs = InMemoryKeyboardPrefs()
        vm = KeyboardController()
        vm.prefs = prefs
        ic = mockk(relaxed = true)
        every { ic.getTextBeforeCursor(any(), 0) } answers { fieldText.takeLast(firstArg<Int>()) }
        vm.editor = { InputConnectionEditor(ic) }
    }

    private fun labels() = vm.state.value.smartActions.map { it.label }

    /** The app reports the cursor at the end of [fieldText], with no composing region. */
    private fun cursorMoved() = vm.onSelectionChanged(fieldText.length, fieldText.length, -1, -1)

    @Test
    fun `a typed amount is offered grouped and in words, and tapping replaces it`() {
        vm.onInputStarted(textField)
        fieldText = "Total 1500000"
        cursorMoved()
        assertEquals(listOf("15,00,000", "Fifteen Lakh"), labels())

        vm.onSmartActionClick(vm.state.value.smartActions[1])
        verifyOrder {
            ic.beginBatchEdit()
            ic.deleteSurroundingText(7, 0)
            ic.commitText("Fifteen Lakh", 1)
            ic.endBatchEdit()
        }
        verify(exactly = 0) { ic.setComposingText(any(), any()) }
        assertTrue(vm.state.value.smartActions.isEmpty())
    }

    @Test
    fun `while a Romanized number is composing, the action replaces it and the marker before it`() {
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        assertEquals(LanguageMode.TRANSLITERATION, vm.state.value.currentLanguage)
        "1500000".forEach { vm.onKeyClick(KeyData(it.toString())) }
        fieldText = "रु 1500000" // the composing Latin digits are in the field
        vm.onSelectionChanged(fieldText.length, fieldText.length, 3, fieldText.length)
        assertEquals(listOf("१५,००,०००", "पन्ध्र लाख रुपैयाँ मात्र"), labels())
        assertEquals(3, vm.state.value.smartActions[1].replace)

        vm.onSmartActionClick(vm.state.value.smartActions[1])
        verifyOrder {
            ic.beginBatchEdit()
            ic.setComposingText("", 1)
            ic.finishComposingText()
            ic.deleteSurroundingText(3, 0)
            ic.commitText("पन्ध्र लाख रुपैयाँ मात्र", 1)
            ic.endBatchEdit()
        }
        assertTrue(vm.state.value.transliterationBuffer.isEmpty())
        assertTrue(vm.state.value.suggestions.isEmpty())
    }

    @Test
    fun `a stale cursor update while composing offers nothing`() {
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        "150000".forEach { vm.onKeyClick(KeyData(it.toString())) }
        fieldText = "15000" // the app hasn't caught up with the last key yet
        vm.onSelectionChanged(5, 5, 0, 5)
        assertTrue(labels().isEmpty())
    }

    @Test
    fun `typing drops the suggestions, shift doesn't`() {
        vm.onInputStarted(textField)
        fieldText = "1500000"
        cursorMoved()
        vm.onKeyClick(KeyData("⇧", keyType = KeyType.MODIFIER))
        assertEquals(2, labels().size)
        vm.onKeyClick(KeyData("a"))
        assertTrue(labels().isEmpty())
    }

    @Test
    fun `switched off in settings, the text isn't even read`() {
        prefs.smartSuggestions = false
        vm.onInputStarted(textField)
        fieldText = "1500000"
        cursorMoved()
        assertTrue(labels().isEmpty())
        verify(exactly = 0) { ic.getTextBeforeCursor(any(), any()) }
    }

    @Test
    fun `password fields are never read`() {
        vm.onInputStarted(passwordField)
        fieldText = "1500000"
        cursorMoved()
        assertTrue(labels().isEmpty())
        verify(exactly = 0) { ic.getTextBeforeCursor(any(), any()) }
    }

    @Test
    fun `a selection offers nothing`() {
        vm.onInputStarted(textField)
        fieldText = "1500000"
        vm.onSelectionChanged(0, 7, -1, -1)
        assertTrue(labels().isEmpty())
    }

    @Test
    fun `a new field starts with no suggestions`() {
        vm.onInputStarted(textField)
        fieldText = "1500000"
        cursorMoved()
        vm.onInputStarted(textField)
        assertTrue(labels().isEmpty())
    }

    @Test
    fun `a point after a digit in Romanized mode stays a decimal point`() {
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        "0.5".forEach { vm.onKeyClick(KeyData(it.toString())) }
        verifyOrder {
            ic.commitText("०", 1)
            ic.commitText(".", 1)
            ic.setComposingText("5", 1)
        }
        verify(exactly = 0) { ic.commitText("।", any()) }
    }

    @Test
    fun `a point after a digit becomes purna biram when no digit follows`() {
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        "25.".forEach { vm.onKeyClick(KeyData(it.toString())) }
        fieldText = "२५."
        vm.onKeyClick(KeyData(" ", keyType = KeyType.ACTION))
        verifyOrder {
            ic.commitText(".", 1)
            ic.deleteSurroundingText(1, 0)
            ic.commitText("।", 1)
            ic.commitText(" ", 1)
        }
    }

    @Test
    fun `a point after a word is still purna biram`() {
        vm.onInputStarted(textField)
        vm.onKeyClick(globe)
        "ho.".forEach { vm.onKeyClick(KeyData(it.toString())) }
        verify { ic.commitText("।", 1) }
        verify(exactly = 0) { ic.commitText(".", any()) }
    }
}
