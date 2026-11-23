package io.github.thegbguy.nepalikeyboard.domain.voice

import kotlin.test.Test
import kotlin.test.assertEquals

class SpokenPunctuationTest {

    @Test
    fun `spoken marks join the word before them`() {
        assertEquals("म आएँ, तिमी", SpokenPunctuation.apply("म आएँ अल्पविराम तिमी"))
        assertEquals("तिमी कहाँ छौ?", SpokenPunctuation.apply("तिमी कहाँ छौ प्रश्नचिन्ह"))
        assertEquals("तिमी कहाँ छौ?", SpokenPunctuation.apply("तिमी कहाँ छौ प्रश्नवाचक चिन्ह"))
        assertEquals("म आएँ। तिमी", SpokenPunctuation.apply("म आएँ पूर्णविराम तिमी"))
        assertEquals("राम्रो!", SpokenPunctuation.apply("राम्रो विस्मयादिबोधक चिन्ह"))
        assertEquals("ठिक छ, धन्यवाद", SpokenPunctuation.apply("ठिक छ कमा धन्यवाद"))
    }

    @Test
    fun `new line starts the next words on their own line`() {
        assertEquals("नमस्ते।\nम", SpokenPunctuation.apply("नमस्ते पूर्णविराम नयाँ लाइन म"))
    }

    @Test
    fun `parts of a phrase on their own stay words`() {
        assertEquals("एउटा प्रश्न छ", SpokenPunctuation.apply("एउटा प्रश्न छ"))
        assertEquals("नयाँ घर", SpokenPunctuation.apply("नयाँ घर"))
        assertEquals("पूर्ण रूपमा", SpokenPunctuation.apply("पूर्ण रूपमा"))
    }

    @Test
    fun `a dictation after earlier text gets a space first`() {
        assertEquals(" तिमी", SpokenPunctuation.withLeadingSpace("तिमी", "म"))
        assertEquals("तिमी", SpokenPunctuation.withLeadingSpace("तिमी", " "))
        assertEquals("तिमी", SpokenPunctuation.withLeadingSpace("तिमी", ""))
        assertEquals("तिमी", SpokenPunctuation.withLeadingSpace("तिमी", null))
        assertEquals("तिमी", SpokenPunctuation.withLeadingSpace("तिमी", "\n"))
        assertEquals("?", SpokenPunctuation.withLeadingSpace("?", "छौ"))
    }
}
