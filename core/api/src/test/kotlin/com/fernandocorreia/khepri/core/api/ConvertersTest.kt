package com.fernandocorreia.khepri.core.api

import com.fernandocorreia.khepri.core.api.endpoint.RecapApi
import com.fernandocorreia.khepri.core.api.endpoint.TrainingApi
import com.fernandocorreia.khepri.core.api.model.HelpfulRequestDto
import com.fernandocorreia.khepri.core.api.model.LiftSetInputDto
import com.fernandocorreia.khepri.core.api.model.MealPlanTypeDto
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import retrofit2.Converter

class ConvertersTest {
    private val urls = mutableListOf<String>()

    private val retrofit =
        retrofit2.Retrofit.Builder()
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
            .addConverterFactory(JsonConverterFactory)
            .build()

    @Suppress("UNCHECKED_CAST")
    private fun queryConverter(type: Class<*>) =
        QueryValueConverterFactory.stringConverter(type, emptyArray(), retrofit) as Converter<Any, String>

    @Suppress("UNCHECKED_CAST")
    private fun <T> bodyConverter(type: Class<T>) =
        JsonConverterFactory.requestBodyConverter(type, emptyArray(), emptyArray(), retrofit)
            as Converter<T, okhttp3.RequestBody>

    private fun okhttp3.RequestBody.text() = Buffer().also { writeTo(it) }.readUtf8()

    @Test
    fun `query enums go out by wire value`() = runBlocking {
        retrofit.create(TrainingApi::class.java).getTrainingWeek(TrainingApi.WeekGetTrainingWeek.NEXT)
        val range = RecapApi.RangeGetRecap.entries.last()
        retrofit.create(RecapApi::class.java).getRecap(range)
        assertEquals("https://stub.invalid/api/v1/training/week?week=next", urls[0])
        assertEquals("https://stub.invalid/api/v1/recap?range=${range.value}", urls[1])
    }

    @TestFactory
    fun `every endpoint enum converts to its wire value`(): List<DynamicTest> {
        val enums =
            TestContract.endpointsDir
                .listFiles { f -> f.extension == "kt" }
                .orEmpty()
                .flatMap { f ->
                    Class.forName("com.fernandocorreia.khepri.core.api.endpoint.${f.nameWithoutExtension}")
                        .declaredClasses
                        .filter { it.isEnum }
                }
        assertEquals(9, enums.size, enums.joinToString { it.simpleName })
        return enums.flatMap { cls ->
            val converter = queryConverter(cls)
            cls.enumConstants.map { constant ->
                DynamicTest.dynamicTest("${cls.simpleName}.$constant") {
                    assertEquals(cls.getMethod("getValue").invoke(constant), converter.convert(constant))
                }
            }
        }
    }

    @Test
    fun `instants go out in the shared format`() =
        assertEquals(
            "2026-11-25T10:00:00.120Z",
            queryConverter(Instant::class.java).convert(Instant.parse("2026-11-25T10:00:00.120456Z")),
        )

    @Test
    fun `UNKNOWN is never sent as a query value`() {
        assertThrows<SerializationException> {
            queryConverter(MealPlanTypeDto::class.java).convert(MealPlanTypeDto.UNKNOWN)
        }
    }

    @Test
    fun `UNKNOWN is never sent in a body`() {
        val body =
            LiftSetInputDto(
                exerciseName = "Squat",
                setNumber = 1,
                weightKg = 100.0,
                reps = 5,
                kind = LiftSetInputDto.Kind.UNKNOWN,
            )
        assertThrows<SerializationException> { bodyConverter(LiftSetInputDto::class.java).convert(body) }
    }

    @Test
    fun `a required nullable field is sent as null and optional nulls are left out`() {
        assertEquals(
            "{\"helpful\":null}",
            bodyConverter(HelpfulRequestDto::class.java).convert(HelpfulRequestDto(helpful = null))?.text(),
        )
        val lift = LiftSetInputDto(exerciseName = "Squat", setNumber = 1, weightKg = 100.0, reps = 5)
        assertEquals(false, bodyConverter(LiftSetInputDto::class.java).convert(lift)?.text()?.contains("null"))
    }
}
