package com.fernandocorreia.khepri.core.model

import java.time.Instant

public data class Session(val token: String, val userId: String, val expiresAt: Instant) {
    override fun toString(): String = "Session(userId=$userId, expiresAt=$expiresAt)"
}
