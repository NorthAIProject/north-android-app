package com.fernandocorreia.khepri.feature.today.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.feature.today.ui.today.Staleness
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * "Offline, showing 23:10" after a failed refresh; "Showing Wed 23:10" while yesterday's Today waits for its refresh.
 */
@Composable
fun StalenessLine(stale: Staleness, today: LocalDate, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time = stalenessTime(stale.fetchedAt, today, ZoneId.systemDefault(), locale, use24Hour)
    Text(
        stringResource(if (stale.offline) R.string.offline_showing_x else R.string.showing_x, time),
        modifier = modifier.fillMaxWidth().testTag("staleness-line"),
        style = MaterialTheme.typography.bodySmall,
        color = KhepriTheme.colors.mutedForeground,
    )
}

/**
 * When [fetchedAt] was, as short as it can be read: the time on [today], the weekday and time within the last week, the
 * date and time before that. 24 h or 12 h as the device is set.
 */
internal fun stalenessTime(
    fetchedAt: Instant,
    today: LocalDate,
    zone: ZoneId,
    locale: Locale,
    use24Hour: Boolean,
): String {
    val at = fetchedAt.atZone(zone)
    val time = clockTime(locale, use24Hour).format(at)
    val daysAgo = ChronoUnit.DAYS.between(at.toLocalDate(), today)
    val day =
        when {
            daysAgo <= 0 -> null
            daysAgo < WEEK -> DateTimeFormatter.ofPattern("EEE", locale).format(at)
            else -> DateTimeFormatter.ofPattern("d MMM", locale).format(at)
        }
    return listOfNotNull(day, time).joinToString(" ")
}

private const val WEEK = 7

/** Hours and minutes as the device shows them: 24 h or 12 h, in [locale]. */
internal fun clockTime(locale: Locale, use24Hour: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)
