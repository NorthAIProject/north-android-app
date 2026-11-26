package com.fernandocorreia.khepri.feature.auth.domain.model

import java.time.ZoneId

/** What sign-up sends. [timezone] is the phone's zone, so the server schedules check-ins in the user's local time. */
data class SignUpForm(
    val name: String,
    val email: String,
    val password: String,
    val confirmation: String,
    val timezone: ZoneId,
) {
    override fun toString(): String = "SignUpForm(name=$name, email=$email, timezone=$timezone)"
}
