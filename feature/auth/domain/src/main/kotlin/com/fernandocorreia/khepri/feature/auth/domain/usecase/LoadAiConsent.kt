package com.fernandocorreia.khepri.feature.auth.domain.usecase

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.auth.domain.repository.AiConsentRepository
import javax.inject.Inject

class LoadAiConsent @Inject constructor(private val repository: AiConsentRepository) {
    suspend operator fun invoke(): Outcome<Unit> = repository.load()
}
