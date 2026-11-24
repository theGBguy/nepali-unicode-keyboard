package io.github.thegbguy.nepalikeyboard.ui.keyboard.hardware

/** The keys the router treats specially; every other key is [CHARACTER]. */
enum class HardKey {
    /** A key that may type something: letters, digits, punctuation, and anything not listed below. */
    CHARACTER,
    SPACE,

    /** Return or the numpad Enter. */
    ENTER,

    /** Delete backwards (Android's DEL, the Mac's Delete). */
    BACKSPACE,
    TAB,
    LEFT,
    RIGHT,
    ESCAPE,

    /** A dedicated language-switch key (some Android keyboards have one). */
    LANGUAGE_SWITCH,

    /** Shift, Control, Alt/Option, Meta/Command, Caps Lock, Fn and the like, pressed on their own. */
    MODIFIER,
}

/**
 * One physical key press, already translated from the platform's event (Android `KeyEvent`, the
 * Mac's `NSEvent`).
 *
 * @property iso the key's ISO position for [HardKey.CHARACTER] keys (`D01` is the Q key; see
 *   [io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.TraditionalHardwareLayout]), or null if
 *   it has none. The traditional layout types by position.
 * @property text what the key types with these modifiers in the keyboard's own layout, or null.
 *   Romanized typing uses it.
 * @property alt Alt used as a shortcut modifier (left Alt on Android).
 * @property altGr the third-level shift that the traditional layout uses (right Alt on Android).
 * @property repeat the key is held down and this is an auto-repeat.
 */
data class HardKeyEvent(
    val key: HardKey,
    val iso: String? = null,
    val text: String? = null,
    val shift: Boolean = false,
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val alt: Boolean = false,
    val altGr: Boolean = false,
    val repeat: Boolean = false,
)
