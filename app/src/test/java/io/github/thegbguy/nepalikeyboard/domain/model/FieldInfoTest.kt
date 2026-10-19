package io.github.thegbguy.nepalikeyboard.domain.model

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldInfoTest {

    private val text = InputType.TYPE_CLASS_TEXT

    @Test
    fun `every password variation is private and forces English with no voice`() {
        listOf(
            text or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            text or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            text or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ).forEach { type ->
            val f = FieldInfo.classify(type, EditorInfo.IME_ACTION_DONE)
            assertEquals(FieldKind.PASSWORD, f.kind)
            assertTrue(f.isPrivate)
            assertTrue(f.noLearning)
            assertFalse(f.allowsTransliteration)
            assertFalse(f.allowsVoice)
            assertTrue(f.prefersEnglish)
        }
    }

    @Test
    fun `plain text allows everything`() {
        val f = FieldInfo.classify(text, EditorInfo.IME_ACTION_UNSPECIFIED)
        assertEquals(FieldKind.TEXT, f.kind)
        assertFalse(f.isPrivate)
        assertTrue(f.allowsTransliteration)
        assertTrue(f.allowsVoice)
        assertFalse(f.prefersEnglish)
    }

    @Test
    fun `incognito keeps Nepali typing but turns off learning and voice`() {
        val f = FieldInfo.classify(text, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertFalse(f.isPrivate)
        assertTrue(f.noLearning)
        assertTrue(f.allowsTransliteration)
        assertFalse(f.allowsVoice)
    }

    @Test
    fun `email and url open in English, number and phone open on numbers`() {
        assertTrue(FieldInfo.classify(text or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 0).prefersEnglish)
        assertTrue(FieldInfo.classify(text or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, 0).prefersEnglish)
        assertTrue(FieldInfo.classify(text or InputType.TYPE_TEXT_VARIATION_URI, 0).prefersEnglish)
        assertTrue(FieldInfo.classify(InputType.TYPE_CLASS_NUMBER, 0).prefersNumbers)
        assertTrue(FieldInfo.classify(InputType.TYPE_CLASS_PHONE, 0).prefersNumbers)
        assertTrue(FieldInfo.classify(InputType.TYPE_CLASS_DATETIME, 0).prefersNumbers)
    }

    @Test
    fun `enter follows the field action`() {
        fun action(opts: Int) = FieldInfo.classify(text, opts).enterAction
        assertEquals(EnterAction.SEND, action(EditorInfo.IME_ACTION_SEND))
        assertEquals(EnterAction.SEARCH, action(EditorInfo.IME_ACTION_SEARCH))
        assertEquals(EnterAction.GO, action(EditorInfo.IME_ACTION_GO))
        assertEquals(EnterAction.NEXT, action(EditorInfo.IME_ACTION_NEXT))
        assertEquals(EnterAction.PREVIOUS, action(EditorInfo.IME_ACTION_PREVIOUS))
        assertEquals(EnterAction.DONE, action(EditorInfo.IME_ACTION_DONE))
        assertEquals(EnterAction.NEWLINE, action(EditorInfo.IME_ACTION_UNSPECIFIED))
        assertEquals(EnterAction.NEWLINE, action(EditorInfo.IME_ACTION_NONE))
        assertEquals(EditorInfo.IME_ACTION_SEND, FieldInfo.classify(text, EditorInfo.IME_ACTION_SEND).imeActionId)
    }

    @Test
    fun `multi-line chat field with no-enter-action flag inserts a new line`() {
        val f = FieldInfo.classify(
            text or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        )
        assertEquals(EnterAction.NEWLINE, f.enterAction)
    }

    @Test
    fun `custom action label wins and carries its id`() {
        val f = FieldInfo.classify(text, EditorInfo.IME_ACTION_DONE, actionLabel = "Post", actionId = 42)
        assertEquals(EnterAction.CUSTOM, f.enterAction)
        assertEquals("Post", f.actionLabel)
        assertEquals(42, f.imeActionId)
        assertNull(FieldInfo.classify(text, EditorInfo.IME_ACTION_DONE, actionLabel = " ").actionLabel)
    }

    @Test
    fun `TYPE_NULL fields get Enter as a key event`() {
        assertTrue(FieldInfo.classify(InputType.TYPE_NULL, 0).sendsKeyEvents)
        assertFalse(FieldInfo.classify(text, 0).sendsKeyEvents)
    }
}
