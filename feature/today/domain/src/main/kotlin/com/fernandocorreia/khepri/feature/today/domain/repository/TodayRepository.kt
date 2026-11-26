package com.fernandocorreia.khepri.feature.today.domain.repository

import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Today and the days behind My Day, cache first. Observing emits what is stored (or null), then every refresh; a failed
 * refresh keeps the value and sets its `refreshError`. Today and yesterday are kept on disk per account; other dates
 * only in memory.
 */
interface TodayRepository {
    fun observeToday(): Flow<Cached<Today>?>

    suspend fun refreshToday(): Outcome<Unit>

    fun observeDay(date: LocalDate): Flow<Cached<Day>?>

    suspend fun refreshDay(date: LocalDate): Outcome<Unit>

    suspend fun startFast(hours: Int): Outcome<Unit>

    suspend fun stopFast(): Outcome<Unit>
}
