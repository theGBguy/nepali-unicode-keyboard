package io.github.thegbguy.nepalikeyboard.domain.units

import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import io.github.thegbguy.nepalikeyboard.domain.numbers.NepaliNumber
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartAction
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartMatch
import io.github.thegbguy.nepalikeyboard.domain.smart.SmartSource
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Land and gold units: `5 aana` → `५ आना (१,७११ वर्ग फिट)`, `2 tola` → `२ तोला (२३.३३ ग्राम)`.
 *
 * The conversion is added after the text, which stays (in Devanagari in the Nepali modes, as typed
 * in English). Understood:
 * - hill land: ropani, aana, paisa, daam, alone or together (`2 ropani 5 aana`), and the
 *   R-A-P-D notation (`0-5-2-0`) → sq ft and m²
 * - Terai land: bigha, kattha, dhur → sq ft and m²
 * - sq ft and m² → each other and ropani-aana-paisa-daam
 * - tola and lal → grams; grams → tola
 *
 * `paisa` alone is money, so it only counts next to another hill unit.
 *
 * Figures (Survey Department of Nepal): 1 ropani = 16 aana = 64 paisa = 256 daam = 5,476 sq ft
 * (74 ft × 74 ft); 1 bigha = 20 kattha = 400 dhur = 72,900 sq ft (270 ft × 270 ft); 1 sq ft =
 * 0.09290304 m². 1 tola = 100 lal = 11.664 g.
 */
object UnitSource : SmartSource {

    private enum class Group { HILL, TERAI, SQFT, M2, TOLA, GRAM }

    /** @property size in sq ft for land and area units, in grams for gold units */
    private class Unit(val group: Group, val size: Double, val ne: String, val en: String, vararg val aliases: String)

    private const val SQ_FT_PER_M2 = 1 / 0.09290304
    private const val SQ_FT_PER_DAAM = 5476.0 / 256

    private val UNITS = listOf(
        Unit(Group.HILL, 5476.0, "रोपनी", "ropani", "ropani", "ropni", "रोपनी", "रोपनि"),
        Unit(Group.HILL, 5476.0 / 16, "आना", "aana", "aana", "ana", "anna", "आना"),
        Unit(Group.HILL, 5476.0 / 64, "पैसा", "paisa", "paisa", "पैसा"),
        Unit(Group.HILL, SQ_FT_PER_DAAM, "दाम", "daam", "daam", "dam", "दाम"),
        Unit(Group.TERAI, 72900.0, "बिघा", "bigha", "bigha", "बिघा", "बिगाहा"),
        Unit(Group.TERAI, 72900.0 / 20, "कट्ठा", "kattha", "kattha", "katha", "कट्ठा", "कठ्ठा"),
        Unit(Group.TERAI, 72900.0 / 400, "धुर", "dhur", "dhur", "धुर"),
        Unit(Group.SQFT, 1.0, "वर्ग फिट", "sq ft", "sqft", "sq ft", "sq.ft", "sq. ft", "ft²", "वर्ग फिट", "वर्गफिट"),
        Unit(Group.M2, SQ_FT_PER_M2, "वर्ग मिटर", "m²", "m2", "m²", "sqm", "sq m", "वर्ग मिटर", "वर्गमिटर"),
        Unit(Group.TOLA, 11.664, "तोला", "tola", "tola", "tolaa", "तोला"),
        Unit(Group.TOLA, 0.11664, "लाल", "lal", "lal", "laal", "लाल"),
        Unit(Group.GRAM, 1.0, "ग्राम", "g", "gram", "grams", "gm", "ग्राम")
    )
    private val BY_ALIAS = UNITS.flatMap { u -> u.aliases.map { it.lowercase() to u } }.toMap()
    private val PAISA = UNITS[2]

    private const val NUM = "[0-9०-९]+(?:\\.[0-9०-९]+)?"
    private val UNIT_ALT = BY_ALIAS.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
    private val PAIR = Regex("($NUM) ?($UNIT_ALT)", RegexOption.IGNORE_CASE)
    private val COMPOUND = Regex("(?:$NUM ?(?:$UNIT_ALT) )*$NUM ?(?:$UNIT_ALT)$", RegexOption.IGNORE_CASE)
    private val RAPD = Regex("([0-9०-९]{1,4})-([0-9०-९]{1,2})-([0-9०-९])-([0-9०-९])$")

    override fun suggest(text: String, mode: LanguageMode): List<SmartMatch> {
        val trailingSpace = text.length >= 2 && text.last() == ' ' && text[text.length - 2] != ' '
        val body = if (trailingSpace) text.dropLast(1) else text
        val space = if (trailingSpace) " " else ""
        val nepali = mode != LanguageMode.ENGLISH
        val found = rapd(body, nepali) ?: compound(body, nepali) ?: return emptyList()
        return found.conversions.map {
            // The strip shows just the conversion; tapping adds it after the typed amount.
            SmartMatch("${found.typed} ($it)$space", found.length + space.length, SmartAction.Kind.UNIT, label = it)
        }
    }

    /**
     * @property length how many characters at the end were recognised
     * @property typed what they're rewritten as (Devanagari in the Nepali modes)
     */
    private class Found(val length: Int, val typed: String, val conversions: List<String>)

