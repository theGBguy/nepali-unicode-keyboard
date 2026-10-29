package io.github.thegbguy.nepalikeyboard.data.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The bundled asset through the shared parser (needs the file, so it stays in the app's tests). */
class EmojiDataTest {

    private val asset = File("src/main/assets/emoji.tsv").readText()

    @Test
    fun `asset has every category except recent`() {
        val parsed = EmojiList.parse(asset)
        assertEquals(EmojiCategory.entries.toSet() - EmojiCategory.RECENT, parsed.keys)
        assertTrue(parsed.values.all { it.size > 50 })
        assertTrue("😀" in parsed.getValue(EmojiCategory.SMILEYS))
        assertTrue("🇳🇵" in parsed.getValue(EmojiCategory.FLAGS))
    }

    @Test
    fun `no skin tone variants in the base list`() {
        val all = EmojiList.parse(asset).values.flatten()
        assertTrue(all.none { e -> e.codePoints().anyMatch { it in 0x1F3FB..0x1F3FF } })
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `recents are most recent first, unique and capped`() {
        var r = emptyList<String>()
        listOf("😀", "🙏", "😀", "❤️").forEach { r = EmojiRecents.add(r, it) }
        assertEquals(listOf("❤️", "😀", "🙏"), r)
        assertEquals(r, EmojiRecents.decode(EmojiRecents.encode(r)))
        assertEquals(2, EmojiRecents.add(listOf("a", "b"), "c", max = 2).size)
        assertEquals(emptyList<String>(), EmojiRecents.decode(null))
    }
}
