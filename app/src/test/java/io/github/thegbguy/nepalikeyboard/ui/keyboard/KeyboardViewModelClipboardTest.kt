package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipItem
import io.github.thegbguy.nepalikeyboard.domain.clipboard.ClipboardSource
import io.github.thegbguy.nepalikeyboard.domain.clipboard.CopiedText
import io.github.thegbguy.nepalikeyboard.domain.clipboard.InMemoryClipboardStorage
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyboardViewModelClipboardTest {

    private class FakeClipboard(var copied: CopiedText? = null, var copyId: Long? = null) : ClipboardSource {
        var reads = 0
        var cleared = false
        override fun read(): CopiedText? = copied.also { reads++ }
        override fun recentCopyId(withinMillis: Long): Long? = copyId
        var observer: (() -> Unit)? = null
        override fun observe(onCopy: (() -> Unit)?) {
            observer = onCopy
        }
        override fun clear() {
            cleared = true
            copied = null
        }
    }

    private lateinit var vm: KeyboardViewModel
    private lateinit var ic: InputConnection
    private lateinit var clipboard: FakeClipboard
    private lateinit var storage: InMemoryClipboardStorage
    private lateinit var prefs: InMemoryKeyboardPrefs
    private var now = 1000L

    private val textField = FieldInfo.classify(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE)
    private val passwordField = FieldInfo.classify(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.IME_ACTION_GO
    )
    private val incognitoField = FieldInfo.classify(
        InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    )

    @Before
    fun setUp() {
        ic = mockk(relaxed = true)
        clipboard = FakeClipboard()
        storage = InMemoryClipboardStorage()
        prefs = InMemoryKeyboardPrefs(typeNepali = false)
        vm = KeyboardViewModel().also {
            it.inputConnectionProvider = { ic }
            it.prefs = prefs
            it.clipboardSource = clipboard
            it.clipboardStorage = storage
            it.clock = { now++ }
        }
        vm.onInputStarted(textField)
    }

    private fun saved() = storage.load().map { it.text }

    @Test
    fun `opening the panel shows and saves the current copy`() {
        clipboard.copied = CopiedText("hello", sensitive = false)
        vm.openClipboard()
        assertTrue(vm.state.value.showClipboard)
        assertEquals("hello", vm.state.value.copied?.text)
        assertEquals(listOf("hello"), saved())
    }

    @Test
    fun `every copy is saved as it happens, newest first`() {
        clipboard.copied = CopiedText("one", sensitive = false)
        vm.onSystemCopy()
        clipboard.copied = CopiedText("two", sensitive = false)
        vm.onSystemCopy()
        clipboard.copied = CopiedText("password", sensitive = true)
        vm.onSystemCopy()
        assertEquals(listOf("two", "one"), saved())
    }

    @Test
    fun `a copy while the panel is open refreshes it`() {
        vm.openClipboard()
        clipboard.copied = CopiedText("fresh", sensitive = false)
        vm.onSystemCopy()
        assertEquals("fresh", vm.state.value.copied?.text)
        assertEquals(listOf("fresh"), vm.state.value.clipItems.map { it.text })
    }

    @Test
    fun `copies aren't saved with history off or after an incognito field`() {
        prefs.clipboardHistory = false
        clipboard.copied = CopiedText("a", sensitive = false)
        vm.onSystemCopy()
        assertEquals(emptyList<String>(), saved())

        prefs.clipboardHistory = true
        vm.onInputStarted(incognitoField)
        vm.onSystemCopy()
        assertEquals(emptyList<String>(), saved())
    }

    @Test
    fun `sensitive copies are shown but never saved`() {
        clipboard.copied = CopiedText("hunter2", sensitive = true)
        vm.openClipboard()
        assertTrue(vm.state.value.copied!!.sensitive)
        vm.onClipPaste("hunter2")
        verify { ic.commitText("hunter2", 1) }
        assertEquals(emptyList<String>(), saved())
    }

    @Test
    fun `nothing is saved in incognito fields or with history off`() {
        clipboard.copied = CopiedText("secret", sensitive = false)
        vm.onInputStarted(incognitoField)
        vm.openClipboard()
        vm.onClipPaste("secret")
        assertEquals(emptyList<String>(), saved())

        prefs.clipboardHistory = false
        vm.onInputStarted(textField)
        vm.openClipboard()
        assertEquals(emptyList<String>(), saved())
    }

    @Test
    fun `pasting commits the Romanized word first, then the item, and moves it to the top`() {
        storage.save(listOf(ClipItem("second", time = 2), ClipItem("first", time = 1)))
        vm.onKeyClick(KeyData("🌐", keyType = KeyType.FUNCTION))
        assertEquals(LanguageMode.TRANSLITERATION, vm.state.value.currentLanguage)
        vm.onKeyClick(KeyData("k"))
        vm.openClipboard()
        vm.onClipPaste("first")
        verifyOrder {
            ic.commitText("क", 1)
            ic.commitText("first", 1)
        }
        assertEquals(listOf("first", "second"), saved())
        assertFalse(vm.state.value.showClipboard)
    }

    @Test
    fun `deleting the current copy clears the phone's clipboard too`() {
        clipboard.copied = CopiedText("copied", sensitive = false)
        storage.save(listOf(ClipItem("older", time = 1)))
        vm.openClipboard()
        vm.onClipDelete("older")
        assertFalse(clipboard.cleared)
        vm.onClipDelete("copied")
        assertTrue(clipboard.cleared)
        assertNull(vm.state.value.copied)
        assertEquals(emptyList<String>(), saved())
    }

    @Test
    fun `clear all clears the phone's clipboard so the current copy doesn't come back`() {
        clipboard.copied = CopiedText("copied", sensitive = false)
        storage.save(listOf(ClipItem("older", time = 1)))
        vm.openClipboard()
        vm.onClipClearAll()
        assertTrue(clipboard.cleared)
        assertNull(vm.state.value.copied)
        assertEquals(emptyList<String>(), saved())

        vm.closeClipboard()
        vm.openClipboard()
        assertNull(vm.state.value.copied)
        assertEquals(emptyList<String>(), vm.state.value.clipItems.map { it.text })
    }

    @Test
    fun `clear all also clears a sensitive copy, which was never saved`() {
        clipboard.copied = CopiedText("hunter2", sensitive = true)
        vm.openClipboard()
        vm.onClipClearAll()
        assertTrue(clipboard.cleared)
        assertNull(vm.state.value.copied)
    }

    @Test
    fun `clear all keeps the phone's clipboard when the current copy is pinned`() {
        clipboard.copied = CopiedText("keep", sensitive = false)
        storage.save(listOf(ClipItem("older", time = 1)))
        vm.openClipboard()
        vm.onClipPin("keep", true)
        vm.onClipClearAll()
        assertFalse(clipboard.cleared)
        assertEquals("keep", vm.state.value.copied?.text)
        assertEquals(listOf("keep"), saved())
    }

    @Test
    fun `pin survives clear all`() {
        storage.save(listOf(ClipItem("b", time = 2), ClipItem("a", time = 1)))
        vm.openClipboard()
        vm.onClipPin("a", true)
        vm.onClipClearAll()
        assertEquals(listOf("a"), saved())
        assertEquals(listOf("a"), vm.state.value.clipItems.map { it.text })
    }

    @Test
    fun `paste suggestion shows for a fresh copy, hides on typing, and isn't offered twice`() {
        clipboard.copied = CopiedText("otp 4821", sensitive = false)
        clipboard.copyId = 77
        vm.onInputStarted(textField)
        assertEquals("otp 4821", vm.state.value.pasteSuggestion?.text)

        vm.onKeyClick(KeyData("a"))
        assertNull(vm.state.value.pasteSuggestion)

        vm.onInputStarted(textField)
        vm.onPasteSuggestion()
        verify { ic.commitText("otp 4821", 1) }
        assertEquals(listOf("otp 4821"), saved())

        vm.onInputStarted(textField)
        assertNull("same copy isn't offered again", vm.state.value.pasteSuggestion)
        clipboard.copyId = 78
        vm.onInputStarted(textField)
        assertEquals("otp 4821", vm.state.value.pasteSuggestion?.text)
    }

    @Test
    fun `no paste suggestion for old copies or in password fields`() {
        clipboard.copied = CopiedText("old", sensitive = false)
        clipboard.copyId = null
        vm.onInputStarted(textField)
        assertNull(vm.state.value.pasteSuggestion)
        assertEquals("an old copy isn't read on keyboard open", 0, clipboard.reads)

        clipboard.copyId = 5
        vm.onInputStarted(passwordField)
        assertNull(vm.state.value.pasteSuggestion)
        clipboard.copied = CopiedText("new", sensitive = false)
        vm.onSystemCopy()
        assertNull(vm.state.value.pasteSuggestion)
    }

    @Test
    fun `copying while typing offers Paste right away`() {
        clipboard.copied = CopiedText("link", sensitive = false)
        vm.onSystemCopy()
        assertEquals("link", vm.state.value.pasteSuggestion?.text)
    }
}
