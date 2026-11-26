package com.fernandocorreia.khepri.feature.auth.ui.consent

import androidx.compose.runtime.Immutable
import com.fernandocorreia.khepri.core.model.AppError

@Immutable
data class AiConsentUiState(
    val recipients: List<String> = emptyList(),
    val saving: Boolean = false,
    /** The last save failed; the question stays open with this message above the buttons. */
    val error: AppError? = null,
)

sealed interface AiConsentEvent {
    data object Allow : AiConsentEvent

    data object Decline : AiConsentEvent
}
