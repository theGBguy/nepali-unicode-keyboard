package io.github.thegbguy.nepalikeyboard.domain.transliteration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordIndexTest {

    private val index = WordIndex.parse(
        """
        # comment
        नेपाल	500
        नेपाली	400
        नेपालको	300
        नेपालमा	300
        काठमाडौं	200	kathmandu, ktm
        कमल	10
        षहर	10
        शहर	10

        not-a-number	x
        नेपाल	100
        """.trimIndent()
    )

    @Test
    fun frequencyOfListedAndUnlistedWords() {
        assertEquals(500, index.frequency("नेपाल"), "a word listed twice keeps its highest frequency")
        assertEquals(10, index.frequency("कमल"))
        assertNull(index.frequency("नेपा"))
        assertNull(index.frequency("not-a-number"))
        assertNull(index.frequency(""))
        assertEquals(8, index.size)
    }

    @Test
    fun lookupFindsWordsBySoundBestFirst() {
        assertEquals("नेपाल", index.lookup("nepal").first().word)
        assertEquals("नेपाल", index.lookup("Nepaal").first().word)
        assertEquals("नेपाली", index.lookup("nepali").first().word)
        assertEquals(emptyList(), index.lookup("xyz"))
    }

    @Test
    fun lookupUsesAliases() {
        assertEquals(listOf("काठमाडौं"), index.lookup("kathmandu").map { it.word })
        assertEquals(listOf("काठमाडौं"), index.lookup("ktm").map { it.word })
    }

    @Test
    fun equalScoresKeepTheFileOrder() {
        // Both are spelled "shahara" and equally common; षहर comes first in the file
        assertEquals(listOf("षहर", "शहर"), index.lookup("shahar").map { it.word })
    }

    @Test
    fun lookupRespectsTheLimit() {
        assertEquals(1, index.lookup("shahar", limit = 1).size)
    }

    @Test
    fun completionsAreLongerWordsStartingWithTheTypedKey() {
        assertEquals(listOf("नेपाली", "नेपालको"), index.completions("nepal"))
        assertEquals(listOf("नेपाली", "नेपालको", "नेपालमा"), index.completions("nepal", limit = 3))
        // Shorter than three letters of key: too early to guess
        assertEquals(emptyList(), index.completions("ne"))
        assertEquals(emptyList(), index.completions("nepalxyz"))
    }

    @Test
    fun emptyIndex() {
        val empty = WordIndex.parse("")
        assertEquals(0, empty.size)
        assertEquals(emptyList(), empty.lookup("nepal"))
        assertEquals(emptyList(), empty.completions("nepal"))
        assertNull(empty.frequency("नेपाल"))
    }
}
