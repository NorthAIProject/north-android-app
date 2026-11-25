package com.fernandocorreia.khepri.core.api

import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Writes `@Query`/`@Path` values the way the server reads them. Without it Retrofit calls `toString()`, which sends a
 * generated enum by its Kotlin name (`week=NEXT`) instead of its wire value (`week=next`).
 */
object QueryValueConverterFactory : Converter.Factory() {
    override fun stringConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<*, String>? {
        val cls = type as? Class<*> ?: return null
        if (!cls.isEnum && cls != Instant::class.java && cls != LocalDate::class.java) return null
        val serializer = ApiJson.strict.serializersModule.serializer(type)
        return Converter<Any, String> { value ->
            (ApiJson.strict.encodeToJsonElement(serializer, value) as JsonPrimitive).content
        }
    }
}
