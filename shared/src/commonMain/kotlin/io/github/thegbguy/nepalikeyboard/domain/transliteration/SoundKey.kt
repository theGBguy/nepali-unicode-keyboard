package io.github.thegbguy.nepalikeyboard.domain.transliteration

/**
 * Turns Devanagari words and Romanized input into comparable Latin strings.
 *
 * - [spell] writes a Devanagari word the way people type it in Roman letters (नेपाल → "nepaal").
 * - [normalize] removes spelling choices that mean the same thing (ee/ii, oo/uu, w/v, f/ph...).
 * - [looseKey] is a coarse key for lookup: it also drops vowel length, the `a` after consonants,
 * *   chh/ch, sh/s, v/b differences. "dhanyabad" and धन्यवाद share the key "Dnybd".
 */
object SoundKey {

    private val consonantSounds: Map<Char, String> = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ng",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h"
    )
    private val independentVowels: Map<Char, String> = mapOf(
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ii", 'उ' to "u", 'ऊ' to "uu",
        'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au", 'ऋ' to "ri"
    )
    private val vowelSigns: Map<Char, String> = mapOf(
        'ा' to "aa", 'ि' to "i", 'ी' to "ii", 'ु' to "u", 'ू' to "uu",
        'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au", 'ृ' to "ri"
    )
    private const val HALANT = '्'
    private const val NUKTA = '़'

    /**
     * Romanized spelling of a Devanagari word. A consonant carries an inherent "a" unless a halant
     * or vowel sign follows; the word-final one is kept too (callers ignore a trailing "a").
     * Anusvara is written "n", chandrabindu is dropped, ज्ञ is "gy".
     */
    fun spell(word: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < word.length) {
            val c = word[i]
            if (c == 'ज' && word.getOrNull(i + 1) == HALANT && word.getOrNull(i + 2) == 'ञ') {
                out.append("gy")
                i += 3
                out.append(inherent(word, i))
                continue
            }
            val consonant = consonantSounds[c]
            when {
                consonant != null -> {
                    out.append(consonant)
                    i++
                    if (word.getOrNull(i) == NUKTA) i++
                    out.append(inherent(word, i))
                    continue
                }
                c in independentVowels -> out.append(independentVowels.getValue(c))
                c in vowelSigns -> out.append(vowelSigns.getValue(c))
                c == 'ं' -> out.append('n')
                c == 'ः' -> out.append('h')
                else -> Unit // halant, chandrabindu, nukta, punctuation
            }
            i++
        }
        return out.toString()
    }

    /** "a" if the consonant just before [next] keeps its inherent vowel. */
    private fun inherent(word: String, next: Int): String {
        val following = word.getOrNull(next) ?: return "a"
        return if (following == HALANT || following in vowelSigns) "" else "a"
    }

    // Compiled once: these run for every word while the word list loads and on every key press
    private val loneC = Regex("c(?!h)")
    private val accentMarks = Regex("[~^]")
    private val mBeforeConsonant = Regex("m(?=[^aeiou])")

    /** Lowercase and fold spelling variants that sound the same. */
    fun normalize(roman: String): String {
        var s = roman.lowercase()
        s = s.replace("ee", "ii").replace("oo", "uu")
            .replace("w", "v").replace("f", "ph").replace("q", "k").replace("z", "j")
            .replace("x", "ksh")
        // A lone "c" (not "ch") is ch
        s = loneC.replace(s, "ch")
        return s
    }

    /** Coarse lookup key, see the class comment. Input must be [normalize]d. */
    fun looseKey(normalized: String): String {
        // Aspirates are one sound: keep "kh" (ख) apart from "k…h" (क…ह) once vowels are dropped
        var s = normalized
            .replace("chh", "C").replace("ch", "C").replace("kh", "K").replace("gh", "G")
            .replace("jh", "J").replace("th", "T").replace("dh", "D").replace("ph", "P")
            .replace("bh", "B").replace("sh", "s").replace("v", "b")
            .replace("aa", "a").replace("ii", "i").replace("uu", "u")
            .replace(accentMarks, "")
        // Drop the "a" after a consonant: whether it's written, long or inherent varies by typist
        val out = StringBuilder()
        for (i in s.indices) {
            val c = s[i]
            if (c == 'a' && i > 0 && s[i - 1] !in VOWELS && s[i - 1] != 'a') continue
            out.append(c)
        }
        s = out.toString()
        // m before a consonant is usually anusvara (sambidhan = संविधान)
        s = mBeforeConsonant.replace(s, "n")
        return s
    }

    /**
     * The normalized spellings a dictionary word is typed with. A nasal mark (ं or ँ) is typed
     * as "n" by some people and skipped by others, so both spellings are listed:
     * आउँछु → "aaunchhu" and "aauchhu".
     */
    fun spellingsForWord(word: String): Set<String> {
        val spellings = linkedSetOf(normalize(spell(word)))
        if ('ं' in word || 'ँ' in word) {
            spellings += normalize(spell(word.replace('ँ', 'ं')))
            spellings += normalize(spell(word.replace("ं", "").replace("ँ", "")))
        }
        return spellings
    }

    /**
     * How far apart two normalized spellings are. Differences the [looseKey] ignores (vowel
     * length, a skipped "a", chh/ch, sh/s, v/b) cost 0.25 each; other letters cost 1. Typing a
     * long vowel the word doesn't have costs 1 more, since that's deliberate. A trailing inherent
     * "a" on [candidate] is optional.
     */
    fun distance(typed: String, candidate: String): Double {
        val withoutFinalA = if (candidate.endsWith("a") && !candidate.endsWith("aa")) candidate.dropLast(1) else candidate
        return minOf(weighted(typed, candidate), weighted(typed, withoutFinalA))
    }

    private fun weighted(a: String, b: String): Double {
        val letters = levenshtein(looseKey(a), looseKey(b))
        val fine = (levenshtein(a, b) - letters).coerceAtLeast(0)
        val missingLong = (longVowels(a) - longVowels(b)).coerceAtLeast(0)
        return letters + 0.25 * fine + missingLong
    }

    private val longVowel = Regex("aa|ii|uu")
    private fun longVowels(s: String) = longVowel.findAll(s).count()

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    private const val VOWELS = "aeiou"
}
