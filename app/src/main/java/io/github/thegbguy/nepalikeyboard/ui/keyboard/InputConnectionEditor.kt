package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/** The focused field through Android's [InputConnection]. Make a new one per use (see [KeyboardController.editor]). */
class InputConnectionEditor(private val ic: InputConnection) : EditorConnection {
    override fun commitText(text: String) {
        ic.commitText(text, 1)
    }

    override fun setComposingText(text: String) {
        ic.setComposingText(text, 1)
    }

    override fun finishComposingText() {
        ic.finishComposingText()
    }

    override fun textBeforeCursor(length: Int): String? = ic.getTextBeforeCursor(length, 0)?.toString()

    override fun selectedText(): String? = ic.getSelectedText(0)?.takeIf { it.isNotEmpty() }?.toString()

    override fun deleteBeforeCursor(length: Int) {
        ic.deleteSurroundingText(length, 0)
    }

    override fun beginBatchEdit() {
        ic.beginBatchEdit()
    }

    override fun endBatchEdit() {
        ic.endBatchEdit()
    }

    override fun sendEnterKey() {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    override fun performEditorAction(actionId: Int) {
        ic.performEditorAction(actionId)
    }
}
