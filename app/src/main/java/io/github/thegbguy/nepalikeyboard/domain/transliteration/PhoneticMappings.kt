package io.github.thegbguy.nepalikeyboard.domain.transliteration

/**
 * Phonetic mappings for Nepali transliteration.
 * Maps romanized text to Devanagari characters.
 * Organized by character length for greedy longest-match-first algorithm.
 */
object PhoneticMappings {

    /**
     * Vowel mappings (स्वर).
     * Ordered by length (longest first) for greedy matching.
     */
    val vowels = listOf(
        // 3-character combinations
        "aai" to "आइ",
        "aau" to "आउ",
        "rii" to "ॠ",

        // 2-character combinations
        "aa" to "आ",
        "ai" to "ऐ",
        "ae" to "ऐ",  // alternate for ऐ (from dataset)
        "au" to "औ",
        "ao" to "औ",  // alternate for औ (from dataset)
        "am" to "अं", // anusvara vowel
        "ah" to "अः", // visarga vowel
        "ii" to "ई",
        "ee" to "ई",  // common variant
        "uu" to "ऊ",
        "oo" to "ऊ",  // common variant
        "ri" to "ऋ",
        "ru" to "रु",  // common usage (from dataset)

        // 1-character combinations
        "a" to "अ",
        "i" to "इ",
        "u" to "उ",
        "e" to "ए",
        "o" to "ओ"
    )

