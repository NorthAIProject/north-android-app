package com.fernandocorreia.khepri.feature.auth.data.remote

import com.fernandocorreia.khepri.core.api.model.AIConsentDto
import com.fernandocorreia.khepri.feature.auth.domain.model.AiConsent
import com.fernandocorreia.khepri.feature.auth.domain.model.ConsentChoice

internal fun AIConsentDto.toAiConsent(): AiConsent = AiConsent(ConsentChoice.of(status), recipients)

internal fun decisionFor(allow: Boolean): String = if (allow) "granted" else "declined"
