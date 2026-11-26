package com.fernandocorreia.khepri.feature.auth.data

import com.fernandocorreia.khepri.core.api.endpoint.AiConsentApi
import com.fernandocorreia.khepri.core.api.endpoint.TodayApi
import com.fernandocorreia.khepri.core.common.SharedFlowConsentSignal
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.network.ConsentRefusalInterceptor
import com.fernandocorreia.khepri.core.network.khepriRetrofit
import com.fernandocorreia.khepri.feature.auth.data.repository.AiConsentRepositoryImpl
import com.fernandocorreia.khepri.feature.auth.domain.model.AiConsent
import com.fernandocorreia.khepri.feature.auth.domain.model.ConsentChoice
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class AiConsentRepositoryImplTest {
    private val server = MockWebServer()
    private val signal = SharedFlowConsentSignal()
    private lateinit var retrofit: Retrofit

    @BeforeEach
    fun start() {
        server.start()
        val client =
            OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .addInterceptor(ConsentRefusalInterceptor(signal))
                .build()
        retrofit = khepriRetrofit(server.url("/").toString(), client)
    }

    @AfterEach fun stop() = server.close()

    private fun TestScope.repository() =
        AiConsentRepositoryImpl(retrofit.create(AiConsentApi::class.java), signal, backgroundScope)

    private fun answer(code: Int, body: String, delayMs: Long = 0) =
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .headersDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
        )

    private val refusal = """{"error":{"message":"Allow AI first","code":"ai_consent_required"}}"""

    /** Waits, in real time, until the server has seen [count] requests (the re-read runs on the background scope). */
    private suspend fun awaitRequests(count: Int) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (server.requestCount < count) delay(10) } }

    private fun consent(status: String) =
        answer(200, """{"status":"$status","version":1,"recipients":["Anthropic","Google"]}""")

    private suspend fun AiConsentRepositoryImpl.awaitChoice(choice: ConsentChoice): AiConsent =
        withContext(Dispatchers.Default) { withTimeout(5_000) { consent.first { it.choice == choice } } }

    @Test
    fun `an unanswered account is asked`() = runTest {
        consent("unset")
        val repository = repository()
        assertEquals(Outcome.Ok(Unit), repository.load())
        assertEquals(AiConsent(ConsentChoice.Unanswered, listOf("Anthropic", "Google")), repository.consent.value)
        assertTrue(repository.consent.value.needsAnswer)
    }

    @Test
    fun `deciding stops the question and sends only the decision`() = runTest {
        consent("unset")
        consent("granted")
        val repository = repository()
        repository.load()
        assertEquals(Outcome.Ok(Unit), repository.decide(allow = true))
        assertEquals(ConsentChoice.Allowed, repository.consent.value.choice)
        server.takeRequest(1, TimeUnit.SECONDS)
        val put = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals("PUT", put.method)
        assertEquals("""{"decision":"granted"}""", put.body?.utf8())
    }

    @Test
    fun `a status this build does not know does not ask`() = runTest {
        consent("paused")
        val repository = repository()
        repository.load()
        assertEquals(ConsentChoice.Unknown, repository.consent.value.choice)
    }

    @Test
    fun `a failed save keeps the question open`() = runTest {
        consent("unset")
        answer(500, """{"error":{"message":"Try again"}}""")
        val repository = repository()
        repository.load()
        val result = repository.decide(allow = false)
        assertTrue(result is Outcome.Err && result.error is AppError.Server)
        assertEquals(ConsentChoice.Unanswered, repository.consent.value.choice)
    }

    @Test
    fun `load asks the server only while the answer is unknown`() = runTest {
        consent("declined")
        val repository = repository()
        repository.load()
        repository.load()
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a consent refusal from any call reloads, and asks only when the account is unanswered`() = runTest {
        consent("declined")
        val repository = repository()
        repository.load()
        val today = retrofit.create(TodayApi::class.java)

        answer(403, refusal)
        consent("declined")
        today.getToday()
        awaitRequests(3)
        repository.load()
        assertEquals(ConsentChoice.Declined, repository.consent.value.choice)

        answer(403, refusal)
        consent("unset")
        today.getToday()
        assertTrue(repository.awaitChoice(ConsentChoice.Unanswered).needsAnswer)
    }

    @Test
    fun `a declined account stays declined while a refusal re-reads it`() = runTest {
        consent("declined")
        val repository = repository()
        repository.load()
        val seen = mutableListOf<ConsentChoice>()
        backgroundScope.launch(Dispatchers.Default) { repository.consent.collect { seen += it.choice } }
        answer(403, refusal)
        answer(200, """{"status":"declined","version":1,"recipients":[]}""", delayMs = 200)
        retrofit.create(TodayApi::class.java).getToday()
        awaitRequests(3)
        repository.load()
        assertEquals(listOf(ConsentChoice.Declined), seen.distinct())
    }

    @Test
    fun `a failed re-read keeps the open question`() = runTest {
        consent("unset")
        val repository = repository()
        repository.load()
        answer(403, refusal)
        answer(500, """{"error":{"message":"down"}}""")
        retrofit.create(TodayApi::class.java).getToday()
        awaitRequests(3)
        repository.load()
        assertEquals(ConsentChoice.Unanswered, repository.consent.value.choice)
    }

    @Test
    fun `a save made while a re-read is in flight is not overwritten by it`() = runTest {
        consent("unset")
        val repository = repository()
        repository.load()
        answer(403, refusal)
        answer(200, """{"status":"unset","version":1,"recipients":[]}""", delayMs = 300)
        consent("granted")
        retrofit.create(TodayApi::class.java).getToday()
        awaitRequests(3)
        assertEquals(Outcome.Ok(Unit), repository.decide(allow = true))
        assertEquals(ConsentChoice.Allowed, repository.consent.value.choice)
        val methods = List(4) { requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)).method }
        assertEquals(listOf("GET", "GET", "GET", "PUT"), methods)
    }

    @Test
    fun `sign-out forgets the answer and drops one still in flight`() = runTest {
        consent("granted")
        val repository = repository()
        repository.load()
        repository.afterLogout(SignOutReason.Explicit)
        assertEquals(AiConsent.Unknown, repository.consent.value)

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"status":"granted","version":1,"recipients":[]}""")
                .headersDelay(300, TimeUnit.MILLISECONDS)
                .build()
        )
        val late = async(Dispatchers.Default) { repository.load() }
        withContext(Dispatchers.Default) { delay(100) }
        repository.reset()
        late.await()
        assertEquals(AiConsent.Unknown, repository.consent.value)
    }
}
