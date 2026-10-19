package io.github.thegbguy.nepalikeyboard.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * The app's UI language: follow the phone, or force English or Nepali.
 *
 * Android 13+ handles per-app languages itself (LocaleManager + locales_config.xml), including the
 * keyboard service. Older versions have no such setting, so the choice is stored here and applied
 * by wrapping each Activity's and the IME service's base context in [wrap].
 */
object AppLanguage {

    /** Language tags the app is translated into. An empty tag means "follow the phone". */
    const val SYSTEM = ""
    const val ENGLISH = "en"
    const val NEPALI = "ne"

    private const val PREFS = "app_language"
    private const val KEY_TAG = "tag"

    /** The chosen language tag, or [SYSTEM]. */
    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (locales.isEmpty) SYSTEM else locales[0].language
        }
        return prefs(context).getString(KEY_TAG, SYSTEM) ?: SYSTEM
    }

    /**
     * Saves the choice. On Android 13+ the system recreates running activities; on older versions
     * the caller must recreate its activity, and the keyboard picks the language up the next time
     * its service starts.
     */
    fun set(context: Context, tag: String) {
        prefs(context).edit { putString(KEY_TAG, tag) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        }
    }

    /** Use from `attachBaseContext`. A no-op on Android 13+ or when following the phone. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY_TAG, SYSTEM)
        if (tag.isNullOrEmpty()) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
