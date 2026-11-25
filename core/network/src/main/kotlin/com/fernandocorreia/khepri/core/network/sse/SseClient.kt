package com.fernandocorreia.khepri.core.network.sse

import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.network.di.StreamingClient
import com.fernandocorreia.khepri.core.network.errorFor
import com.fernandocorreia.khepri.core.network.isInterruption
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/** One server-sent event. `name` is the `event:` field, null for the default event. */
public data class SseEvent(val name: String?, val data: String)

/** A stream that could not start or broke; [error] is mapped like any other call. */
public class SseException(public val error: AppError, cause: Throwable? = null) : Exception(cause)

/** Server-sent event streams on the streaming client (no read timeout, never retried). */
public class SseClient @Inject constructor(@StreamingClient client: OkHttpClient) {
    private val factory = EventSources.createFactory(client)

    /**
     * Cold. Completes when the server closes the stream. Fails with [SseException]; cancelling closes the connection.
     */
    public fun events(request: Request): Flow<SseEvent> = callbackFlow {
        val listener =
            object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    trySendBlocking(SseEvent(type, data))
                }

                override fun onClosed(eventSource: EventSource) {
                    channel.close()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    channel.close(SseException(failure(t, response), t))
                }
            }
        val source = factory.newEventSource(request.newBuilder().header("Accept", EVENT_STREAM).build(), listener)
        awaitClose { source.cancel() }
    }

    private fun failure(t: Throwable?, response: Response?): AppError =
        when {
            response != null && !response.isSuccessful ->
                errorFor(response.code, runCatching { response.body.string() }.getOrNull())
            t is TokenUnreadableException -> AppError.Locked
            t is IOException -> AppError.Network(interrupted = t.isInterruption())
            else -> AppError.InvalidResponse
        }

    private companion object {
        const val EVENT_STREAM = "text/event-stream"
    }
}
