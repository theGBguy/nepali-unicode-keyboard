package io.github.thegbguy.nepalikeyboard.domain.transliteration

import kotlin.concurrent.Volatile

/**
 * The bundled Nepali word list (`words_ne.tsv`), loaded once per process by the platform (Android:
 * `NepaliWordListLoader.kt` in the app) on a background thread. Until it's ready, Romanized typing
 * uses the letter-by-letter converter alone.
 *
 * Built by `tools/wordlist/build_wordlist.py` from Nepali Wikipedia word counts (CC BY-SA 4.0)
 * plus a hand-written list of everyday and colloquial words.
 */
object NepaliWordList {
    const val ASSET = "words_ne.tsv"

    @Volatile
    var index: WordIndex? = null
}
