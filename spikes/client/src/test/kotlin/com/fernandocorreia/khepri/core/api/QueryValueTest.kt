package com.fernandocorreia.khepri.core.api

import com.fernandocorreia.khepri.core.api.endpoint.RecapApi
import com.fernandocorreia.khepri.core.api.endpoint.TrainingApi
import java.io.File
import java.time.Instant
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class QueryValueTest {
    private val urls = mutableListOf<String>()

    private val retrofit =
        Retrofit.Builder()
            .baseUrl("https://stub.invalid/api/v1/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        urls += chain.request().url.toString()
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(204)
                            .message("No Content")
                            .body(ByteArray(0).toResponseBody(null))
                            .build()
                    }
                    .build()
            )
            .addConverterFactory(QueryValueConverterFactory)
            .addConverterFactory(ApiJson.strict.asConverterFactory("application/json".toMediaType()))
            .build()

    @Test
    fun `query enums go out by wire value`() = runBlocking {
        retrofit.create(TrainingApi::class.java).getTrainingWeek(TrainingApi.WeekGetTrainingWeek.NEXT)
        retrofit.create(RecapApi::class.java).getRecap(RecapApi.RangeGetRecap.entries.last())
        assertEquals("https://stub.invalid/api/v1/training/week?week=next", urls[0])
        assertEquals(
            "https://stub.invalid/api/v1/recap?range=${RecapApi.RangeGetRecap.entries.last().value}",
            urls[1],
        )
    }

    /** Every enum declared inside a generated endpoint interface converts to its `value`. */
    @TestFactory
    fun `every endpoint enum converts to its wire value`(): List<DynamicTest> {
        val dir = File(System.getProperty("generated.endpoints"))
        val enums =
            dir.listFiles { f -> f.extension == "kt" }!!.flatMap { f ->
                Class.forName("com.fernandocorreia.khepri.core.api.endpoint.${f.nameWithoutExtension}")
                    .declaredClasses
                    .filter { it.isEnum }
            }
        assertEquals(9, enums.size, "endpoint enums: ${enums.map { it.simpleName }}")
        return enums.flatMap { cls ->
            @Suppress("UNCHECKED_CAST")
            val converter = QueryValueConverterFactory.stringConverter(cls, emptyArray(), retrofit) as retrofit2.Converter<Any, String>
            cls.enumConstants.map { constant ->
                DynamicTest.dynamicTest("${cls.simpleName}.$constant") {
                    val wire = cls.getMethod("getValue").invoke(constant) as String
                    assertEquals(wire, converter.convert(constant))
                }
            }
        }
    }

    @Test
    fun `instants go out in the shared format`() {
        @Suppress("UNCHECKED_CAST")
        val converter =
            QueryValueConverterFactory.stringConverter(Instant::class.java, emptyArray(), retrofit) as retrofit2.Converter<Any, String>
        assertEquals("2026-11-25T10:00:00.120Z", converter.convert(Instant.parse("2026-11-25T10:00:00.120456Z")))
    }
}
