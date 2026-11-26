package com.fernandocorreia.khepri.feature.today.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.fernandocorreia.khepri.core.api.ApiJson
import com.fernandocorreia.khepri.core.api.endpoint.DayApi
import com.fernandocorreia.khepri.core.api.endpoint.FastingApi
import com.fernandocorreia.khepri.core.api.endpoint.TodayApi
import com.fernandocorreia.khepri.core.api.model.DayResponseDto
import com.fernandocorreia.khepri.core.api.model.TodayResponseDto
import com.fernandocorreia.khepri.core.common.DispatcherProvider
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.common.SignedInState
import com.fernandocorreia.khepri.core.database.KhepriDatabase
import com.fernandocorreia.khepri.core.database.snapshot.CachedSnapshot
import com.fernandocorreia.khepri.core.database.snapshot.SnapshotCache
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.network.khepriRetrofit
import com.fernandocorreia.khepri.feature.today.data.repository.TodayRepositoryImpl
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayRepositoryImplTest {
    private val server = MockWebServer()
    private val account = MutableStateFlow<String?>("a")
    private val signedIn =
        object : SignedInState {
            override val account = this@TodayRepositoryImplTest.account
        }
    private val database =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhepriDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val rows = database.snapshots()
    private val today = LocalDate.parse("2026-11-26")
    private val clock = MovableClock(Instant.parse("2026-11-26T09:00:00Z"))
    private val dispatchers =
        object : DispatcherProvider {
            override val main = Dispatchers.Default
            override val io = Dispatchers.Default
            override val default = Dispatchers.Default
        }
    private val cache = SnapshotCache(database, ApiJson.default, clock, dispatchers)
    private val accountScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: TodayRepositoryImpl

    @Before
    fun start() {
        server.start()
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        val retrofit = khepriRetrofit(server.url("/").toString(), client)
        repository =
            TodayRepositoryImpl(
                retrofit.create(TodayApi::class.java),
                retrofit.create(DayApi::class.java),
                retrofit.create(FastingApi::class.java),
                cache,
                Provider { signedIn },
                accountScope,
                clock,
            )
    }

    @After
    fun stop() {
        accountScope.cancel()
        server.close()
        database.close()
    }

    private fun answer(body: String, code: Int = 200, delayMs: Long = 0) =
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .headersDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
        )

    private fun failure() = answer("""{"error":{"message":"down"}}""", code = 500)

    private fun todayWithStreak(streak: Int) = Goldens.today { root ->
        root.edit("snapshot") { it.with("streak", JsonPrimitive(streak)) }
    }

    private fun todayJson(streak: Int) =
        ApiJson.default.encodeToString(TodayResponseDto.serializer(), todayWithStreak(streak))

    private suspend fun seedToday(streak: Int, account: String = "a") =
        cache.putIf({ true }, account, "today", todayWithStreak(streak), TodayResponseDto.serializer())

    private suspend fun stored(account: String, key: String) = rows.observe(account, key).first()

    private suspend fun awaitRequests(count: Int) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (server.requestCount < count) delay(10) } }

    @Test
    fun `observing Today emits the cached value first, then the refreshed one, which replaces the row`() = runTest {
        seedToday(6)
        answer(todayJson(7))
        repository.observeToday().test {
            assertEquals(6, awaitItem()?.value?.streak)
            assertEquals(Outcome.Ok(Unit), repository.refreshToday())
            assertEquals(7, awaitItem()?.value?.streak)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(requireNotNull(stored("a", "today")).json.contains("\"streak\":7"))
    }

    @Test
    fun `a failed refresh keeps the value and sets refreshError, and the next success clears it`() = runTest {
        seedToday(6)
        failure()
        answer(todayJson(6))
        repository.observeToday().test {
            assertNull(awaitItem()?.refreshError)
            assertTrue(repository.refreshToday() is Outcome.Err)
            val failed = awaitItem()
            assertEquals(6, failed?.value?.streak)
            assertTrue(failed?.refreshError is AppError.Server)
            assertEquals(Outcome.Ok(Unit), repository.refreshToday())
            assertNull(awaitItem()?.refreshError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing cached and a failed refresh emits nothing to show and returns the error`() = runTest {
        failure()
        val result = repository.refreshToday()
        assertTrue(result is Outcome.Err && result.error is AppError.Server)
        assertNull(repository.observeToday().first())
    }

    @Test
    fun `the stored day has no blood pressure, sleep stages or blocks`() = runTest {
        answer(Goldens.text("day"))
        assertEquals(Outcome.Ok(Unit), repository.refreshDay(today))
        val json = ApiJson.default.parseToJsonElement(requireNotNull(stored("a", "day:2026-11-26")).json).jsonObject
        assertFalse("bloodPressure" in json.getValue("body").jsonObject)
        val sleep = json.getValue("sleep").jsonObject
        assertEquals(0, sleep.getValue("stages").jsonObject.size)
        assertEquals(0, sleep.getValue("blocks").jsonArray.size)
        assertEquals(433, repository.observeDay(today).first()?.value?.sleep?.totalMinutes)
    }

    @Test
    fun `refreshing today keeps only today's and yesterday's day rows`() = runTest {
        listOf("day:2026-11-23", "day:2026-11-24", "day:2026-11-25", "today").forEach {
            rows.put(CachedSnapshot("a", it, "{}", Instant.EPOCH))
        }
        answer(Goldens.text("day"))
        repository.refreshDay(today)
        val left = listOf("day:2026-11-23", "day:2026-11-24", "day:2026-11-25", "day:2026-11-26", "today")
        assertEquals(listOf("day:2026-11-25", "day:2026-11-26", "today"), left.filter { stored("a", it) != null })
    }

    @Test
    fun `a date three days back is served from memory and never written`() = runTest {
        val older = today.minusDays(3)
        answer(Goldens.text("day"))
        assertEquals(Outcome.Ok(Unit), repository.refreshDay(older))
        assertEquals(91, repository.observeDay(older).first()?.value?.streak)
        assertNull(stored("a", "day:2026-11-23"))
    }

    @Test
    fun `a refresh answered after its account signed out writes nothing, and the next account sees nothing`() =
        runTest {
            answer(todayJson(6), delayMs = 400)
            val late = async(Dispatchers.Default) { repository.refreshToday() }
            awaitRequests(1)
            account.value = null
            repository.afterLogout(SignOutReason.Explicit)
            database.clearAllTables()
            account.value = "b"
            assertTrue(late.await() is Outcome.Err)
            assertNull(stored("a", "today"))
            assertNull(stored("b", "today"))
            assertNull(repository.observeToday().first())
        }

    @Test
    fun `a refresh cancelled by sign-out returns an error to a caller that is still running`() = runTest {
        answer(todayJson(6), delayMs = 2_000)
        val late = async(Dispatchers.Default) { repository.refreshToday() }
        awaitRequests(1)
        accountScope.coroutineContext[kotlinx.coroutines.Job]?.cancelChildren()
        assertTrue(late.await() is Outcome.Err)
    }

    @Test
    fun `sign-out clears the errors and the days held in memory`() = runTest {
        val older = today.minusDays(3)
        answer(Goldens.text("day"))
        failure()
        seedToday(6)
        failure()
        repository.refreshDay(older)
        repository.refreshDay(older)
        repository.refreshToday()
        assertNotNull(repository.observeDay(older).first()?.refreshError)
        assertNotNull(repository.observeToday().first()?.refreshError)

        repository.afterLogout(SignOutReason.Explicit)

        assertNull(repository.observeDay(older).first())
        assertNull(repository.observeToday().first()?.refreshError)
    }

    @Test
    fun `today is asked for without a date and other days with theirs`() = runTest {
        answer(Goldens.text("day"))
        answer(Goldens.text("day"))
        repository.refreshDay(today)
        repository.refreshDay(today.minusDays(3))
        assertFalse("date=" in requireNotNull(server.takeRequest().url).toString())
        assertTrue("date=2026-11-23" in requireNotNull(server.takeRequest().url).toString())
    }

    @Test
    fun `a stored day stops being today at midnight`() = runTest {
        answer(Goldens.text("day"))
        repository.refreshDay(today)
        assertEquals(true, repository.observeDay(today).first()?.value?.isToday)
        clock.now = clock.now.plusSeconds(DAY_SECONDS)
        assertEquals(false, repository.observeDay(today).first()?.value?.isToday)
    }

    @Test
    fun `a day observed across midnight shows the refresh that now lands in memory`() = runTest {
        val yesterday = today.minusDays(1)
        answer(Goldens.text("day"))
        answer(Goldens.day { it.with("streak", JsonPrimitive(92)) }.let { encodeDay(it) })
        repository.observeDay(yesterday).test {
            assertNull(awaitItem())
            repository.refreshDay(yesterday)
            assertEquals(91, awaitItem()?.value?.streak)
            clock.now = clock.now.plusSeconds(DAY_SECONDS)
            repository.refreshDay(yesterday)
            assertEquals(92, awaitItem()?.value?.streak)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `replies held in memory that land after a sign-out are dropped, and the next account sees none`() = runTest {
        val older = today.minusDays(3)
        answer(Goldens.text("day"), delayMs = 400)
        answer("""{"error":{"message":"down"}}""", code = 500, delayMs = 400)
        val day = async(Dispatchers.Default) { repository.refreshDay(older) }
        val failed = async(Dispatchers.Default) { repository.refreshToday() }
        awaitRequests(2)
        account.value = null
        repository.afterLogout(SignOutReason.Explicit)
        account.value = "b"
        // Which reply each call gets depends on arrival order; neither may leave anything behind.
        assertTrue(day.await() is Outcome.Err)
        assertTrue(failed.await() is Outcome.Err)
        assertNull(repository.observeDay(older).first())
        seedToday(6, account = "b")
        assertNull(repository.observeToday().first()?.refreshError)
    }

    private fun encodeDay(day: DayResponseDto) = ApiJson.default.encodeToString(DayResponseDto.serializer(), day)

    private companion object {
        const val DAY_SECONDS = 86_400L
    }
}

/** A clock in UTC whose [now] a test moves. */
private class MovableClock(@Volatile var now: Instant) : Clock() {
    override fun getZone(): ZoneOffset = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = now
}
