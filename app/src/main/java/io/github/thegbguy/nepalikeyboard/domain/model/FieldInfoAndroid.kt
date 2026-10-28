package io.github.thegbguy.nepalikeyboard.domain.model

import android.annotation.SuppressLint
import android.text.InputType
import android.view.inputmethod.EditorInfo

/** [FieldInfo] for the field Android's [EditorInfo] describes. */
fun FieldInfo.Companion.from(info: EditorInfo?): FieldInfo =
    if (info == null) FieldInfo()
    else classify(info.inputType, info.imeOptions, info.actionLabel, info.actionId)

/**
 * Pure mapping from [EditorInfo] fields, kept separate so it can be unit-tested.
 * IME_FLAG_NO_PERSONALIZED_LEARNING is API 26; on 24–25 apps simply never set it.
 */
@SuppressLint("InlinedApi")
fun FieldInfo.Companion.classify(
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
