package com.nuvio.app.features.profiles

import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonalThemeTest {
    private fun themeOn(year: Int, month: Int, day: Int) = seasonalThemeFor(CalendarDate(year, month, day))

    @Test
    fun epochDayRoundTrips() {
        assertEquals(0L, epochDayOf(1970, 1, 1))
        assertEquals(CalendarDate(2026, 11, 1), calendarDateOf(epochDayOf(2026, 11, 1)))
        assertEquals(CalendarDate(2028, 2, 29), calendarDateOf(epochDayOf(2028, 2, 29)))
    }

    @Test
    fun easterSundayMatchesKnownDates() {
        assertEquals(epochDayOf(2026, 4, 5), easterSundayEpochDay(2026))
        assertEquals(epochDayOf(2027, 3, 28), easterSundayEpochDay(2027))
        assertEquals(epochDayOf(2030, 4, 21), easterSundayEpochDay(2030))
    }

    @Test
    fun holidaysWinOverSeasons() {
        assertEquals(SeasonalTheme.HALLOWEEN, themeOn(2026, 10, 31))
        assertEquals(SeasonalTheme.CHRISTMAS, themeOn(2026, 12, 25))
        assertEquals(SeasonalTheme.NEW_YEAR, themeOn(2026, 12, 31))
        assertEquals(SeasonalTheme.NEW_YEAR, themeOn(2027, 1, 1))
        assertEquals(SeasonalTheme.EASTER, themeOn(2027, 3, 28))
        assertEquals(SeasonalTheme.RAMADAN, themeOn(2027, 2, 20))
        assertEquals(SeasonalTheme.EID, themeOn(2027, 3, 10))
        assertEquals(SeasonalTheme.EID, themeOn(2027, 5, 17))
    }

    @Test
    fun ramadanOverlappingChristmasShowsRamadan() {
        assertEquals(SeasonalTheme.RAMADAN, themeOn(2032, 12, 25))
    }

    @Test
    fun seasonsCoverTheRestOfTheYear() {
        assertEquals(SeasonalTheme.WINTER, themeOn(2027, 1, 20))
        assertEquals(SeasonalTheme.SPRING, themeOn(2027, 4, 20))
        assertEquals(SeasonalTheme.SUMMER, themeOn(2027, 7, 15))
        assertEquals(SeasonalTheme.AUTUMN, themeOn(2027, 9, 15))
        assertEquals(SeasonalTheme.AUTUMN, themeOn(2026, 11, 2))
    }
}
