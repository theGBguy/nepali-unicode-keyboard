package io.github.thegbguy.nepalikeyboard.ui.keyboard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Keyboard colours. Warm neutrals with Nepal crimson as the only accent (Enter, active shift,
 * the mic while listening), following the system light/dark setting.
 */
@Immutable
data class KeyboardColors(
    val background: Color,
    val key: Color,
    val functionKey: Color,
    val keyText: Color,
    val hint: Color,
    val accent: Color,
    val onAccent: Color,
    val divider: Color,
    val keyShadow: Color,
    val preview: Color,
    val error: Color
)

val Crimson = Color(0xFFB3122E)
private val CrimsonDark = Color(0xFFC8263F)

val LightKeyboardColors = KeyboardColors(
    background = Color(0xFFECEAE7),
    key = Color(0xFFFFFFFF),
    functionKey = Color(0xFFD6D3CF),
    keyText = Color(0xFF1C1B1A),
    hint = Color(0xFF8D8883),
    accent = Crimson,
    onAccent = Color.White,
    divider = Color(0x338D8883),
    keyShadow = Color(0x1F000000),
    preview = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E)
)

val DarkKeyboardColors = KeyboardColors(
    background = Color(0xFF161514),
    key = Color(0xFF34312F),
    functionKey = Color(0xFF262423),
    keyText = Color(0xFFEEECE9),
    hint = Color(0xFF9A948E),
    accent = CrimsonDark,
    onAccent = Color.White,
    divider = Color(0x339A948E),
    keyShadow = Color.Transparent,
    preview = Color(0xFF45413E),
    error = Color(0xFFFFB4AB)
)

@Composable
fun keyboardColors(dark: Boolean = isSystemInDarkTheme()): KeyboardColors =
    if (dark) DarkKeyboardColors else LightKeyboardColors
