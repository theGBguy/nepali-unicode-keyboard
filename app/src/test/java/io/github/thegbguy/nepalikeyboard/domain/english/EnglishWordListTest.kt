package io.github.thegbguy.nepalikeyboard.domain.english

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** English suggestions with the bundled word list: what people type → what the strip offers first. */
class EnglishWordListTest {

    companion object {
        private lateinit var index: EnglishWordIndex
        private lateinit var lines: List<String>

        @BeforeClass
        @JvmStatic
        fun load() {
            val file = File("src/main/assets/${EnglishWordList.ASSET}")
            lines = file.readLines()
            index = EnglishWordIndex.parse(file.readText())
        }
    }

    private fun checkFirst(vararg pairs: Pair<String, String>) {
        val failures = pairs.mapNotNull { (typed, want) ->
            val got = index.suggest(typed)
            if (got.firstOrNull() == want) null else "$typed → $got (want $want first)"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `everyday completions`() = checkFirst(
        "t" to "the", "th" to "the", "wh" to "what", "kno" to "know", "becau" to "because",
        "tomo" to "tomorrow", "pleas" to "please", "sor" to "sorry", "Thank" to "Thank"
    )

    @Test
    fun `common typos`() = checkFirst(
        "teh" to "the", "becuse" to "because", "recieve" to "receive", "definately" to "definitely",
        "realy" to "really", "jsut" to "just", "tomorow" to "tomorrow", "goin" to "going"
    )

    @Test
    fun `contractions without the apostrophe`() = checkFirst(
        "dont" to "don't", "im" to "I'm", "Im" to "I'm", "youre" to "you're", "cant" to "can't",
        "didnt" to "didn't", "isnt" to "isn't", "wont" to "won't"
    )

    @Test
    fun `I is always a capital`() = checkFirst("i" to "I")

    @Test
    fun `Nepal words are offered`() {
        assertEquals("Nepal", index.suggest("nep").first())
        assertTrue(index.suggest("kath").contains("Kathmandu"))
        assertTrue(index.suggest("pokh").contains("Pokhara"))
    }

    @Test
    fun `blocked words are not in the list`() {
        val words = lines.filterNot { it.startsWith("#") }.map { it.substringBefore('\t').lowercase() }.toSet()
        val blocked = File("../tools/wordlist/blocked_en.txt").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.trim().lowercase() }
        assertEquals(emptyList<String>(), blocked.filter { it in words })
    }

    @Test
    fun `short abbreviations and symbols are left out`() {
        val suggested = listOf("th", "lo", "cd", "na").flatMap { index.suggest(it) }
        assertTrue(suggested.toString(), suggested.none { it in setOf("Th", "Lon", "Cd", "Na") })
    }
}
