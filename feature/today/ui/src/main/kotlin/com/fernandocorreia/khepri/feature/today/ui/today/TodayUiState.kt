package com.fernandocorreia.khepri.feature.today.ui.today

import com.fernandocorreia.khepri.core.ui.Loadable
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import java.time.Instant
import java.time.LocalDate

/**
 * The Today tab: Today's snapshot, the day My Day shows ([date], today unless stepped back), and [stale] while a
 * refresh failed and Today shows what was stored. [stale] is the screen's one staleness signal; `Content.staleSince`
 * stays unset here.
 */
data class TodayUiState(
    val today: Loadable<Today> = Loadable.Loading,
    val day: Loadable<Day> = Loadable.Loading,
    val date: LocalDate,
    /** The calendar day now, from the app clock: the header's date. */
    val calendarDay: LocalDate = date,
    val isToday: Boolean = true,
    val stale: Staleness? = null,
)

/** Today was fetched at [fetchedAt]; [showDay] when that was on an earlier day, so the line names the weekday. */
data class Staleness(val fetchedAt: Instant, val showDay: Boolean)
