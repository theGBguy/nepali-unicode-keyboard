package io.github.thegbguy.nepalikeyboard.data.clipboard

import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileClipboardStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun storage(file: File) = FileClipboardStorage(file, writer = { it.run() })

    @Test
    fun `saved items survive a new instance (keyboard restart)`() {
        val file = File(folder.root, FileClipboardStorage.FILE_NAME)
        var items = ClipboardHistory.add(emptyList(), "पहिलो", 1)
        items = ClipboardHistory.add(items, "second\nline", 2)
        storage(file).save(items)
        assertEquals(items, storage(file).load())
        assertFalse(File(folder.root, FileClipboardStorage.FILE_NAME + ".tmp").exists())
    }

    @Test
    fun `clear deletes the file and the in-memory copy`() {
        val file = File(folder.root, FileClipboardStorage.FILE_NAME)
        val store = storage(file)
        store.save(ClipboardHistory.add(emptyList(), "a", 1))
        assertTrue(file.isFile)
        store.clear()
        assertFalse(file.exists())
        assertEquals(0, store.load().size)
    }

    @Test
    fun `no file means an empty history`() {
        assertEquals(0, storage(File(folder.root, "missing.txt")).load().size)
    }
}
