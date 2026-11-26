package com.fernandocorreia.khepri.feature.today.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.component.Eyebrow
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** iOS `Header`: today's date as an eyebrow ("Thursday, November 26"), then the coach's briefing when there is one. */
@Composable
fun TodayHeader(briefing: String?, date: LocalDate, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
    Column(modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(DateTimeFormatter.ofPattern(pattern, locale).format(date))
        if (!briefing.isNullOrBlank()) {
            Text(briefing, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview
@Composable
private fun TodayHeaderPreview() {
    KhepriTheme { TodayHeader("A steady day: the planned session stays.", LocalDate.of(2026, 11, 26)) }
}
