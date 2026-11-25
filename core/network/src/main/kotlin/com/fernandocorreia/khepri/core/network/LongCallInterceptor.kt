package com.fernandocorreia.khepri.core.network

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** Raises the read timeout to 300 s for the operations the spec index marks long (plan generation, imports). */
public class LongCallInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        if (chain.request().operation()?.isLong == true) {
            chain.withReadTimeout(LONG_READ_SECONDS, TimeUnit.SECONDS).proceed(chain.request())
        } else {
            chain.proceed(chain.request())
        }

    private companion object {
        const val LONG_READ_SECONDS = 300
    }
}
