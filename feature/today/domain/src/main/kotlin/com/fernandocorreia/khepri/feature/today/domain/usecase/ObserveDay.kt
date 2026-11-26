package com.fernandocorreia.khepri.feature.today.domain.usecase

import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDay @Inject constructor(private val repository: TodayRepository) {
    operator fun invoke(date: LocalDate): Flow<Cached<Day>?> = repository.observeDay(date)
}
