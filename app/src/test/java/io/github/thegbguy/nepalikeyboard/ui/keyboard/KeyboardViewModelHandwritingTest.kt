package io.github.thegbguy.nepalikeyboard.ui.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.github.thegbguy.nepalikeyboard.domain.handwriting.HandwritingRecognizer
import io.github.thegbguy.nepalikeyboard.domain.handwriting.InkPoint
import io.github.thegbguy.nepalikeyboard.domain.handwriting.InkStroke
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.classify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyboardViewModelHandwritingTest {

    private class FakeRecognizer : HandwritingRecognizer {
        var ready = false
        var failDownload = false
        var downloads = 0
        var deleted = false
        var lastPreContext: String? = null
        override suspend fun isModelReady() = ready
        override suspend fun download() {
            downloads++
            if (failDownload) error("offline")
            ready = true
        }
        override suspend fun recognize(strokes: List<InkStroke>, width: Float, height: Float, preContext: String): List<String> {
            lastPreContext = preContext
            return listOf("क".repeat(strokes.size), "x")
        }
        override suspend fun deleteModel() { deleted = true }
    }

    private lateinit var vm: KeyboardViewModel
    private lateinit var ic: InputConnection
    private lateinit var prefs: InMemoryKeyboardPrefs
    private lateinit var recognizer: FakeRecognizer
    private val textField = FieldInfo.classify(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE)
    private val stroke: InkStroke = listOf(InkPoint(1f, 1f, 0), InkPoint(5f, 5f, 10))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        prefs = InMemoryKeyboardPrefs()
        recognizer = FakeRecognizer()
        vm = KeyboardViewModel()
        vm.prefs = prefs
        vm.handwritingRecognizer = recognizer
        ic = mockk(relaxed = true)
        every { ic.getTextBeforeCursor(any(), 0) } returns "म "
        vm.inputConnectionProvider = { ic }
        vm.onInputStarted(textField)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun phase() = vm.state.value.handwriting

    @Test
    fun `first open asks before anything is downloaded`() {
        vm.openHandwriting()
        assertEquals(HandwritingPhase.CONSENT, phase())
        assertEquals(0, recognizer.downloads)
        vm.closeHandwriting()
        assertNull(phase())
        assertFalse(prefs.handwriting)
        assertEquals(0, recognizer.downloads)
    }

    @Test
    fun `agreeing remembers it and downloads the model`() {
        vm.openHandwriting()
        vm.onHandwritingAccept()
        assertTrue(prefs.handwriting)
        assertEquals(1, recognizer.downloads)
        assertEquals(HandwritingPhase.READY, phase())

        vm.closeHandwriting()
        vm.openHandwriting() // already downloaded: straight to writing
        assertEquals(HandwritingPhase.READY, phase())
        assertEquals(1, recognizer.downloads)
    }

    @Test
    fun `a failed download offers retry`() {
        recognizer.failDownload = true
        vm.openHandwriting()
        vm.onHandwritingAccept()
        assertEquals(HandwritingPhase.FAILED, phase())
        recognizer.failDownload = false
        vm.onHandwritingRetry()
        assertEquals(HandwritingPhase.READY, phase())
    }

    private fun ready() {
        prefs.handwriting = true
        recognizer.ready = true
        vm.openHandwriting()
        assertEquals(HandwritingPhase.READY, phase())
    }

    @Test
    fun `strokes are recognised with the text before the cursor as context`() {
        ready()
        vm.onInkStroke(stroke, 300f, 100f)
        vm.onInkStroke(stroke, 300f, 100f)
        assertEquals(listOf("कक", "x"), vm.state.value.inkCandidates)
        assertEquals("म ", recognizer.lastPreContext)
    }

    @Test
    fun `tapping a candidate types it and clears the writing`() {
        ready()
        vm.onInkStroke(stroke, 300f, 100f)
        vm.onInkCandidate("x")
        verify { ic.commitText("x", 1) }
        assertTrue(vm.state.value.inkStrokes.isEmpty())
        assertTrue(vm.state.value.inkCandidates.isEmpty())
    }

    @Test
    fun `space types the best guess then a space`() {
        ready()
        vm.onInkStroke(stroke, 300f, 100f)
        vm.onInkSpace()
        verifyOrder {
            ic.commitText("क", 1)
            ic.commitText(" ", 1)
        }
    }

    @Test
    fun `backspace removes the last stroke first, then deletes text`() {
        ready()
        vm.onInkStroke(stroke, 300f, 100f)
        vm.onInkStroke(stroke, 300f, 100f)
        vm.onInkBackspace()
        assertEquals(1, vm.state.value.inkStrokes.size)
        assertEquals("क", vm.state.value.inkCandidates.first())
        vm.onInkBackspace()
        assertTrue(vm.state.value.inkCandidates.isEmpty())
        verify(exactly = 0) { ic.deleteSurroundingText(any(), any()) }
        vm.onInkBackspace()
        verify { ic.deleteSurroundingText(any(), 0) }
    }

    @Test
    fun `closing types what's written so it isn't lost`() {
        ready()
        vm.onInkStroke(stroke, 300f, 100f)
        vm.closeHandwriting()
        verify { ic.commitText("क", 1) }
        assertNull(phase())
    }

    @Test
    fun `strokes before the model is ready are ignored`() {
        vm.openHandwriting() // consent
        vm.onInkStroke(stroke, 300f, 100f)
        assertTrue(vm.state.value.inkStrokes.isEmpty())
    }
}