    /** `0-5-2-0` (ropani-aana-paisa-daam). */
    private fun rapd(body: String, nepali: Boolean): Found? {
        val m = RAPD.find(body) ?: return null
        if (!boundaryBefore(body, m.range.first)) return null
        val (r, a, p, d) = m.groupValues.drop(1).map { NepaliNumber.toLatinDigits(it).toInt() }
        if (a >= 16 || p >= 4 || d >= 4) return null
        val sqft = r * 5476.0 + a * 5476.0 / 16 + p * 5476.0 / 64 + d * SQ_FT_PER_DAAM
        if (sqft == 0.0) return null
        return Found(m.value.length, digits(m.value, nepali), listOf(sqFt(sqft, nepali), m2(sqft, nepali)))
    }

    /** `2 ropani 5 aana`, `5 aana`, `2 tola`, `1711 sqft` … */
    private fun compound(body: String, nepali: Boolean): Found? {
        val m = COMPOUND.find(body) ?: return null
        if (!boundaryBefore(body, m.range.first)) return null
        val pairs = PAIR.findAll(m.value).map { p ->
            NepaliNumber.toLatinDigits(p.groupValues[1]).toDouble() to BY_ALIAS.getValue(p.groupValues[2].lowercase())
        }.toList()
        val group = pairs.first().second.group
        if (pairs.any { it.second.group != group }) return null
        if (pairs.size > 1 && group !in setOf(Group.HILL, Group.TERAI, Group.TOLA)) return null
        if (pairs.all { it.second === PAISA }) return null // "50 paisa" is money
        val total = pairs.sumOf { (n, u) -> n * u.size }
        if (total <= 0.0) return null

        val typed = if (nepali) {
            pairs.joinToString(" ") { (n, u) -> number(n, nepali) + " " + u.ne }
        } else m.value

        val conversions = when (group) {
            Group.HILL, Group.TERAI -> listOf(sqFt(total, nepali), m2(total, nepali))
            Group.SQFT -> listOfNotNull(hill(total, nepali), m2(total, nepali))
            Group.M2 -> listOfNotNull(sqFt(total, nepali), hill(total, nepali))
            Group.TOLA -> listOf(decimal(total, nepali) + if (nepali) " ग्राम" else " g")
            Group.GRAM -> listOf(decimal(total / 11.664, nepali) + if (nepali) " तोला" else " tola")
        }
        return Found(m.value.length, typed, conversions)
    }

    private fun sqFt(sqft: Double, nepali: Boolean): String =
        digits(NepaliNumber.groupIndian(sqft.roundToLong().toString()), nepali) + if (nepali) " वर्ग फिट" else " sq ft"

    private fun m2(sqft: Double, nepali: Boolean): String =
        decimal(sqft / SQ_FT_PER_M2, nepali) + if (nepali) " वर्ग मिटर" else " m²"

    /** Area as `1 रोपनी 3 आना 2 पैसा 1.5 दाम`, daam to one decimal; null if under half a daam. */
    private fun hill(sqft: Double, nepali: Boolean): String? {
        val tenthsOfDaam = (sqft / SQ_FT_PER_DAAM * 10).roundToLong()
        if (tenthsOfDaam < 5) return null
        val wholeDaam = tenthsOfDaam / 10
        val parts = listOf(
            wholeDaam / 256 to UNITS[0],
            wholeDaam % 256 / 16 to UNITS[1],
            wholeDaam % 16 / 4 to UNITS[2]
        ).filter { it.first > 0 }.map { (n, u) -> digits(n.toString(), nepali) + " " + if (nepali) u.ne else u.en }
        val daam = wholeDaam % 4 * 10 + tenthsOfDaam % 10
        val daamText = if (daam == 0L) null else {
            val s = if (daam % 10 == 0L) (daam / 10).toString() else "${daam / 10}.${daam % 10}"
            digits(s, nepali) + " " + if (nepali) UNITS[3].ne else UNITS[3].en
        }
        return (parts + listOfNotNull(daamText)).joinToString(" ")
    }

    /** Two decimals with lakh grouping: `6,772.63`. Doesn't depend on the phone's locale. */
    private fun decimal(value: Double, nepali: Boolean): String {
        val hundredths = (value * 100).roundToLong()
        val text = NepaliNumber.groupIndian((hundredths / 100).toString()) + "." + (hundredths % 100).toString().padStart(2, '0')
        return digits(text, nepali)
    }

    /** The typed number in the mode's digits, without a pointless ".0". */
    private fun number(n: Double, nepali: Boolean): String {
        val s = if (n == floor(n)) n.toLong().toString() else n.toString()
        return digits(s, nepali)
    }

    private fun digits(s: String, nepali: Boolean) =
        if (nepali) NepaliNumber.toNepaliDigits(NepaliNumber.toLatinDigits(s)) else s

    private fun boundaryBefore(body: String, index: Int): Boolean {
        val c = body.getOrNull(index - 1) ?: return true
        if (c.isLetterOrDigit() || c in "-./") return false
        val category = c.category
        return category != CharCategory.NON_SPACING_MARK && category != CharCategory.COMBINING_SPACING_MARK
    }
}
