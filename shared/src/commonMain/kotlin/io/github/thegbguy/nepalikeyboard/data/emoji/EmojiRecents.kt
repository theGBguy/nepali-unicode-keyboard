package io.github.thegbguy.nepalikeyboard.data.emoji

/** Most-recent-first list of used emoji, capped at [max]. */
object EmojiRecents {
    const val MAX = 32

    fun add(recents: List<String>, emoji: String, max: Int = MAX): List<String> =
        (listOf(emoji) + recents.filter { it != emoji }).take(max)

    fun encode(recents: List<String>): String = recents.joinToString(" ")

    fun decode(stored: String?): List<String> =
        stored.orEmpty().split(' ').filter { it.isNotEmpty() }
}
