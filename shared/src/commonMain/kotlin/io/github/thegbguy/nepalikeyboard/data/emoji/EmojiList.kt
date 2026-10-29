package io.github.thegbguy.nepalikeyboard.data.emoji

/** Emoji categories, in the order the emoji panel shows them. */
enum class EmojiCategory(val key: String) {
    RECENT("recent"),
    SMILEYS("smileys"),
    ANIMALS("animals"),
    FOOD("food"),
    ACTIVITIES("activities"),
    TRAVEL("travel"),
    OBJECTS("objects"),
    SYMBOLS("symbols"),
    FLAGS("flags")
}

/**
 * Reads the bundled emoji list (`emoji.tsv`, built by tools/emoji/build_emoji.py): one line per
 * category, `key<TAB>emoji emoji ...`. Loading the file is up to each platform.
 */
object EmojiList {
    const val ASSET = "emoji.tsv"

    /** The emoji per category, in file order. [canDraw] drops emoji the phone's font lacks. */
    fun parse(text: String, canDraw: (String) -> Boolean): Map<EmojiCategory, List<String>> {
        val byKey = EmojiCategory.entries.associateBy { it.key }
        return text.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val (key, list) = line.split('\t', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                val category = byKey[key] ?: return@mapNotNull null
                category to list.split(' ').filter { it.isNotEmpty() && canDraw(it) }
            }
            .toMap()
    }

    fun parse(text: String): Map<EmojiCategory, List<String>> = parse(text) { true }
}
