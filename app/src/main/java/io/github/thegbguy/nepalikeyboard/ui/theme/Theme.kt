package io.github.thegbguy.nepalikeyboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = CardTint,
    onPrimaryContainer = Crimson,
    secondary = InkMuted,
    onSecondary = Color.White,
    tertiary = SuccessGreen,
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
    onPrimary = Color(0xFF3B0710),
    primaryContainer = CardTintDark,
    onPrimaryContainer = CrimsonDark,
    secondary = InkMutedDark,
    tertiary = SuccessGreenDark,
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
