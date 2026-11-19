package io.github.thegbguy.nepalikeyboard.domain.english

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnglishWordIndexTest {

    private val index = EnglishWordIndex.parse(
        """
        # comment
        the	22000000
        they	3000000
        there	3100000
        then	1500000
        that	10000000
        don't	4000000
        done	400000
        I	27000000
        I'm	4300000
        in	7000000
        hello	300000
        help	900000
        because	1200000
        London	40000
        long	700000
        """.trimIndent()
    )

    @Test
    fun `completions come by frequency`() {
        assertEquals(listOf("the", "that"), index.suggest("th").take(2))
        assertEquals(setOf("there", "they", "then"), index.suggest("th").drop(2).take(3).toSet())
    }

    @Test
    fun `a known word comes first even when a longer one is more common`() {
        assertEquals("the", index.suggest("the").first())
        assertEquals("hello", index.suggest("hello").first())
    }

    @Test
    fun `one typo away is corrected`() {
        assertEquals("the", index.suggest("teh").first())
        assertEquals("because", index.suggest("becuse").first())
        assertEquals("hello", index.suggest("helllo").first())
    }

    @Test
    fun `a missing apostrophe is put back`() {
        assertEquals("don't", index.suggest("dont").first())
        assertEquals("I'm", index.suggest("im").first())
    }

    @Test
    fun `typed capitals carry over`() {
        assertEquals("Help", index.suggest("Hel").first())
        assertEquals("HELP", index.suggest("HEL").first())
        assertEquals("I'm", index.suggest("Im").first())
    }

    @Test
    fun `words with their own capitals keep them`() {
        assertTrue("London" in index.suggest("lon"))
        assertEquals("I", index.suggest("i").first())
    }

    @Test
    fun `a curly apostrophe matches a straight one`() {
        assertEquals("don't", index.suggest("don\u2019t").first())
    }

    @Test
    fun `nothing is offered for unknown text`() {
        assertEquals(emptyList(), index.suggest("zzzzqq"))
        assertEquals(emptyList(), index.suggest(""))
    }

    @Test
    fun `the current word is the letters right before the cursor`() {
        assertEquals("hel", EnglishWordIndex.currentWord("say hel", ""))
        assertEquals("hel", EnglishWordIndex.currentWord("hel", null))
        assertEquals("don't", EnglishWordIndex.currentWord("I don't", " know"))
        assertEquals("he", EnglishWordIndex.currentWord("(he", null))
        assertEquals("x", EnglishWordIndex.currentWord("नमस्ते x", null))
    }

    @Test
    fun `no current word after a space or inside a word`() {
        assertNull(EnglishWordIndex.currentWord("hello ", null))
        assertNull(EnglishWordIndex.currentWord("hel", "lo"))
        assertNull(EnglishWordIndex.currentWord("", null))
    }

    @Test
    fun `handles tags links and numbers are not words`() {
        assertNull(EnglishWordIndex.currentWord("@ram", null))
        assertNull(EnglishWordIndex.currentWord("#nepal", null))
        assertNull(EnglishWordIndex.currentWord("example.com/pa", null))
        assertNull(EnglishWordIndex.currentWord("5kg", null))
    }
}
