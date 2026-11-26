package com.fernandocorreia.khepri.feature.auth.data.remote

import com.fernandocorreia.khepri.core.api.model.AuthResponseDto
import com.fernandocorreia.khepri.core.api.model.SignupRequestDto
import com.fernandocorreia.khepri.core.model.Session
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm

internal fun AuthResponseDto.toSession(): Session = Session(token = token, userId = user.id, expiresAt = expiresAt)

/** As iOS: a blank name is left out, so the server picks one from the email. */
internal fun SignUpForm.toDto(): SignupRequestDto =
    SignupRequestDto(
        email = email,
        password = password,
        passwordConfirmation = confirmation,
        displayName = name.trim().ifEmpty { null },
        timezone = timezone.id,
    )
