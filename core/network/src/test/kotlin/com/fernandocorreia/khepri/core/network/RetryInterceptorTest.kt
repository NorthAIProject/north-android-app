package com.fernandocorreia.khepri.core.network

import java.io.IOException
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RetryInterceptorTest {
    private val server = MockWebServer()
    private var pauses = 0
    private val client = client(RetryInterceptor { pauses++ })

    @BeforeEach fun start() = server.start()

    @AfterEach fun stop() = server.close()

    @Test
    fun `GET is retried once after a dropped connection`() {
        server.enqueue(dropped())
        server.enqueue(ok())
        assertEquals(200, client.call(server, "GET", "/api/v1/today").use { it.code })
        assertEquals(2, server.requestCount)
        assertEquals(1, pauses)
    }

    @Test
    fun `GET is not retried twice`() {
        server.enqueue(dropped())
        server.enqueue(dropped())
        server.enqueue(ok())
        assertThrows<IOException> { client.call(server, "GET", "/api/v1/today") }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `POST is never retried`() {
        server.enqueue(dropped())
        server.enqueue(ok())
        assertThrows<IOException> { client.call(server, "POST", "/api/v1/checkins") }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `an event stream is never retried`() {
        server.enqueue(dropped())
        server.enqueue(ok())
        assertThrows<IOException> { client.call(server, "GET", "/api/v1/crews/x/live", accept = "text/event-stream") }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a 500 is not retried`() {
        server.enqueue(status(500))
        server.enqueue(ok())
        assertEquals(500, client.call(server, "GET", "/api/v1/today").use { it.code })
        assertEquals(1, server.requestCount)
    }
}
