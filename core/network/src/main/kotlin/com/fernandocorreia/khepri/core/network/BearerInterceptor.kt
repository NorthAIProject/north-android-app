package com.fernandocorreia.khepri.core.network

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the session token to every operation the spec does not mark public (logout included: it sends the token it
 * revokes). A 401 signs out only when a token was sent. If the token cannot be read the request is not sent and the
 * call fails with `TokenUnreadableException`.
 */
public class BearerInterceptor @Inject constructor(private val tokens: TokenSource) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isPublic = request.operation()?.isPublic == true
        val token = if (isPublic) null else runBlocking { tokens.token() }
        val response =
            chain.proceed(
                if (token == null) request else request.newBuilder().header("Authorization", "Bearer $token").build()
            )
        if (response.code == HTTP_UNAUTHORIZED && token != null) tokens.onUnauthorized()
        return response
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
