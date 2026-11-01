package com.nuvio.app.features.profiles

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.library.LibraryClock
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.seasonal_autumn
import nuvio.composeapp.generated.resources.seasonal_christmas
import nuvio.composeapp.generated.resources.seasonal_easter
import nuvio.composeapp.generated.resources.seasonal_eid
import nuvio.composeapp.generated.resources.seasonal_halloween
import nuvio.composeapp.generated.resources.seasonal_new_year
import nuvio.composeapp.generated.resources.seasonal_ramadan
import nuvio.composeapp.generated.resources.seasonal_spring
import nuvio.composeapp.generated.resources.seasonal_summer
import nuvio.composeapp.generated.resources.seasonal_winter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Seasonal look for the profile picker. Picked automatically from today's date: holidays first,
 * otherwise the (northern-hemisphere) season. Decorations are drawn on a 390x844 phone canvas,
 * see scripts/generate_seasonal_drawables.py.
 */
enum class SeasonalTheme(
    val accent: Color,
    val backgroundStops: List<Pair<Float, Color>>,
    val radialBackground: Boolean,
    val decorations: DrawableResource,
) {
    CHRISTMAS(
        Color(0xFFFF6B6B),
        listOf(0f to Color(0xFF123A2B), 0.55f to Color(0xFF0C2419), 1f to Color(0xFF08140F)), false,
        Res.drawable.seasonal_christmas,
    ),
    HALLOWEEN(
        Color(0xFFFF9F43),
        listOf(0f to Color(0xFF2B1646), 0.55f to Color(0xFF1A0F2B), 1f to Color(0xFF0C0714)), false,
        Res.drawable.seasonal_halloween,
    ),
    EASTER(
        Color(0xFFE9C46A),
        listOf(0f to Color(0xFF141E33), 0.55f to Color(0xFF1E2C45), 1f to Color(0xFF24362A)), false,
        Res.drawable.seasonal_easter,
    ),
    EID(
        Color(0xFFE9C46A),
        listOf(0f to Color(0xFF0E4247), 0.55f to Color(0xFF092C30), 1f to Color(0xFF05181B)), false,
        Res.drawable.seasonal_eid,
    ),
    RAMADAN(
        Color(0xFFF2C14E),
        listOf(0f to Color(0xFF1E1650), 0.55f to Color(0xFF120D33), 1f to Color(0xFF08061C)), false,
        Res.drawable.seasonal_ramadan,
    ),
    NEW_YEAR(
        Color(0xFFFFD166),
        listOf(0f to Color(0xFF2D2260), 0.55f to Color(0xFF0F0C22), 1f to Color(0xFF050409)), true,
        Res.drawable.seasonal_new_year,
    ),
    SUMMER(
        Color(0xFFFFC857),
        listOf(
            0f to Color(0xFF1B1036), 0.38f to Color(0xFF3E1B4D), 0.68f to Color(0xFF8E2F3C),
            0.86f to Color(0xFFE0662F), 1f to Color(0xFFF29E4C),
        ),
        false,
        Res.drawable.seasonal_summer,
    ),
    AUTUMN(
        Color(0xFFF4A261),
        listOf(0f to Color(0xFF3E200D), 0.55f to Color(0xFF26140A), 1f to Color(0xFF140A05)), false,
        Res.drawable.seasonal_autumn,
    ),
    WINTER(
        Color(0xFF9AD8FF),
        listOf(0f to Color(0xFF16304F), 0.55f to Color(0xFF0D1F36), 1f to Color(0xFF081322)), false,
        Res.drawable.seasonal_winter,
    ),
    SPRING(
        Color(0xFF8ED081),
        listOf(0f to Color(0xFF0E2A3D), 0.5f to Color(0xFF123A3A), 1f to Color(0xFF0F2E20)), false,
        Res.drawable.seasonal_spring,
    ),
}

internal data class CalendarDate(val year: Int, val month: Int, val day: Int)

