package io.github.thegbguy.nepalikeyboard

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import io.github.thegbguy.nepalikeyboard.data.clipboard.FileClipboardStorage
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.locale.AppLanguage
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyFeel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.SharedKeyboardPrefs
import io.github.thegbguy.nepalikeyboard.ui.settings.LanguageDialog
import io.github.thegbguy.nepalikeyboard.ui.settings.LicencesScreen
import io.github.thegbguy.nepalikeyboard.ui.settings.SettingsLink
import io.github.thegbguy.nepalikeyboard.ui.settings.SettingsScreen
import io.github.thegbguy.nepalikeyboard.ui.setup.KeyboardStatus
import io.github.thegbguy.nepalikeyboard.ui.setup.PrivacyScreen
import io.github.thegbguy.nepalikeyboard.ui.setup.SetupPrefs
import io.github.thegbguy.nepalikeyboard.ui.setup.SetupScreen
import io.github.thegbguy.nepalikeyboard.ui.setup.WarningSheet
import io.github.thegbguy.nepalikeyboard.ui.theme.NepaliKeyboardTheme

class MainActivity : ComponentActivity() {

    private var status by mutableStateOf(KeyboardStatus(enabled = false, selected = false))
    private var setupDone by mutableStateOf(false)
    private var nepaliStyle by mutableStateOf(LanguageMode.TRANSLITERATION)
    private var feel by mutableStateOf(KeyFeel())
    private var clipboardHistory by mutableStateOf(true)
    private var appLanguage by mutableStateOf(AppLanguage.SYSTEM)
    private val keyboardPrefs by lazy { SharedKeyboardPrefs(this) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupDone = SetupPrefs.isDone(this)
        readPrefs()
        setContent {
            NepaliKeyboardTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
                var showWarning by rememberSaveable { mutableStateOf(false) }
                var showLanguages by rememberSaveable { mutableStateOf(false) }
                var tryText by rememberSaveable { mutableStateOf("") }

                BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }

                val onStyleChange = { style: LanguageMode ->
                    keyboardPrefs.nepaliStyle = style
                    keyboardPrefs.typeNepali = true
                    nepaliStyle = style
                }
                when {
                    screen == Screen.PRIVACY -> PrivacyScreen(onBack = { screen = Screen.MAIN })
                    screen == Screen.LICENCES -> LicencesScreen(onBack = { screen = Screen.MAIN })
                    setupDone -> SettingsScreen(
                        status = status,
                        nepaliStyle = nepaliStyle,
                        feel = feel,
                        appLanguage = appLanguage,
                        versionName = versionName(),
                        tryText = tryText,
                        onTryTextChange = { tryText = it },
                        onStyleChange = onStyleChange,
                        onHeightChange = { keyboardPrefs.keyHeight = it; feel = keyboardPrefs.feel },
                        onVibrateChange = { keyboardPrefs.vibrate = it; feel = keyboardPrefs.feel },
                        onSoundChange = { keyboardPrefs.sound = it; feel = keyboardPrefs.feel },
                        clipboardHistory = clipboardHistory,
                        onClipboardHistoryChange = { on ->
                            keyboardPrefs.clipboardHistory = on
                            clipboardHistory = on
                            if (!on) FileClipboardStorage.shared(this).clear()
                        },
                        onClearClipboard = { FileClipboardStorage.shared(this).clear() },
                        onPickLanguage = { showLanguages = true },
                        onLink = { link ->
                            when (link) {
                                SettingsLink.FIX_KEYBOARD -> if (status.enabled) showKeyboardPicker() else showWarning = true
                                SettingsLink.PRIVACY -> screen = Screen.PRIVACY
                                SettingsLink.LICENCES -> screen = Screen.LICENCES
                                SettingsLink.FEEDBACK -> sendFeedback()
                                SettingsLink.RATE -> openPlayStore()
                                SettingsLink.PRIVACY_POLICY -> open(PRIVACY_POLICY_URL)
                                SettingsLink.TERMS -> open(TERMS_URL)
                            }
                        }
                    )
                    else -> SetupScreen(
                        status = status,
                        nepaliStyle = nepaliStyle,
                        tryText = tryText,
                        onTryTextChange = { tryText = it },
                        onTurnOn = { showWarning = true },
                        onChooseKeyboard = ::showKeyboardPicker,
                        onStyleChange = onStyleChange,
                        onDone = {
                            SetupPrefs.markDone(this)
                            setupDone = true
                        },
                        onPrivacy = { screen = Screen.PRIVACY }
                    )
                }
                if (showWarning) {
                    WarningSheet(
                        onContinue = {
                            showWarning = false
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        },
                        onDismiss = { showWarning = false }
                    )
                }
                if (showLanguages) {
                    LanguageDialog(
                        current = appLanguage,
                        onPick = { tag ->
                            showLanguages = false
                            if (tag != appLanguage) {
                                AppLanguage.set(this, tag)
                                appLanguage = tag
                                // Android 13+ recreates the activity itself
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) recreate()
                            }
                        },
                        onDismiss = { showLanguages = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    // The keyboard picker is a dialog over this activity: focus returns here when it closes.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshStatus()
    }

    private fun refreshStatus() {
        status = KeyboardStatus.read(this)
        readPrefs()
    }

    private fun readPrefs() {
        nepaliStyle = keyboardPrefs.nepaliStyle
        feel = keyboardPrefs.feel
        clipboardHistory = keyboardPrefs.clipboardHistory
        appLanguage = AppLanguage.current(this)
    }

    private fun showKeyboardPicker() {
        getSystemService(InputMethodManager::class.java).showInputMethodPicker()
    }

    private fun versionName(): String =
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

    /** Opens the mail app with the version and device filled in, to help with bug reports. */
    private fun sendFeedback() {
        val body = "\n\n—\n${getString(R.string.settings_version, versionName())} · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}"
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
            .putExtra(Intent.EXTRA_TEXT, body)
        start(intent)
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (_: ActivityNotFoundException) {
            open("https://play.google.com/store/apps/details?id=$packageName")
        }
    }

    private fun open(url: String) = start(Intent(Intent.ACTION_VIEW, url.toUri()))

    private fun start(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }

    private enum class Screen { MAIN, PRIVACY, LICENCES }

    private companion object {
        const val SUPPORT_EMAIL = "nepali.indie.dev@gmail.com"
        // Published from the site repo; must exist before release (Play requires a privacy policy).
        const val PRIVACY_POLICY_URL = "https://nepali-indie-dev.github.io/nepali-keyboard/privacy-policy.html"
        const val TERMS_URL = "https://nepali-indie-dev.github.io/nepali-keyboard/terms-of-service.html"
    }
}
