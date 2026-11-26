package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.common.ConsentSignal
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Posts [ConsentSignal] when the server refuses a call with 403 `ai_consent_required`, so the consent question can be
 * asked again (iOS does not; Android asks rather than showing a failure). The response is passed on unchanged.
 */
public class ConsentRefusalInterceptor @Inject constructor(private val signal: ConsentSignal) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HTTP_FORBIDDEN && codeOf(response) == CODE) signal.post()
        return response
    }

    private fun codeOf(response: Response): String? {
        val body = runCatching { response.peekBody(PEEK_BYTES).string() }.getOrNull() ?: return null
        val json = runCatching { lenient.parseToJsonElement(body) as? JsonObject }.getOrNull()
        return parseEnvelope(json)?.code
    }

    private companion object {
        const val CODE = "ai_consent_required"
        const val PEEK_BYTES = 16L * 1024
        val lenient = Json { ignoreUnknownKeys = true }
    }
}
