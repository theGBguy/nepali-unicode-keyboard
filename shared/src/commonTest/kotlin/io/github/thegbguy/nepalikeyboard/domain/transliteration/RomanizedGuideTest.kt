package io.github.thegbguy.nepalikeyboard.domain.transliteration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RomanizedGuideTest {

    private val all = RomanizedGuide.sections.flatMap { it.entries } + RomanizedGuide.compact

    @Test
    fun `every spelling in the chart types what the chart shows`() {
        val wrong = all.flatMap { entry ->
            entry.typed.mapNotNull { typed ->
                RomanizedConverter.convert(typed).takeIf { it != entry.nepali }?.let { "$typed → $it, chart says ${entry.nepali}" }
            }
        }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun `the compact rows are all in the full chart`() {
        val full = RomanizedGuide.sections.flatMap { it.entries }.toSet()
        assertTrue(RomanizedGuide.compact.all { it in full })
    }
}
