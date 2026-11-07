package io.github.thegbguy.nepalikeyboard.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardTheme
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardThemes
import io.github.thegbguy.nepalikeyboard.ui.keyboard.ThemePalette

private val SwatchShape = RoundedCornerShape(9.dp)

/** Three columns of mini keyboards, one per theme; the try-it field below shows the real thing. */
@Composable
fun ThemePicker(selected: KeyboardTheme, keyBorders: Boolean, onSelect: (KeyboardTheme) -> Unit) {
    val systemDark = isSystemInDarkTheme()
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeyboardTheme.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { theme ->
                    ThemeSwatch(theme, KeyboardThemes.palette(theme, systemDark), keyBorders, theme == selected) { onSelect(theme) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RowScope.ThemeSwatch(
    theme: KeyboardTheme,
    palette: ThemePalette,
    keyBorders: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .border(2.dp, ring, RoundedCornerShape(11.dp))
                .padding(3.dp)
        ) {
            MiniKeyboard(palette, keyBorders)
        }
        Text(
            stringResource(theme.label),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Two rows of letter keys and a bottom row ending in Enter, in the theme's colours. */
@Composable
private fun MiniKeyboard(palette: ThemePalette, keyBorders: Boolean) {
    val background = Color(palette.background)
    val key = if (keyBorders) Color(palette.key) else Color.Transparent
    val functionKey = if (keyBorders) Color(palette.functionKey) else Color.Transparent
    // Flat keys have no face: draw their letters as small marks so the swatch still reads as keys
    val mark = Color(palette.keyText).copy(alpha = 0.45f)
    val outline = MaterialTheme.colorScheme.outline
    Column(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(SwatchShape)
            .background(background)
            .border(1.dp, outline.copy(alpha = 0.5f), SwatchShape)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(2) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(if (row == 0) 7 else 6) { MiniKey(key, if (keyBorders) null else mark) }
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniKey(functionKey, if (keyBorders) null else mark)
            MiniKey(key, null, weight = 3f)
            MiniKey(Color(palette.accent), null, weight = 1.6f)
        }
    }
}

@Composable
private fun RowScope.MiniKey(face: Color, mark: Color?, weight: Float = 1f) {
    Box(
        Modifier.weight(weight).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(face),
        contentAlignment = Alignment.Center
    ) {
        if (mark != null) Box(Modifier.fillMaxWidth(0.4f).height(2.dp).background(mark, RoundedCornerShape(1.dp)))
    }
}

@get:StringRes
val KeyboardTheme.label: Int
    get() = when (this) {
        KeyboardTheme.SYSTEM -> R.string.theme_system
        KeyboardTheme.LIGHT -> R.string.theme_light
        KeyboardTheme.DARK -> R.string.theme_dark
        KeyboardTheme.HIMALAYA -> R.string.theme_himalaya
        KeyboardTheme.LALI_GURANS -> R.string.theme_lali_gurans
        KeyboardTheme.TERAI -> R.string.theme_terai
        KeyboardTheme.MIDNIGHT -> R.string.theme_midnight
        KeyboardTheme.DUSK -> R.string.theme_dusk
        KeyboardTheme.PAPER -> R.string.theme_paper
    }
