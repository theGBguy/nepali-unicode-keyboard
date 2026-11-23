package io.github.thegbguy.nepalikeyboard.update

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import io.github.thegbguy.nepalikeyboard.domain.review.ReviewPrompt

/**
 * Play's in-app review sheet, asked for once by [ReviewPrompt]'s rule. Play decides whether it
 * shows (it has a quota) and never says, so the app counts it as asked either way. The Rate row
 * in Settings opens the store page instead: Play may show nothing for a button.
 */
class AppReview(private val context: Context, private val manager: ReviewManager) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var prompt: ReviewPrompt
        get() = ReviewPrompt(prefs.getInt(KEY_OPENS, 0), prefs.getBoolean(KEY_ASKED, false))
        set(value) = prefs.edit { putInt(KEY_OPENS, value.opens); putBoolean(KEY_ASKED, value.asked) }

    /** Call once per app launch. Returns true when this launch should ask. */
    fun onLaunch(setupDone: Boolean): Boolean {
        prompt = prompt.opened(setupDone)
        return prompt.shouldAsk()
    }

    suspend fun ask(activity: Activity) {
        prompt = prompt.askedNow()
        runCatching { manager.launchReview(activity, manager.requestReview()) }
            .onFailure { Log.d(TAG, "Review flow failed: ${it.message}") }
    }

    companion object {
        private const val TAG = "AppReview"
        private const val PREFS = "review"
        private const val KEY_OPENS = "opens"
        private const val KEY_ASKED = "asked"

        fun create(context: Context) = AppReview(context, ReviewManagerFactory.create(context))
    }
}
