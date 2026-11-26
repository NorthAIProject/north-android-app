package com.fernandocorreia.khepri.feature.today.ui.today

import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.DataScope
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.navigation.AppDestination
import com.fernandocorreia.khepri.core.testing.FakeDataChangeBus
import com.fernandocorreia.khepri.core.testing.MainDispatcherExtension
import com.fernandocorreia.khepri.core.ui.Loadable
import com.fernandocorreia.khepri.feature.today.domain.usecase.ObserveDay
import com.fernandocorreia.khepri.feature.today.domain.usecase.ObserveToday
import com.fernandocorreia.khepri.feature.today.domain.usecase.RefreshDay
import com.fernandocorreia.khepri.feature.today.domain.usecase.RefreshToday
import com.fernandocorreia.khepri.feature.today.ui.FakeNavigator
import com.fernandocorreia.khepri.feature.today.ui.FakeTodayRepository
import com.fernandocorreia.khepri.feature.today.ui.MovableClock
import com.fernandocorreia.khepri.feature.today.ui.NOW
import com.fernandocorreia.khepri.feature.today.ui.TODAY
import com.fernandocorreia.khepri.feature.today.ui.ZONE
import com.fernandocorreia.khepri.feature.today.ui.day
import com.fernandocorreia.khepri.feature.today.ui.emptyToday
import com.fernandocorreia.khepri.feature.today.ui.today
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** Plain tests on a hand-advanced Main: `stateIn` and `viewModelScope` launches only run when the scheduler runs. */
class TodayViewModelTest {
    @JvmField @RegisterExtension val main = MainDispatcherExtension(StandardTestDispatcher())

    private val repository = FakeTodayRepository()
    private val navigator = FakeNavigator()
    private val bus = FakeDataChangeBus()
    private val clock = MovableClock(NOW, ZONE)
    private val watching = CoroutineScope(SupervisorJob())

    /** Built inside each test, after the extension has set Main. */
    private val model by lazy {
        TodayViewModel(
            ObserveToday(repository),
            ObserveDay(repository),
            RefreshToday(repository),
            RefreshDay(repository),
            navigator,
            clock,
            bus,
        )
    }

    private val state
        get() = model.state.value

    private fun settle() = main.dispatcher.scheduler.runCurrent()

    private fun watch() {
        watching.launch(main.dispatcher) { model.state.collect {} }
        settle()
    }

    private fun stored(value: com.fernandocorreia.khepri.feature.today.domain.model.Today, ago: Duration = HOUR) {
        repository.today.value = Cached(value, NOW.minus(ago))
    }

    @AfterEach fun stopWatching() = watching.cancel()

    @Test
    fun `content waits for the day for 2 s, then shows without it`() {
        stored(today)
        repository.gate = CompletableDeferred()
        watch()
        main.dispatcher.scheduler.advanceTimeBy(1_999)
        settle()
        assertEquals(Loadable.Loading, state.today)
        main.dispatcher.scheduler.advanceTimeBy(1)
        settle()
        assertEquals(Loadable.Content(today), state.today)
        assertEquals(Loadable.Loading, state.day)
    }

    @Test
    fun `content shows at once when Today and the day are both stored`() {
        stored(today)
        repository.day(TODAY).value = Cached(day(), NOW)
        watch()
        assertEquals(Loadable.Content(today), state.today)
        assertEquals(Loadable.Content(day()), state.day)
    }

    @Test
    fun `a failed refresh keeps the content and marks it stale, naming the weekday when it is from yesterday`() {
        stored(today, ago = Duration.ofHours(11))
        repository.day(TODAY).value = Cached(day(), NOW)
        repository.todayResult = Outcome.Err(AppError.Network(interrupted = false))
        watch()
        assertEquals(Loadable.Content(today), state.today)
        assertEquals(Staleness(NOW.minus(Duration.ofHours(11)), showDay = true), state.stale)
    }

    @Test
    fun `a refresh that succeeds shows no staleness`() {
        stored(today)
        repository.day(TODAY).value = Cached(day(), NOW)
        watch()
        assertNull(state.stale)
    }

