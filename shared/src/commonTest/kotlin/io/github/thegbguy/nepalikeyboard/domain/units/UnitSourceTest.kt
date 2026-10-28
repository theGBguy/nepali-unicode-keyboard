package io.github.thegbguy.nepalikeyboard.domain.units

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitSourceTest {

    private fun offers(text: String, mode: LanguageMode = LanguageMode.TRANSLITERATION) =
        UnitSource.suggest(text, mode).map { "${it.text}/${it.consumed}" }

    @Test
    fun `aana while it's being typed after a committed number`() {
        assertEquals(
            listOf("५ आना (१,७११ वर्ग फिट)/6", "५ आना (१५८.९८ वर्ग मिटर)/6"),
            offers("५ aana")
        )
    }

    @Test
    fun `the strip shows only the conversion`() {
        assertEquals(listOf("१,७११ वर्ग फिट", "१५८.९८ वर्ग मिटर"), UnitSource.suggest("५ aana", LanguageMode.NEPALI).map { it.label })
    }

    @Test
    fun `Devanagari units with a trailing space`() {
        assertEquals("५ आना (१,७११ वर्ग फिट) /6", offers("५ आना ", LanguageMode.NEPALI).first())
    }

    @Test
    fun `English mode keeps the text as typed`() {
        assertEquals(listOf("5 aana (1,711 sq ft)/6", "5 aana (158.98 m²)/6"), offers("5 aana", LanguageMode.ENGLISH))
    }

    @Test
    fun `compound hill units add up`() {
        assertEquals("२ रोपनी ५ आना (१२,६६३ वर्ग फिट)/15", offers("2 ropani 5 aana").first())
        assertEquals("५ आना २ पैसा (१,८८२ वर्ग फिट)/13", offers("५ आना २ paisa").first())
    }

    @Test
    fun `R-A-P-D notation`() {
        assertEquals(listOf("०-५-२-० (१,८८२ वर्ग फिट)/7", "०-५-२-० (१७४.८८ वर्ग मिटर)/7"), offers("0-5-2-0"))
        assertEquals(emptyList<String>(), offers("0-16-0-0")) // 16 aana is a ropani
        assertEquals(emptyList<String>(), offers("1-0-5-2-0"))
    }

    @Test
    fun `Terai units`() {
        assertEquals(listOf("१ बिघा (७२,९०० वर्ग फिट)/7", "१ बिघा (६,७७२.६३ वर्ग मिटर)/7"), offers("1 bigha"))
        assertEquals("1 bigha 5 kattha (91,125 sq ft)/16", offers("1 bigha 5 kattha", LanguageMode.ENGLISH).first())
    }

    @Test
    fun `area to hill units`() {
        assertEquals(listOf("1711 sqft (5 aana)/9", "1711 sqft (158.96 m²)/9"), offers("1711 sqft", LanguageMode.ENGLISH))
        assertEquals(listOf("100 m2 (1,076 sq ft)/6", "100 m2 (3 aana 2.3 daam)/6"), offers("100 m2", LanguageMode.ENGLISH))
    }

    @Test
    fun `gold`() {
        assertEquals(listOf("२ तोला (२३.३३ ग्राम)/6"), offers("2 tola"))
        assertEquals(listOf("1 tola 50 lal (17.50 g)/13"), offers("1 tola 50 lal", LanguageMode.ENGLISH))
        assertEquals(listOf("10 gram (0.86 tola)/7"), offers("10 gram", LanguageMode.ENGLISH))
        assertEquals(listOf("२.५ तोला (२९.१६ ग्राम)/8"), offers("२.5 tola"))
    }

    @Test
    fun `paisa alone is money mixed systems and glued numbers get nothing`() {
        listOf("50 paisa", "2 ropani 5 kattha", "1 tola 5 gram", "ID5 aana", "5 aanaa").forEach {
            assertEquals(emptyList<String>(), offers(it), it)
        }
    }
}
