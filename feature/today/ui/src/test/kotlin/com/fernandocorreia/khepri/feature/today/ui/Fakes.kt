package com.fernandocorreia.khepri.feature.today.ui

import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.navigation.AppDestination
import com.fernandocorreia.khepri.core.navigation.AppTab
import com.fernandocorreia.khepri.core.navigation.Navigator
import com.fernandocorreia.khepri.core.navigation.Route
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Stored values are set by hand; refreshes count their calls and answer [todayResult] / [dayResult]. */
class FakeTodayRepository : TodayRepository {
    val today = MutableStateFlow<Cached<Today>?>(null)
    private val days = mutableMapOf<LocalDate, MutableStateFlow<Cached<Day>?>>()
    val observedDays = mutableListOf<LocalDate>()
    var todayRefreshes = 0
    val dayRefreshes = mutableListOf<LocalDate>()
    var todayResult: Outcome<Unit> = Outcome.Ok(Unit)
    var dayResult: Outcome<Unit> = Outcome.Ok(Unit)

    /** When set, refreshes wait for it. */
    var gate: CompletableDeferred<Unit>? = null

    fun day(date: LocalDate): MutableStateFlow<Cached<Day>?> = days.getOrPut(date) { MutableStateFlow(null) }

    override fun observeToday(): Flow<Cached<Today>?> = today

    override suspend fun refreshToday(): Outcome<Unit> {
        todayRefreshes++
        gate?.await()
        return todayResult
    }

    override fun observeDay(date: LocalDate): Flow<Cached<Day>?> {
        observedDays += date
        return day(date)
    }

    override suspend fun refreshDay(date: LocalDate): Outcome<Unit> {
        dayRefreshes += date
        gate?.await()
        return dayResult
    }

    override suspend fun startFast(hours: Int): Outcome<Unit> = Outcome.Ok(Unit)

    override suspend fun stopFast(): Outcome<Unit> = Outcome.Ok(Unit)
}

class FakeNavigator : Navigator {
    val opened = mutableListOf<Any>()
    var understands = true

    override fun open(destination: AppDestination) {
        opened += destination
    }

    override fun open(link: String): Boolean {
        opened += link
        return understands
    }

    override fun push(route: Route) = Unit

    override fun pop() = Unit

    override fun popToRoot() = Unit

    override fun select(tab: AppTab) = Unit
}

class MovableClock(@Volatile var now: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MovableClock(now, zone)

    override fun instant(): Instant = now
}
