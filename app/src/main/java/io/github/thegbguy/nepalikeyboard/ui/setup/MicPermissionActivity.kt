package io.github.thegbguy.nepalikeyboard.ui.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.thegbguy.nepalikeyboard.locale.AppLanguage

/**
 * Transparent activity the keyboard opens the first time the mic is tapped: a keyboard service
 * can't show the permission dialog itself. If the user has blocked the permission ("don't ask
 * again"), it opens the app's system settings instead.
 */
class MicPermissionActivity : ComponentActivity() {

    private val request = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) && askedBefore) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        markAsked()
        onResult?.invoke(granted)
        finish()
    }

    private val askedBefore: Boolean
        get() = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    private fun markAsked() = getSharedPreferences(PREFS, MODE_PRIVATE).edit { putBoolean(KEY_ASKED, true) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            finish()
        } else if (savedInstanceState == null) {
            request.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    companion object {
        private const val PREFS = "mic_permission"
        private const val KEY_ASKED = "asked"

        /** Told whether the user allowed the mic, so the keyboard can start listening right away. Set by the IME service. */
        var onResult: ((Boolean) -> Unit)? = null

        fun start(context: Context) {
            context.startActivity(
                Intent(context, MicPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
