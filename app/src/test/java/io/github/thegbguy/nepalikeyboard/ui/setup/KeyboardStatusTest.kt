package io.github.thegbguy.nepalikeyboard.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardStatusTest {
    private val pkg = "io.github.thegbguy.nepalikeyboard"
    private val ours = "$pkg/.service.NepaliIMEService"
    private val gboard = "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME"

    @Test
    fun `not enabled starts at step one`() {
        val s = KeyboardStatus.from(pkg, listOf(gboard), gboard)
        assertFalse(s.enabled)
        assertEquals(0, s.step)
    }

    @Test
    fun `enabled but another keyboard selected is step two`() {
        val s = KeyboardStatus.from(pkg, listOf(gboard, ours), gboard)
        assertTrue(s.enabled)
        assertFalse(s.selected)
        assertEquals(1, s.step)
    }

    @Test
    fun `enabled and selected is step three`() {
        assertEquals(2, KeyboardStatus.from(pkg, listOf(gboard, ours), ours).step)
    }

    @Test
    fun `a stale default id doesn't count when the keyboard is disabled`() {
        assertEquals(0, KeyboardStatus.from(pkg, listOf(gboard), ours).step)
    }

    @Test
    fun `debug build package is matched exactly`() {
        val debug = "$pkg.debug/$pkg.service.NepaliIMEService"
        assertEquals(0, KeyboardStatus.from(pkg, listOf(debug), debug).step)
    }
}
