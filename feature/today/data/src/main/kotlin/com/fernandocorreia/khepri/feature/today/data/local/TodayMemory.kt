package com.fernandocorreia.khepri.feature.today.data.local

import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.core.database.snapshot.StoredValue
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * What Today keeps in memory for the signed-in account: the last refresh failure per cache key, cleared by the next
 * success, and days other than today and yesterday, which are never written to disk. A write lands only while
 * [isCurrent] holds for the account that asked, under the lock [clear] takes, so nothing survives a sign-out.
 */
internal class TodayMemory(private val isCurrent: (account: String) -> Boolean, private val clock: Clock) {
    /** Never held across a call. */
    private val lock = Any()
    private val errors = MutableStateFlow<Map<String, AppError>>(emptyMap())
    private val days = MutableStateFlow<Map<LocalDate, StoredValue<Day>>>(emptyMap())

    /** [stored] with the last refresh failure for [key]; repeats are dropped. */
    fun <T> withError(key: String, stored: Flow<StoredValue<T>?>): Flow<Cached<T>?> =
        combine(stored, errors.map { it[key] }.distinctUntilChanged()) { value, error ->
                value?.let { Cached(it.value, it.fetchedAt, error) }
            }
            .distinctUntilChanged()

    fun observeDay(date: LocalDate): Flow<Cached<Day>?> =
        withError(CacheKeys.day(date), days.map { it[date] }.distinctUntilChanged())

    /** Records [error] for [key], or clears it when null. Returns whether it wrote. */
    fun recordError(account: String, key: String, error: AppError?): Boolean =
        write(account) {
            errors.update { if (error == null) it - key else it + (key to error) }
        }

    /** Keeps [day] as fetched now. Returns whether it wrote. */
    fun rememberDay(account: String, date: LocalDate, day: Day): Boolean =
        write(account) {
            days.update { it + (date to StoredValue(day, clock.instant())) }
        }

    fun clear() {
        synchronized(lock) {
            errors.value = emptyMap()
            days.value = emptyMap()
        }
    }

    private fun write(account: String, change: () -> Unit): Boolean =
        synchronized(lock) { isCurrent(account).also { if (it) change() } }
}
