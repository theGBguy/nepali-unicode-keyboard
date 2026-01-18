package io.github.thegbguy.nepalikeyboard.domain.model

/**
 * Represents a complete keyboard layout with all keys organized in rows.
 *
 * @property rows List of key rows, where each row contains a list of keys
 * @property mode The language mode this layout is for
 */
data class KeyboardLayout(
    val rows: List<List<KeyData>>,
    val mode: LanguageMode
) {
    /**
     * Returns the total number of keys in this layout.
     */
    fun totalKeys(): Int = rows.sumOf { it.size }

    /**
     * Finds a key by its primary value.
     */
    fun findKey(primary: String): KeyData? =
        rows.flatten().find { it.primary == primary }

    /**
     * Returns all character keys (excluding function/modifier keys).
     */
    fun characterKeys(): List<KeyData> =
        rows.flatten().filter { it.keyType == KeyType.CHARACTER }
}
