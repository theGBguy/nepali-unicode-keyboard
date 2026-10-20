package io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts

import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.KeyboardLayout
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode

/**
 * The two symbol pages behind ?123, arranged like Samsung Keyboard: 1/2 and 2/2 flip between
 * them. Every mode shares them; on the traditional Nepali keyboard £ becomes रु and page 2's
 * shapes row becomes the independent vowels and marks the letter page has no room for.
 */
object SymbolLayout {

    const val PAGE_ONE = "1/2"
    const val PAGE_TWO = "2/2"

    fun getLayout(page: Int, mode: LanguageMode): KeyboardLayout {
        val nepali = mode == LanguageMode.NEPALI
        val rows = if (page == 2) pageTwo(nepali) else pageOne(nepali)
        return KeyboardLayout(rows + listOf(bottomRow()), mode)
    }

    private fun pageOne(nepali: Boolean) = listOf(
        keys("1234567890"),
        keys("+", "×", "÷", "=", "/", "_", "<", ">", "[", "]"),
        keys("!", "@", "#", if (nepali) "रु" else "£", "%", "^", "&", "*", "(", ")"),
        pageRow(PAGE_ONE, keys("-", "'", "\"", ":", ";", ",", "?"))
    )

    private fun pageTwo(nepali: Boolean) = listOf(
        keys("1234567890"),
        keys("`", "~", "\\", "|", "{", "}", "€", "$", "¥", "₹"),
        if (nepali) keys("ई", "ऊ", "ऐ", "ओ", "औ", "ॐ", "॥", "।", "ः", "ँ")
        else keys("°", "•", "○", "●", "□", "■", "♤", "♡", "◇", "♧"),
        pageRow(PAGE_TWO, keys("☆", "▪", "¤", "《", "》", "¡", "¿"))
    )

    private fun pageRow(label: String, symbols: List<KeyData>) =
        listOf(KeyData(label, keyType = KeyType.FUNCTION, flex = 1.4f)) + symbols +
            KeyData("⌫", keyType = KeyType.FUNCTION, flex = 1.4f)

    private fun bottomRow() = listOf(
        KeyData("ABC", keyType = KeyType.FUNCTION, flex = 1.6f),
        KeyData(","),
        KeyData(" ", keyType = KeyType.ACTION, flex = 4.8f),
        KeyData("."),
        KeyData("↵", keyType = KeyType.ACTION, flex = 1.6f)
    )

    private fun keys(row: String) = row.map { KeyData(it.toString()) }
    private fun keys(vararg symbols: String) = symbols.map { KeyData(it) }
}
