package io.github.thegbguy.nepalikeyboard.ui.setup

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit

/** Where the user is in setup, read live from Android each time the screen resumes. */
data class KeyboardStatus(val enabled: Boolean, val selected: Boolean) {
    /** The step to show: 0 turn on, 1 switch to it, 2 try it. */
    val step: Int get() = when {
        !enabled -> 0
        !selected -> 1
        else -> 2
    }

    companion object {
        fun read(context: Context): KeyboardStatus {
            val imm = context.getSystemService(InputMethodManager::class.java)
            val enabledIds = imm.enabledInputMethodList.map { it.id }
            val defaultId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            return from(context.packageName, enabledIds, defaultId)
        }

        /** Pure part, unit-tested. IDs are flattened component names ("pkg/.Service"). */
        fun from(packageName: String, enabledIds: List<String>, defaultId: String?): KeyboardStatus {
            fun ours(id: String?) = id != null && id.substringBefore('/') == packageName
            val enabled = enabledIds.any { ours(it) }
            return KeyboardStatus(enabled = enabled, selected = enabled && ours(defaultId))
        }
    }
}

/** Remembers that the user finished setup, so the launcher opens the home screen next time. */
object SetupPrefs {
    private const val PREFS = "setup"
    private const val KEY_DONE = "done"

    fun isDone(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_DONE, true) }
}
