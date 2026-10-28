package io.github.thegbguy.nepalikeyboard.ui.keyboard

/**
 * A text field with the cursor at the end: committed [text], plus the [composing] word shown after
 * it. Records Enter presses and editor actions.
 */
class FakeEditor(var text: String = "") : EditorConnection {
    var composing = ""
        private set
    var selection = ""
    val actions = mutableListOf<Int>()
    var enterKeys = 0
        private set

    /** What the field shows. */
    val shown: String get() = text + composing

    override fun commitText(text: String) {
        this.text += text
        composing = ""
        selection = ""
    }

    override fun setComposingText(text: String) {
        composing = text
    }

    override fun finishComposingText() {
        text += composing
        composing = ""
    }

    override fun textBeforeCursor(length: Int): String = shown.takeLast(length)

    override fun selectedText(): String = selection

    override fun deleteBeforeCursor(length: Int) {
        finishComposingText()
        text = text.dropLast(length)
    }

    override fun beginBatchEdit() {}

    override fun endBatchEdit() {}

    override fun sendEnterKey() {
        enterKeys++
    }

    override fun performEditorAction(actionId: Int) {
        actions += actionId
    }
}
