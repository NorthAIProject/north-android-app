package com.fernandocorreia.khepri.feature.today.domain

import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.DataScope
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.testing.FakeDataChangeBus
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import com.fernandocorreia.khepri.feature.today.domain.usecase.StartFast
import com.fernandocorreia.khepri.feature.today.domain.usecase.StopFast
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StartFastTest {
    private val bus = FakeDataChangeBus()
    private val repository = FakeTodayRepository()

    @Test
    fun `a started fast reloads the day once`() = runTest {
        assertEquals(Outcome.Ok(Unit), StartFast(repository, bus)(16))
        assertEquals(listOf(16), repository.started)
        assertEquals(listOf(DataScope.Day), bus.emitted)
    }

    @Test
    fun `a failed start reloads nothing`() = runTest {
        repository.result = Outcome.Err(AppError.Network(interrupted = false))
        StartFast(repository, bus)(16)
        assertEquals(emptyList<DataScope>(), bus.emitted)
    }

    @Test
    fun `a stopped fast reloads the day, a failed stop does not`() = runTest {
        StopFast(repository, bus)()
        repository.result = Outcome.Err(AppError.Network(interrupted = false))
        StopFast(repository, bus)()
        assertEquals(listOf(DataScope.Day), bus.emitted)
    }
}

private class FakeTodayRepository : TodayRepository {
    var result: Outcome<Unit> = Outcome.Ok(Unit)
    val started = mutableListOf<Int>()

    override fun observeToday(): Flow<Cached<Today>?> = emptyFlow()

    override suspend fun refreshToday(): Outcome<Unit> = result

    override fun observeDay(date: LocalDate): Flow<Cached<Day>?> = emptyFlow()

    override suspend fun refreshDay(date: LocalDate): Outcome<Unit> = result

    override suspend fun startFast(hours: Int): Outcome<Unit> {
        started += hours
        return result
    }

    override suspend fun stopFast(): Outcome<Unit> = result
}
