package io.github.thegbguy.nepalikeyboard.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BsCalendarTest {

    private fun bs(ad: String) = BsCalendar.toBs(LocalDate.parse(ad).toEpochDay())

    @Test
    fun `known dates`() {
        assertEquals(BsDate(2000, 1, 1), bs("1943-04-14"))
        assertEquals(BsDate(2065, 2, 15), bs("2008-05-28")) // Republic Day
        assertEquals(BsDate(2072, 6, 3), bs("2015-09-20")) // Constitution Day
        assertEquals(BsDate(2082, 7, 1), bs("2025-10-18"))
        assertEquals(BsDate(2083, 1, 1), bs("2026-04-14"))
        assertEquals(BsDate(2083, 7, 4), bs("2026-10-21"))
        assertEquals(BsDate(2090, 12, 30), bs("2034-04-13")) // last day in the table
    }

    @Test
    fun `nothing outside the table`() {
        assertNull(bs("1943-04-13"))
        assertNull(bs("2034-04-14"))
        assertNull(BsCalendar.toEpochDay(BsDate(1999, 12, 30)))
        assertNull(BsCalendar.toEpochDay(BsDate(2091, 1, 1)))
        assertNull(BsCalendar.toEpochDay(BsDate(2083, 8, 30))) // Mangsir 2083 has 29 days
        assertNull(BsCalendar.toEpochDay(BsDate(2083, 13, 1)))
    }

    @Test
    fun `2083 month lengths match Hamro Patro`() {
        assertEquals(listOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), (1..12).map { BsCalendar.monthLength(2083, it) })
    }

    @Test
    fun `every day round-trips and Gregorian matches java time`() {
        var day = LocalDate.of(1943, 4, 14)
        val end = LocalDate.of(2034, 4, 13)
        var previous: BsDate? = null
        while (!day.isAfter(end)) {
            val epochDay = day.toEpochDay()
            assertEquals(epochDay, Gregorian.toEpochDay(day.year, day.monthValue, day.dayOfMonth))
            assertEquals(Triple(day.year, day.monthValue, day.dayOfMonth), Gregorian.fromEpochDay(epochDay))
            assertEquals(day.dayOfWeek.value % 7, Gregorian.weekday(epochDay))
            val date = BsCalendar.toBs(epochDay)!!
            assertEquals(epochDay, BsCalendar.toEpochDay(date))
            previous?.let { p -> // consecutive days are consecutive BS days
                val next = if (p.day < BsCalendar.monthLength(p.year, p.month)!!) p.copy(day = p.day + 1)
                else if (p.month < 12) BsDate(p.year, p.month + 1, 1) else BsDate(p.year + 1, 1, 1)
                assertEquals(next, date)
            }
            previous = date
            day = day.plusDays(1)
        }
    }

    @Test
    fun `today in Nepal is ahead of UTC`() {
        // 2026-10-26 19:00 UTC is 00:45 on the 27th in Kathmandu
        val millis = LocalDate.of(2026, 10, 26).toEpochDay() * 86_400_000L + 19 * 3_600_000L
        assertEquals(LocalDate.of(2026, 10, 27).toEpochDay(), Gregorian.todayInNepal(millis))
    }
}
