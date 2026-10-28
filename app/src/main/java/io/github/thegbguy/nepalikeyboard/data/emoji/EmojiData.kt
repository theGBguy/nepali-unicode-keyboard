package io.github.thegbguy.nepalikeyboard.data.emoji

import android.content.Context
import android.graphics.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Emoji categories, in the order the panel shows them. */
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
 * The emoji list from `assets/emoji.tsv` (built by tools/emoji/build_emoji.py), with emoji the
 * phone's font can't draw removed. Loaded once, in the background.
 */
object EmojiData {

    private val _categories = MutableStateFlow<Map<EmojiCategory, List<String>>>(emptyMap())
    val categories: StateFlow<Map<EmojiCategory, List<String>>> = _categories

    @Volatile
    private var loading = false

    fun loadAsync(context: Context) {
        if (loading || _categories.value.isNotEmpty()) return
        loading = true
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            val paint = Paint()
            val text = app.assets.open("emoji.tsv").bufferedReader().use { it.readText() }
            _categories.value = parse(text) { paint.hasGlyph(it) }
        }
    }

    /** Pure part, unit-tested. [canDraw] filters out emoji the font lacks. */
    fun parse(text: String, canDraw: (String) -> Boolean = { true }): Map<EmojiCategory, List<String>> {
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
}
