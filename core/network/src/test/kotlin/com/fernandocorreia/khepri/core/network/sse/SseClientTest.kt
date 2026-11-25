package com.fernandocorreia.khepri.core.network.sse

import com.fernandocorreia.khepri.core.model.AppError
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SseClientTest {
    private val server = MockWebServer()
    private val okHttp = OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build()
    private val sse = SseClient(okHttp)

    @BeforeEach fun start() = server.start()

    @AfterEach fun stop() = server.close()

    private fun stream(body: String) =
        MockResponse.Builder().code(200).setHeader("Content-Type", "text/event-stream").body(body)

    private fun request() = Request.Builder().url(server.url("/api/v1/coach/conversations/c1/reply")).build()

    @Test
    fun `yields named events, skips comments and completes`() = runBlocking {
        server.enqueue(stream("event: token\ndata: {\"text\":\"Hi\"}\n\n: ping\n\nevent: done\ndata: {}\n\n").build())
        val events = sse.events(request()).toList()
        assertEquals(listOf(SseEvent("token", """{"text":"Hi"}"""), SseEvent("done", "{}")), events)
        assertEquals("text/event-stream", server.takeRequest().headers["Accept"])
    }

    @Test
    fun `a refused stream fails with the mapped error`() {
        server.enqueue(
            MockResponse.Builder().code(403).body("""{"error":{"message":"m","code":"ai_consent_required"}}""").build()
        )
        val failure = assertThrows<SseException> { runBlocking { sse.events(request()).toList() } }
        assertEquals(AppError.Refused("ai_consent_required", "m"), failure.error)
    }

    @Test
    fun `cancelling the collector closes the connection`() = runBlocking {
        server.enqueue(
            stream("event: token\ndata: {}\n\n" + "event: token\ndata: {}\n\n".repeat(100))
                .throttleBody(BYTES_BEFORE_STALL, 30, TimeUnit.SECONDS)
                .build()
        )
        assertEquals(SseEvent("token", "{}"), sse.events(request()).first())
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (okHttp.dispatcher.runningCallsCount() > 0 && System.nanoTime() < deadline) Thread.sleep(POLL_MS)
        assertEquals(0, okHttp.dispatcher.runningCallsCount())
    }

    private companion object {
        const val BYTES_BEFORE_STALL = 23L
        const val POLL_MS = 20L
    }
}
