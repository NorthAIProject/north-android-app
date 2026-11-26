package com.fernandocorreia.khepri.core.database

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstantColumnsTest {
    @Test
    fun `an instant is kept to the millisecond the server sends, and finer digits are dropped`() {
        val fetched = Instant.parse("2026-11-26T09:30:00.250Z")
        assertEquals(fetched, InstantColumns.fromMillis(InstantColumns.toMillis(fetched)))
        assertEquals(
            fetched,
            InstantColumns.fromMillis(InstantColumns.toMillis(Instant.parse("2026-11-26T09:30:00.250999Z"))),
        )
    }

    @Test
    fun `instants before 1970 survive`() {
        val early = Instant.parse("1969-12-31T23:59:59.001Z")
        assertEquals(early, InstantColumns.fromMillis(InstantColumns.toMillis(early)))
    }
}
