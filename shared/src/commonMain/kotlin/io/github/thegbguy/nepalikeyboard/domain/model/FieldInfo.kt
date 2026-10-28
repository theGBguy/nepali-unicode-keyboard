package io.github.thegbguy.nepalikeyboard.domain.model

/** What kind of text the focused field expects. */
enum class FieldKind { TEXT, PASSWORD, NUMBER, PHONE, EMAIL, URI }

/** What the Enter key does in the focused field. */
enum class EnterAction { NEWLINE, DONE, GO, SEARCH, SEND, NEXT, PREVIOUS, CUSTOM }

/**
 * The parts of the focused field the keyboard acts on. Each platform fills it from its own
 * field description (Android: `EditorInfo`, see `FieldInfoAndroid.kt` in the app).
 *
 * @property isPrivate Password field: English only, no transliteration buffer, no suggestions,
 *   no voice, nothing learned.
 * @property noLearning The app asked for incognito typing (or it's a password field): never
 *   remember what's typed. Voice is off too, because it sends audio to Google.
 * @property sendsKeyEvents The field has no editor behind it (TYPE_NULL, e.g. a terminal), so
 *   Enter must be sent as a key event instead of committed text.
 */
data class FieldInfo(
    val kind: FieldKind = FieldKind.TEXT,
    val isPrivate: Boolean = false,
    val noLearning: Boolean = false,
    val enterAction: EnterAction = EnterAction.NEWLINE,
    val imeActionId: Int = IME_ACTION_NONE,
    val actionLabel: String? = null,
    val sendsKeyEvents: Boolean = false
) {
    /** Romanized typing and its suggestions are allowed here. */
    val allowsTransliteration: Boolean get() = !isPrivate

    /** The mic is shown here. */
    val allowsVoice: Boolean get() = !isPrivate && !noLearning

    /** Fields where Latin text is expected: open in English instead of the user's Nepali mode. */
    val prefersEnglish: Boolean
        get() = kind == FieldKind.PASSWORD || kind == FieldKind.EMAIL || kind == FieldKind.URI

    /** Fields that open on the numbers layout. */
    val prefersNumbers: Boolean get() = kind == FieldKind.NUMBER || kind == FieldKind.PHONE

    companion object {
        /** Android's `EditorInfo.IME_ACTION_NONE`; no action. */
        const val IME_ACTION_NONE = 1
    }
}
