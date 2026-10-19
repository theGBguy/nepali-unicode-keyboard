package io.github.thegbguy.nepalikeyboard.ui.setup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

private val ScreenPadding = 22.dp

/**
 * One setup screen that follows [status]: turn the keyboard on, switch to it, then try it.
 * The privacy promise sits under the steps, before Android's warning appears.
 */
@Composable
fun SetupScreen(
    status: KeyboardStatus,
    nepaliStyle: LanguageMode,
    tryText: String,
    onTryTextChange: (String) -> Unit,
    onTurnOn: () -> Unit,
    onChooseKeyboard: () -> Unit,
    onStyleChange: (LanguageMode) -> Unit,
    onDone: () -> Unit,
    onPrivacy: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding)
        ) {
            AppNameRow(Modifier.padding(top = 14.dp, bottom = 22.dp))
            when (status.step) {
                2 -> TryItStep(nepaliStyle, tryText, onTryTextChange, onStyleChange)
                else -> {
                    Text(
                        text = stringResource(if (status.step == 0) R.string.setup_title else R.string.setup_title_almost),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(if (status.step == 0) R.string.setup_lede else R.string.setup_lede_almost),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(22.dp))
                    Steps(status.step)
                    if (status.step == 0) {
                        Spacer(Modifier.height(22.dp))
                        PrivacyPromise()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(horizontal = ScreenPadding).padding(bottom = 10.dp)) {
            PrimaryButton(
                text = stringResource(
                    when (status.step) {
                        0 -> R.string.setup_turn_on
                        1 -> R.string.setup_choose
                        else -> R.string.setup_done
                    }
                ),
                onClick = when (status.step) {
                    0 -> onTurnOn
                    1 -> onChooseKeyboard
                    else -> onDone
                }
            )
            TextButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_privacy_link), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AppNameRow(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("अ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.ime_name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Steps(current: Int) {
    val steps = listOf(
        R.string.setup_step1 to R.string.setup_step1_sub,
        R.string.setup_step2 to R.string.setup_step2_sub,
        R.string.setup_step3 to R.string.setup_step3_sub
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    steps.forEachIndexed { i, (title, sub) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            StepNumber(index = i, done = i < current, currentStep = i == current)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(if (i < current) R.string.setup_step_done else sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (i < current) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun StepNumber(index: Int, done: Boolean, currentStep: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (done) colors.tertiary else Color.Transparent)
            .border(1.5.dp, if (done) colors.tertiary else if (currentStep) colors.onSurface else colors.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(painterResource(R.drawable.ic_ui_check), null, tint = Color.White, modifier = Modifier.size(16.dp))
        } else {
            Text(
                "${index + 1}",
                fontSize = 12.sp,
                fontWeight = if (currentStep) FontWeight.SemiBold else FontWeight.Normal,
                color = if (currentStep) colors.onSurface else colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyPromise() {
    Text(
        stringResource(R.string.privacy_promise_title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
    PromiseLine(R.drawable.ic_ui_wifi_off, stringResource(R.string.privacy_promise_internet))
    PromiseLine(R.drawable.ic_ui_lock, stringResource(R.string.privacy_promise_password))
}

@Composable
private fun PromiseLine(@DrawableRes icon: Int, text: String) {
    Row(Modifier.padding(bottom = 8.dp)) {
        Icon(painterResource(icon), null, modifier = Modifier.size(18.dp).padding(top = 1.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TryItStep(
    style: LanguageMode,
    text: String,
    onTextChange: (String) -> Unit,
    onStyleChange: (LanguageMode) -> Unit
) {
    Text(stringResource(R.string.setup_try_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(16.dp))
    StyleChoice(style, onStyleChange)
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
        placeholder = {
            Text(stringResource(if (style == LanguageMode.NEPALI) R.string.setup_try_hint_traditional else R.string.setup_try_hint_romanized))
        },
        shape = RoundedCornerShape(14.dp)
    )
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(R.string.setup_try_tip),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Romanized (with an example) or the traditional layout. */
@Composable
fun StyleChoice(style: LanguageMode, onStyleChange: (LanguageMode) -> Unit) {
    SegmentedChoice(
        options = listOf(
            ChoiceOption(LanguageMode.TRANSLITERATION, stringResource(R.string.mode_romanized), stringResource(R.string.style_romanized_example)),
            ChoiceOption(LanguageMode.NEPALI, stringResource(R.string.style_traditional), stringResource(R.string.style_traditional_example))
        ),
        selected = style,
        onSelect = onStyleChange
    )
}

data class ChoiceOption<T>(val value: T, val title: String, val subtitle: String? = null)

/** A row of equal options in one outlined box; the selected one is tinted. */
@Composable
fun <T> SegmentedChoice(options: List<ChoiceOption<T>>, selected: T, onSelect: (T) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerLowest)
            .selectableGroup()
    ) {
        options.forEachIndexed { i, option ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(colors.outline))
            val isSelected = option.value == selected
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) colors.primaryContainer else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option.value) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                )
                option.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Shown once, before opening Android's keyboard settings: quotes the warning Android shows for
 * every keyboard and explains why it doesn't apply here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningSheet(onContinue: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(Modifier.padding(horizontal = ScreenPadding).padding(bottom = 16.dp)) {
            Text(stringResource(R.string.warning_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.warning_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.warning_android_says), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.warning_quote), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.warning_explain), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.warning_continue), onContinue)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.warning_not_now), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** What the keyboard can and can't do with your data. Every statement is backed by code or the manifest. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
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
            Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = ScreenPadding)) {
            Text(
                stringResource(R.string.privacy_heading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )
            listOf(
                Triple(R.drawable.ic_ui_wifi_off, R.string.privacy_internet_title, R.string.privacy_internet_body),
                Triple(R.drawable.ic_ui_lock, R.string.privacy_password_title, R.string.privacy_password_body),
                Triple(R.drawable.ic_ui_smartphone, R.string.privacy_device_title, R.string.privacy_device_body),
                Triple(R.drawable.ic_ui_mic, R.string.privacy_voice_title, R.string.privacy_voice_body)
            ).forEachIndexed { i, (icon, title, body) ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(Modifier.padding(vertical = 14.dp)) {
                    Icon(painterResource(icon), null, modifier = Modifier.size(20.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
