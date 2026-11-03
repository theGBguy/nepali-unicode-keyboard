package io.github.thegbguy.nepalikeyboard.domain.transliteration

import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Top-1 and top-3 accuracy of Romanized typing on `romanized_corpus.tsv`, with the bundled word
 * list loaded. Top-1 is what space commits; top-3 is what the strip shows without scrolling.
 * Prints a per-category report and the misses, and fails if accuracy drops below the floors,
 * so a rule that helps one word can't quietly break others. Raise the floors when tuning helps.
 */
class RomanizedBenchmarkTest {

    private class Row(val typed: String, val expected: Set<String>, val category: String)

    private val rows: List<Row> =
        File("src/test/resources/romanized_corpus.tsv").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val (typed, expected, category) = line.split('\t')
                Row(typed, expected.replace("{ZWJ}", "\u200D").split('|').toSet(), category)
            }

    private val engine: NepaliTransliterationEngine by lazy {
        val file = File("src/main/assets/${NepaliWordList.ASSET}")
        val index = file.bufferedReader().use { WordIndex.build(it.lineSequence()) }
        NepaliTransliterationEngine { index }
    }

    private fun type(roman: String): TransliterationBuffer {
        var buffer = TransliterationBuffer()
        roman.forEach { buffer = engine.processInput(buffer, it.toString()) }
        return buffer
    }

    @Test
    fun `accuracy on the typing corpus`() {
        var top1 = 0
        var top3 = 0
        val byCategory = linkedMapOf<String, IntArray>() // rows, top1, top3
        val misses = mutableListOf<String>()
        for (row in rows) {
            val buffer = type(row.typed)
            val committed = engine.commitBuffer(buffer)
            val shown = buffer.candidates.take(3).map { it.devanagari }
            val hit1 = committed in row.expected
            val hit3 = hit1 || shown.any { it in row.expected }
            val counts = byCategory.getOrPut(row.category) { IntArray(3) }
            counts[0]++
            if (hit1) { top1++; counts[1]++ }
            if (hit3) { top3++; counts[2]++ }
            if (!hit1) {
                misses += "MISS ${row.category} ${row.typed} → $committed (want ${row.expected.joinToString("|")})" +
                    if (hit3) " [in top 3]" else "  [${shown.joinToString(" ")}]"
            }
        }
        fun pct(n: Int, of: Int) = "%.1f%%".format(100.0 * n / of)
        println("BENCH total ${rows.size}: top-1 ${pct(top1, rows.size)}, top-3 ${pct(top3, rows.size)}")
        byCategory.forEach { (category, c) ->
            println("BENCH $category ${c[0]}: top-1 ${pct(c[1], c[0])}, top-3 ${pct(c[2], c[0])}")
        }
        misses.forEach(::println)

        assertTrue("top-1 fell to ${pct(top1, rows.size)}", top1 * 100 >= TOP1_FLOOR * rows.size)
        assertTrue("top-3 fell to ${pct(top3, rows.size)}", top3 * 100 >= TOP3_FLOOR * rows.size)
    }

    private companion object {
        /** Percent floors; set from the last measured run. */
        const val TOP1_FLOOR = 90
        const val TOP3_FLOOR = 95
    }
}