    @Test
    fun `nothing stored and a failed refresh fails the screen, and a retry that works shows content`() {
        val offline = AppError.Network(interrupted = false)
        repository.todayResult = Outcome.Err(offline)
        repository.dayResult = Outcome.Err(offline)
        watch()
        assertEquals(Loadable.Failed(offline), state.today)
        repository.todayResult = Outcome.Ok(Unit)
        repository.dayResult = Outcome.Ok(Unit)
        model.onEvent(TodayEvent.Refresh)
        stored(today)
        settle()
        assertInstanceOf(Loadable.Content::class.java, state.today)
    }

    @Test
    fun `a refresh dropped by sign-out with nothing stored keeps loading`() {
        repository.todayResult = Outcome.Err(AppError.SignedOut)
        repository.dayResult = Outcome.Err(AppError.SignedOut)
        watch()
        main.dispatcher.scheduler.advanceTimeBy(2_001)
        settle()
        assertEquals(Loadable.Loading, state.today)
    }

    @Test
    fun `stepping days observes each day, and next does nothing on today`() {
        stored(today)
        watch()
        model.onEvent(TodayEvent.NextDay)
        settle()
        assertEquals(TODAY, state.date)
        model.onEvent(TodayEvent.PreviousDay)
        settle()
        assertEquals(TODAY.minusDays(1), state.date)
        assertEquals(false, state.isToday)
        assertTrue(TODAY.minusDays(1) in repository.observedDays)
        assertTrue(TODAY.minusDays(1) in repository.dayRefreshes)
        model.onEvent(TodayEvent.PreviousDay)
        model.onEvent(TodayEvent.NextDay)
        settle()
        assertEquals(TODAY.minusDays(1), state.date)
        model.onEvent(TodayEvent.GoToToday)
        settle()
        assertEquals(TODAY, state.date)
        assertTrue(state.isToday)
    }

    @Test
    fun `a day change, a reload and a re-selected tab each refresh once`() {
        stored(today)
        watch()
        assertEquals(1, repository.todayRefreshes)
        bus.changed(DataScope.Day)
        settle()
        assertEquals(2, repository.todayRefreshes)
        model.onEvent(TodayEvent.Reload)
        settle()
        assertEquals(3, repository.todayRefreshes)
        assertEquals(listOf(TODAY, TODAY, TODAY), repository.dayRefreshes)
    }

    @Test
    fun `reasons to refresh during a refresh run it once more afterwards`() {
        stored(today)
        repository.gate = CompletableDeferred()
        watch()
        bus.changed(DataScope.Day)
        model.onEvent(TodayEvent.Reload)
        settle()
        assertEquals(1, repository.todayRefreshes)
        repository.gate?.complete(Unit)
        settle()
        assertEquals(2, repository.todayRefreshes)
    }

    @Test
    fun `pull to refresh shows the indicator until it ends`() {
        stored(today)
        repository.day(TODAY).value = Cached(day(), NOW)
        watch()
        repository.gate = CompletableDeferred()
        model.onEvent(TodayEvent.Refresh)
        settle()
        assertEquals(Loadable.Content(today, refreshing = true), state.today)
        repository.gate?.complete(Unit)
        settle()
        assertEquals(Loadable.Content(today), state.today)
    }

    @Test
    fun `a screen left on today moves to the new day after midnight`() {
        stored(today)
        watch()
        clock.now = NOW.plus(Duration.ofDays(1))
        model.onEvent(TodayEvent.Reload)
        settle()
        assertEquals(TODAY.plusDays(1), state.date)
        assertTrue(TODAY.plusDays(1) in repository.dayRefreshes)
    }

    @Test
    fun `the next step opens its link, and a check-in step opens the check-in`() {
        stored(today)
        watch()
        main.dispatcher.scheduler.advanceTimeBy(2_001)
        settle()
        model.onEvent(TodayEvent.OpenNextStep)
        stored(emptyToday)
        settle()
        model.onEvent(TodayEvent.OpenNextStep)
        model.onEvent(TodayEvent.OpenCheckIn)
        assertEquals(
            listOf("/app/training/next", AppDestination.CheckInFlow, AppDestination.CheckInFlow),
            navigator.opened,
        )
    }

    private companion object {
        val HOUR: Duration = Duration.ofHours(1)
    }
}
