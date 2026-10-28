package io.github.thegbguy.nepalikeyboard.domain.transliteration

import android.content.Context
import android.util.Log
import kotlin.concurrent.thread

private const val TAG = "NepaliWordList"

@Volatile
private var loading = false

/** Loads assets/[NepaliWordList.ASSET] into [NepaliWordList.index] on a background thread. */
fun NepaliWordList.loadAsync(context: Context) {
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
