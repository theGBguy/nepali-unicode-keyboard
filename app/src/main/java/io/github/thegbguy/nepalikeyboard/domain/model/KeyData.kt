package io.github.thegbguy.nepalikeyboard.domain.model

/**
 * Represents a single key on the keyboard with its properties and behavior.
 *
 * @property primary The primary character/text displayed and output when the key is tapped
 * @property shift The character/text output when Shift is active (optional)
 * @property longPress List of alternate characters shown on long press (optional)
 * @property keyType The type/category of this key
 * @property flex The relative width of the key (default 1.0 = standard key width)
 */
data class KeyData(
    val primary: String,
    val shift: String? = null,
    val longPress: List<String>? = null,
    val keyType: KeyType = KeyType.CHARACTER,
    val flex: Float = 1f
) {
    /**
     * Returns the output text based on current shift state.
     */
    fun getOutput(isShifted: Boolean): String =
        if (isShifted && shift != null) shift else primary

    /**
     * Returns true if this key has alternate characters on long press.
     */
    fun hasAlternates(): Boolean = !longPress.isNullOrEmpty()
}

/**
 * Categories of keys based on their function.
 */
enum class KeyType {
    /**
     * Regular character key (letters, numbers, symbols).
     */
    CHARACTER,

    /**
     * Action key (Enter, Space).
     */
    ACTION,

    /**
     * Modifier key (Shift, Caps Lock).
     */
    MODIFIER,

    /**
     * Function key (Language switch, Settings, Backspace).
     */
    FUNCTION,

    /**
     * Special spacer key for layout alignment.
     */
    SPACER
}
