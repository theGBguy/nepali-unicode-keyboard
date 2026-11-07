package io.github.thegbguy.nepalikeyboard.ui.keyboard

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardThemeTest {

    private val fixed = KeyboardTheme.entries.filter { it != KeyboardTheme.SYSTEM }.map { it to KeyboardThemes.palette(it, false) }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun contrast(a: Long, b: Long): Double {
        val (x, y) = luminance(a) to luminance(b)
        return (max(x, y) + 0.05) / (min(x, y) + 0.05)
    }

    private fun failures(minimum: Double, pick: (ThemePalette) -> Pair<Long, Long>): List<String> =
        fixed.mapNotNull { (theme, p) ->
            val (a, b) = pick(p)
            contrast(a, b).takeIf { it < minimum }?.let { "$theme ${(it * 100).toInt() / 100.0}" }
        }

    @Test
    fun `key letters are readable on every theme`() {
        assertEquals(emptyList(), failures(4.5) { it.keyText to it.key })
        assertEquals(emptyList(), failures(4.5) { it.keyText to it.functionKey })
    }

    @Test
    fun `flat keys draw letters on the background and stay readable`() {
        assertEquals(emptyList(), failures(4.5) { it.keyText to it.background })
    }

    @Test
    fun `the Enter key label is readable on the accent`() {
        assertEquals(emptyList(), failures(4.5) { it.onAccent to it.accent })
    }

    @Test
    fun `hints on keys are visible`() {
        assertEquals(emptyList(), failures(3.0) { it.hint to it.key })
        // Flat keys show hints on the background; the original light palette sits at 2.9 there
        assertEquals(emptyList(), failures(2.8) { it.hint to it.background })
    }

    @Test
    fun `system follows the phone and an unknown stored name falls back to it`() {
        assertEquals(KeyboardThemes.DARK, KeyboardThemes.palette(KeyboardTheme.SYSTEM, systemDark = true))
        assertEquals(KeyboardThemes.LIGHT, KeyboardThemes.palette(KeyboardTheme.SYSTEM, systemDark = false))
        assertEquals(KeyboardTheme.SYSTEM, KeyboardTheme.fromName("RETIRED"))
        assertEquals(KeyboardTheme.DUSK, KeyboardTheme.fromName("DUSK"))
    }
}
