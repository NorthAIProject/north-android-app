package com.fernandocorreia.khepri.feature.auth.domain.usecase

import com.fernandocorreia.khepri.feature.auth.domain.model.AiConsent
import com.fernandocorreia.khepri.feature.auth.domain.repository.AiConsentRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveAiConsent @Inject constructor(private val repository: AiConsentRepository) {
    operator fun invoke(): StateFlow<AiConsent> = repository.consent
}
