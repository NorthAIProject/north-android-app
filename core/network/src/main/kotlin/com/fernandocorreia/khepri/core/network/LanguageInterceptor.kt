package com.fernandocorreia.khepri.core.network

import java.util.Locale
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** Sends the first three preferred languages as `Accept-Language` with falling weights, unless the request has one. */
public class LanguageInterceptor @Inject constructor(private val languages: PreferredLanguages) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val value = if (request.header(HEADER) == null) headerValue(languages.get()) else null
        return chain.proceed(if (value == null) request else request.newBuilder().header(HEADER, value).build())
    }

    private fun headerValue(tags: List<String>): String? =
        tags
            .take(MAX_LANGUAGES)
            .takeIf { it.isNotEmpty() }
            ?.mapIndexed { i, tag ->
                if (i == 0) tag else "$tag;q=${String.format(Locale.ROOT, "%.1f", 1.0 - i * WEIGHT_STEP)}"
            }
            ?.joinToString(", ")

    private companion object {
        const val HEADER = "Accept-Language"
        const val MAX_LANGUAGES = 3
        const val WEIGHT_STEP = 0.1
    }
}
