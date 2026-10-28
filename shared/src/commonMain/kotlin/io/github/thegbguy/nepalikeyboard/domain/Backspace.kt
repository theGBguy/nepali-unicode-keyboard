package io.github.thegbguy.nepalikeyboard.domain

/**
 * How many UTF-16 units backspace removes from the end of [before].
 *
 * One code point, as Nepali typists expect (क्षि → क्ष), except that an emoji is removed whole:
 * its variation selector, keycap mark, skin tone and tag characters, the ZWJ sequence it belongs
 * to (👨‍👩‍👧) and both halves of a flag (🇳🇵).
 */
object Backspace {

    fun length(before: CharSequence): Int {
        if (before.isEmpty()) return 1
        var i = before.length
        var regionalIndicators = 0

        fun codePointBefore(index: Int): Int {
            val low = before[index - 1]
            if (low.isLowSurrogate() && index >= 2) {
                val high = before[index - 2]
                if (high.isHighSurrogate()) return ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
            }
            return low.code
        }
        fun charCount(cp: Int) = if (cp >= 0x10000) 2 else 1
        fun isModifier(cp: Int) = cp == 0xFE0F || cp == 0xFE0E || cp == 0x20E3 ||
            cp in 0x1F3FB..0x1F3FF || cp in 0xE0020..0xE007F
        fun isRegionalIndicator(cp: Int) = cp in 0x1F1E6..0x1F1FF

        // One emoji "unit": trailing modifiers, then its base character
        fun consumeUnit(): Boolean {
            var consumedModifier = false
            while (i > 0 && isModifier(codePointBefore(i))) {
                i -= charCount(codePointBefore(i))
                consumedModifier = true
            }
            if (i == 0) return consumedModifier
            val base = codePointBefore(i)
            i -= charCount(base)
            if (isRegionalIndicator(base)) regionalIndicators++
            return consumedModifier
        }

        consumeUnit()
        // A ZWJ before this unit joins it to the previous one
        while (i > 0 && codePointBefore(i) == ZWJ) {
            i -= 1
            consumeUnit()
        }
        // Flags are pairs of regional indicators: remove the pair, counting from the start of the run
        if (regionalIndicators == 1 && i > 0 && isRegionalIndicator(codePointBefore(i))) {
            var run = 0
            var j = i
            while (j > 0 && isRegionalIndicator(codePointBefore(j))) {
                run++
                j -= 2
            }
            if (run % 2 == 1) i -= 2
        }
        return before.length - i
    }

    private const val ZWJ = 0x200D
}
