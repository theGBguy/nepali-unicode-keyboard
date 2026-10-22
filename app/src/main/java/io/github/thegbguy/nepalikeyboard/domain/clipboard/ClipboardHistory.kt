package io.github.thegbguy.nepalikeyboard.domain.clipboard

/** One saved clipboard entry. [time] is when it was last copied or pasted (epoch millis). */
data class ClipItem(val text: String, val pinned: Boolean = false, val time: Long = 0L)

/**
 * The rules for the keyboard's clipboard history, as pure functions over a list kept
 * most-recent-first. Unpinned items are first in, first out: past [MAX_RECENT] the oldest
 * unpinned one is dropped. Pinned items are never dropped automatically.
 */
object ClipboardHistory {
    const val MAX_RECENT = 50
    const val MAX_PINNED = 50

    /** Longer text can still be pasted from the system clipboard but isn't saved. */
    const val MAX_LENGTH = 5_000

    fun canStore(text: String): Boolean = text.isNotBlank() && text.length <= MAX_LENGTH

    /**
     * Puts [text] at the top. Text already in the list moves up (keeping its pin) instead of
     * appearing twice. Blank or over-long text leaves the list unchanged.
     */
    fun add(items: List<ClipItem>, text: String, now: Long): List<ClipItem> {
        if (!canStore(text)) return items
        val existing = items.firstOrNull { it.text == text }
        val item = ClipItem(text, pinned = existing?.pinned ?: false, time = now)
        return trim(listOf(item) + items.filter { it.text != text })
    }

    fun pinnedCount(items: List<ClipItem>): Int = items.count { it.pinned }

    /** Pins or unpins [text]. Pinning when [MAX_PINNED] are already pinned changes nothing. */
    fun setPinned(items: List<ClipItem>, text: String, pinned: Boolean): List<ClipItem> {
        if (pinned && pinnedCount(items) >= MAX_PINNED) return items
        return trim(items.map { if (it.text == text) it.copy(pinned = pinned) else it })
    }

    fun delete(items: List<ClipItem>, text: String): List<ClipItem> = items.filter { it.text != text }

    /** The panel's "Clear all": pinned items stay. */
    fun clearUnpinned(items: List<ClipItem>): List<ClipItem> = items.filter { it.pinned }

    /** Drops the oldest unpinned items beyond [MAX_RECENT]. */
    private fun trim(items: List<ClipItem>): List<ClipItem> {
        var recent = 0
        return items.filter { it.pinned || ++recent <= MAX_RECENT }
    }

    /**
     * One line per item: `p` or `-`, the time, then the text with `\`, tab and line breaks
     * escaped. Simple enough to read back without a JSON library (org.json is a stub in JVM tests).
     */
    fun encode(items: List<ClipItem>): String = items.joinToString("\n") { item ->
        "${if (item.pinned) "p" else "-"}\t${item.time}\t${escape(item.text)}"
    }

    fun decode(data: String?): List<ClipItem> {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split('\n').mapNotNull { line ->
            val parts = line.split('\t', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val text = unescape(parts[2])
            if (!canStore(text)) return@mapNotNull null
            ClipItem(text, pinned = parts[0] == "p", time = parts[1].toLongOrNull() ?: 0L)
        }.distinctBy { it.text }.let(::trim)
    }

    private fun escape(text: String): String = buildString {
        text.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

    private fun unescape(text: String): String = buildString {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    else -> append(text[i + 1])
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}
