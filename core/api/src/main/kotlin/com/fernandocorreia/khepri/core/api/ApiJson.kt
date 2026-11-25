package com.fernandocorreia.khepri.core.api

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/** The member name every generated model enum has for a value the client does not know. */
const val UNKNOWN_ENUM = "UNKNOWN"

/** The wire value of [UNKNOWN_ENUM]. The server rejects it, so it is never sent. */
const val UNKNOWN_WIRE_VALUE = "unknown_default_open_api"

/** The JSON configurations the app uses. Requests carry no defaults and no nulls. */
object ApiJson {
    val module = SerializersModule {
        contextual(Instant::class, FlexibleInstantSerializer)
        contextual(LocalDate::class, LocalDateSerializer)
    }

    /** Production: a newer server may add fields, so unknown keys are ignored. */
    val default = Json {
        serializersModule = module
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    /** Contract tests: a key the client does not know fails. */
    val strict = Json(from = default) { ignoreUnknownKeys = false }
}
