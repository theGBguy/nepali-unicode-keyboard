package io.github.thegbguy.nepalikeyboard.domain.transliteration

/**
 * Letter-by-letter Romanized → Devanagari conversion, used for words that aren't in the word
 * list (names, places, slang) and as one candidate next to the word-list matches.
 *
 * Rules:
 * - A consonant followed by a vowel takes the vowel sign (`ka` → क, `ki` → कि); `a` is the
 *   inherent vowel and adds nothing.
 * - Two consonants in a row join with a halant (`st` → स्त).
 * - A word-final consonant keeps its inherent `a` (`ghar` → घर), as Nepali is written.
 * - Capitals give retroflex letters (`T` ट, `Th` ठ, `D` ड, `Dh` ढ, `N` ण, `Sh` ष); `S` is श,
 *   as in Hamro Nepali and other Romanized keyboards, so both sibilants have a capital key.
 * - `ng` ङ, `yn` ञ, `gy` ज्ञ.
 * - `M` anusvara ं, `H` visarga ः, `~` or `^` chandrabindu ँ, `.` purna biram ।, digits → Nepali.
 * - `ou` is औ like `au`.
 */
object RomanizedConverter {

    private const val HALANT = "्"

    /** Bare consonants, longest first. Case-sensitive: capitals are retroflex. */
    private val consonants: List<Pair<String, String>> = listOf(
        "ksh" to "क्ष", "chh" to "छ", "Chh" to "छ",
        "kh" to "ख", "gh" to "घ", "ng" to "ङ", "ch" to "च", "jh" to "झ",
        "Th" to "ठ", "Dh" to "ढ", "th" to "थ", "dh" to "ध",
        "ph" to "फ", "bh" to "भ", "Sh" to "ष", "sh" to "श", "gy" to "ज्ञ", "yn" to "ञ", "Yn" to "ञ",
        "Ch" to "च", "Kh" to "ख", "Gh" to "घ", "Jh" to "झ", "Ph" to "फ", "Bh" to "भ",
        "T" to "ट", "D" to "ड", "N" to "ण", "S" to "श",
        "k" to "क", "g" to "ग", "c" to "च", "j" to "ज", "t" to "त", "d" to "द", "n" to "न",
        "p" to "प", "f" to "फ", "b" to "ब", "m" to "म", "y" to "य", "r" to "र", "l" to "ल",
        "w" to "व", "v" to "व", "s" to "स", "h" to "ह", "x" to "क्ष", "z" to "ज", "q" to "क",
        "K" to "क", "G" to "ग", "J" to "ज", "P" to "प", "B" to "ब", "L" to "ल",
        "Y" to "य", "R" to "र", "V" to "व", "W" to "व", "F" to "फ", "C" to "च", "Z" to "ज"
    ).sortedByDescending { it.first.length }

    /** Vowels as (romanized, independent letter, vowel sign), longest first. */
    private val vowels: List<Triple<String, String, String>> = listOf(
        Triple("rri", "ऋ", "ृ"), Triple("Ri", "ऋ", "ृ"),
        Triple("aa", "आ", "ा"), Triple("ai", "ऐ", "ै"), Triple("au", "औ", "ौ"), Triple("ou", "औ", "ौ"),
        Triple("ii", "ई", "ी"), Triple("ee", "ई", "ी"), Triple("uu", "ऊ", "ू"), Triple("oo", "ऊ", "ू"),
        Triple("A", "आ", "ा"), Triple("I", "ई", "ी"), Triple("U", "ऊ", "ू"),
        Triple("a", "अ", ""), Triple("i", "इ", "ि"), Triple("u", "उ", "ु"),
        Triple("e", "ए", "े"), Triple("o", "ओ", "ो"), Triple("E", "ए", "े"), Triple("O", "ओ", "ो")
    ).sortedByDescending { it.first.length }

    private val marks: Map<Char, String> = mapOf(
        'M' to "ं", 'H' to "ः", '~' to "ँ", '^' to "ँ", '.' to "।"
    )

    /**
     * The consonant with a halant when [roman] is exactly one consonant (`b` → ब्, `ng` → ङ्,
     * `yn` → ञ्), the way half letters are typed on their own; null for anything else.
     */
    fun halfLetter(roman: String): String? =
        consonants.firstOrNull { it.first == roman }?.let { it.second + HALANT }

    /**
     * True when [roman] spells one letter or syllable: a vowel (`i`, `ai`), consonants with at
     * most one single vowel after them (`ka`, `tra`, `shra`, `Sha`, not `dai`), or marks alone (`M` ं, `H` ः, `~` ँ),
     * each optionally followed by marks (`kaM`). Typed on its own, that's almost always the letter
     * itself, so its exact spelling goes first. A consonant after the vowel (`ghar`) makes a word.
     */
    fun isSyllable(roman: String): Boolean {
        if (roman.isEmpty()) return false
        var i = 0
        var consonantsAllowed = true
        var vowelSeen = false
        var consonantSeen = false
        while (i < roman.length) {
            val vowel = vowels.firstOrNull { roman.startsWith(it.first, i) }
            if (vowel != null) {
                // ai/au/ou after a consonant is how people type two vowels (dai दाइ, lai लाई,
                // bhau भाउ), so that's a word
                if (vowelSeen || (consonantSeen && vowel.first in TWO_VOWELS)) return false
                vowelSeen = true
                consonantsAllowed = false
                i += vowel.first.length
                continue
            }
            val consonant = consonants.firstOrNull { roman.startsWith(it.first, i) }
            if (consonant != null) {
                if (!consonantsAllowed) return false
                consonantSeen = true
                i += consonant.first.length
                continue
            }
            if (roman[i] !in SYLLABLE_MARKS) return false
            consonantsAllowed = false
            i++
        }
        return true
    }

    /** Marks that belong to a syllable (anusvara, visarga, chandrabindu), unlike `.` (।). */
    private const val SYLLABLE_MARKS = "MH~^"
    private val TWO_VOWELS = setOf("ai", "au", "ou")

    fun convert(roman: String): String {
        val out = StringBuilder()
        var i = 0
        var afterConsonant = false
        while (i < roman.length) {
            val vowel = vowels.firstOrNull { roman.startsWith(it.first, i) }
            // A capital vowel letter that is also a consonant key (e.g. "Ri") was matched above;
            // otherwise try consonants before falling back to marks and other characters.
            if (vowel != null) {
                out.append(if (afterConsonant) vowel.third else vowel.second)
                afterConsonant = false
                i += vowel.first.length
                continue
            }
            val consonant = consonants.firstOrNull { roman.startsWith(it.first, i) }
            if (consonant != null) {
                if (afterConsonant) out.append(HALANT)
                out.append(consonant.second)
                afterConsonant = true
                i += consonant.first.length
                continue
            }
            val c = roman[i]
            when {
                c in marks -> out.append(marks.getValue(c))
                c in '0'..'9' -> out.append('०' + (c - '0'))
                else -> out.append(c)
            }
            afterConsonant = false
            i++
        }
        return out.toString()
    }
}
