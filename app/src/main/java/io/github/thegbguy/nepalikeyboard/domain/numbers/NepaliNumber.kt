package io.github.thegbguy.nepalikeyboard.domain.numbers

/**
 * Nepali number formatting: lakh/crore digit grouping and numbers in words, in Nepali and in
 * (Indian-style) English.
 *
 * Nepali 1–99 are all irregular, so they're a table. Spellings use the long-ī forms seen on
 * cheques and in most writing (बीस, तीस, चालीस, पच्चीस), not the short-i forms of the 2069 BS
 * Pragya-Pratishthan orthography (बिस, तिस). Sources compared: Wikipedia "Numbers in Nepali
 * language", omniglot.com/language/numbers/nepali.htm and barala.com.np/number-words.php, which
 * disagree on many forms; this table picks one spelling per number.
 */
object NepaliNumber {

    /** The largest number with a word form: 99 खर्ब 99 अर्ब ... 99. */
    const val MAX_WORDS = 9_999_999_999_999L

    private val NEPALI_BELOW_100 = listOf(
        "शून्य", "एक", "दुई", "तीन", "चार", "पाँच", "छ", "सात", "आठ", "नौ",
        "दस", "एघार", "बाह्र", "तेह्र", "चौध", "पन्ध्र", "सोह्र", "सत्र", "अठार", "उन्नाइस",
        "बीस", "एक्काइस", "बाइस", "तेइस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताइस", "अट्ठाइस", "उनन्तीस",
        "तीस", "एकतीस", "बत्तीस", "तेत्तीस", "चौँतीस", "पैँतीस", "छत्तीस", "सैँतीस", "अठतीस", "उनन्चालीस",
        "चालीस", "एकचालीस", "बयालीस", "त्रिचालीस", "चवालीस", "पैँतालीस", "छयालीस", "सतचालीस", "अठचालीस", "उनन्चास",
        "पचास", "एकाउन्न", "बाउन्न", "त्रिपन्न", "चउन्न", "पचपन्न", "छपन्न", "सन्ताउन्न", "अन्ठाउन्न", "उनन्साठी",
        "साठी", "एकसट्ठी", "बयसट्ठी", "त्रिसट्ठी", "चौसट्ठी", "पैँसट्ठी", "छयसट्ठी", "सतसट्ठी", "अठसट्ठी", "उनन्सत्तरी",
        "सत्तरी", "एकहत्तर", "बहत्तर", "त्रिहत्तर", "चौहत्तर", "पचहत्तर", "छयहत्तर", "सतहत्तर", "अठहत्तर", "उनासी",
        "असी", "एकासी", "बयासी", "त्रियासी", "चौरासी", "पचासी", "छयासी", "सतासी", "अठासी", "उनान्नब्बे",
        "नब्बे", "एकान्नब्बे", "बयान्नब्बे", "त्रियान्नब्बे", "चौरान्नब्बे", "पन्चान्नब्बे", "छयान्नब्बे", "सन्तान्नब्बे", "अन्ठान्नब्बे", "उनान्सय"
    )

    private val ENGLISH_ONES = listOf(
        "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    private val ENGLISH_TENS = listOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    /** Place value and its Nepali and English name, largest first. Each takes 0–99 except सय (0–9). */
    private val SCALES = listOf(
        Triple(100_000_000_000L, "खर्ब", "Kharab"),
        Triple(1_000_000_000L, "अर्ब", "Arab"),
        Triple(10_000_000L, "करोड", "Crore"),
        Triple(100_000L, "लाख", "Lakh"),
        Triple(1_000L, "हजार", "Thousand"),
        Triple(100L, "सय", "Hundred")
    )

    fun nepaliWords(n: Long): String? = words(n, nepali = true)

    fun englishWords(n: Long): String? = words(n, nepali = false)

    /** Reads each digit after the decimal point: "05" → "शून्य पाँच" / "Zero Five". */
    fun digitWords(digits: String, nepali: Boolean): String =
        digits.map { below100(digitValue(it), nepali) }.joinToString(" ")

    private fun words(n: Long, nepali: Boolean): String? {
        if (n < 0 || n > MAX_WORDS) return null
        if (n == 0L) return below100(0, nepali)
        var rest = n
        val parts = mutableListOf<String>()
        for ((place, ne, en) in SCALES) {
            val count = (rest / place).toInt()
            rest %= place
            if (count > 0) parts += below100(count, nepali) + " " + if (nepali) ne else en
        }
        if (rest > 0) parts += below100(rest.toInt(), nepali)
        return parts.joinToString(" ")
    }

    private fun below100(n: Int, nepali: Boolean): String = when {
        nepali -> NEPALI_BELOW_100[n]
        n < 20 -> ENGLISH_ONES[n]
        n % 10 == 0 -> ENGLISH_TENS[n / 10]
        else -> ENGLISH_TENS[n / 10] + "-" + ENGLISH_ONES[n % 10]
    }

    /**
     * Lakh/crore grouping: the last three digits, then pairs. "1500000" → "15,00,000".
     * [digits] is plain Latin digits.
     */
    fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3)
        val pairs = head.reversed().chunked(2).joinToString(",").reversed()
        return pairs + "," + digits.takeLast(3)
    }

    /** Latin and Devanagari digits → their value. */
    fun digitValue(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in '०'..'९' -> c - '०'
        else -> -1
    }

    fun isDigit(c: Char): Boolean = digitValue(c) >= 0

    /** Any digits → Latin digits. Other characters are kept. */
    fun toLatinDigits(s: String): String =
        buildString { s.forEach { append(if (isDigit(it)) '0' + digitValue(it) else it) } }

    /** Latin digits → Devanagari digits. Other characters are kept. */
    fun toNepaliDigits(s: String): String =
        buildString { s.forEach { append(if (it in '0'..'9') '०' + (it - '0') else it) } }
}
