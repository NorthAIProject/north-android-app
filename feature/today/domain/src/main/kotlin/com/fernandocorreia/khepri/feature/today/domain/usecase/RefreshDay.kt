package com.fernandocorreia.khepri.feature.today.domain.usecase

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import java.time.LocalDate
import javax.inject.Inject

class RefreshDay @Inject constructor(private val repository: TodayRepository) {
    suspend operator fun invoke(date: LocalDate): Outcome<Unit> = repository.refreshDay(date)
}