/** Days since 1970-01-01 for a Gregorian date (Howard Hinnant's days_from_civil). */
internal fun epochDayOf(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (month + if (month > 2) -3 else 9).toLong()
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** Gregorian date for a number of days since 1970-01-01 (Howard Hinnant's civil_from_days). */
internal fun calendarDateOf(epochDay: Long): CalendarDate {
    val z = epochDay + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
    return CalendarDate(year, month, day)
}

/** Western (Gregorian) Easter Sunday, as an epoch day (Anonymous Gregorian algorithm). */
internal fun easterSundayEpochDay(year: Int): Long {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return epochDayOf(year, month, day)
}

/**
 * Expected Islamic dates (astronomical calculation). Local moon sighting can move them by a day.
 * Extend this table to keep Ramadan and Eid themes working past 2035.
 */
private val ramadanStarts = listOf(
    epochDayOf(2026, 2, 18), epochDayOf(2027, 2, 8), epochDayOf(2028, 1, 28), epochDayOf(2029, 1, 16),
    epochDayOf(2030, 1, 5), epochDayOf(2030, 12, 26), epochDayOf(2031, 12, 15), epochDayOf(2032, 12, 4),
    epochDayOf(2033, 11, 23), epochDayOf(2034, 11, 12), epochDayOf(2035, 11, 1),
)
private val eidAlFitrDays = listOf(
    epochDayOf(2026, 3, 20), epochDayOf(2027, 3, 10), epochDayOf(2028, 2, 27), epochDayOf(2029, 2, 14),
    epochDayOf(2030, 2, 4), epochDayOf(2031, 1, 24), epochDayOf(2032, 1, 14), epochDayOf(2033, 1, 2),
    epochDayOf(2033, 12, 23), epochDayOf(2034, 12, 12), epochDayOf(2035, 12, 1),
)
private val eidAlAdhaDays = listOf(
    epochDayOf(2026, 5, 27), epochDayOf(2027, 5, 16), epochDayOf(2028, 5, 5), epochDayOf(2029, 4, 24),
    epochDayOf(2030, 4, 13), epochDayOf(2031, 4, 2), epochDayOf(2032, 3, 22), epochDayOf(2033, 3, 11),
    epochDayOf(2034, 3, 1), epochDayOf(2035, 2, 18),
)

/**
 * Theme for a date. Priority when holidays overlap: Eid, Ramadan, New Year, Christmas, Easter,
 * Halloween, then the season (spring Mar-May, summer Jun-Aug, autumn Sep-Nov, winter Dec-Feb).
 */
internal fun seasonalThemeFor(date: CalendarDate): SeasonalTheme {
    val today = epochDayOf(date.year, date.month, date.day)
    val (month, day) = date.month to date.day
    return when {
        eidAlFitrDays.any { today in it..(it + 2) } || eidAlAdhaDays.any { today in it..(it + 3) } -> SeasonalTheme.EID
        ramadanStarts.zip(eidAlFitrDays).any { (start, eid) -> today in start until eid } -> SeasonalTheme.RAMADAN
        (month == 12 && day >= 30) || (month == 1 && day <= 2) -> SeasonalTheme.NEW_YEAR
        month == 12 -> SeasonalTheme.CHRISTMAS
        easterSundayEpochDay(date.year).let { today in (it - 3)..(it + 1) } -> SeasonalTheme.EASTER
        (month == 10 && day >= 20) || (month == 11 && day == 1) -> SeasonalTheme.HALLOWEEN
        month in 3..5 -> SeasonalTheme.SPRING
        month in 6..8 -> SeasonalTheme.SUMMER
        month in 9..11 -> SeasonalTheme.AUTUMN
        else -> SeasonalTheme.WINTER
    }
}

private const val MS_PER_DAY = 86_400_000L

/** Theme for today (UTC date). */
internal fun currentSeasonalTheme(nowEpochMs: Long = LibraryClock.nowEpochMs()): SeasonalTheme =
    seasonalThemeFor(
        calendarDateOf(if (nowEpochMs >= 0) nowEpochMs / MS_PER_DAY else (nowEpochMs - MS_PER_DAY + 1) / MS_PER_DAY),
    )

/** Seasonal gradient plus decorations, used as the profile picker's default background. */
@Composable
fun SeasonalBackdrop(
    theme: SeasonalTheme,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.drawBehind {
            val stops = theme.backgroundStops.toTypedArray()
            val brush = if (theme.radialBackground) {
                Brush.radialGradient(
                    *stops,
                    center = Offset(size.width / 2f, 0f),
                    radius = size.height * 0.75f,
                )
            } else {
                Brush.verticalGradient(*stops)
            }
            drawRect(brush)
        },
    ) {
        // Decorations are laid out for a portrait phone; skip them on tablets and in landscape.
        if (maxHeight > maxWidth && maxWidth < 600.dp) {
            Image(
                painter = painterResource(theme.decorations),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}
