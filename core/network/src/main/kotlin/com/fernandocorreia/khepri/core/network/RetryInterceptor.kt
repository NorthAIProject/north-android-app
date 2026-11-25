package com.fernandocorreia.khepri.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retries a GET or HEAD once after a dropped connection, after [pause]. Writes and event streams are never retried, and
 * an HTTP error status is never retried. The clients also turn off OkHttp's own connection retry.
 */
public class RetryInterceptor(private val pause: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val retryable = request.method in IDEMPOTENT && request.header("Accept")?.contains("text/event-stream") != true
        if (!retryable) return chain.proceed(request)
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            pause()
            chain.proceed(request)
        }
    }

    public companion object {
        private val IDEMPOTENT = setOf("GET", "HEAD")
        public const val PRODUCTION_PAUSE_MS: Long = 500
    }
}
