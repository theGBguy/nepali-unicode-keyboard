package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipItem
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardHistory
import io.github.thegbguy.nepalikeyboard.domain.clipboard.CopiedText
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyFeel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardColors
import kotlinx.coroutines.delay

private val RowShape = RoundedCornerShape(8.dp)

/**
 * Clipboard panel, drawn in place of the strip and keys at the same [height]. A staggered grid
 * of tiles (two columns on a phone; each tile as tall as its text, up to 4 lines): the phone's
 * current copy first, then pinned items (pin icon), then recent ones. Tap pastes; long-press
 * shows pin and delete on that tile. A "Clipboard" title sits on top; the bottom row has ABC,
 * Clear all (tap twice; pinned items stay) and backspace.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardPanel(
    colors: KeyboardColors,
    height: Dp,
    bottomRowHeight: Dp,
    feel: KeyFeel,
    copied: CopiedText?,
    items: List<ClipItem>,
    onPaste: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onBackspace: () -> Unit,
    onBackspaceRepeatStart: () -> Unit,
    onBackspaceRepeatEnd: () -> Unit,
    onClose: () -> Unit
) {
    var actionsFor by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    LaunchedEffect(confirmClear) {
        if (confirmClear) {
            delay(3_000)
            confirmClear = false
        }
    }
    val copiedItem = copied?.let { c -> items.firstOrNull { it.text == c.text } }
    val others = items.filter { it.text != copied?.text }
    val pinned = others.filter { it.pinned }
    val recent = others.filter { !it.pinned }
    val pinsFull = ClipboardHistory.pinnedCount(items) >= ClipboardHistory.MAX_PINNED
    val view = LocalView.current
    val paste = { text: String ->
        if (feel.vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        actionsFor = null
        onPaste(text)
    }

    val title = stringResource(R.string.clipboard_title)
    val letters = stringResource(R.string.key_letters)
    Column(Modifier.fillMaxWidth().height(height).semantics { paneTitle = title }) {
        Text(
            title,
            color = colors.keyText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 10.dp).semantics { heading() }
        )
        if (copied == null && items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.clipboard_empty),
                    color = colors.hint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalItemSpacing = 6.dp
            ) {
                if (copied != null) {
                    item(key = "copied") {
                        ClipTile(
                            colors = colors,
                            text = if (copied.sensitive) stringResource(R.string.clipboard_hidden) else copied.text,
                            muted = copied.sensitive,
                            pinned = copiedItem?.pinned == true,
                            showActions = actionsFor == copied.text && copiedItem != null,
                            pinEnabled = copiedItem?.pinned == true || !pinsFull,
                            onClick = { paste(copied.text) },
                            onLongClick = if (copiedItem != null) ({ actionsFor = copied.text }) else null,
                            onPin = { onPin(copied.text, copiedItem?.pinned != true); actionsFor = null },
                            onDelete = { onDelete(copied.text); actionsFor = null }
                        )
                    }
                }
                items(pinned + recent, key = { "i-${it.text.hashCode()}-${it.time}" }) { item ->
                        ClipTile(
                            colors = colors,
                            text = item.text,
                            muted = false,
                            pinned = item.pinned,
                            showActions = actionsFor == item.text,
                            pinEnabled = item.pinned || !pinsFull,
                            onClick = { paste(item.text) },
                            onLongClick = { actionsFor = item.text },
                            onPin = { onPin(item.text, !item.pinned); actionsFor = null },
                            onDelete = { onDelete(item.text); actionsFor = null }
                        )
                }
                item(key = "end", span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(4.dp)) }
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
                    .clickable(onClick = onClose)
                    .clearAndSetSemantics { contentDescription = letters; onClick { onClose(); true } },
                contentAlignment = Alignment.Center
            ) {
                Text("ABC", color = colors.keyText, fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
            // Shown while anything would go: unpinned history or an unpinned current copy
            if (items.any { !it.pinned } || (copied != null && copiedItem?.pinned != true)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .clickable {
                            if (confirmClear) {
                                confirmClear = false
                                actionsFor = null
                                onClearAll()
                            } else {
                                confirmClear = true
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(if (confirmClear) R.string.clipboard_clear_confirm else R.string.clipboard_clear_all),
                        // "Tap again to clear" is announced when it appears
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = if (confirmClear) colors.error else colors.keyText,
                        fontSize = 14.sp
                    )
                }
            }
            BackspaceKey(colors, feel, onBackspace, onBackspaceRepeatStart, onBackspaceRepeatEnd)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipTile(
    colors: KeyboardColors,
    text: String,
    muted: Boolean,
    pinned: Boolean,
    showActions: Boolean,
    pinEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val pasteLabel = stringResource(R.string.clipboard_paste)
    val pinLabel = stringResource(if (pinned) R.string.clipboard_unpin else R.string.clipboard_pin)
    val deleteLabel = stringResource(R.string.clipboard_delete)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(colors.key)
            .combinedClickable(onClickLabel = pasteLabel, onClick = onClick, onLongClick = onLongClick)
            // Pin and delete as TalkBack actions, instead of long-press then the small buttons
            .semantics {
                if (onLongClick != null) {
                    customActions = listOfNotNull(
                        if (pinEnabled) CustomAccessibilityAction(pinLabel) { onPin(); true } else null,
                        CustomAccessibilityAction(deleteLabel) { onDelete(); true }
                    )
                }
            }
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.Top) {
            val pinMark = pinned && !showActions
            Column(Modifier.weight(1f).padding(start = 12.dp, end = if (pinMark) 0.dp else 12.dp, top = 10.dp, bottom = 10.dp)) {
                Text(
                    text = withNepaliSpeech(text),
                    color = if (muted) colors.hint else colors.keyText,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (pinMark) {
                Icon(
                    painterResource(R.drawable.ic_ui_keep),
                    contentDescription = stringResource(R.string.clipboard_pinned),
                    tint = colors.hint,
                    modifier = Modifier.padding(top = 11.dp, start = 4.dp, end = 8.dp).size(14.dp)
                )
            }
        }
        if (showActions) {
            Row(
                Modifier.fillMaxWidth().padding(end = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                RowAction(
                    colors,
                    R.drawable.ic_ui_keep,
                    if (pinned) R.string.clipboard_unpin else R.string.clipboard_pin,
                    active = pinned,
                    enabled = pinEnabled,
                    onClick = onPin
                )
                RowAction(colors, R.drawable.ic_ui_delete, R.string.clipboard_delete, active = false, enabled = true, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun RowAction(
    colors: KeyboardColors,
    @DrawableRes icon: Int,
    @StringRes label: Int,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(icon),
            contentDescription = stringResource(label),
            tint = when {
                !enabled -> colors.divider
                active -> colors.accent
                else -> colors.keyText
            },
            modifier = Modifier.size(20.dp)
        )
    }
}
