package io.github.thegbguy.nepalikeyboard.ui.keyboard.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle

// Hindi rather than Nepali: Samsung's speech engine (the default on Galaxy phones) has a Hindi
// voice but no Nepali one, and a locale the engine lacks is ignored, leaving Devanagari silent.
// Hindi reads Devanagari letters and most Nepali words the same way.
private val nepaliSpeech = SpanStyle(localeList = LocaleList("hi-IN"))

private fun isDevanagari(c: Char) = c in '\u0900'..'\u097F'

/**
 * [text] with its Devanagari runs tagged for a Devanagari voice. TalkBack reads text in the phone's language,
 * which leaves Nepali letters silent on an English phone; a locale span (Compose turns it into a
 * `LocaleSpan`) makes it switch voices for those runs.
 */
fun withNepaliSpeech(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val devanagari = isDevanagari(text[i])
        var end = i
        while (end < text.length && (isDevanagari(text[end]) == devanagari || (devanagari && text[end] == '\u200D'))) end++
        if (devanagari) withStyle(nepaliSpeech) { append(text, i, end) } else append(text, i, end)
        i = end
    }
}
