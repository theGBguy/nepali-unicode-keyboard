package io.github.thegbguy.nepalikeyboard.domain.english

import android.content.Context
import android.util.Log
import kotlin.concurrent.thread

private const val TAG = "EnglishWordList"

@Volatile
private var loading = false

/** Loads assets/[EnglishWordList.ASSET] into [EnglishWordList.index] on a background thread. */
fun EnglishWordList.loadAsync(context: Context) {
    if (index != null || loading) return
    loading = true
    val appContext = context.applicationContext
    thread(name = "english-word-list", priority = Thread.MIN_PRIORITY) {
        try {
            val started = System.currentTimeMillis()
            index = appContext.assets.open(ASSET).bufferedReader().use { EnglishWordIndex.build(it.lineSequence()) }
            Log.d(TAG, "Loaded ${index?.size} words in ${System.currentTimeMillis() - started} ms")
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't load the word list", e)
        } finally {
            loading = false
        }
    }
}
