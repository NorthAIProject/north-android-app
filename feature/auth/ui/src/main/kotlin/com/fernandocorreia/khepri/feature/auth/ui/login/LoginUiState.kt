package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.runtime.Immutable
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.feature.auth.domain.model.AuthValidation
import com.fernandocorreia.khepri.feature.auth.domain.model.PasswordStrength

enum class LoginMode {
    SignIn,
    SignUp,
}

/** The one message above the form. */
sealed interface LoginBanner {
    data class Failed(val error: AppError) : LoginBanner

    data object ResetSent : LoginBanner
}

/**
 * The forgot-password sheet, while open. [email] is checked trimmed (iOS checks it as typed, so a trailing space from a
 * keyboard suggestion left Send disabled with no reason). A failed request shows its [error] in the sheet; on iOS it
 * went to the banner under the sheet, where it could not be seen.
 */
@Immutable
data class ForgotPasswordState(val email: String, val sending: Boolean = false, val error: AppError? = null) {
    val canSend: Boolean
        get() = AuthValidation.isValidEmail(email.trim()) && !sending
}

@Immutable
data class LoginUiState(
    val mode: LoginMode = LoginMode.SignIn,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val running: Boolean = false,
    val banner: LoginBanner? = null,
    val forgotPassword: ForgotPasswordState? = null,
) {
    /** iOS `canSubmitSignIn` / `canSubmitSignUp`, and nothing while a request runs. */
    val canSubmit: Boolean
        get() {
            val emailValid = AuthValidation.isValidEmail(email.trim())
            val ready =
                when (mode) {
                    LoginMode.SignIn -> emailValid && password.isNotEmpty()
                    LoginMode.SignUp ->
                        emailValid &&
                            name.isNotBlank() &&
                            AuthValidation.length(password) >= AuthValidation.MIN_PASSWORD_LENGTH &&
                            password == confirmation
                }
            return ready && !running
        }

    /** Shown once the confirmation has been typed into. */
    val passwordsDiffer: Boolean
        get() = confirmation.isNotEmpty() && password != confirmation

    val strength: PasswordStrength
        get() = AuthValidation.strength(password)

    override fun toString(): String = "LoginUiState(mode=$mode, email=$email, running=$running, banner=$banner)"
}
