package io.github.thegbguy.nepalikeyboard.domain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Manager class for handling speech recognition.
 * Wraps Android's SpeechRecognizer API with Nepali language support.
 */
class SpeechRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "NepaliVoiceRecognition"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: SpeechRecognitionListener? = null
    private var isListening = false

    /**
     * Listener interface for speech recognition events.
     */
    interface SpeechRecognitionListener {
        fun onReadyForSpeech()
        fun onBeginningOfSpeech()
        fun onEndOfSpeech()
        fun onResults(results: List<String>)
        fun onError(errorMessage: String)
        fun onPartialResults(partialResults: List<String>)
    }

    /**
     * Check if speech recognition is available on this device.
     */
    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Set the listener for speech recognition events.
     */
    fun setListener(listener: SpeechRecognitionListener) {
        this.listener = listener
    }

    /**
     * Start listening for speech input.
     * Uses Nepali (ne-NP) locale for recognition.
     */
    fun startListening() {
        if (isListening) return

        if (!isRecognitionAvailable()) {
            listener?.onError("Speech recognition not available")
            return
        }

        // Create SpeechRecognizer on main thread
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ne-NP") // Nepali locale
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ne-NP")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ne-NP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            Log.d(TAG, "Starting Nepali speech recognition (ne-NP)")
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition", e)
            listener?.onError("Failed to start speech recognition")
            isListening = false
        }
    }

    /**
     * Stop listening for speech input.
     */
    fun stopListening() {
        if (!isListening) return

        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore errors when stopping
        }
        isListening = false
    }

    /**
     * Cancel the current recognition session.
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignore errors when canceling
        }
        isListening = false
    }

    /**
     * Release all resources.
     * Call this when the manager is no longer needed.
     */
    fun destroy() {
        cancel()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore errors when destroying
        }
        speechRecognizer = null
        listener = null
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech input")
                listener?.onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "User started speaking")
                listener?.onBeginningOfSpeech()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Could be used for audio level visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "User stopped speaking, processing...")
                isListening = false
                listener?.onEndOfSpeech()
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = getErrorMessage(error)
                Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")
                listener?.onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "=== Final Speech Recognition Results ===")
                    matches.forEachIndexed { index, result ->
                        Log.d(TAG, "Result [$index]: $result")
                    }
                    Log.d(TAG, "Selected (best match): ${matches.first()}")
                    listener?.onResults(matches)
                } else {
                    Log.d(TAG, "No speech results detected")
                    listener?.onError("No speech detected")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial result: ${matches.first()}")
                    listener?.onPartialResults(matches)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        }
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Recognition error"
        }
    }
}
