package io.github.thegbguy.nepalikeyboard.service

import android.view.KeyEvent
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardController
import io.github.thegbguy.nepalikeyboard.ui.keyboard.hardware.HardKey
import io.github.thegbguy.nepalikeyboard.ui.keyboard.hardware.HardKeyEvent
import io.github.thegbguy.nepalikeyboard.ui.keyboard.hardware.HardwareKeyRouter

/**
 * Android's physical key events → the shared [HardwareKeyRouter]. Takes plain key codes and meta
 * state, so it runs in JVM tests without Android's KeyEvent objects.
 */
class AndroidHardwareKeys(keyboard: () -> KeyboardController) {

    private val router = HardwareKeyRouter(keyboard)

    /** Keys whose down we handled, so their up doesn't reach the app either. */
    private val consumed = mutableSetOf<Int>()

    /**
     * @param unicodeChar what the key types with [metaState] in the keyboard's own layout (0 if
     *   nothing), used for Romanized letters.
     * @return true if the key was handled and the app mustn't see it.
     */
    fun onKeyDown(keyCode: Int, metaState: Int, unicodeChar: Int, repeatCount: Int): Boolean {
        val handled = router.onKeyDown(event(keyCode, metaState, unicodeChar, repeatCount))
        if (handled) consumed += keyCode
        return handled
    }

    fun onKeyUp(keyCode: Int): Boolean = consumed.remove(keyCode)

    companion object {
        fun event(keyCode: Int, metaState: Int, unicodeChar: Int, repeatCount: Int) = HardKeyEvent(
            key = KEYS[keyCode] ?: if (keyCode in MODIFIER_KEYS) HardKey.MODIFIER else HardKey.CHARACTER,
            iso = ISO_POSITIONS[keyCode],
            // Dead keys set KeyCharacterMap.COMBINING_ACCENT (the sign bit): no text
            text = unicodeChar.takeIf { it > 0 && Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) },
            shift = metaState and KeyEvent.META_SHIFT_ON != 0,
            ctrl = metaState and KeyEvent.META_CTRL_ON != 0,
            meta = metaState and KeyEvent.META_META_ON != 0,
            alt = metaState and KeyEvent.META_ALT_LEFT_ON != 0,
            altGr = metaState and KeyEvent.META_ALT_RIGHT_ON != 0,
            repeat = repeatCount > 0,
        )

        private val KEYS = mapOf(
            KeyEvent.KEYCODE_SPACE to HardKey.SPACE,
            KeyEvent.KEYCODE_ENTER to HardKey.ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER to HardKey.ENTER,
            KeyEvent.KEYCODE_DEL to HardKey.BACKSPACE,
            KeyEvent.KEYCODE_TAB to HardKey.TAB,
            KeyEvent.KEYCODE_DPAD_LEFT to HardKey.LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT to HardKey.RIGHT,
            KeyEvent.KEYCODE_ESCAPE to HardKey.ESCAPE,
            KeyEvent.KEYCODE_LANGUAGE_SWITCH to HardKey.LANGUAGE_SWITCH,
        )

        private val MODIFIER_KEYS = setOf(
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_CAPS_LOCK, KeyEvent.KEYCODE_NUM_LOCK, KeyEvent.KEYCODE_SCROLL_LOCK,
            KeyEvent.KEYCODE_FUNCTION, KeyEvent.KEYCODE_SYM
        )

        /** Android key codes are physical positions on a US keyboard; these are their ISO names. */
        private val ISO_POSITIONS: Map<Int, String> = buildMap {
            put(KeyEvent.KEYCODE_GRAVE, "E00")
            listOf(
                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
                KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_0
            ).forEachIndexed { i, code -> put(code, "E%02d".format(i + 1)) }
            put(KeyEvent.KEYCODE_MINUS, "E11")
            put(KeyEvent.KEYCODE_EQUALS, "E12")
            "QWERTYUIOP".forEachIndexed { i, c -> put(KeyEvent.KEYCODE_A + (c - 'A'), "D%02d".format(i + 1)) }
            put(KeyEvent.KEYCODE_LEFT_BRACKET, "D11")
            put(KeyEvent.KEYCODE_RIGHT_BRACKET, "D12")
            "ASDFGHJKL".forEachIndexed { i, c -> put(KeyEvent.KEYCODE_A + (c - 'A'), "C%02d".format(i + 1)) }
            put(KeyEvent.KEYCODE_SEMICOLON, "C10")
            put(KeyEvent.KEYCODE_APOSTROPHE, "C11")
            put(KeyEvent.KEYCODE_BACKSLASH, "C12")
            "ZXCVBNM".forEachIndexed { i, c -> put(KeyEvent.KEYCODE_A + (c - 'A'), "B%02d".format(i + 1)) }
            put(KeyEvent.KEYCODE_COMMA, "B08")
            put(KeyEvent.KEYCODE_PERIOD, "B09")
            put(KeyEvent.KEYCODE_SLASH, "B10")
        }
    }
}
