package io.github.thegbguy.nepalikeyboard.domain.calendar

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.numbers.NepaliNumber
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartAction
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartMatch
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartSource

/**
 * Bikram Sambat dates in the strip.
 *
 * - A day word: Romanized `aaja`/`bholi`/`hijo`/`parsi`/`asti` while it's being typed, the same
 *   words in Devanagari (आज, भोलि …) just before the cursor, or `today`/`tomorrow`/`yesterday` in
 *   English. Offers that day as `२०८३ कार्तिक १०, मंगलबार` and `२०८३/०७/१०`.
 * - `miti` / मिति: `मिति: २०८३/०७/१०`, for letters and forms.
 * - A typed date (`2026-10-21`, `21/10/2026`, `२०८३-०७-०४`, with - / or .): an AD year gets its BS
 *   date and a BS year its AD date. Years 2000–2034 could be either, so both are offered.
 *
 * The suggestion replaces what was typed. Nepali modes give Devanagari, English mode Latin.
 *
 * @param today today's date as an epoch day; Nepal time by default
 */
class DateSource(private val today: () -> Long = { Gregorian.todayInNepal() }) : SmartSource {

    override fun suggest(text: String, mode: LanguageMode): List<SmartMatch> {
        val trailingSpace = text.length >= 2 && text.last() == ' ' && text[text.length - 2] != ' '
        val body = if (trailingSpace) text.dropLast(1) else text
        val space = if (trailingSpace) " " else ""
        val nepali = mode != LanguageMode.ENGLISH
        return dayWord(body, mode, nepali, space) ?: typedDate(body, nepali, space) ?: emptyList()
    }

    private fun dayWord(body: String, mode: LanguageMode, nepali: Boolean, space: String): List<SmartMatch>? {
        val word = lastWord(body) ?: return null
        if (!boundaryBefore(body, body.length - word.length)) return null
        val key = word.lowercase()
        val offset = when (mode) {
            LanguageMode.ENGLISH -> ENGLISH_DAYS[key]
            else -> ROMAN_DAYS[key] ?: NEPALI_DAYS[word]
        } ?: return null
        val day = today() + (if (offset == MITI) 0 else offset)
        val bs = BsCalendar.toBs(day) ?: return null
        val consumed = word.length + space.length
        val results = if (offset == MITI) {
            val label = if (nepali) "मिति: " else "Date: "
            listOf(label + numeric(bs, nepali), label + long(bs, day, nepali))
        } else {
            listOf(long(bs, day, nepali), numeric(bs, nepali))
        }
        return results.map { SmartMatch(it + space, consumed, SmartAction.Kind.DATE) }
    }

    private fun typedDate(body: String, nepali: Boolean, space: String): List<SmartMatch>? {
        val match = DATE.find(body) ?: return null
        if (!boundaryBefore(body, match.range.first)) return null
        val g = match.groupValues
        val (year, month, day) = if (g[1].isNotEmpty()) {
            if (g[2] != g[4]) return null // 2026-10/21: mixed separators
            Triple(number(g[1]), number(g[3]), number(g[5]))
        } else {
            if (g[7] != g[9]) return null
            Triple(number(g[10]), number(g[8]), number(g[6]))
        }
        val consumed = match.value.length + space.length
        val results = mutableListOf<String>()
        // As an AD date → BS
        if (Gregorian.isValid(year, month, day)) {
            val epochDay = Gregorian.toEpochDay(year, month, day)
            BsCalendar.toBs(epochDay)?.let { results += long(it, epochDay, nepali) }
        }
        // As a BS date → AD
        BsCalendar.toEpochDay(BsDate(year, month, day))?.let { results += ad(it, nepali) }
        return results.map { SmartMatch(it + space, consumed, SmartAction.Kind.DATE) }
    }

