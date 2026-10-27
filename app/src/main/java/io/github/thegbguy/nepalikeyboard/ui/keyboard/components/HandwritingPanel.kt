package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import io.github.thegbguy.nepalikeyboard.domain.handwriting.InkPoint
import io.github.thegbguy.nepalikeyboard.domain.handwriting.InkStroke
import io.github.thegbguy.nepalikeyboard.ui.keyboard.EnterKeyLabel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.HandwritingPhase
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyFeel
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardColors

private val KeyShape = RoundedCornerShape(7.dp)

/**
 * Handwriting, drawn in place of the strip and keys at the same [height]: candidates on top
 * (best in bold), the writing area, and a bottom row with ABC, ।, space, ?, backspace and Enter.
 * Before the user has agreed, the writing area explains what turning handwriting on means.
 */
@Composable
fun HandwritingPanel(
    colors: KeyboardColors,
    height: Dp,
    bottomRowHeight: Dp,
    feel: KeyFeel,
    enter: EnterKeyLabel,
    phase: HandwritingPhase,
    strokes: List<InkStroke>,
    candidates: List<String>,
    onAccept: () -> Unit,
    onRetry: () -> Unit,
    onStroke: (InkStroke, Float, Float) -> Unit,
    onCandidate: (String) -> Unit,
    onText: (String) -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onBackspace: () -> Unit,
    onBackspaceRepeatStart: () -> Unit,
    onBackspaceRepeatEnd: () -> Unit,
    onClose: () -> Unit
) {
    val view = LocalView.current
    val tap = { action: () -> Unit ->
        if (feel.vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        action()
    }
    Column(Modifier.fillMaxWidth().height(height)) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            candidates.take(4).forEachIndexed { index, text ->
                if (index > 0) Box(Modifier.width(1.dp).height(18.dp).background(colors.divider))
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { tap { onCandidate(text) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text,
                        color = colors.keyText,
                        fontSize = 17.sp,
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.key)
        ) {
            when (phase) {
                HandwritingPhase.READY -> WritingArea(colors, strokes, onStroke)
                HandwritingPhase.CONSENT -> Consent(colors, onAccept = { tap(onAccept) }, onDecline = { tap(onClose) })
                HandwritingPhase.CHECKING -> Unit
                HandwritingPhase.DOWNLOADING -> Message(colors, stringResource(R.string.handwriting_downloading)) {
                    LinearProgressIndicator(color = colors.accent, trackColor = colors.divider, modifier = Modifier.width(160.dp))
                }
                HandwritingPhase.FAILED -> Message(colors, stringResource(R.string.handwriting_failed)) {
                    PanelButton(colors, stringResource(R.string.handwriting_retry), primary = true) { tap(onRetry) }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(bottomRowHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BottomKey(colors, Modifier.width(56.dp), onClick = { tap(onClose) }) { Text("ABC", color = colors.keyText, fontSize = 15.sp) }
            BottomKey(colors, Modifier.width(40.dp), onClick = { tap { onText("।") } }) { Text("।", color = colors.keyText, fontSize = 18.sp) }
            BottomKey(colors, Modifier.weight(1f), background = colors.key, onClick = { tap(onSpace) }) {}
            BottomKey(colors, Modifier.width(40.dp), onClick = { tap { onText("?") } }) { Text("?", color = colors.keyText, fontSize = 18.sp) }
            BackspaceKey(colors, feel, onBackspace, onBackspaceRepeatStart, onBackspaceRepeatEnd)
            BottomKey(colors, Modifier.width(56.dp), background = colors.accent, onClick = { tap(onEnter) }) {
                if (enter.icon != null) {
                    Icon(painterResource(enter.icon), enter.description, tint = colors.onAccent, modifier = Modifier.size(22.dp))
                } else {
                    Text(enter.text.orEmpty(), color = colors.onAccent, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

/** Draws finished strokes plus the one under the finger, and reports each finished stroke. */
@Composable
private fun WritingArea(colors: KeyboardColors, strokes: List<InkStroke>, onStroke: (InkStroke, Float, Float) -> Unit) {
    val current = remember { mutableStateListOf<InkPoint>() }
    Box(Modifier.fillMaxSize()) {
        if (strokes.isEmpty() && current.isEmpty()) {
            Text(
                stringResource(R.string.handwriting_hint),
                color = colors.hint,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        current.clear()
                        current += InkPoint(down.position.x, down.position.y, down.uptimeMillis)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.historical.forEach { current += InkPoint(it.position.x, it.position.y, it.uptimeMillis) }
                            current += InkPoint(change.position.x, change.position.y, change.uptimeMillis)
                            change.consume()
                            if (!change.pressed) break
                        }
                        val stroke = current.toList()
                        current.clear()
                        onStroke(stroke, size.width.toFloat(), size.height.toFloat())
                    }
                }
        ) {
            val style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            (strokes + listOf(current.toList())).forEach { stroke ->
                if (stroke.isEmpty()) return@forEach
                val path = Path().apply {
                    moveTo(stroke[0].x, stroke[0].y)
                    stroke.drop(1).forEach { lineTo(it.x, it.y) }
                }
                if (stroke.size == 1) drawCircle(colors.keyText, 2.dp.toPx(), Offset(stroke[0].x, stroke[0].y))
                else drawPath(path, colors.keyText, style = style)
            }
        }
    }
}

/** What turning handwriting on means, before anything is downloaded. */
@Composable
private fun Consent(colors: KeyboardColors, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.handwriting_title), color = colors.keyText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        // Scrolls on short key heights, so the buttons always show
        Text(
            stringResource(R.string.handwriting_consent),
            color = colors.hint,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            PanelButton(colors, stringResource(R.string.handwriting_not_now), primary = false, onClick = onDecline)
            Spacer(Modifier.width(8.dp))
            PanelButton(colors, stringResource(R.string.handwriting_download), primary = true, onClick = onAccept)
        }
    }
}

@Composable
private fun Message(colors: KeyboardColors, text: String, below: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = colors.keyText, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        below()
    }
}

@Composable
private fun PanelButton(colors: KeyboardColors, text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) colors.onAccent else colors.keyText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowScope.BottomKey(
    colors: KeyboardColors,
    modifier: Modifier,
    background: androidx.compose.ui.graphics.Color = colors.functionKey,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier.fillMaxHeight().clip(KeyShape).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
