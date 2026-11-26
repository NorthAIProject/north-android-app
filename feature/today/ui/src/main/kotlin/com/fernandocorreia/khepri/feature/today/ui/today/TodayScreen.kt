package com.fernandocorreia.khepri.feature.today.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.navigation.AppTab
import com.fernandocorreia.khepri.core.navigation.ScreenAsk
import com.fernandocorreia.khepri.core.navigation.ui.ConsumeAsks
import com.fernandocorreia.khepri.core.ui.ErrorState
import com.fernandocorreia.khepri.core.ui.Loadable
import com.fernandocorreia.khepri.feature.today.domain.model.Hydration
import com.fernandocorreia.khepri.feature.today.domain.model.SleepSummary
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.ui.components.DateToolbar
import com.fernandocorreia.khepri.feature.today.ui.components.NextStepCard
import com.fernandocorreia.khepri.feature.today.ui.components.StalenessLine
import com.fernandocorreia.khepri.feature.today.ui.components.StatGrid
import com.fernandocorreia.khepri.feature.today.ui.components.TodayHeader
import java.time.LocalDate

/**
 * The Today tab's root. Refreshes on appear, on pull, when the app returns to the foreground, when the tab is picked
 * again and after any write, as iOS.
 */
@Composable
fun TodayScreen(modifier: Modifier = Modifier, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConsumeAsks(pick = { (it as? ScreenAsk.ReloadRoot)?.takeIf { ask -> ask.tab == AppTab.Today } }) {
        viewModel.onEvent(TodayEvent.Reload)
    }
    // Every start: back in the foreground, or back on the tab. A refresh already running is joined, not repeated.
    LifecycleStartEffect(viewModel) {
        viewModel.onEvent(TodayEvent.Reload)
        onStopOrDispose {}
    }
    TodayContent(state, viewModel::onEvent, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayContent(state: TodayUiState, onEvent: (TodayEvent) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxSize().testTag("today-screen"),
        color = KhepriTheme.colors.background,
        contentColor = KhepriTheme.colors.foreground,
    ) {
        Column {
            DateToolbar(
                date = state.date,
                isToday = state.isToday,
                onPrevious = { onEvent(TodayEvent.PreviousDay) },
                onNext = { onEvent(TodayEvent.NextDay) },
                onToday = { onEvent(TodayEvent.GoToToday) },
            )
            when (val today = state.today) {
                Loadable.Loading ->
                    Box(Modifier.fillMaxSize().testTag("today-loading"), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KhepriTheme.colors.signal)
                    }
                is Loadable.Failed -> ErrorState(today.error, onRetry = { onEvent(TodayEvent.Refresh) })
                is Loadable.Content ->
                    PullToRefreshBox(
                        isRefreshing = today.refreshing,
                        onRefresh = { onEvent(TodayEvent.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        TodayColumn(today.value, state, onEvent)
                    }
            }
        }
    }
}

@Composable
private fun TodayColumn(today: Today, state: TodayUiState, onEvent: (TodayEvent) -> Unit) {
    val spacing = KhepriTheme.spacing
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg)
            .padding(top = spacing.sm, bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        state.stale?.let { StalenessLine(it, today = state.calendarDay) }
        TodayHeader(today.briefing, state.calendarDay)
        StatGrid(today, onCheckIn = { onEvent(TodayEvent.OpenCheckIn) })
        today.nextStep?.let { NextStepCard(it, onAction = { onEvent(TodayEvent.OpenNextStep) }) }
    }
}

@Preview
@Composable
private fun TodayContentPreview() {
    KhepriTheme {
        TodayContent(
            TodayUiState(
                today =
                    Loadable.Content(
                        Today(
                            briefing = "A steady day.",
                            streak = 3,
                            checkedInToday = false,
                            checkIn = null,
                            hydration = Hydration(500, 2_500, 20),
                            sleep = SleepSummary(logged = false, durationMinutes = 0, quality = null),
                            nextStep = null,
                        )
                    ),
                date = LocalDate.of(2026, 11, 26),
            ),
            onEvent = {},
        )
    }
}
