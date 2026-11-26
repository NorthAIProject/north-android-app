package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.common.ApiBaseUrl
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Adds the session token to every spec operation on the API's own origin that is not public (logout included: it sends
 * the token it revokes). Any other URL never gets the token. A 401 is reported with the token that was sent. If the
 * token cannot be read the request is not sent and the call fails with `TokenUnreadableException`.
 */
public class BearerInterceptor @Inject constructor(private val tokens: TokenSource, @ApiBaseUrl baseUrl: String) :
    Interceptor {
    private val api = baseUrl.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = if (needsToken(request)) runBlocking { tokens.token() } else null
        val response =
            chain.proceed(
                if (token == null) request else request.newBuilder().header("Authorization", "Bearer $token").build()
            )
        if (response.code == HTTP_UNAUTHORIZED && token != null) runBlocking { tokens.onUnauthorized(token) }
        return response
    }

    private fun needsToken(request: Request): Boolean {
        val url = request.url
        val sameOrigin = url.scheme == api.scheme && url.host == api.host && url.port == api.port
        val operation = if (sameOrigin) request.operation() else null
        return operation != null && !operation.isPublic
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
