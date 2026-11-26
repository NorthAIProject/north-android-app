package com.fernandocorreia.khepri.feature.today.ui.today

sealed interface TodayEvent {
    /** Pull to refresh: shows the indicator while it runs. */
    data object Refresh : TodayEvent

    /** Back in the foreground, or the tab re-selected: refresh without the indicator. */
    data object Reload : TodayEvent

    data object PreviousDay : TodayEvent

    /** Does nothing on today: the future has no day. */
    data object NextDay : TodayEvent

    data object GoToToday : TodayEvent

    data object OpenNextStep : TodayEvent

    data object OpenCheckIn : TodayEvent
}