    /**
     * Consonant mappings (व्यञ्जन).
     * Ordered by length (longest first) for greedy matching.
     *
     * Dental vs Retroflex convention:
     *   Lowercase (t/th/d/dh/n) → dental: त थ द ध न
     *   Uppercase (T/Th/D/Dh/N) → retroflex: ट ठ ड ढ ण
     *   Legacy double-letter (tt/tth/dd/ddh/nn) also supported for retroflex
     */
    val consonants = listOf(
        // 6-character combinations
        "kshetr" to "क्षेत्र",  // common word pattern

        // 5-character combinations
        "shree" to "श्री",
        "Shree" to "श्री",
        "sthaa" to "स्था",
        "shraa" to "श्रा",
        "sthra" to "स्थ्र",

        // 4-character combinations
        "ttha" to "ठ",   // retroflex aspirated (legacy)
        "ddha" to "ढ",   // retroflex aspirated (legacy)
        "chha" to "छ",
        "shri" to "श्री",
        "Shri" to "श्री",
        "ksha" to "क्ष",
        "gyaa" to "ज्ञा",
        "stha" to "स्थ",  // common conjunct (from dataset)
        "sthi" to "स्थि",
        "nkha" to "ङ्ख",  // nasal conjunct
        "ngha" to "ङ्घ",  // nasal conjunct
        "ncha" to "ञ्च",  // nasal conjunct
        "njha" to "ञ्झ",  // nasal conjunct
        "ddhy" to "द्ध्य", // complex conjunct
        "tthy" to "त्थ्य",

        // 3-character combinations
        // Capital-based retroflex
        "Tha" to "ठ",    // retroflex aspirated
        "Dha" to "ढ",    // retroflex aspirated
        "Sha" to "ष",    // retroflex sha (ष)

        // Legacy double-letter retroflex
        "tta" to "ट",    // retroflex unaspirated
        "dda" to "ड",    // retroflex unaspirated
        "nna" to "ण",    // retroflex na

        // Standard 3-char consonants
        "kha" to "ख",
        "gha" to "घ",
        "nga" to "ङ",
        "cha" to "च",
        "chh" to "छ",    // shortcut for छ
        "jha" to "झ",
        "nya" to "ञ",
        "yna" to "ञ",    // alternate for ञ
        "tha" to "थ",    // dental aspirated
        "dha" to "ध",    // dental aspirated
        "pha" to "फ",
        "bha" to "भ",
        "sha" to "श",    // palatal sha (श)
        "shh" to "ष",    // retroflex sha (ष)
        "shr" to "श्र",
        "ksh" to "क्ष",
        "gya" to "ज्ञ",
        "tra" to "त्र",
        "dny" to "ज्ञ",  // alternate for ज्ञ

        // Consonant + y combinations (expanded from dataset patterns)
        "kya" to "क्य",
        "khy" to "ख्य",
        "ghy" to "घ्य",
        "chy" to "च्य",
        "chya" to "च्य",
        "jya" to "ज्य",
        "jhy" to "झ्य",
        "tya" to "त्य",   // common (from dataset: tyetti)
        "thy" to "थ्य",
        "dya" to "द्य",
        "dhy" to "ध्य",
        "nhy" to "न्य",   // न्य (use nhy to avoid conflict with nya=ञ)
        "pya" to "प्य",
        "phy" to "फ्य",
        "bya" to "ब्य",
        "bhy" to "भ्य",
        "mya" to "म्य",
        "rya" to "र्य",
        "lya" to "ल्य",
        "vya" to "व्य",
        "sya" to "स्य",
        "hya" to "ह्य",

        // Consonant + w/v combinations (from dataset)
        "twa" to "त्व",
        "tva" to "त्व",
        "dwa" to "द्व",
        "dva" to "द्व",
        "swa" to "स्व",
        "sva" to "स्व",
        "hwa" to "ह्व",
        "kwa" to "क्व",
        "gwa" to "ग्व",

        // Consonant + r combinations (from dataset)
        "kra" to "क्र",
        "gra" to "ग्र",
        "pra" to "प्र",
        "bra" to "ब्र",
        "dra" to "द्र",
        "fra" to "फ्र",  // loan words

        // Nasal + consonant combinations (from dataset)
        "nka" to "ङ्क",
        "nkh" to "ङ्ख",
        "ngh" to "ङ्घ",
        "nch" to "ञ्च",
        "nja" to "ञ्ज",
        "nta" to "न्त",
        "nth" to "न्थ",
        "nda" to "न्द",
        "ndh" to "न्ध",
        "mpa" to "म्प",
        "mph" to "म्फ",
        "mba" to "म्ब",
        "mbh" to "म्भ",

        // Geminate consonants (from dataset: kammar, etc.)
        "kka" to "क्क",
        "gga" to "ग्ग",
        "cca" to "च्च",
        "jja" to "ज्ज",
        "lla" to "ल्ल",
        "ssa" to "स्स",
        "ppa" to "प्प",
        "bba" to "ब्ब",
        "mma" to "म्म",
        "nna" to "न्न",  // dental geminate (न्न)
        "ttta" to "त्त", // dental geminate (use ttta to avoid conflict with tta=ट)
        "ddda" to "द्द", // dental geminate (use ddda to avoid conflict with dda=ड)

        // Additional common conjuncts (from dataset)
        "ddh" to "द्ध",
        "tth" to "त्थ",
        "cch" to "च्छ",
        "chchh" to "च्छ",
        "shch" to "श्च",
        "shth" to "श्थ",
        "shna" to "श्न",
        "shla" to "श्ल",
        "shwa" to "श्व",
        "shya" to "श्य",
        "ska" to "स्क",
        "sta" to "स्त",
        "sna" to "स्न",
        "spa" to "स्प",
        "sma" to "स्म",
        "hna" to "ह्न",
        "hma" to "ह्म",
        "hra" to "ह्र",
        "hla" to "ह्ल",

        // 2-character combinations
        // Capital-based retroflex
        "Ta" to "ट",     // retroflex
        "Da" to "ड",     // retroflex
        "Na" to "ण",     // retroflex na
        "Th" to "ठ",     // retroflex aspirated
        "Dh" to "ढ",     // retroflex aspirated
        "Sh" to "ष",     // retroflex sha (ष)
        "yn" to "ञ",     // alternate for ञ

        // Standard consonants
        "ka" to "क",
        "kh" to "ख",
        "ga" to "ग",
        "gh" to "घ",
        "ng" to "ङ",
        "ca" to "च",     // alternate for cha
        "ch" to "च",
        "ja" to "ज",
        "jh" to "झ",
        "ta" to "त",     // dental
        "th" to "थ",     // dental aspirated
        "da" to "द",     // dental
        "dh" to "ध",     // dental aspirated
        "na" to "न",     // dental na
        "pa" to "प",
        "ph" to "फ",
        "fa" to "फ",     // loan word support
        "ba" to "ब",
        "bh" to "भ",
        "ma" to "म",
        "ya" to "य",
        "ra" to "र",
        "la" to "ल",
        "wa" to "व",
        "va" to "व",
        "sa" to "स",
        "ha" to "ह",
        "sh" to "श",
        "tr" to "त्र",
        "gy" to "ज्ञ",   // shortcut for ज्ञ
        "xa" to "क्ष",   // alternate for क्ष
        "za" to "ज",     // loan word support
        "qa" to "क",     // loan word support

        // 1-character combinations (no halant - engine adds when needed)
        // Capital = retroflex
        "T" to "ट",      // retroflex
        "D" to "ड",      // retroflex
        "N" to "ण",      // retroflex na
        "S" to "ष",      // retroflex sha

        // Lowercase = dental/standard
        "k" to "क",
        "g" to "ग",
        "c" to "च",
        "j" to "ज",
        "t" to "त",      // dental
        "d" to "द",      // dental
        "n" to "न",      // dental na
        "p" to "प",
        "f" to "फ",      // loan word support
        "b" to "ब",
        "m" to "म",
        "y" to "य",
        "r" to "र",
        "l" to "ल",
        "w" to "व",
        "v" to "व",
        "s" to "स",
        "h" to "ह",
        "x" to "क्ष",    // alternate for क्ष
        "z" to "ज",      // loan word support
        "q" to "क"       // loan word support
    )

