package io.github.thegbguy.nepalikeyboard.domain.transliteration

/**
 * The Romanized typing chart shown to new users in Settings and under the setup try-it field.
 * Every entry is what [RomanizedConverter] gives for that spelling (checked by a test), so the
 * chart can't drift from the keyboard. The iOS app keeps a copy in `iosApp/App/TypingGuide.swift`.
 */
object RomanizedGuide {

    /** [typed] lists the spellings that give [nepali], the usual one first (`aa`, `A` → आ). */
    data class Entry(val typed: List<String>, val nepali: String) {
        constructor(vararg typed: String, nepali: String) : this(typed.toList(), nepali)
    }

    enum class Kind { VOWELS, CONSONANTS, SIGNS, MARKS }

    data class Section(val kind: Kind, val entries: List<Entry>)

    val sections: List<Section> = listOf(
        Section(
            Kind.VOWELS, listOf(
                Entry("a", nepali = "अ"), Entry("aa", "A", nepali = "आ"), Entry("i", nepali = "इ"),
                Entry("ii", "ee", "I", nepali = "ई"), Entry("u", nepali = "उ"), Entry("uu", "oo", "U", nepali = "ऊ"),
                Entry("rri", "Ri", nepali = "ऋ"), Entry("e", nepali = "ए"), Entry("ai", nepali = "ऐ"),
                Entry("o", nepali = "ओ"), Entry("au", "ou", nepali = "औ"), Entry("aM", nepali = "अं")
            )
        ),
        Section(
            Kind.CONSONANTS, listOf(
                Entry("ka", nepali = "क"), Entry("kha", nepali = "ख"), Entry("ga", nepali = "ग"), Entry("gha", nepali = "घ"),
                Entry("nga", nepali = "ङ"), Entry("cha", nepali = "च"), Entry("chha", nepali = "छ"), Entry("ja", nepali = "ज"),
                Entry("jha", nepali = "झ"), Entry("yna", nepali = "ञ"), Entry("Ta", nepali = "ट"), Entry("Tha", nepali = "ठ"),
                Entry("Da", nepali = "ड"), Entry("Dha", nepali = "ढ"), Entry("Na", nepali = "ण"), Entry("ta", nepali = "त"),
                Entry("tha", nepali = "थ"), Entry("da", nepali = "द"), Entry("dha", nepali = "ध"), Entry("na", nepali = "न"),
                Entry("pa", nepali = "प"), Entry("pha", "fa", nepali = "फ"), Entry("ba", nepali = "ब"), Entry("bha", nepali = "भ"),
                Entry("ma", nepali = "म"), Entry("ya", nepali = "य"), Entry("ra", nepali = "र"), Entry("la", nepali = "ल"),
                Entry("wa", "va", nepali = "व"), Entry("sha", "Sa", nepali = "श"), Entry("Sha", nepali = "ष"), Entry("sa", nepali = "स"),
                Entry("ha", nepali = "ह"), Entry("ksha", "xa", nepali = "क्ष"), Entry("tra", nepali = "त्र"), Entry("gya", nepali = "ज्ञ")
            )
        ),
        Section(
            Kind.SIGNS, listOf(
                Entry("kaa", nepali = "का"), Entry("ki", nepali = "कि"), Entry("kii", nepali = "की"), Entry("ku", nepali = "कु"),
                Entry("kuu", nepali = "कू"), Entry("kRi", nepali = "कृ"), Entry("ke", nepali = "के"), Entry("kai", nepali = "कै"),
                Entry("ko", nepali = "को"), Entry("kau", nepali = "कौ"), Entry("kta", nepali = "क्त"), Entry("rya", nepali = "र्य")
            )
        ),
        Section(
            Kind.MARKS, listOf(
                Entry("kaM", nepali = "कं"), Entry("ka~", "ka^", nepali = "कँ"), Entry("kaH", nepali = "कः"),
                Entry(".", nepali = "।"), Entry("123", nepali = "१२३")
            )
        )
    )

    /** The few rows under the setup try-it field; the full chart is one tap away. */
    val compact: List<Entry> = listOf(
        Entry("aa", "A", nepali = "आ"), Entry("ii", "ee", "I", nepali = "ई"), Entry("uu", "oo", "U", nepali = "ऊ"),
        Entry("chha", nepali = "छ"), Entry("Ta", nepali = "ट"), Entry("Na", nepali = "ण"),
        Entry("sha", "Sa", nepali = "श"), Entry("Sha", nepali = "ष"), Entry("kaM", nepali = "कं"),
        Entry("ka~", "ka^", nepali = "कँ")
    )
}
