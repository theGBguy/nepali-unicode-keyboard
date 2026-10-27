package io.github.thegbguy.nepalikeyboard.domain.calendar

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateSourceTest {

    /** Tuesday 27 October 2026 = 2083 Kartik 10. */
    private val source = DateSource { LocalDate.of(2026, 10, 27).toEpochDay() }

    private fun offers(text: String, mode: LanguageMode = LanguageMode.TRANSLITERATION) =
        source.suggest(text, mode).map { "${it.text}/${it.consumed}" }

    @Test
    fun `Romanized day words`() {
        assertEquals(listOf("२०८३ कार्तिक १०, मंगलबार/4", "२०८३/०७/१०/4"), offers("aaja"))
        assertEquals(listOf("२०८३ कार्तिक ११, बुधबार/5", "२०८३/०७/११/5"), offers("ma bholi"))
        assertEquals("२०८३ कार्तिक ९, सोमबार/4", offers("hijo").first())
        assertEquals("२०८३ कार्तिक १२, बिहीबार/5", offers("parsi").first())
        assertEquals("२०८३ कार्तिक ८, आइतबार/4", offers("asti").first())
    }

    @Test
    fun `Devanagari day words, with the space after them`() {
        assertEquals(listOf("२०८३ कार्तिक १०, मंगलबार /3", "२०८३/०७/१० /3"), offers("आज ", LanguageMode.NEPALI))
        assertEquals("२०८३ कार्तिक ११, बुधबार/4", offers("भोलि").first())
    }

    @Test
    fun `miti gives a form date`() {
        assertEquals(listOf("मिति: २०८३/०७/१०/4", "मिति: २०८३ कार्तिक १०, मंगलबार/4"), offers("miti"))
    }

    @Test
    fun `English mode`() {
        assertEquals(listOf("2083 Kartik 10 B.S./5", "2083/07/10 B.S./5"), offers("Today", LanguageMode.ENGLISH))
        assertEquals("2083 Kartik 11 B.S./8", offers("tomorrow", LanguageMode.ENGLISH).first())
        assertEquals(emptyList<String>(), offers("aaja", LanguageMode.ENGLISH))
    }

    @Test
    fun `day words inside other words get nothing`() {
        assertEquals(emptyList<String>(), offers("raaja"))
        assertEquals(emptyList<String>(), offers("आजको"))
    }

    @Test
    fun `years after 2034 are read as BS only, and past 2090 there's nothing`() {
        assertEquals(listOf("सन् १९८३ अप्रिल १४, बिहीबार/10"), offers("2040-01-01"))
        assertEquals(emptyList<String>(), offers("2095-01-01"))
    }

    @Test
    fun `typed dates in the overlap get both readings`() {
        assertEquals(
            listOf("२०८३ कार्तिक ४, बुधबार/10", "सन् १९७० फेब्रुअरी ३, मंगलबार/10"),
            offers("2026-10-21")
        )
        assertEquals(listOf("2083 Kartik 4 B.S./10"), offers("21/10/2026", LanguageMode.ENGLISH).take(1))
    }

    @Test
    fun `typed BS date gets its AD date`() {
        assertEquals(listOf("सन् २०२६ अक्टोबर २१, बुधबार/10"), offers("२०८३-०७-०४"))
        assertEquals(listOf("21 October 2026/10"), offers("2083.07.04", LanguageMode.ENGLISH))
        assertEquals(listOf("सन् २०२६ अक्टोबर २१, बुधबार /9"), offers("मिति २०८३/७/४ "))
    }

    @Test
    fun `old AD dates are only AD to BS`() {
        assertEquals(listOf("२०४२ बैशाख १, शनिबार/10"), offers("1985-04-13"))
    }

    @Test
    fun `invalid and mixed dates get nothing`() {
        listOf("2083-13-01", "2026-02-33", "2026-10/21", "12026-10-21", "1-2-3").forEach {
            assertEquals(it, emptyList<String>(), offers(it))
        }
    }
}
