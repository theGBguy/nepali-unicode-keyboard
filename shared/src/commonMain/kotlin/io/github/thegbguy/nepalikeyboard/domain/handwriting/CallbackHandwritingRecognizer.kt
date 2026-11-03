package io.github.thegbguy.nepalikeyboard.domain.handwriting

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [HandwritingRecognizer] with completion callbacks instead of suspend functions, for platforms
 * that can't implement Kotlin's suspend functions (Swift). Wrap one in
 * [CallbackHandwritingRecognizer] to give it to the keyboard. Each callback must be called once.
 */
interface HandwritingEngine {
    fun isModelReady(done: (Boolean) -> Unit)

    /** Calls [done] with null when the model is on the phone, else with what went wrong. */
    fun download(done: (String?) -> Unit)

    /** Calls [done] with the candidates, best first; an empty list when recognition failed. */
    fun recognize(strokes: List<InkStroke>, width: Float, height: Float, preContext: String, done: (List<String>) -> Unit)

    fun deleteModel(done: () -> Unit)
}

/** Thrown by [CallbackHandwritingRecognizer.download] when the engine reports a failure. */
class HandwritingDownloadException(message: String) : Exception(message)

class CallbackHandwritingRecognizer(private val engine: HandwritingEngine) : HandwritingRecognizer {

    override suspend fun isModelReady(): Boolean = suspendCancellableCoroutine { cont ->
        engine.isModelReady { ready -> if (cont.isActive) cont.resume(ready) }
    }

    override suspend fun download(): Unit = suspendCancellableCoroutine { cont ->
        engine.download { error ->
            if (!cont.isActive) return@download
            if (error == null) cont.resume(Unit) else cont.resumeWithException(HandwritingDownloadException(error))
        }
    }

    override suspend fun recognize(strokes: List<InkStroke>, width: Float, height: Float, preContext: String): List<String> {
        if (strokes.isEmpty()) return emptyList()
        return suspendCancellableCoroutine { cont ->
            engine.recognize(strokes, width, height, preContext) { candidates -> if (cont.isActive) cont.resume(candidates) }
        }
    }

    override suspend fun deleteModel(): Unit = suspendCancellableCoroutine { cont ->
        engine.deleteModel { if (cont.isActive) cont.resume(Unit) }
    }
}
