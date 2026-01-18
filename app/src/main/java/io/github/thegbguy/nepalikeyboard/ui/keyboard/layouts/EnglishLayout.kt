package io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts

import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/**
 * English QWERTY keyboard layout provider.
 * Provides standard QWERTY layout for English typing.
 */
object EnglishLayout {

    /**
     * Creates and returns the standard English QWERTY keyboard layout.
     */
    fun getLayout(): KeyboardLayout {
        val rows = listOf(
            // Row 1: QWERTY
            listOf(
                KeyData("q", "Q"),
                KeyData("w", "W"),
                KeyData("e", "E"),
                KeyData("r", "R"),
                KeyData("t", "T"),
                KeyData("y", "Y"),
                KeyData("u", "U"),
                KeyData("i", "I"),
                KeyData("o", "O"),
                KeyData("p", "P")
            ),

            // Row 2: ASDFGH
            listOf(
                KeyData("a", "A", listOf("@", "á", "à", "â")),
                KeyData("s", "S"),
                KeyData("d", "D"),
                KeyData("f", "F"),
                KeyData("g", "G"),
                KeyData("h", "H"),
                KeyData("j", "J"),
                KeyData("k", "K"),
                KeyData("l", "L")
            ),

            // Row 3: ZXCVBN
            listOf(
                KeyData("⇧", keyType = KeyType.MODIFIER, flex = 1.3f),
                KeyData("z", "Z"),
                KeyData("x", "X"),
                KeyData("c", "C"),
                KeyData("v", "V"),
                KeyData("b", "B"),
                KeyData("n", "N"),
                KeyData("m", "M"),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.3f)
            ),

            // Row 4: Bottom row with function keys
            listOf(
                KeyData("?123", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION),
                KeyData(",", "!", longPress = listOf("!", "?", ";", ":")),
                KeyData(" ", keyType = KeyType.ACTION, flex = 3f),
                KeyData(".", ".", longPress = listOf(".", ",", "!", "?")),
                KeyData("↵", keyType = KeyType.ACTION, flex = 1.2f)
            )
        )

        return KeyboardLayout(rows, LanguageMode.ENGLISH)
    }

    /**
     * Returns the numbers/symbols variant of the English layout.
     */
    fun getNumbersLayout(): KeyboardLayout {
        val rows = listOf(
            // Row 1: Numbers
            listOf(
                KeyData("1", "!"),
                KeyData("2", "@"),
                KeyData("3", "#"),
                KeyData("4", "$"),
                KeyData("5", "%"),
                KeyData("6", "^"),
                KeyData("7", "&"),
                KeyData("8", "*"),
                KeyData("9", "("),
                KeyData("0", ")")
            ),

            // Row 2: Top row symbols
            listOf(
                KeyData("-", "_"),
                KeyData("/", "\\"),
                KeyData(":", ";"),
                KeyData(";", ":"),
                KeyData("(", "{"),
                KeyData(")", "}"),
                KeyData("$", "₹"),
                KeyData("&", "§"),
                KeyData("@", "©")
            ),

            // Row 3: Bottom symbols
            listOf(
                KeyData("=+/", keyType = KeyType.FUNCTION, flex = 1.3f),
                KeyData(".", "·"),
                KeyData(",", "،"),
                KeyData("?", "¿"),
                KeyData("!", "¡"),
                KeyData("'", "\""),
                KeyData("\"", "'"),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.3f)
            ),

            // Row 4: Bottom row
            listOf(
                KeyData("ABC", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION),
                KeyData(",", "!", longPress = listOf("!", "?", ";", ":")),
                KeyData(" ", keyType = KeyType.ACTION, flex = 3f),
                KeyData(".", ".", longPress = listOf(".", ",", "!", "?")),
                KeyData("↵", keyType = KeyType.ACTION, flex = 1.2f)
            )
        )

        return KeyboardLayout(rows, LanguageMode.ENGLISH)
    }

    /**
     * Returns the symbols variant of the English layout (accessed via =+/ key).
     */
    fun getSymbolsLayout(): KeyboardLayout {
        val rows = listOf(
            // Row 1: Extended symbols
            listOf(
                KeyData("[", "{"),
                KeyData("]", "}"),
                KeyData("{", "<"),
                KeyData("}", ">"),
                KeyData("#", "№"),
                KeyData("%", "‰"),
                KeyData("^", "↑"),
                KeyData("*", "★"),
                KeyData("+", "±"),
                KeyData("=", "≠")
            ),

            // Row 2: More symbols
            listOf(
                KeyData("_", "—"),
                KeyData("\\", "|"),
                KeyData("|", "¦"),
                KeyData("~", "`"),
                KeyData("<", "«"),
                KeyData(">", "»"),
                KeyData("€", "£"),
                KeyData("£", "¥"),
                KeyData("¥", "₹")
            ),

            // Row 3: Special characters
            listOf(
                KeyData("123", keyType = KeyType.FUNCTION, flex = 1.3f),
                KeyData("•", "°"),
                KeyData("©", "®"),
                KeyData("™", "℠"),
                KeyData("§", "¶"),
                KeyData("[", "("),
                KeyData("]", ")"),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.3f)
            ),

            // Row 4: Bottom row
            listOf(
                KeyData("ABC", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION),
                KeyData(",", "!", longPress = listOf("!", "?", ";", ":")),
                KeyData(" ", keyType = KeyType.ACTION, flex = 3f),
                KeyData(".", ".", longPress = listOf(".", ",", "!", "?")),
                KeyData("↵", keyType = KeyType.ACTION, flex = 1.2f)
            )
        )

        return KeyboardLayout(rows, LanguageMode.ENGLISH)
    }
}
