package io.github.thegbguy.nepalikeyboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = CardTint,
    onPrimaryContainer = Crimson,
    secondary = InkMuted,
    onSecondary = Color.White,
    tertiary = SuccessGreen,
    error = Crimson,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Line,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Card,
    surfaceContainerLow = Card,
    surfaceContainer = Card,
    surfaceContainerHigh = Card,
    surfaceContainerHighest = Line,
    outline = Line,
    outlineVariant = Line
)

private val DarkColors = darkColorScheme(
    primary = CrimsonDark,
    onPrimary = Color.White,
    primaryContainer = CardTintDark,
    onPrimaryContainer = RoseDark,
    secondary = InkMutedDark,
    tertiary = SuccessGreenDark,
    error = RoseDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = LineDark,
    onSurfaceVariant = InkMutedDark,
    surfaceContainerLowest = CardDark,
    surfaceContainerLow = CardDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardDark,
    surfaceContainerHighest = LineDark,
    outline = LineDark,
    outlineVariant = LineDark
)

/** App theme: crimson accent on warm neutrals. Wallpaper (dynamic) colours are off on purpose. */
@Composable
fun NepaliKeyboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = Typography) {
        // Screens draw their own backgrounds, so set the default text and icon colour here;
        // without it, text outside a Surface falls back to black.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
