package com.fernandocorreia.khepri.feature.today.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.feature.today.ui.R as TodayR
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * iOS `TodayScreen`'s title and toolbar: "Today", or the stepped-to day ("Wed 25 Nov"), with previous and next, and
 * Today while on another day. Next is off on today: there is no tomorrow to show.
 */
@Composable
fun DateToolbar(
    date: LocalDate,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val title =
        if (isToday) stringResource(R.string.today)
        else DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).format(date)
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        IconButton(onClick = onPrevious, modifier = Modifier.testTag("day-previous")) {
            Icon(painterResource(TodayR.drawable.ic_chevron_left), stringResource(R.string.previous_day))
        }
        if (!isToday) {
            TextButton(onClick = onToday, modifier = Modifier.testTag("day-today")) {
                Text(stringResource(R.string.today), color = KhepriTheme.colors.signal)
            }
        }
        IconButton(onClick = onNext, enabled = !isToday, modifier = Modifier.testTag("day-next")) {
            Icon(painterResource(TodayR.drawable.ic_chevron_right), stringResource(R.string.next_day))
        }
    }
}

@Preview
@Composable
private fun DateToolbarPreview() {
    KhepriTheme { DateToolbar(LocalDate.of(2026, 11, 25), isToday = false, onPrevious = {}, onNext = {}, onToday = {}) }
}
