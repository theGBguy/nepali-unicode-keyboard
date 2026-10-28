package io.github.thegbguy.nepalikeyboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BackspaceTest {

    private fun deletes(before: String, expected: String) =
        assertEquals(expected.length, Backspace.length(before), "deleting from \"$before\"")

    @Test
    fun `devanagari goes one code point at a time`() {
        deletes("नमस्ते", "े")
        deletes("क्षि", "ि")
        deletes("क्", "्")
        deletes("आमा", "ा")
    }

    @Test
    fun `latin and empty`() {
        deletes("hello", "o")
        assertEquals(1, Backspace.length(""))
    }

    @Test
    fun `emoji are removed whole`() {
        deletes("hi 😀", "😀")
        deletes("ok ❤️", "❤️")
        deletes("👍🏽", "👍🏽")
        deletes("a1️⃣", "1️⃣")
        deletes("x👨‍👩‍👧", "👨‍👩‍👧")
        deletes("🧑🏽‍💻", "🧑🏽‍💻")
        deletes("🏴󠁧󠁢󠁥󠁮󠁧󠁿", "🏴󠁧󠁢󠁥󠁮󠁧󠁿")
    }

    @Test
    fun `flags come off in pairs`() {
        deletes("नेपाल 🇳🇵", "🇳🇵")
        deletes("🇳🇵🇮🇳", "🇮🇳")
        deletes("🇳🇵🇮🇳🇺", "🇺") // a lone indicator after two whole flags
    }
}
