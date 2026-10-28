package io.github.thegbguy.nepalikeyboard.domain.numbers

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AmountSourceTest {

    private val nepali = LanguageMode.NEPALI
    private val english = LanguageMode.ENGLISH

    /** What the strip would offer for [text], as "replacement/consumed" pairs. */
    private fun offers(text: String, mode: LanguageMode = nepali): List<String> =
        AmountSource.suggest(text, mode).map { "${it.text}/${it.consumed}" }

    @Test
    fun `large number gets grouping and words`() {
        assertEquals(listOf("१५,००,०००/7", "पन्ध्र लाख/7"), offers("1500000"))
        assertEquals(listOf("15,00,000/7", "Fifteen Lakh/7"), offers("1500000", english))
    }

    @Test
    fun `devanagari digits are read too`() {
        assertEquals(listOf("१५,००,०००/7", "पन्ध्र लाख/7"), offers("अब १५०००००"))
    }

    @Test
    fun `already grouped numbers only get words`() {
        assertEquals(listOf("पन्ध्र लाख/9"), offers("१५,००,०००"))
        assertEquals(listOf("15,00,000/9", "Fifteen Lakh/9"), offers("1,500,000", english))
    }

    @Test
    fun `one space after the number is kept`() {
        assertEquals(listOf("१५,००० /6", "पन्ध्र हजार /6"), offers("15000 "))
        assertEquals(emptyList<String>(), offers("15000  "))
    }

    @Test
    fun `short numbers years PINs and phone numbers get nothing`() {
        listOf("2083", "1234", "बजेट 1500", "9841234567", "01234567", "12345678901234").forEach {
            assertEquals(emptyList<String>(), offers(it), it)
        }
    }

    @Test
    fun `numbers glued to letters get nothing`() {
        assertEquals(emptyList<String>(), offers("ID123456"))
        assertEquals(emptyList<String>(), offers("कोड१२३४५")) // after a Devanagari letter
        assertEquals(emptyList<String>(), offers("हो१२३४५")) // after a vowel sign
    }

    @Test
    fun `dates and lists with commas are not amounts`() {
        assertEquals(emptyList<String>(), offers("21.10.2026"))
        assertEquals(emptyList<String>(), offers("1,2,3,4,5"))
        assertEquals(emptyList<String>(), offers("12,34"))
    }

    @Test
    fun `currency marker gives cheque wording and replaces the marker`() {
        assertEquals(listOf("पाँच सय रुपैयाँ मात्र/6"), offers("रु 500"))
        assertEquals(listOf("१,५००/4", "एक हजार पाँच सय रुपैयाँ मात्र/8"), offers("रु. 1500"))
        assertEquals(listOf("1,500/4", "One Thousand Five Hundred Rupees Only/8"), offers("Rs. 1500", english))
        assertEquals(listOf("Twenty Rupees Only/6"), offers("NPR 20", english))
        assertEquals(listOf("One Rupee Only/3"), offers("rs1", english))
    }

    @Test
    fun `paisa`() {
        assertEquals(listOf("पाँच सय रुपैयाँ पचास पैसा मात्र/8"), offers("रु 500.5"))
        assertEquals(listOf("Five Hundred Rupees and Five Paisa Only/9"), offers("Rs 500.05", english))
        assertEquals(listOf("पचास पैसा मात्र/7"), offers("रु 0.50"))
    }

    @Test
    fun `marker must be its own word`() {
        // "pars" ends in "rs", but it's not a marker, and 500 is too short on its own
        assertEquals(emptyList<String>(), offers("pars 500", english))
    }

    @Test
    fun `decimals without a marker are read digit by digit`() {
        assertEquals(listOf("१२,३४५.६७/8", "बाह्र हजार तीन सय पैँतालीस दशमलव छ सात/8"), offers("12345.67"))
    }
}
