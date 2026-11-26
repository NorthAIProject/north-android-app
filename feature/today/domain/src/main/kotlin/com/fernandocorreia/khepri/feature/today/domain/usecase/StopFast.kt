package com.fernandocorreia.khepri.feature.today.domain.usecase

import com.fernandocorreia.khepri.core.common.DataChangeBus
import com.fernandocorreia.khepri.core.model.DataScope
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import javax.inject.Inject

/** Ends the current fast; on success the day screens reload. */
class StopFast @Inject constructor(private val repository: TodayRepository, private val bus: DataChangeBus) {
    suspend operator fun invoke(): Outcome<Unit> =
        repository.stopFast().also { if (it is Outcome.Ok) bus.changed(DataScope.Day) }
}
