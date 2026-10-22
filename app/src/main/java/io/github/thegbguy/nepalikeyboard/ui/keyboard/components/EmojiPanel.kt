package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.data.emoji.EmojiCategory
import io.github.thegbguy.nepalikeyboard.data.emoji.EmojiData
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyFeel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardColors
import kotlinx.coroutines.launch

/**
 * Emoji panel, drawn in place of the strip and keys at the same [height] so the keyboard doesn't
 * jump. One scrolling grid with a label per category; the bottom row has ABC, a tab per category
 * (follows the scroll, tap to jump) and backspace. Recents come first once there are any.
 */
@Composable
fun EmojiPanel(
    colors: KeyboardColors,
    height: Dp,
    bottomRowHeight: Dp,
    feel: KeyFeel,
    recents: List<String>,
    onEmoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceRepeatStart: () -> Unit,
    onBackspaceRepeatEnd: () -> Unit,
    onClose: () -> Unit
) {
    val data by EmojiData.categories.collectAsState()
    val sections = remember(data, recents) {
        buildList {
            if (recents.isNotEmpty()) add(EmojiCategory.RECENT to recents)
            EmojiCategory.entries.forEach { c -> data[c]?.takeIf { it.isNotEmpty() }?.let { add(c to it) } }
        }
    }
    // Grid index of each section's label: labels and emoji share one list
    val headerIndex = remember(sections) {
        var index = 0
        sections.associate { (category, list) -> (category to index).also { index += list.size + 1 } }
    }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val current by remember(headerIndex) {
        derivedStateOf {
            val first = gridState.firstVisibleItemIndex
            headerIndex.entries.lastOrNull { it.value <= first }?.key ?: sections.firstOrNull()?.first
        }
    }
    val view = LocalView.current

    Column(Modifier.fillMaxWidth().height(height)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(44.dp),
            state = gridState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            sections.forEach { (category, list) ->
                item(key = "h-${category.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    Text(
                        text = stringResource(category.label),
                        color = colors.hint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp)
                    )
                }
                items(list, key = { "${category.key}-$it" }, contentType = { "emoji" }) { emoji ->
                    Box(
                        Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (feel.vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onEmoji(emoji)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 27.sp, color = colors.keyText)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().height(bottomRowHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.functionKey)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("ABC", color = colors.keyText, fontSize = 15.sp)
            }
            Row(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                sections.forEach { (category, _) ->
                    val selected = category == current
                    val label = stringResource(category.label)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics { contentDescription = label }
                            .clickable {
                                scope.launch { gridState.scrollToItem(headerIndex.getValue(category)) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (selected) colors.functionKey else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(category.icon),
                                contentDescription = null,
                                tint = if (selected) colors.keyText else colors.hint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            BackspaceKey(colors, feel, onBackspace, onBackspaceRepeatStart, onBackspaceRepeatEnd)
        }
    }
}

/** Deletes on press and repeats while held, like the keyboard's own backspace. */
@Composable
internal fun BackspaceKey(
    colors: KeyboardColors,
    feel: KeyFeel,
    onTap: () -> Unit,
    onRepeatStart: () -> Unit,
    onRepeatEnd: () -> Unit
) {
    val view = LocalView.current
    val tap by rememberUpdatedState(onTap)
    val start by rememberUpdatedState(onRepeatStart)
    val end by rememberUpdatedState(onRepeatEnd)
    val description = stringResource(R.string.key_delete)
    Box(
        Modifier
            .width(56.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(7.dp))
            .background(colors.functionKey)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (feel.vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        tap()
                        tryAwaitRelease()
                        end()
                    },
                    onLongPress = { start() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(R.drawable.ic_ui_backspace), null, tint = colors.keyText, modifier = Modifier.size(22.dp))
    }
}

private val EmojiCategory.label: Int
    @StringRes get() = when (this) {
        EmojiCategory.RECENT -> R.string.emoji_recent
        EmojiCategory.SMILEYS -> R.string.emoji_smileys
        EmojiCategory.ANIMALS -> R.string.emoji_animals
        EmojiCategory.FOOD -> R.string.emoji_food
        EmojiCategory.ACTIVITIES -> R.string.emoji_activities
        EmojiCategory.TRAVEL -> R.string.emoji_travel
        EmojiCategory.OBJECTS -> R.string.emoji_objects
        EmojiCategory.SYMBOLS -> R.string.emoji_symbols
        EmojiCategory.FLAGS -> R.string.emoji_flags
    }

private val EmojiCategory.icon: Int
    @DrawableRes get() = when (this) {
        EmojiCategory.RECENT -> R.drawable.ic_ui_schedule
        EmojiCategory.SMILEYS -> R.drawable.ic_ui_mood
        EmojiCategory.ANIMALS -> R.drawable.ic_ui_pets
        EmojiCategory.FOOD -> R.drawable.ic_ui_restaurant
        EmojiCategory.ACTIVITIES -> R.drawable.ic_ui_sports_soccer
        EmojiCategory.TRAVEL -> R.drawable.ic_ui_directions_car
        EmojiCategory.OBJECTS -> R.drawable.ic_ui_lightbulb
        EmojiCategory.SYMBOLS -> R.drawable.ic_ui_emoji_symbols
        EmojiCategory.FLAGS -> R.drawable.ic_ui_flag
    }
