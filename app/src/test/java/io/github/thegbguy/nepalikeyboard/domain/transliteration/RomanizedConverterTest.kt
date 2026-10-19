package io.github.thegbguy.nepalikeyboard.domain.transliteration

import org.junit.Assert.assertEquals
import org.junit.Test

/** The letter-by-letter converter, used for names and words that aren't in the word list. */
class RomanizedConverterTest {

    private fun conv(roman: String) = RomanizedConverter.convert(roman)

    @Test
    fun `consonant plus vowel becomes consonant plus vowel sign`() {
        assertEquals("नमस्ते", conv("namaste"))
        assertEquals("मेरो", conv("mero"))
        assertEquals("तिम्रो", conv("timro"))
        assertEquals("नै", conv("nai"))
        assertEquals("दुइ", conv("dui"))
        assertEquals("आउनु", conv("aaunu"))
    }

    @Test
    fun `word-final consonant keeps its inherent a`() {
        assertEquals("घर", conv("ghar"))
        assertEquals("नेपाल", conv("nepaal"))
        assertEquals("किताब", conv("kitaab"))
        assertEquals("रम", conv("ram"))
    }

    @Test
    fun `long vowels and initial vowels`() {
        assertEquals("आमा", conv("aamaa"))
        assertEquals("तिमी", conv("timii"))
        assertEquals("तिमी", conv("timee"))
        assertEquals("ऊ", conv("uu"))
        assertEquals("एक", conv("ek"))
    }

    @Test
    fun `aspirates, sibilants and conjuncts`() {
        assertEquals("छ", conv("chha"))
        assertEquals("धन्यवाद", conv("dhanyavaad"))
        assertEquals("श्रेष्ठ", conv("shreShTha"))
        assertEquals("क्षमा", conv("kshamaa"))
        assertEquals("ज्ञान", conv("gyaan"))
        assertEquals("गर्छु", conv("garchhu"))
    }

    @Test
    fun `capitals give retroflex letters`() {
        assertEquals("ठमेल", conv("Thamel"))
        assertEquals("डर", conv("Dar"))
        assertEquals("गणेश", conv("gaNesh"))
    }

    @Test
    fun `nasal and other marks`() {
        assertEquals("संसार", conv("saMsaar"))
        assertEquals("दुःख", conv("duHkha"))
        assertEquals("आँप", conv("aa~p"))
        assertEquals("राम।", conv("raam."))
        assertEquals("२०८३", conv("2083"))
    }
}
