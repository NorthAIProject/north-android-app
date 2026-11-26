package com.fernandocorreia.khepri.feature.auth.domain.usecase

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.auth.domain.repository.AiConsentRepository
import javax.inject.Inject

class DecideAiConsent @Inject constructor(private val repository: AiConsentRepository) {
    suspend operator fun invoke(allow: Boolean): Outcome<Unit> = repository.decide(allow)
}
