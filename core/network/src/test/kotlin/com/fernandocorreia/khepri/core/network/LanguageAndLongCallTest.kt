package com.fernandocorreia.khepri.core.network

import mockwebserver3.MockWebServer
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LanguageAndLongCallTest {
    private val server = MockWebServer()

    @BeforeEach fun start() = server.start()

    @AfterEach fun stop() = server.close()

    @Test
    fun `sends the first three languages with falling weights`() {
        server.enqueue(ok())
        client(LanguageInterceptor { listOf("pt-PT", "en", "es", "fr") }).call(server, "GET", "/api/v1/me").close()
        assertEquals("pt-PT, en;q=0.9, es;q=0.8", server.next()?.headers?.get("Accept-Language"))
    }

    @Test
    fun `keeps a header the request already has`() {
        server.enqueue(ok())
        val request = Request.Builder().url(server.url("/api/v1/me")).header("Accept-Language", "de").build()
        client(LanguageInterceptor { listOf("pt-PT") }).newCall(request).execute().close()
        assertEquals("de", server.next()?.headers?.get("Accept-Language"))
    }

    @Test
    fun `an empty list adds nothing`() {
        server.enqueue(ok())
        client(LanguageInterceptor { emptyList() }).call(server, "GET", "/api/v1/me").close()
        assertNull(server.next()?.headers?.get("Accept-Language"))
    }

    @Test
    fun `a long operation gets a 300 s read timeout and others keep the default`() {
        val seen = mutableListOf<Int>()
        val probe = Interceptor { chain ->
            seen += chain.readTimeoutMillis()
            chain.proceed(chain.request())
        }
        val client =
            client(LongCallInterceptor(), probe).newBuilder().readTimeout(java.time.Duration.ofSeconds(60)).build()
        server.enqueue(ok())
        server.enqueue(ok())
        client.call(server, "POST", "/api/v1/training/plans").close()
        client.call(server, "GET", "/api/v1/today").close()
        assertEquals(listOf(300_000, 60_000), seen)
    }
}
