package com.fernandocorreia.khepri.feature.auth.ui.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.auth.domain.usecase.DecideAiConsent
import com.fernandocorreia.khepri.feature.auth.domain.usecase.ObserveAiConsent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** iOS `AIConsentScreen`'s use of `AIConsentCenter`: the answer itself lives in the repository. */
@HiltViewModel
class AiConsentViewModel @Inject constructor(observe: ObserveAiConsent, private val decide: DecideAiConsent) :
    ViewModel() {
    private data class Save(val saving: Boolean = false, val error: AppError? = null)

    private val save = MutableStateFlow(Save())

    val state: StateFlow<AiConsentUiState> =
        combine(observe(), save) { consent, save -> AiConsentUiState(consent.recipients, save.saving, save.error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AiConsentUiState())

    fun onEvent(event: AiConsentEvent) {
        when (event) {
            AiConsentEvent.Allow -> answer(allow = true)
            AiConsentEvent.Decline -> answer(allow = false)
        }
    }

    private fun answer(allow: Boolean) {
        if (save.value.saving) return
        save.value = Save(saving = true)
        viewModelScope.launch {
            val result = decide(allow)
            save.update { Save(error = (result as? Outcome.Err)?.error) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
