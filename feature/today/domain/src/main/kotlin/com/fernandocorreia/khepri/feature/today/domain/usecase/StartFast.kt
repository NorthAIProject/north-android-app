package com.fernandocorreia.khepri.feature.today.domain.usecase

import com.fernandocorreia.khepri.core.common.DataChangeBus
import com.fernandocorreia.khepri.core.model.DataScope
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import javax.inject.Inject

/** Starts a fast of [hours]; on success the day screens reload. */
class StartFast @Inject constructor(private val repository: TodayRepository, private val bus: DataChangeBus) {
    suspend operator fun invoke(hours: Int): Outcome<Unit> =
        repository.startFast(hours).also { if (it is Outcome.Ok) bus.changed(DataScope.Day) }
}
