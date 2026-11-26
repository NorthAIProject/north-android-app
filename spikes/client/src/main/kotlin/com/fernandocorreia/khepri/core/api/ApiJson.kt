package com.fernandocorreia.khepri.core.api

import com.fernandocorreia.khepri.core.api.infrastructure.LocalDateAdapter
import com.fernandocorreia.khepri.core.api.infrastructure.UUIDAdapter
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/** The JSON configurations the app uses. Never use the generated `Serializer.kotlinxSerializationJson`: it encodes defaults. */
object ApiJson {
    val module = SerializersModule {
        contextual(Instant::class, FlexibleInstantSerializer)
        contextual(UUID::class, UUIDAdapter)
        contextual(LocalDate::class, LocalDateAdapter)
    }

    /** Contract tests: a key the client does not know fails. Requests: no defaults and no nulls on the wire. */
    val strict = Json {
        serializersModule = module
        ignoreUnknownKeys = false
        explicitNulls = false
        encodeDefaults = false
    }

    /** Production decoding: a newer server may add fields. */
    val tolerant = Json(from = strict) { ignoreUnknownKeys = true }
}
