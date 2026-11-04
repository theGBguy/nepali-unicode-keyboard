package io.github.thegbguy.nepalikeyboard.service

import android.view.KeyEvent
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardController
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.TraditionalHardwareLayout

/**
 * Decides what a physical key does: Nepali typing goes through the controller like screen keys,
 * everything else (English mode, shortcuts, arrows, Enter, Tab) goes to the app. Takes plain key
 * codes and meta state, so it runs in JVM tests without Android's KeyEvent.
 */
class HardwareKeyRouter(private val keyboard: () -> KeyboardController) {

    /** Keys whose down we handled, so their up doesn't reach the app either. */
    private val consumed = mutableSetOf<Int>()

    /**
     * @param unicodeChar what the key types with [metaState] in the keyboard's own layout (0 if
     *   nothing), used for Romanized letters.
     * @return true if the key was handled and the app mustn't see it.
     */
    fun onKeyDown(keyCode: Int, metaState: Int, unicodeChar: Int, repeatCount: Int): Boolean {
        val handled = handle(keyCode, metaState, unicodeChar, repeatCount)
        if (handled) consumed += keyCode
        return handled
    }

    fun onKeyUp(keyCode: Int): Boolean = consumed.remove(keyCode)

    private fun handle(keyCode: Int, metaState: Int, unicodeChar: Int, repeatCount: Int): Boolean {
        if (keyCode in MODIFIER_KEYS) return false
        val ctrl = metaState and KeyEvent.META_CTRL_ON != 0
        val meta = metaState and KeyEvent.META_META_ON != 0
        val leftAlt = metaState and KeyEvent.META_ALT_LEFT_ON != 0
        val altGr = metaState and KeyEvent.META_ALT_RIGHT_ON != 0 && !ctrl && !meta && !leftAlt
        val shift = metaState and KeyEvent.META_SHIFT_ON != 0

        // Ctrl + Space switches language like the language key (as in Gboard and Windows). Win
        // alone is Home on Android, so Win + Space can't be used.
        val languageKey = keyCode == KeyEvent.KEYCODE_LANGUAGE_SWITCH ||
            (keyCode == KeyEvent.KEYCODE_SPACE && ctrl && !meta && !leftAlt && !altGr)
        if (languageKey) {
            if (repeatCount == 0) keyboard().switchLanguage()
            return true
        }

        val controller = keyboard()
        val mode = controller.state.value.currentLanguage
        if (mode == LanguageMode.ENGLISH) return false
        if (ctrl || meta || leftAlt) {
            controller.onHardwarePassThrough()
            return false
        }
        when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                controller.onHardwareSpace()
                return true
            }
            KeyEvent.KEYCODE_DEL -> return controller.onHardwareBackspace()
        }
        val text = when (mode) {
            LanguageMode.NEPALI -> ISO_POSITIONS[keyCode]?.let { TraditionalHardwareLayout.output(it, shift, altGr) }
            else -> unicodeChar.takeIf { !altGr && it > ' '.code && !Character.isISOControl(it) }?.let { String(Character.toChars(it)) }
        }
        if (text == null) {
            controller.onHardwarePassThrough()
            return false
        }
        controller.onHardwareText(text)
        return true
    }

    companion object {
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
