package com.fernandocorreia.khepri.core.network.di

import android.os.LocaleList
import com.fernandocorreia.khepri.core.common.ApiBaseUrl
import com.fernandocorreia.khepri.core.network.BearerInterceptor
import com.fernandocorreia.khepri.core.network.ConsentRefusalInterceptor
import com.fernandocorreia.khepri.core.network.LanguageInterceptor
import com.fernandocorreia.khepri.core.network.LongCallInterceptor
import com.fernandocorreia.khepri.core.network.PreferredLanguages
import com.fernandocorreia.khepri.core.network.RetryInterceptor
import com.fernandocorreia.khepri.core.network.khepriRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/** Requests and responses with a 60 s read timeout (300 s for long operations). */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class DefaultClient

/**
 * Event streams: 300 s read timeout, as the iOS generation client, so a half-open stream after a network switch still
 * fails and reconnects. A feature with server pings (crew live, 20 s) sets a shorter per-call timeout.
 */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class StreamingClient

/**
 * Creates generated Retrofit interfaces, once each. Data modules bind the ones they use:
 * `services.create<TrainingApi>()`.
 */
@Singleton
public class ApiServices @Inject constructor(private val retrofit: Retrofit) {
    private val created = ConcurrentHashMap<Class<*>, Any>()

    public fun <T : Any> create(type: Class<T>): T = type.cast(created.getOrPut(type) { retrofit.create(type) })

    public inline fun <reified T : Any> create(): T = create(T::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    private val connectTimeout = Duration.ofSeconds(15)
    private val readTimeout = Duration.ofSeconds(60)
    private val streamReadTimeout = Duration.ofSeconds(300)

    @Provides @Singleton fun connectionPool(): ConnectionPool = ConnectionPool()

    @Provides
    fun preferredLanguages(): PreferredLanguages = PreferredLanguages {
        val list = LocaleList.getAdjustedDefault()
        (0 until list.size()).map { list[it].toLanguageTag() }
    }

    @Provides fun retry(): RetryInterceptor = RetryInterceptor { Thread.sleep(RetryInterceptor.PRODUCTION_PAUSE_MS) }

    @Provides
    @Singleton
    @DefaultClient
    fun defaultClient(
        pool: ConnectionPool,
        language: LanguageInterceptor,
        bearer: BearerInterceptor,
        consent: ConsentRefusalInterceptor,
        longCall: LongCallInterceptor,
        retry: RetryInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectionPool(pool)
            .connectTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .retryOnConnectionFailure(false)
            .addInterceptor(language)
            .addInterceptor(bearer)
            .addInterceptor(consent)
            .addInterceptor(longCall)
            .addInterceptor(retry)
            .build()

    @Provides
    @Singleton
    @StreamingClient
    fun streamingClient(
        pool: ConnectionPool,
        language: LanguageInterceptor,
        bearer: BearerInterceptor,
        consent: ConsentRefusalInterceptor,
        retry: RetryInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectionPool(pool)
            .connectTimeout(connectTimeout)
            .readTimeout(streamReadTimeout)
            .retryOnConnectionFailure(false)
            .addInterceptor(language)
            .addInterceptor(bearer)
            .addInterceptor(consent)
            .addInterceptor(retry)
            .build()

    @Provides
    @Singleton
    fun retrofit(@ApiBaseUrl baseUrl: String, @DefaultClient client: OkHttpClient): Retrofit =
        khepriRetrofit(baseUrl, client)
}
