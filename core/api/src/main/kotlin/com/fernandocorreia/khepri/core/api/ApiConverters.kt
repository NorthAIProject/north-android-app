package com.fernandocorreia.khepri.core.api

import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
        @Suppress("UNCHECKED_CAST")
        val serializer = ApiJson.default.serializersModule.serializer(type) as KSerializer<Any?>
        return Converter<Any, RequestBody> { value ->
            val element = encodeRequest(serializer, value)
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
        return Converter<ResponseBody, Any?> { body ->
            body.use { ApiJson.default.decodeFromString(serializer, it.string()) }
        }
    }

    /**
     * Encodes without nulls, then puts back `null` for properties the spec marks required and nullable
     * (`HelpfulRequest.helpful`: null clears the vote). An optional property that is null stays absent.
     */
    fun encodeRequest(serializer: KSerializer<Any?>, value: Any?): JsonElement =
        ApiJson.default.encodeToJsonElement(serializer, value).withRequiredNulls(serializer.descriptor)

    private fun JsonElement.withRequiredNulls(descriptor: SerialDescriptor): JsonElement =
        when {
            this is JsonObject && descriptor.kind == StructureKind.CLASS ->
                JsonObject(
                    buildMap {
                        putAll(this@withRequiredNulls)
                        for (i in 0 until descriptor.elementsCount) {
                            val name = descriptor.getElementName(i)
                            val child = descriptor.getElementDescriptor(i)
                            val current = this@withRequiredNulls[name]
                            when {
                                current != null -> put(name, current.withRequiredNulls(child))
                                !descriptor.isElementOptional(i) && child.isNullable -> put(name, JsonNull)
                            }
                        }
                    }
                )
            this is JsonArray && descriptor.kind == StructureKind.LIST ->
                JsonArray(map { it.withRequiredNulls(descriptor.getElementDescriptor(0)) })
            else -> this
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
    private val textTypes = setOf<Class<*>>(Instant::class.java, LocalDate::class.java)

    private fun isQueryValue(type: Type): Boolean = type is Class<*> && (type.isEnum || type in textTypes)

    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, String>? {
        if (!isQueryValue(type)) return null
        @Suppress("UNCHECKED_CAST")
        val serializer = ApiJson.default.serializersModule.serializer(type) as KSerializer<Any>
        return Converter<Any, String> { value ->
            val text = (ApiJson.default.encodeToJsonElement(serializer, value) as JsonPrimitive).content
            if (text == UNKNOWN_WIRE_VALUE) throw SerializationException("Refusing to send $UNKNOWN_ENUM to the server")
            text
        }
    }
}
