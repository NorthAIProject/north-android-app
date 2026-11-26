package com.fernandocorreia.khepri.feature.today.domain.usecase

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import javax.inject.Inject

class RefreshToday @Inject constructor(private val repository: TodayRepository) {
    suspend operator fun invoke(): Outcome<Unit> = repository.refreshToday()
}
