package com.fernandocorreia.khepri.core.api

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DateSerializersTest {
    private fun decode(json: String) = ApiJson.strict.decodeFromString(FlexibleInstantSerializer, json)

    private fun encode(value: Instant) = ApiJson.strict.encodeToString(FlexibleInstantSerializer, value)

    @Test
    fun `reads whole seconds`() =
        assertEquals(Instant.parse("2026-09-24T07:15:00Z"), decode("\"2026-09-24T07:15:00Z\""))

    @Test
    fun `reads nanoseconds`() =
        assertEquals(Instant.parse("2026-09-24T07:15:00.123456789Z"), decode("\"2026-09-24T07:15:00.123456789Z\""))

    @Test
    fun `reads an offset`() =
        assertEquals(Instant.parse("2026-09-24T05:15:00Z"), decode("\"2026-09-24T07:15:00+02:00\""))

    @Test
    fun `writes milliseconds`() =
        assertEquals("\"2026-09-24T07:15:00.000Z\"", encode(Instant.parse("2026-09-24T07:15:00Z")))

    @Test
    fun `truncates to milliseconds`() =
        assertEquals("\"2026-09-24T07:15:00.123Z\"", encode(Instant.parse("2026-09-24T07:15:00.123456789Z")))

    @Test
    fun `rejects a non-date with a serialization error`() {
        assertThrows<SerializationException> { decode("\"yesterday\"") }
    }

    @Test
    fun `local dates round trip and reject garbage with a serialization error`() {
        assertEquals(LocalDate.of(2026, 9, 24), ApiJson.strict.decodeFromString(LocalDateSerializer, "\"2026-09-24\""))
        assertEquals("\"2026-09-24\"", ApiJson.strict.encodeToString(LocalDateSerializer, LocalDate.of(2026, 9, 24)))
        assertThrows<SerializationException> { ApiJson.strict.decodeFromString(LocalDateSerializer, "\"24/09/2026\"") }
    }
}
