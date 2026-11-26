package com.fernandocorreia.khepri.core.common

import java.time.ZoneId
import java.util.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SystemZoneClockTest {
    private val original = TimeZone.getDefault()

    @AfterEach fun restore() = TimeZone.setDefault(original)

    @Test
    fun `the zone follows a time zone change made after the clock was first used`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon"))
        assertEquals(ZoneId.of("Europe/Lisbon"), SystemZoneClock.zone)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        assertEquals(ZoneId.of("Asia/Tokyo"), SystemZoneClock.zone)
    }
}
