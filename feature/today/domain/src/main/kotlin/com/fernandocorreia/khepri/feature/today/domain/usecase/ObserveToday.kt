package com.fernandocorreia.khepri.feature.today.domain.usecase

import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveToday @Inject constructor(private val repository: TodayRepository) {
    operator fun invoke(): Flow<Cached<Today>?> = repository.observeToday()
}
