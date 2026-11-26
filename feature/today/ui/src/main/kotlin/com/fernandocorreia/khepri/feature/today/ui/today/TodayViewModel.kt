package com.fernandocorreia.khepri.feature.today.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.core.common.DataChangeBus
import com.fernandocorreia.khepri.core.common.reloads
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.DataScope
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.navigation.AppDestination
import com.fernandocorreia.khepri.core.navigation.Navigator
import com.fernandocorreia.khepri.core.ui.Loadable
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.usecase.ObserveDay
import com.fernandocorreia.khepri.feature.today.domain.usecase.ObserveToday
import com.fernandocorreia.khepri.feature.today.domain.usecase.RefreshDay
import com.fernandocorreia.khepri.feature.today.domain.usecase.RefreshToday
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * iOS `TodayScreen`: Today and My Day's day load beside each other, cache first. The screen waits for both, or
 * [PATIENCE_MS], before showing content. Refreshes run one at a time; a reason to refresh that lands during one runs
 * one more afterwards, so a write is never left showing its old value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel
@Inject
constructor(
    observeToday: ObserveToday,
    private val observeDay: ObserveDay,
    private val refreshToday: RefreshToday,
    private val refreshDay: RefreshDay,
    private val navigator: Navigator,
    private val clock: Clock,
    bus: DataChangeBus,
) : ViewModel() {
    /** The stepped-to date; null follows today, so a screen left open over midnight moves on with it. */
    private val selection = MutableStateFlow<LocalDate?>(null)

    /** Bumped on every refresh, so "today" is read again after midnight. */
    private val tick = MutableStateFlow(0)
    private val todayError = MutableStateFlow<AppError?>(null)
    private val dayErrors = MutableStateFlow<Map<LocalDate, AppError>>(emptyMap())
    private val pulling = MutableStateFlow(false)
    private val revealed = MutableStateFlow(false)
    private var refreshing: Job? = null
    private var again = false
    private var patience: Job? = null

    private val date: Flow<LocalDate> =
        combine(selection, tick) { picked, _ -> picked ?: today() }.distinctUntilChanged()

    private val day: Flow<Pair<LocalDate, Cached<Day>?>> = date.flatMapLatest { d -> observeDay(d).map { d to it } }

    private val inputs: Flow<Inputs> =
        combine(observeToday(), day, todayError, dayErrors, pulling) { today, (date, day), todayError, dayErrors, pull
            ->
            Inputs(today, todayError, date, day, dayErrors[date], pull)
        }

    val state: StateFlow<TodayUiState> =
        combine(inputs, revealed, ::render)
            .onEach { if (it.today !is Loadable.Loading) revealed.value = true }
            .onStart {
                launchRefresh()
                startPatience()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TodayUiState(date = today()))

    init {
        viewModelScope.launch { bus.reloads(*DataScope.entries.toTypedArray()).collect { launchRefresh() } }
    }

    fun onEvent(event: TodayEvent) {
        when (event) {
            TodayEvent.Refresh -> {
                pulling.value = true
                launchRefresh()
            }
            TodayEvent.Reload -> launchRefresh()
            TodayEvent.PreviousDay -> pick(shown().minusDays(1))
            TodayEvent.NextDay -> if (shown() < today()) pick(shown().plusDays(1))
            TodayEvent.GoToToday -> pick(today())
            TodayEvent.OpenNextStep -> openNextStep()
            TodayEvent.OpenCheckIn -> navigator.open(AppDestination.CheckInFlow)
        }
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun shown(): LocalDate = selection.value ?: today()

    private fun pick(date: LocalDate) {
        val next = date.takeIf { it != today() }
        if (next == selection.value) return
        selection.value = next
        viewModelScope.launch { loadDay(date) }
    }

    private fun openNextStep() {
        val step = (state.value.today as? Loadable.Content)?.value?.nextStep ?: return
        // As iOS: a path this build does not map does nothing.
        if (step.kind == CHECK_IN) navigator.open(AppDestination.CheckInFlow) else navigator.open(step.href)
    }

    /** Starts a refresh and returns at once, so stored content reaches the screen before the network answers. */
    private fun launchRefresh() {
        if (refreshing?.isActive == true) {
            again = true
            return
        }
        refreshing = viewModelScope.launch {
            do {
                again = false
                tick.update { it + 1 }
                val date = shown()
                val today = launch { loadToday() }
                loadDay(date)
                today.join()
            } while (again)
            pulling.value = false
        }
    }

    private suspend fun loadToday() {
        val result = refreshToday()
        todayError.value = (result as? Outcome.Err)?.error?.takeUnless { it is AppError.SignedOut }
    }

    private suspend fun loadDay(date: LocalDate) {
        val error = (refreshDay(date) as? Outcome.Err)?.error?.takeUnless { it is AppError.SignedOut }
        dayErrors.update { if (error == null) it - date else it + (date to error) }
    }

    private fun startPatience() {
        if (revealed.value || patience?.isActive == true) return
        patience = viewModelScope.launch {
            delay(PATIENCE_MS)
            revealed.value = true
        }
    }

    private fun render(inputs: Inputs, revealed: Boolean): TodayUiState {
        val today = loadable(inputs.today, inputs.todayError, refreshing = inputs.pulling)
        val day = loadable(inputs.day, inputs.dayError)
        val ready = revealed || today is Loadable.Failed || (today !is Loadable.Loading && day !is Loadable.Loading)
        val now = today()
        val stale =
            inputs.today
                ?.takeIf { (it.refreshError ?: inputs.todayError)?.let { e -> e !is AppError.SignedOut } == true }
                ?.let { Staleness(it.fetchedAt, showDay = it.fetchedAt.atZone(clock.zone).toLocalDate() != now) }
        return TodayUiState(
            today = if (ready) today else Loadable.Loading,
            day = if (ready) day else Loadable.Loading,
            date = inputs.date,
            calendarDay = now,
            isToday = inputs.date == now,
            stale = stale.takeIf { ready },
        )
    }

    private fun <T> loadable(cached: Cached<T>?, error: AppError?, refreshing: Boolean = false): Loadable<T> =
        when {
            cached != null -> Loadable.Content(cached.value, refreshing = refreshing)
            error != null -> Loadable.Failed(error)
            else -> Loadable.Loading
        }

    private data class Inputs(
        val today: Cached<Today>?,
        val todayError: AppError?,
        val date: LocalDate,
        val day: Cached<Day>?,
        val dayError: AppError?,
        val pulling: Boolean,
    )

    internal companion object {
        const val PATIENCE_MS = 2_000L
        const val STOP_TIMEOUT_MS = 5_000L
        const val CHECK_IN = "check_in"
    }
}
