package io.github.thegbguy.nepalikeyboard.domain.transliteration

import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationBuffer
import io.github.thegbguy.nepalikeyboard.domain.model.TransliterationCandidate

/**
 * Nepali transliteration engine implementation.
 * Uses greedy longest-match-first algorithm for phonetic conversion.
 */
class NepaliTransliterationEngine : TransliterationEngine {

    override fun processInput(
        currentBuffer: TransliterationBuffer,
        input: String
    ): TransliterationBuffer {
        val newRomanized = currentBuffer.romanizedText + input
        val candidates = generateSuggestions(newRomanized)

        return TransliterationBuffer(
            romanizedText = newRomanized,
            primaryCandidate = candidates.firstOrNull()?.devanagari ?: newRomanized,
            candidates = candidates,
            cursorPosition = newRomanized.length
        )
    }

    override fun commitBuffer(buffer: TransliterationBuffer): String {
        return buffer.primaryCandidate.ifEmpty { buffer.romanizedText }
    }

    override fun handleBackspace(buffer: TransliterationBuffer): TransliterationBuffer {
        if (buffer.isEmpty()) return buffer

        val newRomanized = buffer.romanizedText.dropLast(1)
        val candidates = if (newRomanized.isEmpty()) {
            emptyList()
        } else {
            generateSuggestions(newRomanized)
        }

        return TransliterationBuffer(
            romanizedText = newRomanized,
            primaryCandidate = candidates.firstOrNull()?.devanagari ?: newRomanized,
            candidates = candidates,
            cursorPosition = newRomanized.length
        )
    }

    override fun generateSuggestions(romanizedText: String): List<TransliterationCandidate> {
        if (romanizedText.isEmpty()) {
            return emptyList()
        }

        val candidates = mutableListOf<TransliterationCandidate>()
        val seenDevanagari = mutableSetOf<String>()

        // Primary candidate: exact transliteration
        val primaryDevanagari = transliterateGreedy(romanizedText)
        candidates.add(
            TransliterationCandidate(
                devanagari = primaryDevanagari,
                romanized = romanizedText,
                confidence = 1.0f,
                isComplete = true
            )
        )
        seenDevanagari.add(primaryDevanagari)

        // Generate alternative suggestions based on possible completions
        // Find the last incomplete segment (after last vowel or at start)
        val lastSegmentStart = findLastSegmentStart(romanizedText)
        val prefix = romanizedText.substring(0, lastSegmentStart)
        val lastSegment = romanizedText.substring(lastSegmentStart)

        if (lastSegment.isNotEmpty()) {
            val prefixDevanagari = if (prefix.isNotEmpty()) transliterateGreedy(prefix) else ""

            // Get all possible completions for the last segment
            val completions = PhoneticMappings.findSuggestionsForPrefix(lastSegment)

            for ((romanized, devanagari) in completions) {
                val fullDevanagari = prefixDevanagari + devanagari

                if (fullDevanagari !in seenDevanagari) {
                    // Calculate confidence based on match quality
                    val confidence = when {
                        romanized.equals(lastSegment, ignoreCase = true) -> 0.95f
                        romanized.length == lastSegment.length -> 0.85f
                        else -> 0.7f - (romanized.length - lastSegment.length) * 0.05f
                    }.coerceIn(0.3f, 0.95f)

                    candidates.add(
                        TransliterationCandidate(
                            devanagari = fullDevanagari,
                            romanized = prefix + romanized,
                            confidence = confidence,
                            isComplete = romanized.equals(lastSegment, ignoreCase = true)
                        )
                    )
                    seenDevanagari.add(fullDevanagari)
                }

                // Limit suggestions to avoid overwhelming the UI
                if (candidates.size >= 6) break
            }
        }

        return candidates.sortedByDescending { it.confidence }
    }

    /**
     * Find the start position of the last transliteration segment.
     * A segment ends after a complete consonant+vowel combination.
     */
    private fun findLastSegmentStart(text: String): Int {
        if (text.isEmpty()) return 0

        // Simple heuristic: find position after last vowel
        for (i in text.length - 1 downTo 0) {
            val char = text[i].lowercaseChar()
            if (char in listOf('a', 'e', 'i', 'o', 'u') && i < text.length - 1) {
                return i + 1
            }
        }
        return 0
    }

    /**
     * Transliterates romanized text to Devanagari using greedy longest-match algorithm.
     *
     * @param romanizedText The romanized input text
     * @return Devanagari output text
     */
    private fun transliterateGreedy(romanizedText: String): String {
        val result = StringBuilder()
        var position = 0
        var lastWasConsonant = false

        while (position < romanizedText.length) {
            val remaining = romanizedText.substring(position)

            // Find the longest matching sequence
            var match = PhoneticMappings.findLongestMatch(remaining)

            if (match != null) {
                var (matchedRomanized, devanagariOutput) = match

                // Smart vowel look-ahead: if we matched consonant+a (like "pa"),
                // check if there's a longer vowel ahead (like "aa" in "paal").
                // If so, prefer the shorter consonant match to allow the long vowel.
                if (matchedRomanized.length >= 2 &&
                    matchedRomanized.last().lowercaseChar() == 'a' &&
                    PhoneticMappings.isConsonant(matchedRomanized.dropLast(1))
                ) {
                    val afterConsonant = remaining.substring(matchedRomanized.length - 1)
                    val vowelMatch = PhoneticMappings.findLongestVowelMatch(afterConsonant)
                    if (vowelMatch != null && vowelMatch.first.length > 1) {
                        // There's a longer vowel (aa, ai, au, etc.) - use shorter consonant
                        val shorterConsonant = matchedRomanized.dropLast(1)
                        val consonantMatch = PhoneticMappings.findLongestMatch(shorterConsonant)
                        if (consonantMatch != null) {
                            matchedRomanized = consonantMatch.first
                            devanagariOutput = consonantMatch.second
                        }
                    }
                }

                // Handle consonant-vowel combinations
                if (lastWasConsonant && PhoneticMappings.isVowel(matchedRomanized)) {
                    // If last was consonant and this is a vowel, use vowel sign (matra)
                    val vowelSign = PhoneticMappings.getVowelSign(matchedRomanized)
                    if (vowelSign != null) {
                        result.append(vowelSign)
                        position += matchedRomanized.length
                        lastWasConsonant = false
                        continue
                    }
                }

                // Append the Devanagari character
                result.append(devanagariOutput)
                position += matchedRomanized.length

                // Track if this was a consonant (for next iteration)
                lastWasConsonant = PhoneticMappings.isConsonant(matchedRomanized)

                // Add halant after consonant if next character exists and is also a consonant
                if (lastWasConsonant && position < romanizedText.length) {
                    val nextRemaining = romanizedText.substring(position)
                    val nextMatch = PhoneticMappings.findLongestMatch(nextRemaining)

                    if (nextMatch != null && PhoneticMappings.isConsonant(nextMatch.first)) {
                        result.append(PhoneticMappings.HALANT)
                    }
                }
            } else {
                // No match found, append character as-is
                result.append(remaining.first())
                position++
                lastWasConsonant = false
            }
        }

        return result.toString()
    }
}
