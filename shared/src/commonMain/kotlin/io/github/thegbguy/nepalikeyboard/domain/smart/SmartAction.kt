package io.github.thegbguy.nepalikeyboard.domain.smart

/**
 * A conversion offered in the strip, e.g. `1500000` → `१५,००,०००`.
 *
 * Tapping it replaces the Romanized word being typed (if any) and [replace] characters before
 * it with [text]. `replace = 0` inserts after the cursor without removing anything.
 *
 * @property label what the strip shows (usually [text] without a trailing space)
 */
data class SmartAction(
    val label: String,
    val text: String,
    val replace: Int,
    val kind: Kind
) {
    /** Picks the small icon in front of the label, so it reads as a conversion and not a word guess. */
    enum class Kind { AMOUNT, DATE, UNIT }
}
