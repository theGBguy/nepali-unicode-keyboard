package io.github.thegbguy.nepalikeyboard.domain.clipboard

/** The phone's current copied text. [sensitive] is set by apps that copy passwords or card numbers. */
data class CopiedText(val text: String, val sensitive: Boolean)

/**
 * The system clipboard, as the keyboard sees it. Kept behind an interface so the view model
 * never touches ClipboardManager and tests can use a fake.
 */
interface ClipboardSource {
    /**
     * Reads the copied text, or null when there's none (or it isn't text). Android 12+ shows
     * "pasted from your clipboard" when an app reads another app's copy, but never for the
     * default keyboard (ClipboardService.showAccessNotificationLocked skips isDefaultIme).
     */
    fun read(): CopiedText?

    /** Calls [onCopy] each time something is copied anywhere on the phone; null stops. */
    fun observe(onCopy: (() -> Unit)?)

    /**
     * An id for the current copy (its timestamp) if it was made in the last [withinMillis].
     * Always null before Android 8, which doesn't record when a copy was made.
     */
    fun recentCopyId(withinMillis: Long): Long?

    /** Clears the phone's clipboard (used when the user deletes the current copy). */
    fun clear()
}

/** Where the clipboard history is kept. */
interface ClipboardStorage {
    fun load(): List<ClipItem>
    fun save(items: List<ClipItem>)
}

/** Used by tests and until the IME service supplies the on-device store. */
class InMemoryClipboardStorage(private var items: List<ClipItem> = emptyList()) : ClipboardStorage {
    override fun load(): List<ClipItem> = items
    override fun save(items: List<ClipItem>) {
        this.items = items
    }
}
