package io.github.thegbguy.nepalikeyboard.domain.voice

import androidx.annotation.StringRes
import io.github.thegbguy.nepalikeyboard.R

/** The message the strip shows for this error. */
@get:StringRes
val VoiceError.messageRes: Int
    get() = when (this) {
        VoiceError.UNAVAILABLE -> R.string.voice_error_unavailable
        VoiceError.START_FAILED -> R.string.voice_error_start_failed
        VoiceError.MIC_PERMISSION -> R.string.voice_error_mic_permission
        VoiceError.AUDIO -> R.string.voice_error_audio
        VoiceError.NETWORK -> R.string.voice_error_network
        VoiceError.NO_SPEECH -> R.string.voice_error_no_speech
        VoiceError.BUSY -> R.string.voice_error_busy
        VoiceError.SERVER -> R.string.voice_error_server
        VoiceError.LANGUAGE -> R.string.voice_error_language
        VoiceError.GENERIC -> R.string.voice_error_generic
    }
