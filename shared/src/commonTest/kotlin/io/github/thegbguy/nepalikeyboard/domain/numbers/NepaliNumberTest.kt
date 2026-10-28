package io.github.thegbguy.nepalikeyboard.domain.numbers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NepaliNumberTest {

    @Test
    fun `nepali words`() {
        val cases = listOf(
            0L to "शून्य",
            7L to "सात",
            15L to "पन्ध्र",
            99L to "उनान्सय",
            100L to "एक सय",
            101L to "एक सय एक",
            999L to "नौ सय उनान्सय",
            1_000L to "एक हजार",
            1_500L to "एक हजार पाँच सय",
            21_051L to "एक्काइस हजार एकाउन्न",
            1_00_000L to "एक लाख",
            15_00_000L to "पन्ध्र लाख",
            1_00_00_000L to "एक करोड",
            1_20_30_501L to "एक करोड बीस लाख तीस हजार पाँच सय एक",
            1_00_00_00_000L to "एक अर्ब",
            1_00_00_00_00_000L to "एक खर्ब",
            NepaliNumber.MAX_WORDS to "उनान्सय खर्ब उनान्सय अर्ब उनान्सय करोड उनान्सय लाख उनान्सय हजार नौ सय उनान्सय"
        )
        cases.forEach { (n, want) -> assertEquals(want, NepaliNumber.nepaliWords(n), "$n") }
    }

    @Test
    fun `every number below 100 has its own word`() {
        val words = (0L..99L).map { NepaliNumber.nepaliWords(it)!! }
        assertEquals(100, words.toSet().size)
        words.forEach { assertEquals(1, it.split(" ").size, it) }
        // Spot checks against the irregular forms
        assertEquals("उनन्तीस", words[29])
        assertEquals("उनन्चास", words[49])
        assertEquals("उनन्साठी", words[59])
        assertEquals("उनासी", words[79])
        assertEquals("उनान्नब्बे", words[89])
    }

    @Test
    fun `english words use lakh and crore`() {
        assertEquals("Zero", NepaliNumber.englishWords(0))
        assertEquals("Twenty-One", NepaliNumber.englishWords(21))
        assertEquals("Fifteen Lakh", NepaliNumber.englishWords(15_00_000))
        assertEquals(
            "One Crore Twenty Lakh Thirty Thousand Five Hundred One",
            NepaliNumber.englishWords(1_20_30_501)
        )
        assertEquals("Two Arab Fifty Crore", NepaliNumber.englishWords(2_50_00_00_000))
    }

    @Test
    fun `no words beyond 99 kharab`() {
        assertNull(NepaliNumber.nepaliWords(NepaliNumber.MAX_WORDS + 1))
        assertNull(NepaliNumber.englishWords(-1))
    }

    @Test
    fun `lakh grouping`() {
        assertEquals("999", NepaliNumber.groupIndian("999"))
        assertEquals("1,000", NepaliNumber.groupIndian("1000"))
        assertEquals("15,00,000", NepaliNumber.groupIndian("1500000"))
        assertEquals("1,20,30,501", NepaliNumber.groupIndian("12030501"))
    }

    @Test
    fun `digit scripts`() {
        assertEquals("१५,००,०००", NepaliNumber.toNepaliDigits("15,00,000"))
        assertEquals("2083", NepaliNumber.toLatinDigits("२०८३"))
        assertEquals("शून्य पाँच", NepaliNumber.digitWords("05", nepali = true))
    }
}
