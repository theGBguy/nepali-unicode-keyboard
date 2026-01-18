package io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts

import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/**
 * Nepali keyboard layout provider.
 * Provides a phonetic/practical Devanagari layout optimized for Nepali typing.
 */
object NepaliLayout {

    /**
     * Creates and returns the standard Nepali keyboard layout.
     * Based on traditional Nepali Devanagari keyboard layout with Devanagari numbers on top row.
     */
    fun getLayout(): KeyboardLayout {
        val rows = listOf(
            // Row 1: Devanagari numbers (remain the same when shift is pressed)
            listOf(
                KeyData("१"),
                KeyData("२"),
                KeyData("३"),
                KeyData("४"),
                KeyData("५"),
                KeyData("६"),
                KeyData("७"),
                KeyData("८"),
                KeyData("९"),
                KeyData("०")
            ),

            // Row 2: Top character row
            listOf(
                KeyData("ज", "ज्ञ"),
                KeyData("घ", "घ्"),
                KeyData("ड", "ड्"),
                KeyData("ङ", "ङ्"),
                KeyData("छ", "छ्"),
                KeyData("ट", "ट्"),
                KeyData("ठ", "ठ्"),
                KeyData("ड", "ड्"),
                KeyData("ढ", "ढ्"),
                KeyData("ण", "ण्")
            ),

            // Row 3: Home row
            listOf(
                KeyData("ध", "्"),
                KeyData("भ", "भ्"),
                KeyData("च", "त्र"),
                KeyData("त", "ऐ"),
                KeyData("थ", "ए"),
                KeyData("ग", "ई"),
                KeyData("ष", "इ"),
                KeyData("य", "उं"),
                KeyData("उ", "ऊ")
            ),

            // Row 4: Third row
            listOf(
                KeyData("ब", "न्ह"),
                KeyData("क", "क्ष"),
                KeyData("म", "ओ"),
                KeyData("ा", "ो"),
                KeyData("न", "म्ह"),
                KeyData("ज", "ज्ञ"),
                KeyData("व", "ह्म"),
                KeyData("प", "श्र"),
                KeyData("ि", "ी"),
                KeyData("स", "अं")
            ),

            // Row 5: Shift row
            listOf(
                KeyData("⇧", keyType = KeyType.MODIFIER, flex = 1.3f),
                KeyData("श", "अः"),
                KeyData("ह", "ॐ"),
                KeyData("अ", "ऋ"),
                KeyData("ख", "आ"),
                KeyData("द", "त्त"),
                KeyData("ल", "द्ध"),
                KeyData("फ", "ै"),
                KeyData(",", "े"),
                KeyData("ु", "ू"),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.3f)
            ),

            // Row 6: Bottom function row
            listOf(
                KeyData("?123", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION),
                KeyData(" ", keyType = KeyType.ACTION, flex = 4f),
                KeyData("।", "॥"),
                KeyData("↵", keyType = KeyType.ACTION, flex = 1.2f)
            )
        )

        return KeyboardLayout(rows, LanguageMode.NEPALI)
    }

    /**
     * Returns the numbers/symbols variant of the Nepali layout.
     */
    fun getNumbersLayout(): KeyboardLayout {
        val rows = listOf(
            // Row 1: Arabic numerals (common international usage)
            listOf(
                KeyData("1"),
                KeyData("2"),
                KeyData("3"),
                KeyData("4"),
                KeyData("5"),
                KeyData("6"),
                KeyData("7"),
                KeyData("8"),
                KeyData("9"),
                KeyData("0")
            ),

            // Row 2: Common symbols
            listOf(
                KeyData("@"),
                KeyData("#"),
                KeyData("₹"),
                KeyData("&"),
                KeyData("*"),
                KeyData("-"),
                KeyData("+"),
                KeyData("("),
                KeyData(")"),
                KeyData("/")
            ),

            // Row 3: Punctuation
            listOf(
                KeyData("।"),
                KeyData("॥"),
                KeyData("!"),
                KeyData("?"),
                KeyData(":"),
                KeyData(";"),
                KeyData("\""),
                KeyData("'"),
                KeyData("=")
            ),

            // Row 4: More symbols
            listOf(
                KeyData("⇧", keyType = KeyType.MODIFIER, flex = 1.3f),
                KeyData("%"),
                KeyData("$"),
                KeyData("€"),
                KeyData("^"),
                KeyData("_"),
                KeyData("["),
                KeyData("]"),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.3f)
            ),

            // Row 5: Bottom row
            listOf(
                KeyData("ABC", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION),
                KeyData(" ", keyType = KeyType.ACTION, flex = 4f),
                KeyData(".", ","),
                KeyData("↵", keyType = KeyType.ACTION, flex = 1.2f)
            )
        )

        return KeyboardLayout(rows, LanguageMode.NEPALI)
    }
}
