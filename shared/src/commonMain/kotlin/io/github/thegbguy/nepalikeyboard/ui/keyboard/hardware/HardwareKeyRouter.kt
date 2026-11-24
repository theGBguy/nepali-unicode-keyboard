package io.github.thegbguy.nepalikeyboard.ui.keyboard.hardware

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.ui.keyboard.KeyboardController
import io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts.TraditionalHardwareLayout

/**
 * Decides what a physical key does: Nepali typing goes through the controller like screen keys,
 * everything else (English mode, shortcuts, arrows, Enter, Tab) goes to the app. Platform-neutral:
 * each app turns its own key event into a [HardKeyEvent].
 *
 * @param ownsLanguageKey whether Ctrl + Space and the language-switch key cycle the language here.
 *   True on Android. False on the Mac, where the system switches input sources and English is
 *   another input source.
 */
class HardwareKeyRouter(
    private val keyboard: () -> KeyboardController,
    private val ownsLanguageKey: Boolean = true,
) {

    /** @return true if the key was handled and the app mustn't see it. */
    fun onKeyDown(event: HardKeyEvent): Boolean {
        if (event.key == HardKey.MODIFIER) return false
        val shift = event.shift
        // AltGr combined with any other modifier is a shortcut, not a third-level character
        val altGr = event.altGr && !event.ctrl && !event.meta && !event.alt

        // Ctrl + Space switches language like the language key (as in Gboard and Windows). Win
        // alone is Home on Android, so Win + Space can't be used.
        val languageKey = event.key == HardKey.LANGUAGE_SWITCH ||
            (event.key == HardKey.SPACE && event.ctrl && !event.meta && !event.alt && !altGr)
        if (languageKey && ownsLanguageKey) {
            if (!event.repeat) keyboard().switchLanguage()
            return true
        }

        val controller = keyboard()
        val mode = controller.state.value.currentLanguage
        if (mode == LanguageMode.ENGLISH) return false
        if (event.ctrl || event.meta || event.alt) {
            controller.onHardwarePassThrough()
            return false
        }
        // Picking another suggestion while a Romanized word is being typed (held keys repeat)
        val pick = when (event.key) {
            HardKey.TAB -> controller.onHardwareMoveSuggestion(if (shift) -1 else 1)
            HardKey.RIGHT -> !shift && controller.onHardwareMoveSuggestion(1)
            HardKey.LEFT -> !shift && controller.onHardwareMoveSuggestion(-1)
            HardKey.ESCAPE -> controller.onHardwareResetSuggestion()
            else -> false
        }
        if (pick) return true
        // Space or Enter types the smart suggestion (today's date…) when Tab picked it
        val confirm = event.key == HardKey.SPACE || event.key == HardKey.ENTER
        if (confirm && controller.onHardwareConfirmSmart()) return true
        when (event.key) {
            HardKey.SPACE -> {
                controller.onHardwareSpace()
                return true
            }
            HardKey.BACKSPACE -> return controller.onHardwareBackspace()
            // Romanized "." is ।; Shift + "." is the English full stop, which the screen keys give
            // with a long-press (a physical keyboard has none).
            HardKey.CHARACTER -> if (event.iso == PERIOD && shift && !altGr && mode == LanguageMode.TRANSLITERATION) {
                controller.onKeyLongPress(".")
                return true
            }
            else -> {}
        }
        val text = when {
            event.key != HardKey.CHARACTER -> null
            mode == LanguageMode.NEPALI -> event.iso?.let { TraditionalHardwareLayout.output(it, shift, altGr) }
            else -> event.text?.takeIf { !altGr && it.isNotEmpty() && it.none(::isControlOrSpace) }
        }
        if (text == null) {
            controller.onHardwarePassThrough()
            return false
        }
        controller.onHardwareText(text)
        return true
    }

    private companion object {
        /** ISO position of the full-stop key. */
        const val PERIOD = "B09"

        fun isControlOrSpace(c: Char) = c <= ' ' || c in '\u007F'..'\u009F'
    }
}
