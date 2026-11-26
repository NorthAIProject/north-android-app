package com.fernandocorreia.khepri.feature.today.data.repository

import com.fernandocorreia.khepri.core.api.endpoint.DayApi
import com.fernandocorreia.khepri.core.api.endpoint.FastingApi
import com.fernandocorreia.khepri.core.api.endpoint.TodayApi
import com.fernandocorreia.khepri.core.api.model.DayResponseDto
import com.fernandocorreia.khepri.core.api.model.FastStartInputDto
import com.fernandocorreia.khepri.core.api.model.TodayResponseDto
import com.fernandocorreia.khepri.core.common.AccountScope
import com.fernandocorreia.khepri.core.common.Cached
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.common.SignedInState
import com.fernandocorreia.khepri.core.database.snapshot.Prune
import com.fernandocorreia.khepri.core.database.snapshot.SnapshotCache
import com.fernandocorreia.khepri.core.database.snapshot.StoredValue
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.network.apiCall
import com.fernandocorreia.khepri.feature.today.data.local.CacheKeys
import com.fernandocorreia.khepri.feature.today.data.local.TodayMemory
import com.fernandocorreia.khepri.feature.today.data.remote.forCache
import com.fernandocorreia.khepri.feature.today.data.remote.toDay
import com.fernandocorreia.khepri.feature.today.data.remote.toToday
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer

/**
 * iOS `TodayStore` and `DayStore`, cache first. A refresh runs on the account scope, so leaving the screen does not
 * cancel it, and writes only if the account it was asked for is still signed in: a reply that lands after a sign-out or
 * an account switch writes nothing, on disk or in memory.
 */
@Singleton
internal class TodayRepositoryImpl
@Inject
constructor(
    private val todayApi: TodayApi,
    private val dayApi: DayApi,
    private val fastingApi: FastingApi,
    private val cache: SnapshotCache,
    // A Provider: the session runs this cleaner, so injecting it directly would be a cycle.
    signedInState: Provider<SignedInState>,
    @AccountScope private val accountScope: CoroutineScope,
    private val clock: Clock,
) : TodayRepository, SignOutCleaner {
    private val signedIn by lazy { signedInState.get() }
    private val memory = TodayMemory(::current, clock)

    override fun observeToday(): Flow<Cached<Today>?> =
        observeStored(CacheKeys.TODAY, TodayResponseDto.serializer()) { it.toToday() }

    override fun observeDay(date: LocalDate): Flow<Cached<Day>?> =
        if (date in CacheKeys.keptDays(clock)) {
            observeStored(CacheKeys.day(date), DayResponseDto.serializer()) { it.toDay() }
        } else {
            memory.observeDay(date)
        }

    override suspend fun refreshToday(): Outcome<Unit> =
        refresh(
            CacheKeys.TODAY,
            fetch = { apiCall(map = { it.also(TodayResponseDto::toToday) }) { todayApi.getToday() } },
        ) { account, response ->
            cache.putIf({ current(account) }, account, CacheKeys.TODAY, response, TodayResponseDto.serializer())
        }

    override suspend fun refreshDay(date: LocalDate): Outcome<Unit> {
        val key = CacheKeys.day(date)
        val fetch = suspend { apiCall(map = { it to it.toDay() }) { dayApi.getDay(date) } }
        return refresh(key, fetch) { account, (response, day) ->
            val kept = CacheKeys.keptDays(clock)
            if (date in kept) {
                val prune = Prune(CacheKeys.DAY_PREFIX, kept.map(CacheKeys::day).toSet())
                cache.putIf({ current(account) }, account, key, response.forCache(), DayResponseDto.serializer(), prune)
            } else {
                memory.rememberDay(account, date, day)
            }
        }
    }

    override suspend fun startFast(hours: Int): Outcome<Unit> =
        apiCall(map = {}) { fastingApi.startFast(FastStartInputDto(targetHours = hours)) }

    override suspend fun stopFast(): Outcome<Unit> = apiCall(map = {}) { fastingApi.stopFast() }

    override suspend fun afterLogout(reason: SignOutReason) = memory.clear()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <D, T> observeStored(key: String, serializer: KSerializer<D>, map: (D) -> T): Flow<Cached<T>?> =
        signedIn.account.flatMapLatest { account ->
            if (account == null) {
                flowOf(null)
            } else {
                memory.withError(key, cache.observe(account, key, serializer).map { it?.mapOrNull(map) })
            }
        }

    /**
     * Fetches on the account scope and stores the result through [store], which returns whether it wrote. Cancelled by
     * a sign-out, it returns an error instead of throwing into a caller that is still running.
     */
    private suspend fun <D> refresh(
        key: String,
        fetch: suspend () -> Outcome<D>,
        store: suspend (account: String, D) -> Boolean,
    ): Outcome<Unit> {
        val account = signedIn.account.value ?: return Outcome.Err(SIGNED_OUT)
        val work = accountScope.async {
            when (val result = fetch()) {
                is Outcome.Ok ->
                    if (store(account, result.value) && memory.recordError(account, key, null)) {
                        Outcome.Ok(Unit)
                    } else {
                        Outcome.Err(SIGNED_OUT)
                    }
                is Outcome.Err -> result.also { memory.recordError(account, key, it.error) }
            }
        }
        return try {
            work.await()
        } catch (expected: CancellationException) {
            currentCoroutineContext().ensureActive()
            Outcome.Err(SIGNED_OUT)
        }
    }

    private fun current(account: String) = signedIn.account.value == account

    private companion object {
        val SIGNED_OUT = AppError.Unauthorized(null)
    }
}

/** A stored value this build can no longer map is treated as nothing stored. */
private fun <D, T> StoredValue<D>.mapOrNull(map: (D) -> T): StoredValue<T>? =
    try {
        StoredValue(map(value), fetchedAt)
    } catch (@Suppress("TooGenericExceptionCaught") expected: RuntimeException) {
        null
    }
