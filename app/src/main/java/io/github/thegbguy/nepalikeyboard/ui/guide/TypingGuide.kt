package io.github.thegbguy.nepalikeyboard.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.thegbguy.nepalikeyboard.R
import io.github.thegbguy.nepalikeyboard.domain.transliteration.RomanizedGuide

/** The full Romanized chart, opened from Settings and from the setup try-it step. */
@Composable
fun TypingGuideScreen(onBack: () -> Unit) {
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
            Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.guide_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            RomanizedGuide.sections.forEach { section ->
                Text(
                    stringResource(section.kind.title()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp)
                )
                GuideGrid(section.entries)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A few common rows under the try-it field, with a link to the full chart. */
@Composable
fun CompactGuide(onOpenGuide: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        GuideGrid(RomanizedGuide.compact, minCell = 64)
        TextButton(onClick = onOpenGuide, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(R.string.guide_open))
        }
    }
}

/** Entries in an outlined grid: the Nepali letter large, what to type under it. */
@Composable
private fun GuideGrid(entries: List<RomanizedGuide.Entry>, minCell: Int = 76) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(vertical = 6.dp)
    ) {
        val columns = (maxWidth.value / minCell).toInt().coerceIn(3, 8)
        Column {
            entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    row.forEach { GuideCell(it, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun GuideCell(entry: RomanizedGuide.Entry, modifier: Modifier) {
    val typed = entry.typed.joinToString("/")
    Column(
        modifier
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .clearAndSetSemantics { contentDescription = "$typed: ${entry.nepali}" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(entry.nepali, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            typed,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

private fun RomanizedGuide.Kind.title(): Int = when (this) {
    RomanizedGuide.Kind.VOWELS -> R.string.guide_vowels
    RomanizedGuide.Kind.CONSONANTS -> R.string.guide_consonants
    RomanizedGuide.Kind.SIGNS -> R.string.guide_signs
    RomanizedGuide.Kind.MARKS -> R.string.guide_marks
}
