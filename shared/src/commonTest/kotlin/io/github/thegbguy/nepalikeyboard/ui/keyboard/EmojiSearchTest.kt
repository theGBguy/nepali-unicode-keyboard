package io.github.thegbguy.nepalikeyboard.ui.keyboard

import io.github.thegbguy.nepalikeyboard.data.emoji.EmojiKeywords
import io.github.thegbguy.nepalikeyboard.domain.model.FieldInfo
import io.github.thegbguy.nepalikeyboard.domain.model.KeyData
import io.github.thegbguy.nepalikeyboard.domain.model.KeyType
import io.github.thegbguy.nepalikeyboard.domain.model.LanguageMode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Emoji search: the words index, and the controller typing into the search box. */
class EmojiSearchTest {

    // A few lines in the format of emoji_keywords.tsv (words from Unicode CLDR)
    private val words = EmojiKeywords.parse(
        """
        # test
        😀	grinning face smile happy	मुस्कुराएको अनुहार खुशी
        😂	face with tears of joy crying laugh lol	खुसीको आँसु देखिएको अनुहार खुशी हाँसो
        🤣	rolling on the floor laughing laugh rofl	भुइँमा लडीबुडी गर्दै हाँसो
        🍕	pizza cheese food slice	पिज्जा स्लाइस
        🇳🇵	flag nepal	झण्डा नेपाल
        ❤️	red heart love	रातो मुटु
        """.trimIndent()
    )

    private lateinit var keyboard: KeyboardController
    private lateinit var field: FakeEditor

    private val space = KeyData(" ", keyType = KeyType.ACTION)
    private val enter = KeyData("↵", keyType = KeyType.ACTION)
    private val backspace = KeyData("⌫", keyType = KeyType.FUNCTION)
    private val globe = KeyData("🌐", keyType = KeyType.FUNCTION)

    @BeforeTest
    fun setUp() {
        field = FakeEditor()
        keyboard = KeyboardController().also {
            it.editor = { field }
            it.prefs = InMemoryKeyboardPrefs(typeNepali = false, emojiRecents = listOf("❤️"))
            it.emojiKeywords = words
        }
        keyboard.onInputStarted(FieldInfo())
    }

    @AfterTest
    fun tearDown() = keyboard.close()

    private fun type(text: String) = text.forEach { keyboard.onKeyClick(if (it == ' ') space else KeyData(it.toString())) }

    private fun search() = keyboard.state.value.emojiSearch!!

    private fun openSearch() {
        keyboard.openEmoji()
        keyboard.openEmojiSearch()
    }

    @Test
    fun `whole words and names rank before prefixes`() {
        assertEquals(listOf("😂", "🤣"), words.search(listOf("laugh")))
        assertEquals(listOf("🍕"), words.search(listOf("piz")))
        assertEquals(listOf("🇳🇵"), words.search(listOf("Nepal flag")))
        assertEquals(emptyList(), words.search(listOf("xyz")))
        assertEquals(emptyList(), words.search(listOf("  ")))
    }

    @Test
    fun `Nepali words match across chandrabindu and long and short vowels`() {
        assertEquals(listOf("😂", "🤣"), words.search(listOf("हासो")))
        assertEquals(listOf("😀", "😂"), words.search(listOf("खुसी")))
    }

    @Test
    fun `emoji the phone can't draw are left out`() {
        val noFlags = EmojiKeywords.parse("🇳🇵\tflag nepal\t\n🍕\tpizza\t") { it != "🇳🇵" }
        assertEquals(1, noFlags.size)
        assertEquals(emptyList(), noFlags.search(listOf("nepal")))
    }

    @Test
    fun `the keys type into the search box instead of the field`() {
        openSearch()
        assertEquals(listOf("❤️"), search().results) // recents before anything is typed
        type("laugh")
        assertEquals("laugh", search().text)
        assertEquals("", field.text)
        assertEquals(listOf("😂", "🤣"), search().results)
        keyboard.onKeyClick(backspace)
        assertEquals("laug", search().text)
    }

    @Test
    fun `picking a result types it into the field and keeps the search`() {
        openSearch()
        type("pizza")
        keyboard.onEmojiClick("🍕")
        keyboard.onEmojiClick("🍕")
        assertEquals("🍕🍕", field.text)
        assertEquals("pizza", search().text)
        assertEquals(listOf("🍕"), search().results)
    }

    @Test
    fun `Romanized typing searches the Nepali words`() {
        keyboard.onKeyClick(globe)
        assertEquals(LanguageMode.TRANSLITERATION, keyboard.state.value.currentLanguage)
        openSearch()
        type("hanso")
        assertEquals("hanso", search().composing)
        assertTrue("😂" in search().results, "results: ${search().results} candidates: ${keyboard.state.value.suggestions}")
        assertEquals("", field.shown)
    }

    @Test
    fun `back goes to the grid and Enter goes back to typing`() {
        openSearch()
        type("lol")
        keyboard.closeEmojiSearch()
        assertNull(keyboard.state.value.emojiSearch)
        assertTrue(keyboard.state.value.showEmoji)
        type("hi")
        assertEquals("hi", field.text) // keys type into the field again

        openSearch()
        keyboard.onKeyClick(enter)
        assertNull(keyboard.state.value.emojiSearch)
        assertFalse(keyboard.state.value.showEmoji)
        assertEquals("hi", field.text)
    }

    @Test
    fun `a new field closes search`() {
        openSearch()
        type("lo")
        keyboard.onInputStarted(FieldInfo())
        assertNull(keyboard.state.value.emojiSearch)
        type("x")
        assertEquals("x", field.text)
    }
}
