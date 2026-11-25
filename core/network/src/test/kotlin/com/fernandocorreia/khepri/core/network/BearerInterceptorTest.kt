package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BearerInterceptorTest {
    private val server = MockWebServer()
    private val tokens = FakeTokens()
    private val client = client(BearerInterceptor(tokens))

    @BeforeEach fun start() = server.start()

    @AfterEach fun stop() = server.close()

    @Test
    fun `adds the token to a protected path`() {
        server.enqueue(ok())
        client.call(server, "GET", "/api/v1/me").close()
        assertEquals("Bearer t0ken", server.next()?.headers?.get("Authorization"))
    }

    @Test
    fun `sends no token to a public path`() {
        server.enqueue(ok())
        client.call(server, "POST", "/api/v1/auth/login").close()
        assertNull(server.next()?.headers?.get("Authorization"))
    }

    @Test
    fun `logout carries the token`() {
        server.enqueue(ok())
        client.call(server, "POST", "/api/v1/auth/logout").close()
        assertEquals("Bearer t0ken", server.next()?.headers?.get("Authorization"))
    }

    @Test
    fun `401 with a token sent calls onUnauthorized`() {
        server.enqueue(status(401))
        client.call(server, "GET", "/api/v1/me").close()
        assertEquals(1, tokens.unauthorized)
    }

    @Test
    fun `401 on a public path does not call onUnauthorized`() {
        server.enqueue(status(401))
        client.call(server, "POST", "/api/v1/auth/login").close()
        assertEquals(0, tokens.unauthorized)
    }

    @Test
    fun `401 when signed out does not call onUnauthorized`() {
        tokens.value = null
        server.enqueue(status(401))
        client.call(server, "GET", "/api/v1/me").close()
        assertEquals(0, tokens.unauthorized)
    }

    @Test
    fun `unreadable token stops the request`() {
        tokens.unreadable = true
        server.enqueue(ok())
        assertThrows<TokenUnreadableException> { client.call(server, "GET", "/api/v1/me") }
        assertEquals(0, server.requestCount)
        assertEquals(0, tokens.unauthorized)
    }
}
