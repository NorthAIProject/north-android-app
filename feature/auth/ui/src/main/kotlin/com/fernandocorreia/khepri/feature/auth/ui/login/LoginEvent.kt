package com.fernandocorreia.khepri.feature.auth.ui.login

import com.fernandocorreia.khepri.core.ui.Haptic

sealed interface LoginEvent {
    data class ModeChanged(val mode: LoginMode) : LoginEvent

    data class NameChanged(val value: String) : LoginEvent

    data class EmailChanged(val value: String) : LoginEvent

    data class PasswordChanged(val value: String) : LoginEvent

    data class ConfirmationChanged(val value: String) : LoginEvent

    data object Submit : LoginEvent

    data object ForgotPasswordOpened : LoginEvent

    data class ResetEmailChanged(val value: String) : LoginEvent

    data object ResetRequested : LoginEvent

    data object ForgotPasswordDismissed : LoginEvent
}

sealed interface LoginEffect {
    data class Play(val haptic: Haptic) : LoginEffect
}