    /**
     * Vowel sign mappings (मात्रा).
     * Applied after consonants to replace the inherent 'a' vowel.
     */
    val vowelSigns = listOf(
        // 3-character combinations
        "aai" to "ाइ",   // आइ matra
        "aau" to "ाउ",   // आउ matra

        // 2-character combinations
        "aa" to "ा",
        "ai" to "ै",
        "ae" to "ै",     // alternate (from dataset)
        "au" to "ौ",
        "ao" to "ौ",     // alternate (from dataset)
        "am" to "ं",     // anusvara matra
        "ah" to "ः",     // visarga matra
        "ii" to "ी",
        "ee" to "ी",     // common variant
        "uu" to "ू",
        "oo" to "ू",     // common variant
        "ri" to "ृ",     // vowel sign for ऋ
        "ru" to "ु",     // common usage for रु (handled specially)

        // 1-character combinations
        "a" to "",       // inherent vowel, removes halant
        "i" to "ि",
        "u" to "ु",
        "e" to "े",
        "o" to "ो"
    )

    /**
     * Special character mappings.
     */
    val special = listOf(
        // Multi-character
        ".." to "॥",     // double danda
        "om" to "ॐ",     // om symbol
        "OM" to "ॐ",
        "AUM" to "ॐ",    // alternate om

        // Single character
        "." to "।",      // purna biram (danda)
        "M" to "ं",      // anusvara (bindu)
        "H" to "ः",      // visarga
        "~" to "ँ",      // chandrabindu
        "^" to "ँ",      // chandrabindu (alternate)
        "`" to "ँ"       // chandrabindu (another alternate)
    )

    /**
     * Nepali numerals mapping.
     */
    val numerals = listOf(
        "0" to "०",
        "1" to "१",
        "2" to "२",
        "3" to "३",
        "4" to "४",
        "5" to "५",
        "6" to "६",
        "7" to "७",
        "8" to "८",
        "9" to "९"
    )

    /**
     * Halant (virama) character for conjuncts.
     */
    const val HALANT = "्"

    /**
     * All mappings combined and sorted by length (longest first).
     */
    val allMappings: List<Pair<String, String>> by lazy {
        (vowels + consonants + special).sortedByDescending { it.first.length }
    }

    /**
     * Get the longest matching romanized sequence from the start of the input.
     * Case-sensitive to distinguish dental (lowercase) vs retroflex (uppercase).
     *
     * @param input The romanized text to match
     * @return Pair of (matched romanized text, Devanagari output) or null if no match
     */
    fun findLongestMatch(input: String): Pair<String, String>? {
        if (input.isEmpty()) return null

        // Try to find longest match (case-sensitive for T/t, D/d, N/n distinction)
        for ((romanized, devanagari) in allMappings) {
            if (input.startsWith(romanized)) {
                return romanized to devanagari
            }
        }

        // No match found, return the first character as-is
        return input.first().toString() to input.first().toString()
    }

