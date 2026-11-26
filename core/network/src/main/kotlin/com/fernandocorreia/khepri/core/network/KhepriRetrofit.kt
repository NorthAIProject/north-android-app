package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.api.JsonConverterFactory
import com.fernandocorreia.khepri.core.api.QueryValueConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * The app's Retrofit: `/api/v1/` under [baseUrl], query enums by wire value, the contract's JSON. Feature data tests
 * build it over a stub server with a plain client.
 */
public fun khepriRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl("${baseUrl.trimEnd('/')}/api/v1/")
        .client(client)
        .addConverterFactory(QueryValueConverterFactory)
        .addConverterFactory(JsonConverterFactory)
        .build()
