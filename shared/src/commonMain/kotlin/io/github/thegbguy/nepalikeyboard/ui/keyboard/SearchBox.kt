package io.github.thegbguy.nepalikeyboard.ui.keyboard

/**
 * The emoji search box: while it's open the keys type here instead of into the app's field, so
 * Shift, backspace and Romanized typing all work as usual. [onChange] runs after every edit.
 */
internal class SearchBox(private val onChange: () -> Unit) : EditorConnection {
    var text = ""
        private set
    var composing = ""
        private set

    override fun commitText(text: String) {
        this.text += text
        composing = ""
        onChange()
    }

    override fun setComposingText(text: String) {
        composing = text
        onChange()
    }

    override fun finishComposingText() {
        text += composing
        composing = ""
        onChange()
    }

    override fun textBeforeCursor(length: Int): String = (text + composing).takeLast(length)

    override fun selectedText(): String? = null

    override fun deleteBeforeCursor(length: Int) {
        text += composing
        composing = ""
        text = text.dropLast(length)
        onChange()
    }

    override fun beginBatchEdit() {}

    override fun endBatchEdit() {}

    override fun sendEnterKey() {}

    override fun performEditorAction(actionId: Int) {}
}
