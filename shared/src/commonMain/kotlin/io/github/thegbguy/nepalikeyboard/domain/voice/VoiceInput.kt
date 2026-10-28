package io.github.thegbguy.nepalikeyboard.domain.voice

/** Why voice typing stopped; each platform shows its own message for it. */
enum class VoiceError {
    UNAVAILABLE,
    START_FAILED,
    MIC_PERMISSION,
    AUDIO,
    NETWORK,
    NO_SPEECH,
    BUSY,
    SERVER,
    LANGUAGE,
    GENERIC
}

/** What a recognition session reports back to the keyboard. */
interface VoiceListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onEndOfSpeech()
    fun onResults(results: List<String>)
    fun onError(error: VoiceError)
    fun onPartialResults(partialResults: List<String>)
}

/** Speech recognition in Nepali (Android: SpeechRecognitionManager). */
interface VoiceInput {
    fun isRecognitionAvailable(): Boolean
    fun setListener(listener: VoiceListener)
    fun startListening()
    fun stopListening()
    fun destroy()
}
