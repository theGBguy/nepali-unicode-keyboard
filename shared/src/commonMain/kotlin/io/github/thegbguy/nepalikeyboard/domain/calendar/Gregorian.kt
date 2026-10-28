package io.github.thegbguy.nepalikeyboard.domain.calendar

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Gregorian dates as epoch days (days since 1970-01-01), without java.time (minSdk 24). */
object Gregorian {

    /** Days from 1970-01-01 to [year]-[month]-[day] (proleptic Gregorian, H. Hinnant's algorithm). */
    fun toEpochDay(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = y.floorDiv(400L)
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** The year, month and day of [epochDay]. */
    fun fromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468
        val era = z.floorDiv(146097L)
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
        return Triple(year, month, day)
    }

    fun isValid(year: Int, month: Int, day: Int): Boolean {
        if (month !in 1..12 || day < 1) return false
        val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        val length = when (month) {
            2 -> if (leap) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return day <= length
    }

    /** 0 = Sunday … 6 = Saturday. 1970-01-01 was a Thursday. */
    fun weekday(epochDay: Long): Int = (epochDay + 4).mod(7L).toInt()

    /** Today in Nepal (UTC+5:45, no daylight saving), so a phone set to UTC doesn't give yesterday. */
    @OptIn(ExperimentalTime::class)
    fun todayInNepal(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): Long =
        (nowMillis + NEPAL_OFFSET_MS).floorDiv(86_400_000L)

    private const val NEPAL_OFFSET_MS = (5 * 60 + 45) * 60_000L
}
