package com.fernandocorreia.khepri.feature.today.data.local

import java.time.Clock
import java.time.LocalDate

/** Snapshot keys. Only today's and yesterday's `day:` rows are kept. */
internal object CacheKeys {
    const val TODAY = "today"
    const val DAY_PREFIX = "day:"

    fun day(date: LocalDate): String = DAY_PREFIX + date

    /** The days kept on disk: today and yesterday, by [clock]. */
    fun keptDays(clock: Clock): List<LocalDate> = LocalDate.now(clock).let { listOf(it, it.minusDays(1)) }
}
