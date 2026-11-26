package com.fernandocorreia.khepri.feature.today.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.component.Eyebrow
import com.fernandocorreia.khepri.core.designsystem.component.SurfaceCard
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.feature.today.domain.model.Hydration
import com.fernandocorreia.khepri.feature.today.domain.model.SleepSummary
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.ui.R as TodayR
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** iOS `StatGrid`: streak, check-in, water and sleep, two by two; one column from font scale [ONE_COLUMN_SCALE]. */
@Composable
fun StatGrid(today: Today, onCheckIn: () -> Unit, modifier: Modifier = Modifier) {
    val tiles: List<@Composable (Modifier) -> Unit> =
        listOf(
            { StreakTile(today.streak, it) },
            { CheckInTile(today, onCheckIn, it) },
            { WaterTile(today.hydration, it) },
            { SleepTile(today.sleep, it) },
        )
    val spacing = KhepriTheme.spacing.md
    if (LocalDensity.current.fontScale >= ONE_COLUMN_SCALE) {
        Column(modifier.testTag("stat-grid-one-column"), verticalArrangement = Arrangement.spacedBy(spacing)) {
            tiles.forEach { it(Modifier) }
        }
    } else {
        Column(modifier.testTag("stat-grid-two-columns"), verticalArrangement = Arrangement.spacedBy(spacing)) {
            tiles.chunked(2).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun StreakTile(streak: Int, modifier: Modifier = Modifier) {
    StatTile(
        title = stringResource(R.string.streak),
        icon = TodayR.drawable.ic_streak,
        tint = KhepriTheme.colors.ember,
        value = NumberFormat.getIntegerInstance().format(streak),
        unit = stringResource(if (streak == 1) R.string.day else R.string.days),
        modifier = modifier.testTag("stat-streak"),
    )
}

@Composable
private fun CheckInTile(today: Today, onCheckIn: () -> Unit, modifier: Modifier = Modifier) {
    val checkIn = today.checkIn
    val tag = modifier.testTag("stat-check-in")
    when {
        checkIn != null -> {
            val time =
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(checkIn.at.atZone(ZoneId.systemDefault()))
            val label = stringResource(R.string.check_in_at_x_mood_x_energy_x, time, checkIn.mood, checkIn.energy)
            StatTile(
                title = stringResource(R.string.check_in_3),
                icon = TodayR.drawable.ic_check_in_done,
                tint = KhepriTheme.colors.signal,
                value = "${checkIn.mood} · ${checkIn.energy}",
                caption = stringResource(R.string.mood_energy_x, time),
                onClick = onCheckIn,
                modifier = tag.semantics { contentDescription = label },
            )
        }
        // An older server says only that today is done.
        today.checkedInToday ->
            StatTile(
                title = stringResource(R.string.check_in_3),
                icon = TodayR.drawable.ic_check_in_done,
                tint = KhepriTheme.colors.signal,
                value = stringResource(R.string.done),
                modifier = tag,
            )
        else ->
            StatTile(
                title = stringResource(R.string.check_in_3),
                icon = TodayR.drawable.ic_check_in_open,
                tint = KhepriTheme.colors.signal,
                value = stringResource(R.string.open),
                onClick = onCheckIn,
                modifier = tag,
            )
    }
}

@Composable
private fun WaterTile(hydration: Hydration, modifier: Modifier = Modifier) {
    val litres = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    val drunk =
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
    StatTile(
        title = stringResource(R.string.water),
        icon = TodayR.drawable.ic_water,
        tint = KhepriTheme.colors.dayWater,
        value = drunk.format(hydration.todayMl / ML_PER_LITRE),
        unit = stringResource(R.string.of_x_l, litres.format(hydration.targetMl / ML_PER_LITRE)),
        progress = if (hydration.targetMl > 0) hydration.todayMl.toFloat() / hydration.targetMl else null,
        modifier = modifier.testTag("stat-water"),
    )
}

@Composable
private fun SleepTile(sleep: SleepSummary, modifier: Modifier = Modifier) {
    val hours =
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
    StatTile(
        title = stringResource(R.string.sleep),
        icon = TodayR.drawable.ic_sleep,
        tint = KhepriTheme.colors.daySleep,
        value = if (sleep.logged) hours.format(sleep.durationMinutes / MINUTES_PER_HOUR) else "–",
        unit = stringResource(if (sleep.logged) R.string.h else R.string.not_logged),
        modifier = modifier.testTag("stat-sleep"),
    )
}

/** iOS `StatTile`: an eyebrow with its icon, a big value with its unit, and an optional caption or progress bar. */
@Composable
private fun StatTile(
    title: String,
    @DrawableRes icon: Int,
    tint: Color,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    SurfaceCard(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        contentPadding = KhepriTheme.spacing.lg,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Eyebrow(title, Modifier.weight(1f))
            if (onClick != null) {
                Icon(
                    painterResource(TodayR.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = KhepriTheme.colors.mutedForeground,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KhepriTheme.colors.mutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = KhepriTheme.colors.mutedForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = tint,
                trackColor = KhepriTheme.colors.muted,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            )
        }
    }
}

private const val ONE_COLUMN_SCALE = 1.5f
private const val ML_PER_LITRE = 1000.0
private const val MINUTES_PER_HOUR = 60.0

@Preview
@Composable
private fun StatGridPreview() {
    KhepriTheme {
        StatGrid(
            Today(
                briefing = null,
                streak = 12,
                checkedInToday = false,
                checkIn = null,
                hydration = Hydration(todayMl = 1_250, targetMl = 2_500, percent = 50),
                sleep = SleepSummary(logged = true, durationMinutes = 445, quality = 4),
                nextStep = null,
            ),
            onCheckIn = {},
        )
    }
}
