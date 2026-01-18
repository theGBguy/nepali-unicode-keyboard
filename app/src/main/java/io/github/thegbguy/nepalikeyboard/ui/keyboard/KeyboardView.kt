package io.github.thegbguy.nepalikeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType

/**
 * Main keyboard view composable.
 */
@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E) // Dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            // Language indicator bar
            LanguageIndicator(
                currentLanguage = state.currentLanguage.displayName(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Keyboard rows
            state.currentLayout.rows.forEach { row ->
                KeyboardRow(
                    keys = row,
                    isShifted = state.isShifted,
                    onKeyClick = { key -> viewModel.onKeyClick(key) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Language indicator at the top of keyboard.
 */
@Composable
fun LanguageIndicator(
    currentLanguage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentLanguage,
            color = Color(0xFF9E9E9E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * A single row of keys.
 */
@Composable
fun KeyboardRow(
    keys: List<KeyData>,
    isShifted: Boolean,
    onKeyClick: (KeyData) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            Key(
                keyData = key,
                isShifted = isShifted,
                onClick = { onKeyClick(key) },
                modifier = Modifier.weight(key.flex)
            )
        }
    }
}

/**
 * Individual key composable.
 */
@Composable
fun Key(
    keyData: KeyData,
    isShifted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (keyData.keyType) {
        KeyType.CHARACTER -> Color(0xFF3A3A3A)
        KeyType.ACTION -> Color(0xFF0A84FF) // iOS-style blue for action keys
        KeyType.MODIFIER -> Color(0xFF3A3A3A)
        KeyType.FUNCTION -> Color(0xFF2C2C2C)
        KeyType.SPACER -> Color.Transparent
    }

    val textColor = when (keyData.keyType) {
        KeyType.ACTION -> Color.White
        else -> Color(0xFFE5E5E5)
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(enabled = keyData.keyType != KeyType.SPACER) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (keyData.keyType != KeyType.SPACER) {
            val displayText = when {
                keyData.keyType == KeyType.MODIFIER && isShifted -> keyData.primary
                keyData.keyType == KeyType.CHARACTER && isShifted && keyData.shift != null -> keyData.shift
                else -> keyData.primary
            }

            Text(
                text = displayText,
                color = textColor,
                fontSize = when (keyData.keyType) {
                    KeyType.CHARACTER -> 20.sp
                    KeyType.FUNCTION -> 16.sp
                    else -> 18.sp
                },
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
