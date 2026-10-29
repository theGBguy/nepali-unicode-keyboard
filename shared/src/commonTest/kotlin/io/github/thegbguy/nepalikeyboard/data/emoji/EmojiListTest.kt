package io.github.thegbguy.nepalikeyboard.data.emoji

import kotlin.test.Test
import kotlin.test.assertEquals

class EmojiListTest {

    @Test
    fun parsesCategoriesInFileOrderAndSkipsComments() {
        val parsed = EmojiList.parse("# comment\nsmileys\t😀 😃\n\nflags\t🇳🇵 🏁\n")
        assertEquals(listOf("😀", "😃"), parsed[EmojiCategory.SMILEYS])
        assertEquals(listOf("🇳🇵", "🏁"), parsed[EmojiCategory.FLAGS])
        assertEquals(setOf(EmojiCategory.SMILEYS, EmojiCategory.FLAGS), parsed.keys)
    }

    @Test
    fun unknownCategoriesAndMalformedLinesAreIgnored() {
        val parsed = EmojiList.parse("people\t😀\nno-tab-here\nfood\t🍎  🍌\n")
        assertEquals(mapOf(EmojiCategory.FOOD to listOf("🍎", "🍌")), parsed)
    }

    @Test
    fun emojiTheFontCantDrawAreDropped() {
        val parsed = EmojiList.parse("smileys\t😀 🫨 😃\n") { it != "🫨" }
        assertEquals(listOf("😀", "😃"), parsed[EmojiCategory.SMILEYS])
    }
}
