package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.domain.model.CapsMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoCapsTest {

    private fun sentence(before: String?) = AutoCaps.wants(CapsMode.SENTENCES, before)

    @Test
    fun `sentences start at the beginning of the field and of a line`() {
        assertTrue(sentence(""))
        assertTrue(sentence("  "))
        assertTrue(sentence("Dear Ram\n"))
        assertTrue(sentence("Dear Ram\n  "))
    }

    @Test
    fun `sentences start after an end mark and a space`() {
        assertTrue(sentence("Done. "))
        assertTrue(sentence("Really? "))
        assertTrue(sentence("Wow!  "))
        assertTrue(sentence("He said \"go.\" "))
        assertTrue(sentence("(See above.) "))
        assertTrue(sentence("गयो। "))
    }

    @Test
    fun `no capital inside a sentence or right after the end mark`() {
        assertFalse(sentence("Done."))
        assertFalse(sentence("hello "))
        assertFalse(sentence("hello"))
        assertFalse(sentence("3.5 "))
        assertFalse(sentence(null))
    }

    @Test
    fun `words start after any space and the other modes don't read the text`() {
        assertTrue(AutoCaps.wants(CapsMode.WORDS, ""))
        assertTrue(AutoCaps.wants(CapsMode.WORDS, "Ram "))
        assertFalse(AutoCaps.wants(CapsMode.WORDS, "Ram"))
        assertTrue(AutoCaps.wants(CapsMode.CHARACTERS, null))
        assertFalse(AutoCaps.wants(CapsMode.NONE, ""))
    }
}
