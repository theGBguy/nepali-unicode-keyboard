package io.github.thegbguy.nepalikeyboard.domain.handwriting

/** One touch sample of a handwritten stroke, in panel pixels; [t] in milliseconds. */
data class InkPoint(val x: Float, val y: Float, val t: Long)

/** A pen-down-to-pen-up stroke. */
typealias InkStroke = List<InkPoint>

/**
 * Turns handwriting into text. The real one is Google ML Kit's digital ink recognition, which
 * runs on the phone after a one-time model download; tests use a fake.
 */
interface HandwritingRecognizer {
    /** Whether the Nepali model is on the phone. */
    suspend fun isModelReady(): Boolean

    /** Downloads the Nepali model. Throws when it fails (no connection, storage full). */
    suspend fun download()

    /**
     * Candidates for [strokes], best first.
     *
     * @param width size of the writing area, which helps tell a small mark from a big letter
     * @param preContext text before the cursor, which helps with the next word
     */
    suspend fun recognize(strokes: List<InkStroke>, width: Float, height: Float, preContext: String): List<String>

    /** Deletes the downloaded model. */
    suspend fun deleteModel()
}