    /**
     * Check if a string represents a bare consonant (without inherent vowel).
     * Returns false for syllables like "pa", "ka" that end with a vowel,
     * since they already have the inherent अ vowel and don't need halant.
     */
    fun isConsonant(romanized: String): Boolean {
        val isInConsonantList = consonants.any { it.first.equals(romanized, ignoreCase = true) }
        if (!isInConsonantList) return false

        // If it ends with a vowel, it's a complete syllable, not a bare consonant
        // e.g., "pa" → प is complete, but "p" → प् may need halant before next consonant
        val lastChar = romanized.lastOrNull()?.lowercaseChar()
        if (lastChar in listOf('a', 'e', 'i', 'o', 'u')) {
            return false
        }

        return true
    }

    /**
     * Check if a string represents a vowel.
     */
    fun isVowel(romanized: String): Boolean {
        return vowels.any { it.first.equals(romanized, ignoreCase = true) }
    }

    /**
     * Get vowel sign for a given vowel romanization.
     * Used when a vowel follows a consonant.
     */
    fun getVowelSign(romanized: String): String? {
        return vowelSigns.firstOrNull {
            it.first.equals(romanized, ignoreCase = true)
        }?.second
    }

    /**
     * Check if a character is a vowel starter (for detecting vowel after consonant).
     */
    fun isVowelStarter(char: Char): Boolean {
        return char.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')
    }

    /**
     * Find the longest matching vowel sequence from the start of the input.
     * Used for look-ahead when deciding between consonant+a vs consonant + long vowel.
     *
     * @param input The romanized text to match
     * @return Pair of (matched romanized text, Devanagari output) or null if no vowel match
     */
    fun findLongestVowelMatch(input: String): Pair<String, String>? {
        if (input.isEmpty()) return null

        // Check vowels list (sorted by length in the list definition)
        for ((romanized, devanagari) in vowels) {
            if (input.startsWith(romanized, ignoreCase = true)) {
                return romanized to devanagari
            }
        }

        return null
    }

    /**
     * Find all mappings that could match the given prefix (case-insensitive).
     * Used for generating suggestions.
     *
     * For example, "t" returns mappings for: t, T, ta, Ta, th, Th, tha, Tha, tra, etc.
     *
     * @param prefix The romanized prefix to match
     * @return List of all matching mappings (romanized to Devanagari)
     */
    fun findAllCompletions(prefix: String): List<Pair<String, String>> {
        if (prefix.isEmpty()) return emptyList()

        val lowerPrefix = prefix.lowercase()
        return allMappings.filter { (romanized, _) ->
            romanized.lowercase().startsWith(lowerPrefix)
        }
    }

    /**
     * Find mappings that match the given prefix (case-sensitive for first char).
     * Respects user's case choice: "t" → dental, "T" → retroflex.
     *
     * For "t" → suggests त, थ, त्र (dental/lowercase only)
     * For "T" → suggests ट, ठ (retroflex/uppercase only)
     *
     * @param prefix The romanized prefix
     * @return List of unique Devanagari suggestions with their romanized forms
     */
    fun findSuggestionsForPrefix(prefix: String): List<Pair<String, String>> {
        if (prefix.isEmpty()) return emptyList()

        val suggestions = mutableListOf<Pair<String, String>>()
        val seenDevanagari = mutableSetOf<String>()
        val firstChar = prefix.first()
        val isUpperCase = firstChar.isUpperCase()

        // Match based on case of first character
        allMappings.forEach { (romanized, devanagari) ->
            if (romanized.isEmpty()) return@forEach

            val mappingFirstChar = romanized.first()
            val mappingIsUpper = mappingFirstChar.isUpperCase()

            // Only match if case of first character matches user input
            // (lowercase input → lowercase mappings, uppercase → uppercase)
            val caseMatches = (isUpperCase == mappingIsUpper) ||
                    // Allow lowercase continuations after first char (e.g., "t" matches "th", "tra")
                    (!isUpperCase && !mappingIsUpper)

            if (caseMatches &&
                romanized.startsWith(prefix, ignoreCase = true) &&
                devanagari !in seenDevanagari
            ) {
                suggestions.add(romanized to devanagari)
                seenDevanagari.add(devanagari)
            }
        }

        // Sort: exact length matches first, then by romanized length
        return suggestions.sortedWith(
            compareBy(
                { it.first.length != prefix.length }, // exact matches first
                { it.first.length }                    // then shorter ones
            )
        )
    }
}
