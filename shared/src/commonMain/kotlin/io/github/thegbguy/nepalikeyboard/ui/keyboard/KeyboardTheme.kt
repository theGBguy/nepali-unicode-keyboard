package io.github.thegbguy.nepalikeyboard.ui.keyboard

/**
 * The keyboard's colour themes. [SYSTEM] (the default) follows the phone's light/dark setting;
 * every other theme is fixed. Stored by [name], so a theme removed later falls back to [SYSTEM].
 */
enum class KeyboardTheme {
    SYSTEM, LIGHT, DARK, HIMALAYA, LALI_GURANS, TERAI, MIDNIGHT, DUSK, PAPER;

    companion object {
        fun fromName(name: String?): KeyboardTheme = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * One theme's colours as `0xAARRGGBB`, the same tokens on Android and iOS. One accent each
 * (Enter, active shift, the mic while listening). Divider, key shadow and error colours derive
 * from [dark].
 */
data class ThemePalette(
    val background: Long,
    val key: Long,
    val functionKey: Long,
    val keyText: Long,
    val hint: Long,
    val accent: Long,
    val onAccent: Long,
    /** The key-preview bubble and long-press popup. */
    val preview: Long,
    val dark: Boolean
)

object KeyboardThemes {

    /** The palette to draw for [theme]; [systemDark] only matters for [KeyboardTheme.SYSTEM]. */
    fun palette(theme: KeyboardTheme, systemDark: Boolean): ThemePalette = when (theme) {
        KeyboardTheme.SYSTEM -> if (systemDark) DARK else LIGHT
        KeyboardTheme.LIGHT -> LIGHT
        KeyboardTheme.DARK -> DARK
        KeyboardTheme.HIMALAYA -> HIMALAYA
        KeyboardTheme.LALI_GURANS -> LALI_GURANS
        KeyboardTheme.TERAI -> TERAI
        KeyboardTheme.MIDNIGHT -> MIDNIGHT
        KeyboardTheme.DUSK -> DUSK
        KeyboardTheme.PAPER -> PAPER
    }

    /** Warm neutrals with Nepal crimson: the keyboard's original look. */
    val LIGHT = ThemePalette(
        background = 0xFFECEAE7, key = 0xFFFFFFFF, functionKey = 0xFFD6D3CF, keyText = 0xFF1C1B1A,
        hint = 0xFF8D8883, accent = 0xFFB3122E, onAccent = 0xFFFFFFFF, preview = 0xFFFFFFFF, dark = false
    )
    val DARK = ThemePalette(
        background = 0xFF161514, key = 0xFF34312F, functionKey = 0xFF262423, keyText = 0xFFEEECE9,
        hint = 0xFF9A948E, accent = 0xFFC8263F, onAccent = 0xFFFFFFFF, preview = 0xFF45413E, dark = true
    )
    val HIMALAYA = ThemePalette(
        background = 0xFFE4E9EE, key = 0xFFFFFFFF, functionKey = 0xFFC9D3DD, keyText = 0xFF16202A,
        hint = 0xFF768594, accent = 0xFF2F5D8A, onAccent = 0xFFFFFFFF, preview = 0xFFFFFFFF, dark = false
    )
    val LALI_GURANS = ThemePalette(
        background = 0xFF1E0F14, key = 0xFF3A1E27, functionKey = 0xFF2B161D, keyText = 0xFFF5E9EC,
        hint = 0xFFB08F98, accent = 0xFFD33650, onAccent = 0xFFFFFFFF, preview = 0xFF4A2732, dark = true
    )
    val TERAI = ThemePalette(
        background = 0xFFE3E8E0, key = 0xFFFBFCF9, functionKey = 0xFFC9D2C3, keyText = 0xFF1A2118,
        hint = 0xFF768270, accent = 0xFF2E6B3A, onAccent = 0xFFFFFFFF, preview = 0xFFFBFCF9, dark = false
    )
    val MIDNIGHT = ThemePalette(
        background = 0xFF000000, key = 0xFF1C1C1E, functionKey = 0xFF111111, keyText = 0xFFF2F2F2,
        hint = 0xFF8E8E93, accent = 0xFFC8263F, onAccent = 0xFFFFFFFF, preview = 0xFF2C2C2E, dark = true
    )
    val DUSK = ThemePalette(
        background = 0xFF1A1A26, key = 0xFF2E2E42, functionKey = 0xFF232333, keyText = 0xFFECEBF5,
        hint = 0xFF9896B0, accent = 0xFF6E5BD8, onAccent = 0xFFFFFFFF, preview = 0xFF3B3B52, dark = true
    )
    /** High contrast: black on white. */
    val PAPER = ThemePalette(
        background = 0xFFF0F0F0, key = 0xFFFFFFFF, functionKey = 0xFFD9D9D9, keyText = 0xFF000000,
        hint = 0xFF4D4D4D, accent = 0xFF000000, onAccent = 0xFFFFFFFF, preview = 0xFFFFFFFF, dark = false
    )
}
