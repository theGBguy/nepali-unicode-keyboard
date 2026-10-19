package io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts

import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NepaliLayoutTest {

    private val keys = NepaliLayout.getLayout().rows.flatten().filter { it.keyType == KeyType.CHARACTER }
    private val base = keys.map { it.primary }
    private val reachable = (base + keys.mapNotNull { it.shift }).toSet()

    @Test
    fun `every consonant has its own key`() {
        val consonants = "कखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसह".map { it.toString() }
        val missing = consonants.filter { it !in base }
        assertTrue("Missing consonants: $missing", missing.isEmpty())
    }

    @Test
    fun `no letter appears on two keys`() {
        val dupes = base.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        assertTrue("Duplicated keys: $dupes", dupes.isEmpty())
    }

    @Test
    fun `every vowel sign and mark is reachable`() {
        val needed = listOf("ा", "ि", "ी", "ु", "ू", "ृ", "े", "ै", "ो", "ौ", "ं", "ँ", "ः", "्", "।")
        val missing = needed.filter { it !in reachable }
        assertTrue("Missing signs: $missing", missing.isEmpty())
    }

    @Test
    fun `common independent vowels and conjuncts are reachable`() {
        val needed = listOf("अ", "इ", "उ", "ए", "ऋ", "क्ष", "ज्ञ")
        val missing = needed.filter { it !in reachable }
        assertTrue("Missing: $missing", missing.isEmpty())
    }

    @Test
    fun `standard key positions`() {
        val rows = NepaliLayout.getLayout().rows
        assertEquals("ञघङझछटठडढण", rows[1].joinToString("") { it.primary })
        assertEquals("बकमानजवपिस", rows[3].joinToString("") { it.primary })
    }

    @Test
    fun `numbers layout has the independent vowels the letter layout lacks`() {
        val symbols = NepaliLayout.getNumbersLayout().rows.flatten().map { it.primary }
        listOf("ई", "ऊ", "ऐ", "ओ", "औ", "ॐ", "॥").forEach { assertTrue(it, it in symbols) }
    }
}
