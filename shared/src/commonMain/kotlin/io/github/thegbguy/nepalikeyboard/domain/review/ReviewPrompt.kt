package io.github.thegbguy.nepalikeyboard.domain.review

/**
 * When the app asks for a store review: once, on the [OPENS]th launch that starts with the
 * keyboard already set up (the same day is fine). The launch where setup is finished doesn't
 * count, so people come back after typing with the keyboard. Never from the keyboard, so nobody
 * is asked mid-sentence, and the store decides whether the sheet actually appears.
 *
 * @param opens app launches that started with the keyboard set up
 * @param asked the review sheet was already requested
 */
data class ReviewPrompt(val opens: Int = 0, val asked: Boolean = false) {

    /** The app was launched; only launches that start with the keyboard set up count. */
    fun opened(setupDone: Boolean): ReviewPrompt = if (setupDone && !asked) copy(opens = opens + 1) else this

    fun shouldAsk(): Boolean = !asked && opens >= OPENS

    fun askedNow(): ReviewPrompt = copy(asked = true)

    companion object {
        const val OPENS = 1
    }
}
