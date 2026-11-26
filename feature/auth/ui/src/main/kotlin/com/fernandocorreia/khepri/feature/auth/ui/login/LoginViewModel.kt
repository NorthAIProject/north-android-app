package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.ui.Haptic
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm
import com.fernandocorreia.khepri.feature.auth.domain.usecase.RequestPasswordReset
import com.fernandocorreia.khepri.feature.auth.domain.usecase.SignInWithEmail
import com.fernandocorreia.khepri.feature.auth.domain.usecase.SignUpWithEmail
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * iOS `LoginViewModel`, email and password only. A success needs no handling here: the gateway moves the app to its
 * next phase, which replaces this screen.
 */
@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val signIn: SignInWithEmail,
    private val signUp: SignUpWithEmail,
    private val requestReset: RequestPasswordReset,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = effectChannel.receiveAsFlow()

    /** The reset request in flight; dismissing the sheet cancels it, so it never acts on a sheet opened later. */
    private var reset: Job? = null

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.ModeChanged -> mutableState.update { it.copy(mode = event.mode, banner = null) }
            is LoginEvent.NameChanged -> mutableState.update { it.copy(name = event.value) }
            is LoginEvent.EmailChanged -> mutableState.update { it.copy(email = event.value) }
            is LoginEvent.PasswordChanged -> mutableState.update { it.copy(password = event.value) }
            is LoginEvent.ConfirmationChanged -> mutableState.update { it.copy(confirmation = event.value) }
            LoginEvent.Submit -> submit()
            LoginEvent.ForgotPasswordOpened ->
                mutableState.update { it.copy(forgotPassword = ForgotPasswordState(email = it.email.trim())) }
            is LoginEvent.ResetEmailChanged ->
                mutableState.update {
                    it.copy(forgotPassword = it.forgotPassword?.copy(email = event.value, error = null))
                }
            LoginEvent.ResetRequested -> requestPasswordReset()
            LoginEvent.ForgotPasswordDismissed -> {
                reset?.cancel()
                mutableState.update { it.copy(forgotPassword = null) }
            }
        }
    }

    private fun submit() {
        val form = mutableState.value
        if (!form.canSubmit) return
        mutableState.update { it.copy(running = true, banner = null) }
        viewModelScope.launch {
            val email = form.email.trim()
            val outcome =
                when (form.mode) {
                    LoginMode.SignIn -> signIn(email, form.password)
                    LoginMode.SignUp ->
                        signUp(SignUpForm(form.name.trim(), email, form.password, form.confirmation, clock.zone))
                }
            when (outcome) {
                // Nothing typed outlives the sign-in: as iOS, where the screen's model goes with the screen.
                is Outcome.Ok -> {
                    effectChannel.trySend(LoginEffect.CommitAutofill)
                    mutableState.value = LoginUiState()
                }
                is Outcome.Err -> {
                    mutableState.update { it.copy(running = false) }
                    fail(outcome.error)
                }
            }
        }
    }

    private fun requestPasswordReset() {
        val sheet = mutableState.value.forgotPassword ?: return
        if (!sheet.canSend) return
        mutableState.update { it.copy(forgotPassword = sheet.copy(sending = true, error = null)) }
        reset = viewModelScope.launch {
            when (val outcome = requestReset(sheet.email.trim())) {
                is Outcome.Ok -> mutableState.update { it.copy(forgotPassword = null, banner = LoginBanner.ResetSent) }
                is Outcome.Err -> {
                    mutableState.update {
                        it.copy(forgotPassword = it.forgotPassword?.copy(sending = false, error = outcome.error))
                    }
                    effectChannel.trySend(LoginEffect.Play(Haptic.Error))
                }
            }
        }
    }

    private fun fail(error: AppError) {
        mutableState.update { it.copy(banner = LoginBanner.Failed(error)) }
        effectChannel.trySend(LoginEffect.Play(Haptic.Error))
    }
}
