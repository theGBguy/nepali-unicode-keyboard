package io.github.thegbguy.nepalikeyboard.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.locale.AppLanguage
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyFeel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyHeight
import io.github.thegbguy.nepalikeyboard.ui.setup.AppNameRow
import io.github.thegbguy.nepalikeyboard.ui.setup.ChoiceOption
import io.github.thegbguy.nepalikeyboard.ui.setup.KeyboardStatus
import io.github.thegbguy.nepalikeyboard.ui.setup.SegmentedChoice
import io.github.thegbguy.nepalikeyboard.ui.setup.StyleChoice

private val ScreenPadding = 20.dp
private val CardShape = RoundedCornerShape(20.dp)

/** Everything a settings row can open; MainActivity turns these into intents and screens. */
enum class SettingsLink { FIX_KEYBOARD, PRIVACY, FEEDBACK, RATE, PRIVACY_POLICY, TERMS, LICENCES }

/**
 * The app's home once setup is done: keyboard status, typing settings with a field to try them,
 * app language, and help and legal links. Follows the ProperPdf pattern: section label, one
 * outlined card per section, monochrome icons, value subtitles.
 */
@Composable
fun SettingsScreen(
    status: KeyboardStatus,
    nepaliStyle: LanguageMode,
    feel: KeyFeel,
    appLanguage: String,
    versionName: String,
    tryText: String,
    onTryTextChange: (String) -> Unit,
    onStyleChange: (LanguageMode) -> Unit,
    onHeightChange: (KeyHeight) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onPickLanguage: () -> Unit,
    onLink: (SettingsLink) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
    ) {
        AppNameRow(Modifier.padding(top = 14.dp, bottom = 18.dp))

        val ready = status.step == 2
        SettingsCard {
            LinkRow(
                icon = R.drawable.ic_ui_keyboard,
                title = stringResource(R.string.ready_keyboard),
                subtitle = stringResource(if (ready) R.string.ready_on else R.string.ready_off),
                subtitleColor = if (ready) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                onClick = if (ready) null else ({ onLink(SettingsLink.FIX_KEYBOARD) })
            )
        }

        Section(stringResource(R.string.settings_section_typing)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_nepali_typing), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                StyleChoice(nepaliStyle, onStyleChange)
            }
            RowDivider(inset = 16.dp)
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_key_height), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                SegmentedChoice(
                    options = listOf(
                        ChoiceOption(KeyHeight.SHORT, stringResource(R.string.settings_height_short)),
                        ChoiceOption(KeyHeight.NORMAL, stringResource(R.string.settings_height_normal)),
                        ChoiceOption(KeyHeight.TALL, stringResource(R.string.settings_height_tall))
                    ),
                    selected = feel.height,
                    onSelect = onHeightChange
                )
            }
            RowDivider(inset = 16.dp)
            SwitchRow(R.drawable.ic_ui_vibration, stringResource(R.string.settings_vibrate), feel.vibrate, onVibrateChange)
            RowDivider()
            SwitchRow(R.drawable.ic_ui_volume_up, stringResource(R.string.settings_sound), feel.sound, onSoundChange)
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tryText,
            onValueChange = onTryTextChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
            placeholder = { Text(stringResource(R.string.settings_try_hint)) },
            shape = RoundedCornerShape(14.dp)
        )

        Section(stringResource(R.string.settings_section_app)) {
            LinkRow(
                icon = R.drawable.ic_ui_translate,
                title = stringResource(R.string.settings_app_language),
                subtitle = languageName(appLanguage),
                onClick = onPickLanguage
            )
        }

        Section(stringResource(R.string.settings_section_help)) {
            LinkRow(R.drawable.ic_ui_shield, stringResource(R.string.privacy_title), stringResource(R.string.settings_privacy_sub)) { onLink(SettingsLink.PRIVACY) }
            RowDivider()
            LinkRow(R.drawable.ic_ui_mail, stringResource(R.string.settings_feedback), stringResource(R.string.settings_feedback_sub)) { onLink(SettingsLink.FEEDBACK) }
            RowDivider()
            LinkRow(R.drawable.ic_ui_star, stringResource(R.string.settings_rate), stringResource(R.string.settings_rate_sub)) { onLink(SettingsLink.RATE) }
        }

        Section(stringResource(R.string.settings_section_legal)) {
            LinkRow(R.drawable.ic_ui_policy, stringResource(R.string.settings_privacy_policy), null) { onLink(SettingsLink.PRIVACY_POLICY) }
            RowDivider()
            LinkRow(R.drawable.ic_ui_gavel, stringResource(R.string.settings_terms), null) { onLink(SettingsLink.TERMS) }
            RowDivider()
            LinkRow(R.drawable.ic_ui_info, stringResource(R.string.settings_licences), stringResource(R.string.settings_version, versionName)) { onLink(SettingsLink.LICENCES) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun languageName(tag: String) = stringResource(
    when (tag) {
        AppLanguage.ENGLISH -> R.string.language_english
        AppLanguage.NEPALI -> R.string.language_nepali
        else -> R.string.language_system
    }
)

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp)
    )
    SettingsCard(content)
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        content = content
    )
}

@Composable
private fun RowDivider(inset: Dp = 56.dp) = HorizontalDivider(Modifier.padding(start = inset), color = MaterialTheme.colorScheme.outline)

@Composable
private fun LinkRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String?,
    subtitleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = subtitleColor) }
        }
        if (onClick != null) {
            Icon(painterResource(R.drawable.ic_ui_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(@DrawableRes icon: Int, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        )
    }
}

/** Follow the phone, English or Nepali. Each option is named in its own language. */
@Composable
fun LanguageDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_app_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.language_system),
                    AppLanguage.ENGLISH to stringResource(R.string.language_english),
                    AppLanguage.NEPALI to stringResource(R.string.language_nepali)
                ).forEach { (tag, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .selectable(selected = tag == current, role = Role.RadioButton) { onPick(tag) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = tag == current, onClick = null, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    )
}

/** Credits and licences. The word list's CC BY-SA 4.0 attribution is required. */
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_ui_arrow_back), stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings_licences), style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            listOf(
                R.string.licences_words_title to R.string.licences_words_body,
                R.string.licences_libraries_title to R.string.licences_libraries_body
            ).forEachIndexed { i, (title, body) ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
