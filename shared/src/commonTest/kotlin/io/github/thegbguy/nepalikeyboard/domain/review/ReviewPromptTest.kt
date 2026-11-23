package io.github.thegbguy.nepalikeyboard.domain.review

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewPromptTest {

    @Test
    fun `asks on the first launch after the one where setup was done`() {
        // Setup is finished during this launch, so it started without the keyboard set up
        var prompt = ReviewPrompt().opened(setupDone = false)
        assertFalse(prompt.shouldAsk())
        prompt = prompt.opened(setupDone = true)
        assertTrue(prompt.shouldAsk())
    }

    @Test
    fun `asks only once`() {
        val prompt = ReviewPrompt().opened(setupDone = true).askedNow().opened(setupDone = true)
        assertFalse(prompt.shouldAsk())
    }
}
