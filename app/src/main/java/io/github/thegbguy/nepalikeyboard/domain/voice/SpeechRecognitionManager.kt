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
class SpeechRecognitionManager(private val context: Context) : VoiceInput {

    companion object {
        private const val TAG = "NepaliVoiceRecognition"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: VoiceListener? = null
    private var isListening = false

    /**
     * Check if speech recognition is available on this device.
     */
    override fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Set the listener for speech recognition events.
     */
    override fun setListener(listener: VoiceListener) {
        this.listener = listener
    }

    /**
     * Start listening for speech input.
     * Uses Nepali (ne-NP) locale for recognition.
     */
    override fun startListening() {
        if (isListening) return

        if (!isRecognitionAvailable()) {
            listener?.onError(VoiceError.UNAVAILABLE)
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
            listener?.onError(VoiceError.START_FAILED)
            isListening = false
        }
    }

    /**
     * Stop listening for speech input.
     */
    override fun stopListening() {
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
    override fun destroy() {
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
                Log.e(TAG, "Speech recognition error code: $error")
                listener?.onError(voiceError(error))
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    listener?.onResults(matches)
                } else {
                    Log.d(TAG, "No speech results detected")
                    listener?.onError(VoiceError.NO_SPEECH)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    listener?.onPartialResults(matches)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        }
    }

    private fun voiceError(errorCode: Int): VoiceError {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> VoiceError.AUDIO
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.MIC_PERMISSION
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceError.NETWORK
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.NO_SPEECH
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceError.BUSY
            SpeechRecognizer.ERROR_SERVER -> VoiceError.SERVER
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> VoiceError.LANGUAGE
            else -> VoiceError.GENERIC
        }
    }
}
