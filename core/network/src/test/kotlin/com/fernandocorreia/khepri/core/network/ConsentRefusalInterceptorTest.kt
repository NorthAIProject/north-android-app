package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.common.ConsentSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConsentRefusalInterceptorTest {
    private val server = MockWebServer()
    private val signal = CountingSignal()
    private lateinit var client: OkHttpClient

    private class CountingSignal : ConsentSignal {
        var posts = 0
        override val required: Flow<Unit> = emptyFlow()

        override fun post() {
            posts++
        }
    }

    @BeforeEach
    fun start() {
        server.start()
        client = client(ConsentRefusalInterceptor(signal))
    }

    @AfterEach fun stop() = server.close()

    private fun answer(code: Int, body: String) = server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private val refusal = """{"error":{"message":"Allow AI data sharing first","code":"ai_consent_required"}}"""

    @Test
    fun `a consent refusal posts once and the caller still reads the body`() {
        answer(403, refusal)
        val body = client.call(server, "POST", "/api/v1/check-ins/today/reply").use { it.body.string() }
        assertEquals(1, signal.posts)
        assertEquals(refusal, body)
    }

    @Test
    fun `other refusals, an empty 403 and a success post nothing`() {
        answer(403, """{"error":{"message":"no","code":"terms_required"}}""")
        answer(403, "")
        answer(200, refusal)
        answer(500, refusal)
        repeat(4) { client.call(server, "GET", "/api/v1/today").close() }
        assertEquals(0, signal.posts)
    }
}
