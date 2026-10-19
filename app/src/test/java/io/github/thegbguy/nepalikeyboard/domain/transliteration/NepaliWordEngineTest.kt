package io.github.thegbguy.nepalikeyboard.domain.transliteration

import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Romanized typing as people actually type it, with the bundled word list loaded.
 * Each pair is what a user types → the word the keyboard should put in the text on space.
 */
class NepaliWordEngineTest {

    companion object {
        private lateinit var engine: NepaliTransliterationEngine

        @BeforeClass
        @JvmStatic
        fun load() {
            val file = File("src/main/assets/${NepaliWordList.ASSET}")
            val index = file.bufferedReader().use { WordIndex.build(it.lineSequence()) }
            engine = NepaliTransliterationEngine { index }
        }
    }

    private fun type(roman: String): TransliterationBuffer {
        var buffer = TransliterationBuffer()
        roman.forEach { buffer = engine.processInput(buffer, it.toString()) }
        return buffer
    }

    private fun commit(roman: String) = engine.commitBuffer(type(roman))

    private fun check(vararg pairs: Pair<String, String>) {
        val failures = pairs.mapNotNull { (roman, expected) ->
            val got = commit(roman)
            if (got == expected) null else "$roman → $got (want $expected)"
        }
        assertTrue("Wrong words:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `greetings and everyday words`() = check(
        "namaste" to "नमस्ते",
        "dhanyabad" to "धन्यवाद",
        "dhanyawad" to "धन्यवाद",
        "nepal" to "नेपाल",
        "nepali" to "नेपाली",
        "kathmandu" to "काठमाडौं",
        "sathi" to "साथी",
        "aama" to "आमा",
        "buwa" to "बुवा",
        "didi" to "दिदी",
        "ramro" to "राम्रो",
        "dherai" to "धेरै",
        "ghar" to "घर",
        "khana" to "खाना",
        "paani" to "पानी",
        "maya" to "माया"
    )

    @Test
    fun `pronouns`() = check(
        "ma" to "म",
        "mero" to "मेरो",
        "malai" to "मलाई",
        "timi" to "तिमी",
        "timro" to "तिम्रो",
        "tapai" to "तपाईं",
        "hami" to "हामी",
        "yo" to "यो",
        "tyo" to "त्यो"
    )

    @Test
    fun `verbs and particles`() = check(
        "chha" to "छ",
        "cha" to "छ",
        "chhaina" to "छैन",
        "huncha" to "हुन्छ",
        "garchu" to "गर्छु",
        "garnuhos" to "गर्नुहोस्",
        "bhayo" to "भयो",
        "thiyo" to "थियो",
        "pani" to "पनि",
        "ra" to "र",
        "tara" to "तर",
        "kina" to "किन",
        "kasto" to "कस्तो",
        "kasari" to "कसरी",
        "aaja" to "आज",
        "bholi" to "भोलि",
        "hijo" to "हिजो",
        "ahile" to "अहिले"
    )

    @Test
    fun `loose spellings, polite forms and loanwords`() = check(
        "ekchin" to "एकछिन",
        "parkhanus" to "पर्खनुस्",
        "hernus" to "हेर्नुस्",
        "bhannuhos" to "भन्नुहोस्",
        "keta" to "केटा",
        "manchhe" to "मान्छे",
        "bidyalaya" to "विद्यालय",
        "sambidhan" to "संविधान",
        "prajatantra" to "प्रजातन्त्र",
        "kripaya" to "कृपया",
        "shubhakamana" to "शुभकामना",
        "aayera" to "आएर",
        "pathau" to "पठाऊ",
        "phone" to "फोन",
        "message" to "म्यासेज",
        "aaunchhu" to "आउँछु",
        "aauchhu" to "आउँछु",
        "tapain" to "तपाईं"
    )

    @Test
    fun `unknown names fall back to letter-by-letter conversion`() {
        assertEquals("Thamel", "ठमेल", commit("Thamel"))
        assertTrue(type("namaste").candidates.size <= 6)
    }

    @Test
    fun `without the word list the converter still works`() {
        val bare = NepaliTransliterationEngine { null }
        var b = TransliterationBuffer()
        "namaste".forEach { b = bare.processInput(b, it.toString()) }
        assertEquals("नमस्ते", bare.commitBuffer(b))
    }
}
