package io.github.thegbguy.nepalikeyboard.data.handwriting

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import io.github.thegbguy.nepalikeyboard.domain.handwriting.HandwritingRecognizer
import io.github.thegbguy.nepalikeyboard.domain.handwriting.InkStroke
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Nepali handwriting with Google ML Kit digital ink recognition. Recognition runs on the phone;
 * the model (`ne`) is downloaded from Google once.
 *
 * ML Kit's start-up provider is removed from the manifest, so nothing in ML Kit runs (or
 * contacts Google) until this class is first used, which only happens after the user agrees.
 * Google's ML Kit then sends usage diagnostics (device model, app version, an install ID,
 * timings, error codes; see developers.google.com/ml-kit/android-data-disclosure). The ink
 * itself isn't sent.
 */
class MlKitHandwriting(context: Context) : HandwritingRecognizer {

    private val appContext = context.applicationContext
    /** Starts ML Kit on first use (its start-up provider is removed from the manifest). */
    private val started by lazy { MlKitContext.initializeIfNeeded(appContext) }
    private val model by lazy {
        started
        DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.fromLanguageTag(LANGUAGE)!!).build()
    }
    private val manager: RemoteModelManager
        get() {
            started
            return RemoteModelManager.getInstance()
        }
    private var recognizer: DigitalInkRecognizer? = null

    override suspend fun isModelReady(): Boolean = manager.isModelDownloaded(model).await() == true

    override suspend fun download() {
        try {
            manager.download(model, DownloadConditions.Builder().build()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Handwriting model download failed", e)
            throw e
        }
    }

    override suspend fun recognize(strokes: List<InkStroke>, width: Float, height: Float, preContext: String): List<String> {
        if (strokes.isEmpty()) return emptyList()
        val ink = Ink.builder().apply {
            strokes.forEach { stroke ->
                addStroke(Ink.Stroke.builder().apply { stroke.forEach { addPoint(Ink.Point.create(it.x, it.y, it.t)) } }.build())
            }
        }.build()
        val context = RecognitionContext.builder()
            .setPreContext(preContext)
            .setWritingArea(WritingArea(width, height))
            .build()
        val client = recognizer ?: DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).setMaxResultCount(MAX_CANDIDATES).build()
        ).also { recognizer = it }
        return client.recognize(ink, context).await().candidates.map { it.text }
    }

    override suspend fun deleteModel() {
        recognizer?.close()
        recognizer = null
        if (isModelReady()) manager.deleteDownloadedModel(model).await()
    }

    companion object {
        private const val TAG = "Handwriting"
        private const val LANGUAGE = "ne"
        private const val MAX_CANDIDATES = 5

        @Volatile private var instance: MlKitHandwriting? = null

        /** One per process: the keyboard and Settings (which deletes the model) share it. */
        fun shared(context: Context): MlKitHandwriting =
            instance ?: synchronized(this) { instance ?: MlKitHandwriting(context).also { instance = it } }
    }
}

/** Awaits a Play services [Task] without pulling in kotlinx-coroutines-play-services. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
