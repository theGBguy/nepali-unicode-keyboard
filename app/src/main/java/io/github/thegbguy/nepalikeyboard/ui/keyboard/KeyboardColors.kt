package io.github.thegbguy.nepalikeyboard.ui.keyboard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Keyboard colours, built from the chosen [KeyboardTheme]'s shared [ThemePalette]. One accent per
 * theme (Enter, active shift, the mic while listening). With [flatKeys] the keys draw no fill or
 * shadow; panels still use [key] and [functionKey] for their tiles.
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
    val error: Color,
    val dark: Boolean,
    val flatKeys: Boolean = false
)

val Crimson = Color(0xFFB3122E)

fun keyboardColors(palette: ThemePalette, flatKeys: Boolean = false): KeyboardColors = KeyboardColors(
    background = Color(palette.background),
    key = Color(palette.key),
    functionKey = Color(palette.functionKey),
    keyText = Color(palette.keyText),
    hint = Color(palette.hint),
    accent = Color(palette.accent),
    onAccent = Color(palette.onAccent),
    divider = Color(palette.hint).copy(alpha = 0.2f),
    keyShadow = if (palette.dark || flatKeys) Color.Transparent else Color(0x1F000000),
    preview = Color(palette.preview),
    error = if (palette.dark) Color(0xFFFFB4AB) else Color(0xFFB3261E),
    dark = palette.dark,
    flatKeys = flatKeys
)

val LightKeyboardColors = keyboardColors(KeyboardThemes.LIGHT)
val DarkKeyboardColors = keyboardColors(KeyboardThemes.DARK)

/** The colours for the user's theme and key-border setting; System follows the phone. */
@Composable
fun keyboardColors(feel: KeyFeel = KeyFeel(), systemDark: Boolean = isSystemInDarkTheme()): KeyboardColors =
    keyboardColors(KeyboardThemes.palette(feel.theme, systemDark), flatKeys = !feel.keyBorders)
