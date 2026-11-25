package com.fernandocorreia.khepri.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OutcomeTest {
    @Test
    fun `map transforms an Ok value`() {
        assertEquals(Outcome.Ok(4), Outcome.Ok("four").map { it.length })
    }

    @Test
    fun `map keeps an Err and does not call the transform`() {
        val error: Outcome<String> = Outcome.Err(AppError.Locked)
        var called = false
        assertEquals(error, error.map { called = true })
        assertEquals(false, called)
    }

    @Test
    fun `a session never prints its token`() {
        val session = Session("secret-token", "u1", java.time.Instant.EPOCH)
        assertEquals(false, session.toString().contains("secret-token"))
    }
}
