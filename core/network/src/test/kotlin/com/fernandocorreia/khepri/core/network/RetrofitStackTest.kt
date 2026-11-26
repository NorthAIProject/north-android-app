package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.api.endpoint.AuthApi
import com.fernandocorreia.khepri.core.api.endpoint.MeApi
import com.fernandocorreia.khepri.core.api.endpoint.TrainingApi
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.network.di.ApiServices
import com.fernandocorreia.khepri.core.network.di.NetworkModule
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/** The production client stack, end to end through Retrofit's suspend (async) path. */
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class RetrofitStackTest {
    private val server = MockWebServer()
    private val tokens = FakeTokens()
    private lateinit var services: ApiServices

    @BeforeEach
    fun start() {
        server.start()
        val base = server.url("/").toString()
        val client =
            NetworkModule.defaultClient(
                NetworkModule.connectionPool(),
                LanguageInterceptor { listOf("en") },
                BearerInterceptor(tokens, base),
                LongCallInterceptor(),
                RetryInterceptor {},
            )
        services = ApiServices(NetworkModule.retrofit(base, client))
    }

    @AfterEach fun stop() = server.close()

    @Test
    fun `an unreadable token is Locked and nothing is sent`() = runBlocking {
        tokens.unreadable = true
        assertEquals(Outcome.Err(AppError.Locked), apiCall { services.create<MeApi>().getMe() })
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a 401 is Unauthorized and signs out`() = runBlocking {
        server.enqueue(status(401, """{"error":{"message":"expired"}}"""))
        assertEquals(Outcome.Err(AppError.Unauthorized("expired")), apiCall { services.create<MeApi>().getMe() })
        assertEquals(1, tokens.unauthorized)
    }

    @Test
    fun `query enums go out by wire value under the api prefix`() = runBlocking {
        server.enqueue(status(500))
        apiCall { services.create<TrainingApi>().getTrainingWeek(TrainingApi.WeekGetTrainingWeek.NEXT) }
        assertEquals("/api/v1/training/week?week=next", server.next()?.target)
    }

    @Test
    fun `an undecodable body is InvalidResponse`() = runBlocking {
        server.enqueue(ok("""{"user":"not an object"}"""))
        assertEquals(Outcome.Err(AppError.InvalidResponse), apiCall { services.create<MeApi>().getMe() })
    }

    @Test
    fun `a connection dropped mid-response is an interruption`() = runBlocking {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build())
        assertEquals(
            Outcome.Err(AppError.Network(interrupted = true)),
            apiCallUnit { services.create<AuthApi>().logOut() },
        )
    }

    @Test
    fun `one ApiServices builds each interface once`() {
        assertSame(services.create<MeApi>(), services.create<MeApi>())
    }
}
