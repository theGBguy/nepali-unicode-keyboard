package io.github.thegbguy.nepalikeyboard.ui.keyboard.layouts

/**
 * The traditional Nepali layout on a physical keyboard: Microsoft's Nepali driver, as published in
 * CLDR (`keyboards/windows/ne-t-k0-windows.xml`). Keys are named by ISO position (`D01` is the Q
 * key, `C01` A, `B01` Z, `E01` the 1 key, `E00` the key left of 1, `C12` the backslash key), so the
 * table doesn't depend on the keyboard's own printed layout. Caps Lock doesn't change anything;
 * digits are on Shift, as in the standard.
 */
object TraditionalHardwareLayout {

    private class Key(val base: String, val shift: String, val altGr: String? = null)

    private val keys: Map<String, Key> = mapOf(
        "E00" to Key("ञ", "ञ्"),
        "E01" to Key("ज्ञ", "१", "1"),
        "E02" to Key("घ", "२", "2"),
        "E03" to Key("ङ", "३", "3"),
        "E04" to Key("झ", "४", "4"),
        "E05" to Key("छ", "५", "5"),
        "E06" to Key("ट", "६", "6"),
        "E07" to Key("ठ", "७", "7"),
        "E08" to Key("ड", "८", "8"),
        "E09" to Key("ढ", "९", "9"),
        "E10" to Key("ण", "०", "0"),
        "E11" to Key("(", ")"),
        "E12" to Key(".", "ं", "+"),
        "D01" to Key("त्र", "ो"),
        "D02" to Key("ध", "ध्"),
        "D03" to Key("भ", "भ्"),
        "D04" to Key("च", "च्"),
        "D05" to Key("त", "त्"),
        "D06" to Key("थ", "थ्"),
        "D07" to Key("ग", "ग्"),
        "D08" to Key("ष", "क्ष"),
        "D09" to Key("य", "इ"),
        "D10" to Key("उ", "ए", "ऊ"),
        "D11" to Key("ृ", "र्"),
        "D12" to Key("े", "ै", "औ"),
        "C01" to Key("ब", "ब्"),
        "C02" to Key("क", "क्"),
        "C03" to Key("म", "म्"),
        "C04" to Key("ा", "ँ"),
        "C05" to Key("न", "न्"),
        "C06" to Key("ज", "ज्"),
        "C07" to Key("व", "व्"),
        "C08" to Key("प", "प्"),
        "C09" to Key("ि", "ी"),
        "C10" to Key("स", "स्"),
        "C11" to Key("ु", "ू"),
        "C12" to Key("्र", "्"),
        "B01" to Key("श", "श्"),
        "B02" to Key("ह", "ह्"),
        "B03" to Key("अ", "ऋ", "आ"),
        "B04" to Key("ख", "ख्"),
        "B05" to Key("द", "द्"),
        "B06" to Key("ल", "ल्"),
        "B07" to Key("फ", "ः"),
        "B08" to Key(",", "?"),
        "B09" to Key("।", "श्र"),
        "B10" to Key("र", "रू")
    )

    /** What the key at ISO position [iso] types, or null if the layout has nothing there. */
    fun output(iso: String, shift: Boolean, altGr: Boolean = false): String? {
        val key = keys[iso] ?: return null
        return when {
            altGr -> key.altGr
            shift -> key.shift
            else -> key.base
        }
    }

    /** Every character the layout can type, for tests. */
    val allOutputs: List<String>
        get() = keys.values.flatMap { listOfNotNull(it.base, it.shift, it.altGr) }
}
