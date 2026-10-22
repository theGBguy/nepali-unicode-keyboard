package io.github.thegbguy.nepalikeyboard.data.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipItem
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardHistory
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardSource
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardStorage
import io.github.thegbguy.nepalikeyboard.domain.clipboard.CopiedText
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Clipboard history in one small file in the app's private storage. The keyboard and the
 * settings screen run in the same process and share [shared], so clearing it in Settings is
 * seen by the open keyboard. The file is excluded from Android backup (backup_rules.xml and data_extraction_rules.xml).
 * Saves happen on a background thread; the in-memory copy is what everyone reads.
 */
class FileClipboardStorage(
    private val file: File,
    private val writer: Executor = Executors.newSingleThreadExecutor()
) : ClipboardStorage {
    private val lock = Any()
    private var cache: List<ClipItem>? = null

    override fun load(): List<ClipItem> = synchronized(lock) {
        cache ?: ClipboardHistory.decode(file.takeIf { it.isFile }?.readText()).also { cache = it }
    }

    override fun save(items: List<ClipItem>) {
        synchronized(lock) { cache = items }
        writer.execute { write(items) }
    }

    /** Deletes everything, pinned items included. */
    fun clear() {
        synchronized(lock) { cache = emptyList() }
        writer.execute { file.delete() }
    }

    private fun write(items: List<ClipItem>) {
        if (items.isEmpty()) {
            file.delete()
            return
        }
        // Write a temporary file, then rename, so a crash mid-write can't leave half a file
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(ClipboardHistory.encode(items))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        const val FILE_NAME = "clipboard.txt"

        @Volatile private var instance: FileClipboardStorage? = null

        fun shared(context: Context): FileClipboardStorage =
            instance ?: synchronized(this) {
                instance ?: FileClipboardStorage(File(context.applicationContext.filesDir, FILE_NAME)).also { instance = it }
            }
    }
}

/** The system clipboard. Only the default keyboard (or the focused app) may read it. */
class SystemClipboardSource(context: Context) : ClipboardSource {
    private val manager = context.applicationContext.getSystemService(ClipboardManager::class.java)

    override fun read(): CopiedText? {
        val description = manager?.primaryClipDescription ?: return null
        if (!description.hasMimeType("text/*")) return null
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val text = clip.getItemAt(0).text?.toString()?.takeIf { it.isNotEmpty() } ?: return null
        return CopiedText(text, sensitive = isSensitive(description))
    }

    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun observe(onCopy: (() -> Unit)?) {
        listener?.let { manager?.removePrimaryClipChangedListener(it) }
        listener = onCopy?.let { callback ->
            ClipboardManager.OnPrimaryClipChangedListener { callback() }.also { manager?.addPrimaryClipChangedListener(it) }
        }
    }

    override fun recentCopyId(withinMillis: Long): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val description = manager?.primaryClipDescription ?: return null
        if (!description.hasMimeType("text/*")) return null
        // ClipboardService stamps copies with System.currentTimeMillis() (Android 8 through 16)
        val age = System.currentTimeMillis() - description.timestamp
        return description.timestamp.takeIf { age in 0..withinMillis }
    }

    override fun clear() {
        val manager = manager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.clearPrimaryClip()
        } else {
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun isSensitive(description: ClipDescription): Boolean =
        description.extras?.getBoolean(EXTRA_IS_SENSITIVE, false) == true

    private companion object {
        // ClipDescription.EXTRA_IS_SENSITIVE (API 33). The copying app sets it, so it can appear on older versions too
        const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    }
}
