package com.fernandocorreia.khepri.core.network

import java.util.concurrent.TimeUnit
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

internal class FakeTokens(var value: String? = "t0ken", var unreadable: Boolean = false) : TokenSource {
    var unauthorized = 0

    override suspend fun token(): String? {
        if (unreadable) throw com.fernandocorreia.khepri.core.common.TokenUnreadableException()
        return value
    }

    override suspend fun onUnauthorized(sentToken: String) {
        unauthorized++
    }
}

internal fun ok(body: String = "{}") = MockResponse.Builder().code(200).body(body).build()

internal fun status(code: Int, body: String = "") = MockResponse.Builder().code(code).body(body).build()

internal fun dropped() = MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build()

internal fun client(vararg interceptors: Interceptor): OkHttpClient =
    OkHttpClient.Builder().retryOnConnectionFailure(false).apply { interceptors.forEach { addInterceptor(it) } }.build()

internal fun OkHttpClient.call(server: MockWebServer, method: String, path: String, accept: String? = null): Response {
    val body = if (method == "GET" || method == "HEAD") null else "{}".toRequestBody()
    val request =
        Request.Builder()
            .url(server.url(path))
            .method(method, body)
            .apply { accept?.let { header("Accept", it) } }
            .build()
    return newCall(request).execute()
}

internal fun MockWebServer.next(): RecordedRequest? = takeRequest(1, TimeUnit.SECONDS)
