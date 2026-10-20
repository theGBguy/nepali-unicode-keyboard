package io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts

import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/**
 * The traditional Nepali Unicode layout, fitted to a phone.
 *
 * Letter rows follow the standard layout (Microsoft's Nepali driver / CLDR `ne-t-k0-windows`):
 * the number-row letters ञ घ ङ झ छ ट ठ ड ढ ण, then ध भ च त थ ग ष य उ, ब क म ा न ज व प ि स and
 * श ह अ ख द ल फ र. Shift gives the standard second character (the halant form, क्ष on ष, इ on य,
 * ए on उ, ँ on ा, ी on ि...). Keys the standard puts beyond ten columns (े, ृ, ्, ु, ो) are placed
 * at the row ends and on the bottom row; ई ऊ ऐ ओ औ ॐ are on symbol page 2/2 (SymbolLayout). Typing अ then
 * ा/ो/ौ/ै gives आ/ओ/औ/ऐ (see KeyboardViewModel).
 */
object NepaliLayout {

    fun getLayout(): KeyboardLayout {
        val rows = listOf(
            // Nepali digits
            "१२३४५६७८९०".map { KeyData(it.toString()) },

            // Standard number-row letters
            listOf(
                KeyData("ञ", "ज्ञ"),
                KeyData("घ"),
                KeyData("ङ"),
                KeyData("झ"),
                KeyData("छ"),
                KeyData("ट"),
                KeyData("ठ"),
                KeyData("ड"),
                KeyData("ढ"),
                KeyData("ण")
            ),

            // Q row, plus े (standard: the ] key)
            listOf(
                KeyData("ध", "ध्"),
                KeyData("भ", "भ्"),
                KeyData("च", "च्"),
                KeyData("त", "त्"),
                KeyData("थ", "थ्"),
                KeyData("ग", "ग्"),
                KeyData("ष", "क्ष"),
                KeyData("य", "इ"),
                KeyData("उ", "ए"),
                KeyData("े", "ै")
            ),

            // A row, exactly as the standard
            listOf(
                KeyData("ब", "ब्"),
                KeyData("क", "क्"),
                KeyData("म", "म्"),
                KeyData("ा", "ँ"),
                KeyData("न", "न्"),
                KeyData("ज", "ज्"),
                KeyData("व", "व्"),
                KeyData("प", "प्"),
                KeyData("ि", "ी"),
                KeyData("स", "स्")
            ),

            // Z row: standard letters, with र (the / key) and ु (the ' key) brought in
            listOf(
                KeyData("⇧", keyType = KeyType.MODIFIER, flex = 1.3f),
                KeyData("श", "श्"),
                KeyData("ह", "ह्"),
                KeyData("अ", "ऋ"),
                KeyData("ख", "ख्"),
                KeyData("द", "द्"),
                KeyData("ल", "ल्"),
                KeyData("फ", "ः"),
                KeyData("र", "ृ"),
                KeyData("ु", "ू"),
                KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.3f)
            ),

            // Bottom row: halant (anusvara on shift), ो (ौ), purna biram (comma)
            listOf(
                KeyData("?123", keyType = KeyType.FUNCTION, flex = 1.2f),
                KeyData("🌐", keyType = KeyType.FUNCTION, flex = 1.1f),
                KeyData("्", "ं"),
                KeyData(" ", keyType = KeyType.ACTION, flex = 3f),
                KeyData("ो", "ौ"),
                KeyData("।", ","),
                KeyData("↵", keyType = KeyType.ACTION, flex = 1.2f)
            )
        )

        return KeyboardLayout(rows, LanguageMode.NEPALI)
    }
}
