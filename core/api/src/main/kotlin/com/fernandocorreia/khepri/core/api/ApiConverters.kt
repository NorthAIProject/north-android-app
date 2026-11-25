package com.fernandocorreia.khepri.core.api

import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Bodies through [ApiJson.default]. A request whose JSON holds the `UNKNOWN` enum value anywhere fails with
 * [SerializationException] before it is sent: the server rejects that value.
 */
object JsonConverterFactory : Converter.Factory() {
    private val mediaType = "application/json; charset=UTF-8".toMediaType()

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody> {
        val serializer = ApiJson.default.serializersModule.serializer(type)
        return Converter<Any, RequestBody> { value ->
            val element = ApiJson.default.encodeToJsonElement(serializer, value)
            if (element.holdsUnknown()) throw SerializationException("Refusing to send $UNKNOWN_ENUM to the server")
            element.toString().toRequestBody(mediaType)
        }
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val serializer = ApiJson.default.serializersModule.serializer(type)
        return Converter<ResponseBody, Any?> { body -> body.use { ApiJson.default.decodeFromString(serializer, it.string()) } }
    }

    private fun JsonElement.holdsUnknown(): Boolean =
        when (this) {
            is JsonPrimitive -> isString && content == UNKNOWN_WIRE_VALUE
            is JsonArray -> any { it.holdsUnknown() }
            is JsonObject -> values.any { it.holdsUnknown() }
        }
}

/**
 * Retrofit turns `@Query` and `@Path` values into text with `toString()`, and generated enums do not override it, so
 * `week = NEXT` would be sent as `NEXT` instead of `next`. This writes enums, [Instant] and [LocalDate] exactly as they
 * are written in JSON bodies, and refuses `UNKNOWN`.
 */
object QueryValueConverterFactory : Converter.Factory() {
    override fun stringConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<*, String>? {
        val cls = type as? Class<*> ?: return null
        if (!cls.isEnum && cls != Instant::class.java && cls != LocalDate::class.java) return null
        @Suppress("UNCHECKED_CAST")
        val serializer = ApiJson.default.serializersModule.serializer(type) as KSerializer<Any>
        return Converter<Any, String> { value ->
            val text = (ApiJson.default.encodeToJsonElement(serializer, value) as JsonPrimitive).content
            if (text == UNKNOWN_WIRE_VALUE) throw SerializationException("Refusing to send $UNKNOWN_ENUM to the server")
            text
        }
    }
}
