package com.fernandocorreia.khepri.feature.today.ui.today

import com.fernandocorreia.khepri.core.ui.Loadable
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import java.time.Instant
import java.time.LocalDate

/**
 * The Today tab: Today's snapshot, the day My Day shows ([date], today unless stepped back), and [stale] while Today
 * shows what was stored after a failed refresh, or from an earlier day. [stale] is the screen's one staleness signal;
 * `Content.staleSince` stays unset here.
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

/**
 * Today on screen was fetched at [fetchedAt]: shown after a failed refresh ([offline]), and also while a refresh runs
 * when it is from an earlier day, so yesterday's numbers never read as today's.
 */
data class Staleness(val fetchedAt: Instant, val offline: Boolean)
