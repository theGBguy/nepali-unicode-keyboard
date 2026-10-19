package io.github.thegbguy.nepalikeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.locale.AppLanguage
import io.github.thegbguy.nepalikeyboard.ui.keyboard.SharedKeyboardPrefs
import io.github.thegbguy.nepalikeyboard.ui.setup.KeyboardStatus
import io.github.thegbguy.nepalikeyboard.ui.setup.PrivacyScreen
import io.github.thegbguy.nepalikeyboard.ui.setup.ReadyScreen
import io.github.thegbguy.nepalikeyboard.ui.setup.SetupPrefs
import io.github.thegbguy.nepalikeyboard.ui.setup.SetupScreen
import io.github.thegbguy.nepalikeyboard.ui.setup.WarningSheet
import io.github.thegbguy.nepalikeyboard.ui.theme.NepaliKeyboardTheme

class MainActivity : ComponentActivity() {

    private var status by mutableStateOf(KeyboardStatus(enabled = false, selected = false))
    private var setupDone by mutableStateOf(false)
    private var nepaliStyle by mutableStateOf(LanguageMode.TRANSLITERATION)
    private val keyboardPrefs by lazy { SharedKeyboardPrefs(this) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupDone = SetupPrefs.isDone(this)
        nepaliStyle = keyboardPrefs.nepaliStyle
        setContent {
            NepaliKeyboardTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
                var showWarning by rememberSaveable { mutableStateOf(false) }
                var tryText by rememberSaveable { mutableStateOf("") }

                BackHandler(enabled = screen == Screen.PRIVACY) { screen = Screen.MAIN }

                val onStyleChange = { style: LanguageMode ->
                    keyboardPrefs.nepaliStyle = style
                    keyboardPrefs.typeNepali = true
                    nepaliStyle = style
                }
                when {
                    screen == Screen.PRIVACY -> PrivacyScreen(onBack = { screen = Screen.MAIN })
                    setupDone -> ReadyScreen(
                        status = status,
                        nepaliStyle = nepaliStyle,
                        tryText = tryText,
                        onTryTextChange = { tryText = it },
                        onStyleChange = onStyleChange,
                        onFix = { if (status.enabled) showKeyboardPicker() else showWarning = true },
                        onPrivacy = { screen = Screen.PRIVACY }
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
        nepaliStyle = keyboardPrefs.nepaliStyle
    }

    private fun showKeyboardPicker() {
        getSystemService(InputMethodManager::class.java).showInputMethodPicker()
    }

    private enum class Screen { MAIN, PRIVACY }
}