    /** `२०८३ कार्तिक १०, मंगलबार` / `2083 Kartik 10 B.S.` */
    private fun long(bs: BsDate, epochDay: Long, nepali: Boolean): String =
        if (nepali) {
            val digits = NepaliNumber::toNepaliDigits
            "${digits(bs.year.toString())} ${BS_MONTHS_NE[bs.month - 1]} ${digits(bs.day.toString())}, ${WEEKDAYS_NE[Gregorian.weekday(epochDay)]}"
        } else {
            "${bs.year} ${BS_MONTHS_EN[bs.month - 1]} ${bs.day} B.S."
        }

    /** `२०८३/०७/१०` / `2083/07/10 B.S.` */
    private fun numeric(bs: BsDate, nepali: Boolean): String {
        val s = "${bs.year}/${bs.month.toString().padStart(2, '0')}/${bs.day.toString().padStart(2, '0')}"
        return if (nepali) NepaliNumber.toNepaliDigits(s) else "$s B.S."
    }

    /** `सन् २०२६ अक्टोबर २१, बुधबार` / `21 October 2026` */
    private fun ad(epochDay: Long, nepali: Boolean): String {
        val (y, m, d) = Gregorian.fromEpochDay(epochDay)
        return if (nepali) {
            val digits = NepaliNumber::toNepaliDigits
            "सन् ${digits(y.toString())} ${AD_MONTHS_NE[m - 1]} ${digits(d.toString())}, ${WEEKDAYS_NE[Gregorian.weekday(epochDay)]}"
        } else {
            "$d ${AD_MONTHS_EN[m - 1]} $y"
        }
    }

    private fun number(s: String): Int = NepaliNumber.toLatinDigits(s).toInt()

    /** The word at the end of [body] (letters and vowel signs), if it starts a word. */
    private fun lastWord(body: String): String? {
        var start = body.length
        while (start > 0 && isWordChar(body[start - 1])) start--
        if (start == body.length) return null
        return body.substring(start)
    }

    private fun isWordChar(c: Char): Boolean {
        if (c.isLetter()) return true
        val type = Character.getType(c)
        return type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()
    }

    private fun boundaryBefore(body: String, index: Int): Boolean {
        val c = body.getOrNull(index - 1) ?: return true
        return !c.isLetterOrDigit() && !isWordChar(c)
    }

    private companion object {
        /** Offset used for मिति: today, written as a form date. */
        const val MITI = Int.MIN_VALUE

        val ROMAN_DAYS = mapOf(
            "aaja" to 0, "aaj" to 0, "aja" to 0,
            "bholi" to 1, "bholli" to 1,
            "parsi" to 2, "parshi" to 2,
            "hijo" to -1,
            "asti" to -2,
            "miti" to MITI
        )
        val NEPALI_DAYS = mapOf(
            "आज" to 0, "भोलि" to 1, "भोली" to 1, "पर्सि" to 2, "पर्सी" to 2,
            "हिजो" to -1, "अस्ति" to -2, "मिति" to MITI
        )
        val ENGLISH_DAYS = mapOf("today" to 0, "tomorrow" to 1, "yesterday" to -1)

        private const val D = "[0-9०-९]"
        /** Y-M-D or D-M-Y with - / or . at the end of the text. */
        val DATE = Regex("(?:($D{4})([-/.])($D{1,2})([-/.])($D{1,2})|($D{1,2})([-/.])($D{1,2})([-/.])($D{4}))$")

        val BS_MONTHS_NE = listOf("बैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज", "कार्तिक", "मंसिर", "पुस", "माघ", "फागुन", "चैत")
        val BS_MONTHS_EN = listOf("Baisakh", "Jestha", "Asar", "Shrawan", "Bhadra", "Asoj", "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra")
        val WEEKDAYS_NE = listOf("आइतबार", "सोमबार", "मंगलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार")
        val AD_MONTHS_NE = listOf("जनवरी", "फेब्रुअरी", "मार्च", "अप्रिल", "मे", "जुन", "जुलाई", "अगस्ट", "सेप्टेम्बर", "अक्टोबर", "नोभेम्बर", "डिसेम्बर")
        val AD_MONTHS_EN = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
}
