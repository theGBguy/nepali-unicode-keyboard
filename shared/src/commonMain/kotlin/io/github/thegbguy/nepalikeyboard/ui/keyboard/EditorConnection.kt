package io.github.thegbguy.nepalikeyboard.ui.keyboard

/**
 * The focused text field, as the keyboard edits it. Android wraps its `InputConnection`; iOS will
 * wrap the extension's `textDocumentProxy`. Get a fresh one for every edit: the field changes
 * each time another one gets focus.
 */
interface EditorConnection {
    /** Types [text] at the cursor, replacing the composing text if there is any. */
    fun commitText(text: String)

    /** Shows [text] as the word being composed (underlined), replacing the previous one. */
    fun setComposingText(text: String)

    /** Keeps the composing text as it is and ends composing. */
    fun finishComposingText()

    /** Up to [length] characters before the cursor, or null if the field can't say. */
    fun textBeforeCursor(length: Int): String?

    /** The selected text, or null/empty when nothing is selected. */
    fun selectedText(): String?

    /** Deletes [length] UTF-16 units before the cursor. */
    fun deleteBeforeCursor(length: Int)

    /** Groups the edits until [endBatchEdit] so the app sees them as one change. */
    fun beginBatchEdit()
    fun endBatchEdit()

    /** Enter as a key press, for fields with no editor behind them (e.g. terminals). */
    fun sendEnterKey()

    /** Runs the field's Enter action (Send, Search, Go...). */
    fun performEditorAction(actionId: Int)
}
