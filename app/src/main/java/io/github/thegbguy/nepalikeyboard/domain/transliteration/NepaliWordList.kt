package io.github.thegbguy.nepalikeyboard.domain.transliteration

import android.content.Context
import android.util.Log
import kotlin.concurrent.thread

/**
 * The bundled Nepali word list (assets/words_ne.tsv), loaded once per process on a background
 * thread. Until it's ready, Romanized typing uses the letter-by-letter converter alone.
 *
 * Built by `tools/wordlist/build_wordlist.py` from Nepali Wikipedia word counts (CC BY-SA 4.0)
 * plus a hand-written list of everyday and colloquial words.
 */
object NepaliWordList {
    const val ASSET = "words_ne.tsv"
    private const val TAG = "NepaliWordList"

    @Volatile
    var index: WordIndex? = null
        private set

    @Volatile
    private var loading = false

    fun loadAsync(context: Context) {
        if (index != null || loading) return
        loading = true
        val appContext = context.applicationContext
        thread(name = "nepali-word-list", priority = Thread.MIN_PRIORITY) {
            try {
                val started = System.currentTimeMillis()
                index = appContext.assets.open(ASSET).bufferedReader().use { WordIndex.build(it.lineSequence()) }
                Log.d(TAG, "Loaded ${index?.size} words in ${System.currentTimeMillis() - started} ms")
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't load the word list", e)
            } finally {
                loading = false
            }
        }
    }
}
