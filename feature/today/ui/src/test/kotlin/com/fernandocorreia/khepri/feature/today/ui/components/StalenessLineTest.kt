package com.fernandocorreia.khepri.feature.today.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StalenessLineTest {
    private val lisbon = ZoneId.of("Europe/Lisbon")
    private val wednesday = LocalDate.of(2026, 11, 25)
    private val at = Instant.parse("2026-11-24T23:10:00Z")

    private fun time(today: LocalDate, locale: Locale = Locale.UK, use24Hour: Boolean = true) =
        stalenessTime(at, today, lisbon, locale, use24Hour)

    @Test
    fun `fetched today shows the time only`() {
        assertEquals("23:10", time(today = LocalDate.of(2026, 11, 24)))
    }

    @Test
    fun `fetched yesterday names the weekday, in the device's language`() {
        assertEquals("Tue 23:10", time(today = wednesday))
        assertEquals("terça 23:10", time(today = wednesday, locale = Locale.forLanguageTag("pt-PT")))
    }

    @Test
    fun `a 12 hour device reads 12 hour time`() {
        assertEquals("Tue 11:10 pm", time(today = wednesday, use24Hour = false))
        assertEquals("Tue 11:10 PM", time(today = wednesday, locale = Locale.US, use24Hour = false))
    }

    @Test
    fun `a week or more ago shows the date`() {
        assertEquals("24 Nov 23:10", time(today = LocalDate.of(2026, 12, 1)))
    }

    @Test
    fun `the device's zone decides the day`() {
        assertEquals(
            "Wed 08:10",
            stalenessTime(at, LocalDate.of(2026, 11, 26), ZoneId.of("Asia/Tokyo"), Locale.UK, true),
        )
    }
}
