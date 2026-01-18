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
     * This is a phonetic layout where characters are grouped logically.
     */
    fun getLayout(): KeyboardLayout {
        val rows = listOf(
            // Row 1: Common vowels and consonants
            listOf(
                KeyData("अ", "आ", listOf("आ", "इ", "ई")),
                KeyData("क", "ख", listOf("ख", "ग", "घ", "ङ")),
                KeyData("च", "छ", listOf("छ", "ज", "झ", "ञ")),
                KeyData("त", "थ", listOf("थ", "द", "ध")),
                KeyData("न"),
                KeyData("प", "फ", listOf("फ", "ब", "भ", "म")),
                KeyData("य"),
                KeyData("र"),
                KeyData("ल"),
                KeyData("व")
            ),

            // Row 2: More consonants and matras
            listOf(
                KeyData("ा", "ि", listOf("ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ")),
                KeyData("स", "श", listOf("श", "ष")),
                KeyData("ह"),
                KeyData("क्ष", "त्र", listOf("त्र", "ज्ञ")),
                KeyData("ड", "ढ", listOf("ढ", "ण")),
                KeyData("ट", "ठ"),
                KeyData("ृ", "ॄ"),
                KeyData("ं", "ः", listOf("ः", "ँ", "्")),
                KeyData("।", "॥")
            ),

            // Row 3: Numbers and special characters
            listOf(
                KeyData("१", "!"),
                KeyData("२", "@"),
                KeyData("३", "#"),
                KeyData("४", "$"),
                KeyData("५", "%"),
                KeyData("६", "^"),
                KeyData("७", "&"),
                KeyData("८", "*"),
                KeyData("९", "("),
                KeyData("०", ")")
            ),

            // Row 4: Bottom row with function keys
            listOf(
                KeyData("?123", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION),
                KeyData(" ", keyType = KeyType.ACTION, flex = 3.5f),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.2f),
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
            // Row 1: Numbers
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

            // Row 2: Special characters
            listOf(
                KeyData("@"),
                KeyData("#"),
                KeyData("₹"),
                KeyData("%"),
                KeyData("&"),
                KeyData("*"),
                KeyData("-"),
                KeyData("+"),
                KeyData("=")
            ),

            // Row 3: More symbols
            listOf(
                KeyData("("),
                KeyData(")"),
                KeyData("।"),
                KeyData("॥"),
                KeyData("\""),
                KeyData("'"),
                KeyData(":"),
                KeyData(";"),
                KeyData("?")
            ),

            // Row 4: Bottom row
            listOf(
                KeyData("ABC", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION),
                KeyData(" ", keyType = KeyType.ACTION, flex = 3.5f),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("↵", keyType = KeyType.ACTION, flex = 1.2f)
            )
        )

        return KeyboardLayout(rows, LanguageMode.NEPALI)
    }
}
