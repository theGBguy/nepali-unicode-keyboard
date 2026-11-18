package io.github.thegbguy.nepalikeyboard.data.emoji

import android.content.Context
import android.graphics.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The emoji list from `assets/emoji.tsv` (parsed by the shared [EmojiList]) and the search words
 * from `assets/emoji_keywords.tsv` ([EmojiKeywords]), with emoji the phone's font can't draw
 * removed. Loaded once, in the background.
 */
object EmojiData {

    private val _categories = MutableStateFlow<Map<EmojiCategory, List<String>>>(emptyMap())
    val categories: StateFlow<Map<EmojiCategory, List<String>>> = _categories

    private val _keywords = MutableStateFlow<EmojiKeywords?>(null)
    val keywords: StateFlow<EmojiKeywords?> = _keywords

    @Volatile
    private var loading = false

    fun loadAsync(context: Context) {
        if (loading || _categories.value.isNotEmpty()) return
        loading = true
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            val paint = Paint()
            val text = app.assets.open(EmojiList.ASSET).bufferedReader().use { it.readText() }
            _categories.value = EmojiList.parse(text) { paint.hasGlyph(it) }
            val words = app.assets.open(EmojiKeywords.ASSET).bufferedReader().use { it.readText() }
            _keywords.value = EmojiKeywords.parse(words) { paint.hasGlyph(it) }
        }
    }
}
