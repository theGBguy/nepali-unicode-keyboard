package io.github.thegbguy.nepalikeyboard.domain.model

import android.annotation.SuppressLint
import android.text.InputType
import android.view.inputmethod.EditorInfo

/** What kind of text the focused field expects. */
enum class FieldKind { TEXT, PASSWORD, NUMBER, PHONE, EMAIL, URI }

/** What the Enter key does in the focused field. */
enum class EnterAction { NEWLINE, DONE, GO, SEARCH, SEND, NEXT, PREVIOUS, CUSTOM }

/**
 * The parts of the focused field's [EditorInfo] the keyboard acts on.
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
    val imeActionId: Int = EditorInfo.IME_ACTION_NONE,
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
        fun from(info: EditorInfo?): FieldInfo =
            if (info == null) FieldInfo()
            else classify(info.inputType, info.imeOptions, info.actionLabel, info.actionId)

        /**
         * Pure mapping from [EditorInfo] fields, kept separate so it can be unit-tested.
         * IME_FLAG_NO_PERSONALIZED_LEARNING is API 26; on 24–25 apps simply never set it.
         */
        @SuppressLint("InlinedApi")
        fun classify(
            inputType: Int,
            imeOptions: Int,
            actionLabel: CharSequence? = null,
            actionId: Int = 0
        ): FieldInfo {
            val inputClass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION

            val kind = when (inputClass) {
                InputType.TYPE_CLASS_NUMBER ->
                    if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) FieldKind.PASSWORD
                    else FieldKind.NUMBER
                InputType.TYPE_CLASS_PHONE -> FieldKind.PHONE
                InputType.TYPE_CLASS_DATETIME -> FieldKind.NUMBER
                InputType.TYPE_CLASS_TEXT -> when (variation) {
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> FieldKind.PASSWORD
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> FieldKind.EMAIL
                    InputType.TYPE_TEXT_VARIATION_URI -> FieldKind.URI
                    else -> FieldKind.TEXT
                }
                else -> FieldKind.TEXT
            }
            val isPrivate = kind == FieldKind.PASSWORD
            val incognito = (imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0

            val label = actionLabel?.toString()?.takeIf { it.isNotBlank() }
            val action = imeOptions and EditorInfo.IME_MASK_ACTION
            val enterAction = when {
                (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0 -> EnterAction.NEWLINE
                label != null -> EnterAction.CUSTOM
                else -> when (action) {
                    EditorInfo.IME_ACTION_GO -> EnterAction.GO
                    EditorInfo.IME_ACTION_SEARCH -> EnterAction.SEARCH
                    EditorInfo.IME_ACTION_SEND -> EnterAction.SEND
                    EditorInfo.IME_ACTION_NEXT -> EnterAction.NEXT
                    EditorInfo.IME_ACTION_PREVIOUS -> EnterAction.PREVIOUS
                    EditorInfo.IME_ACTION_DONE -> EnterAction.DONE
                    else -> EnterAction.NEWLINE
                }
            }
            return FieldInfo(
                kind = kind,
                isPrivate = isPrivate,
                noLearning = isPrivate || incognito,
                enterAction = enterAction,
                imeActionId = if (enterAction == EnterAction.CUSTOM) actionId else action,
                actionLabel = if (enterAction == EnterAction.CUSTOM) label else null,
                sendsKeyEvents = inputType == InputType.TYPE_NULL
            )
        }
    }
}
