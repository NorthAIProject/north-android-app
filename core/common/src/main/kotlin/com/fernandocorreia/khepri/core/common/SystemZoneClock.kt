package com.fernandocorreia.khepri.core.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The system clock in the device's current time zone, read on every call. `Clock.systemDefaultZone()` keeps the zone it
 * was created in, so after the user changes time zone "today" would still be the old zone's.
 */
public object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
