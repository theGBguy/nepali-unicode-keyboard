package io.github.thegbguy.nepalikeyboard.domain.numbers

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartAction
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartMatch
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartSource

/**
 * Amounts typed just before the cursor: `1500000` → `१५,००,०००` and `पन्ध्र लाख`.
 *
 * A number counts when it has at least 5 digits, or a currency marker in front (`rs`, `Rs.`,
 * `NPR`, `रु`, `रु.`). So years, PINs and short numbers in a sentence get nothing. Phone numbers
 * (10 digits starting with 9), numbers with a leading zero and anything over 99 खर्ब are left
 * alone too. One space after the number is allowed, because Romanized typing commits the
 * number with a space.
 *
 * The grouped form keeps the marker as typed. The words replace the marker as well, cheque style:
 * `रु 1500000` → `पन्ध्र लाख रुपैयाँ मात्र`. Digits follow the mode: Devanagari in the Nepali
 * modes, Latin in English.
 */
object AmountSource : SmartSource {

    private const val MIN_DIGITS = 5
    private val MARKERS = listOf("rs.", "rs", "npr", "रु.", "रु", "रू.", "रू")

    override fun suggest(text: String, mode: LanguageMode): List<SmartMatch> {
        val amount = parse(text) ?: return emptyList()
        val nepali = mode != LanguageMode.ENGLISH
        val space = if (amount.trailingSpace) " " else ""
        val extra = space.length
        return listOfNotNull(
            grouped(amount, nepali)?.let { SmartMatch(it + space, amount.number.length + extra, SmartAction.Kind.AMOUNT) },
            words(amount, nepali)?.let {
                val consumed = (if (amount.marker) amount.withMarker else amount.number.length) + extra
                SmartMatch(it + space, consumed, SmartAction.Kind.AMOUNT)
            }
        )
    }

    /**
     * @property number the number as typed (digits, commas, decimal part)
     * @property integer its whole part in Latin digits, without commas
     * @property decimals the part after the point in Latin digits, or null
     * @property withMarker length of marker + optional space + number
     */
    internal data class Amount(
        val number: String,
        val integer: String,
        val decimals: String?,
        val marker: Boolean,
        val withMarker: Int,
        val trailingSpace: Boolean
    )

    internal fun parse(text: String): Amount? {
        val trailingSpace = text.length >= 2 && text.last() == ' ' && text[text.length - 2] != ' '
        val body = if (trailingSpace) text.dropLast(1) else text

        // The run of digits, commas and points at the end, starting with a digit.
        var start = body.length
        while (start > 0 && (NepaliNumber.isDigit(body[start - 1]) || body[start - 1] in ",.")) start--
        while (start < body.length && !NepaliNumber.isDigit(body[start])) start++
        if (start == body.length || !NepaliNumber.isDigit(body.last())) return null
        val number = body.substring(start)

        val pieces = number.split('.')
        if (pieces.size > 2) return null // 21.10.2026 is a date, not an amount
        val whole = pieces[0]
        if (!validCommas(whole)) return null
        val integer = NepaliNumber.toLatinDigits(whole.replace(",", ""))
        val decimals = pieces.getOrNull(1)?.let(NepaliNumber::toLatinDigits)
        if (decimals != null && decimals.isEmpty()) return null

        val prefix = body.substring(0, start)
        val markerLength = markerAtEnd(prefix)
        val marker = markerLength > 0
        if (!marker && !isBoundary(prefix)) return null

        if (integer.length > 13) return null
        if (integer.length > 1 && integer[0] == '0') return null
        if (!marker) {
            if (integer.length < MIN_DIGITS) return null
            if (integer.length == 10 && integer[0] == '9' && ',' !in whole) return null
        }
        return Amount(number, integer, decimals, marker, markerLength + number.length, trailingSpace)
    }

    /**
     * Length of a currency marker (plus one space) at the end of [prefix], or 0. The marker must
     * be a word of its own: "pars 15000" has none.
     */
    private fun markerAtEnd(prefix: String): Int {
        val p = if (prefix.endsWith(" ")) prefix.dropLast(1) else prefix
        val spaced = prefix.length - p.length
        val found = MARKERS.firstOrNull { p.endsWith(it, ignoreCase = true) } ?: return 0
        return if (isBoundary(p.dropLast(found.length))) found.length + spaced else 0
    }

    /** True when [prefix] ends where a word may start: nothing, a space or punctuation. */
    private fun isBoundary(prefix: String): Boolean {
        val c = prefix.lastOrNull() ?: return true
        if (c.isLetterOrDigit()) return false
        val type = Character.getType(c)
        return type != Character.NON_SPACING_MARK.toInt() && type != Character.COMBINING_SPACING_MARK.toInt()
    }

    /** No commas, or lakh grouping (15,00,000), or thousands grouping (1,500,000). */
    private fun validCommas(whole: String): Boolean {
        if (',' !in whole) return true
        val groups = whole.split(',')
        if (groups.last().length != 3 || groups.any { it.isEmpty() }) return false
        val middle = groups.drop(1).dropLast(1)
        val first = groups.first().length
        val indian = first in 1..2 && middle.all { it.length == 2 }
        val western = first in 1..3 && middle.all { it.length == 3 }
        return indian || western
    }

    private fun digits(latin: String, nepali: Boolean) = if (nepali) NepaliNumber.toNepaliDigits(latin) else latin

    /** `१५,००,०००`, or null when there's nothing to group or it's already grouped that way. */
    private fun grouped(amount: Amount, nepali: Boolean): String? {
        if (amount.integer.length < 4) return null
        val decimals = amount.decimals?.let { "." + digits(it, nepali) } ?: ""
        val result = digits(NepaliNumber.groupIndian(amount.integer), nepali) + decimals
        return result.takeIf { it != amount.number }
    }

    private fun words(amount: Amount, nepali: Boolean): String? {
        val value = amount.integer.toLong()
        if (!amount.marker) {
            val whole = (if (nepali) NepaliNumber.nepaliWords(value) else NepaliNumber.englishWords(value)) ?: return null
            val decimals = amount.decimals ?: return whole
            val point = if (nepali) "दशमलव" else "Point"
            return "$whole $point ${NepaliNumber.digitWords(decimals, nepali)}"
        }
        // Cheque style: rupees and paisa in words, then मात्र / Only.
        val paisa = when (amount.decimals?.length) {
            null -> 0
            1 -> amount.decimals.toInt() * 10
            2 -> amount.decimals.toInt()
            else -> return null
        }
        if (value == 0L && paisa == 0) return null
        val parts = mutableListOf<String>()
        if (nepali) {
            if (value > 0) parts += NepaliNumber.nepaliWords(value) + " रुपैयाँ"
            if (paisa > 0) parts += NepaliNumber.nepaliWords(paisa.toLong()) + " पैसा"
            return parts.joinToString(" ") + " मात्र"
        }
        if (value > 0) parts += NepaliNumber.englishWords(value) + if (value == 1L) " Rupee" else " Rupees"
        if (paisa > 0) parts += NepaliNumber.englishWords(paisa.toLong()) + " Paisa"
        return parts.joinToString(" and ") + " Only"
    }
}
