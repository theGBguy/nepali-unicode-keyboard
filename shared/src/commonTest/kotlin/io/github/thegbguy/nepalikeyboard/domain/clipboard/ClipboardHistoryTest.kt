package io.github.thegbguy.nepalikeyboard.domain.clipboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipboardHistoryTest {

    private fun texts(items: List<ClipItem>) = items.map { it.text }

    @Test
    fun `new text goes on top and a repeat moves up instead of appearing twice`() {
        var items = ClipboardHistory.add(emptyList(), "a", 1)
        items = ClipboardHistory.add(items, "b", 2)
        items = ClipboardHistory.add(items, "a", 3)
        assertEquals(listOf("a", "b"), texts(items))
        assertEquals(3L, items.first().time)
    }

    @Test
    fun `a repeat keeps its pin`() {
        var items = ClipboardHistory.add(emptyList(), "a", 1)
        items = ClipboardHistory.setPinned(items, "a", true)
        items = ClipboardHistory.add(items, "a", 2)
        assertTrue(items.single().pinned)
    }

    @Test
    fun `blank and over-long text is not stored`() {
        assertEquals(emptyList<ClipItem>(), ClipboardHistory.add(emptyList(), "  ", 1))
        val long = "x".repeat(ClipboardHistory.MAX_LENGTH + 1)
        assertEquals(emptyList<ClipItem>(), ClipboardHistory.add(emptyList(), long, 1))
        val max = "x".repeat(ClipboardHistory.MAX_LENGTH)
        assertEquals(1, ClipboardHistory.add(emptyList(), max, 1).size)
    }

    @Test
    fun `oldest unpinned item drops first and pinned items never drop`() {
        var items = ClipboardHistory.add(emptyList(), "old pinned", 0)
        items = ClipboardHistory.setPinned(items, "old pinned", true)
        for (i in 1..ClipboardHistory.MAX_RECENT + 5) items = ClipboardHistory.add(items, "item $i", i.toLong())
        val recent = items.filter { !it.pinned }
        assertEquals(ClipboardHistory.MAX_RECENT, recent.size)
        assertEquals("item ${ClipboardHistory.MAX_RECENT + 5}", recent.first().text)
        assertEquals("item 6", recent.last().text)
        assertTrue(items.any { it.text == "old pinned" && it.pinned })
    }

    @Test
    fun `unpinning past the cap trims the oldest recent item`() {
        var items = emptyList<ClipItem>()
        for (i in 1..ClipboardHistory.MAX_RECENT) items = ClipboardHistory.add(items, "item $i", i.toLong())
        items = ClipboardHistory.setPinned(items, "item 1", true)
        items = ClipboardHistory.add(items, "new", 100)
        assertEquals(ClipboardHistory.MAX_RECENT, items.count { !it.pinned })
        items = ClipboardHistory.setPinned(items, "item 1", false)
        assertEquals(ClipboardHistory.MAX_RECENT, items.count { !it.pinned })
        assertTrue(items.none { it.text == "item 1" }, "the oldest unpinned item goes")
    }

    @Test
    fun `pinning stops at the pin cap`() {
        var items = emptyList<ClipItem>()
        for (i in 0..ClipboardHistory.MAX_PINNED) {
            items = ClipboardHistory.add(items, "p$i", i.toLong())
            items = ClipboardHistory.setPinned(items, "p$i", true)
        }
        assertEquals(ClipboardHistory.MAX_PINNED, ClipboardHistory.pinnedCount(items))
        assertTrue(items.first { it.text == "p${ClipboardHistory.MAX_PINNED}" }.pinned.not())
    }

    @Test
    fun `clear all keeps pinned items and delete removes one`() {
        var items = ClipboardHistory.add(emptyList(), "a", 1)
        items = ClipboardHistory.add(items, "b", 2)
        items = ClipboardHistory.add(items, "c", 3)
        items = ClipboardHistory.setPinned(items, "a", true)
        assertEquals(listOf("c", "a"), texts(ClipboardHistory.delete(items, "b")))
        assertEquals(listOf("a"), texts(ClipboardHistory.clearUnpinned(items)))
    }

    @Test
    fun `encode and decode round trip text with tabs line breaks backslashes and Nepali`() {
        var items = ClipboardHistory.add(emptyList(), "line one\nline two\ttab \\n literal", 5)
        items = ClipboardHistory.add(items, "नमस्ते 🙏", 6)
        items = ClipboardHistory.setPinned(items, "नमस्ते 🙏", true)
        items = ClipboardHistory.add(items, "ends with backslash \\", 7)
        assertEquals(items, ClipboardHistory.decode(ClipboardHistory.encode(items)))
    }

    @Test
    fun `decode skips damaged lines and duplicates`() {
        val data = "-\t1\tgood\nbroken line\n-\t2\tgood\np\tx\tpinned"
        val items = ClipboardHistory.decode(data)
        assertEquals(listOf("good", "pinned"), texts(items))
        assertTrue(items[1].pinned)
        assertEquals(emptyList<ClipItem>(), ClipboardHistory.decode(null))
    }
}
