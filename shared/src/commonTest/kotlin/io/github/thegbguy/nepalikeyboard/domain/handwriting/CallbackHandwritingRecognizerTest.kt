package io.github.thegbguy.nepalikeyboard.domain.handwriting

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallbackHandwritingRecognizerTest {

    /** Holds each callback until the test answers it, like ML Kit answering later. */
    private class FakeEngine : HandwritingEngine {
        var ready: ((Boolean) -> Unit)? = null
        var downloaded: ((String?) -> Unit)? = null
        var recognized: ((List<String>) -> Unit)? = null
        var recognizeCalls = 0
        var lastPreContext: String? = null
        var deleted = false

        override fun isModelReady(done: (Boolean) -> Unit) { ready = done }
        override fun download(done: (String?) -> Unit) { downloaded = done }
        override fun recognize(strokes: List<InkStroke>, width: Float, height: Float, preContext: String, done: (List<String>) -> Unit) {
            recognizeCalls++
            lastPreContext = preContext
            recognized = done
        }
        override fun deleteModel(done: () -> Unit) { deleted = true; done() }
    }

    private val stroke = listOf(InkPoint(1f, 2f, 0), InkPoint(3f, 4f, 16))

    @Test
    fun `model readiness comes from the callback`() = runTest {
        val engine = FakeEngine()
        val recognizer = CallbackHandwritingRecognizer(engine)
        val ready = async { recognizer.isModelReady() }
        testScheduler.runCurrent()
        engine.ready!!(true)
        assertTrue(ready.await())
    }

    @Test
    fun `a download error is thrown so the panel shows Try again`() = runTest {
        val engine = FakeEngine()
        val recognizer = CallbackHandwritingRecognizer(engine)
        val download = async { runCatching { recognizer.download() } }
        testScheduler.runCurrent()
        engine.downloaded!!("offline")
        val error = download.await().exceptionOrNull()
        assertTrue(error is HandwritingDownloadException)
        assertEquals("offline", error.message)
    }

    @Test
    fun `a finished download returns normally`() = runTest {
        val engine = FakeEngine()
        val recognizer = CallbackHandwritingRecognizer(engine)
        val download = async { recognizer.download() }
        testScheduler.runCurrent()
        engine.downloaded!!(null)
        download.await()
        assertFalse(download.isCancelled)
    }

    @Test
    fun `candidates and context pass through`() = runTest {
        val engine = FakeEngine()
        val recognizer = CallbackHandwritingRecognizer(engine)
        val result = async { recognizer.recognize(listOf(stroke), 300f, 150f, "नमस्ते ") }
        testScheduler.runCurrent()
        assertEquals("नमस्ते ", engine.lastPreContext)
        engine.recognized!!(listOf("नेपाली", "नेपाल"))
        assertEquals(listOf("नेपाली", "नेपाल"), result.await())
    }

    @Test
    fun `nothing written asks the engine nothing`() = runTest {
        val engine = FakeEngine()
        assertEquals(emptyList(), CallbackHandwritingRecognizer(engine).recognize(emptyList(), 300f, 150f, ""))
        assertEquals(0, engine.recognizeCalls)
    }

    @Test
    fun `a late answer after cancelling is ignored`() = runTest {
        val engine = FakeEngine()
        val recognizer = CallbackHandwritingRecognizer(engine)
        val result = async { recognizer.recognize(listOf(stroke), 300f, 150f, "") }
        testScheduler.runCurrent()
        result.cancel()
        testScheduler.runCurrent()
        // Must not crash with "already resumed"
        engine.recognized!!(listOf("न"))
        assertTrue(result.isCancelled)
    }

    @Test
    fun `deleting the model reaches the engine`() = runTest {
        val engine = FakeEngine()
        CallbackHandwritingRecognizer(engine).deleteModel()
        assertTrue(engine.deleted)
    }

    @Test
    fun `cancelling a download is not reported as a failure later`() = runTest {
        val engine = FakeEngine()
        val recognizer = CallbackHandwritingRecognizer(engine)
        val download = async { recognizer.download() }
        testScheduler.runCurrent()
        download.cancel()
        testScheduler.runCurrent()
        engine.downloaded!!("cancelled")
        assertFailsWith<kotlinx.coroutines.CancellationException> { download.await() }
    }
}
