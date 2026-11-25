package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import retrofit2.Response

class ApiCallTest {
    private val json = "application/json".toMediaType()

    private fun failing(code: Int, body: String? = null): Response<String> =
        Response.error(code, (body ?: "").toResponseBody(if (body == null) null else json))

    private fun outcome(response: Response<String>) = kotlinx.coroutines.runBlocking { apiCall { response } }

    @Test fun `200 with a body is Ok`() = runTest { assertEquals(Outcome.Ok("x"), apiCall { Response.success("x") }) }

    @Test
    fun `204 through apiCallUnit is Ok`() = runTest {
        assertEquals(Outcome.Ok(Unit), apiCallUnit { Response.success(204, Unit) })
    }

    @TestFactory
    fun `error statuses map in the iOS order`(): List<DynamicTest> {
        val envelope = """{"error":{"message":"m"}}"""
        val cases: List<Triple<String, Response<String>, AppError>> =
            listOf(
                Triple("401", failing(401, envelope), AppError.Unauthorized("m")),
                Triple("404", failing(404, envelope), AppError.NotFound("m")),
                Triple("409 envelope", failing(409, envelope), AppError.Conflict("m", null)),
                Triple("409 newer plan", failing(409, """{"id":"p1"}"""), AppError.Conflict(null, """{"id":"p1"}""")),
                Triple(
                    "422 fields",
                    failing(422, """{"error":{"message":"m","fields":{"email":"taken"}}}"""),
                    AppError.FieldValidation("m", mapOf("email" to "taken")),
                ),
                Triple(
                    "429 fields",
                    failing(429, """{"error":{"message":"m","fields":{"quota":"daily"}}}"""),
                    AppError.FieldValidation("m", mapOf("quota" to "daily")),
                ),
                Triple("429 no fields", failing(429, envelope), AppError.Server(429, "m", null)),
                Triple(
                    "403 code",
                    failing(403, """{"error":{"message":"m","code":"ai_consent_required"}}"""),
                    AppError.Refused("ai_consent_required", "m"),
                ),
                Triple(
                    "500 code",
                    failing(500, """{"error":{"message":"m","code":"import_partial"}}"""),
                    AppError.Server(500, "m", "import_partial"),
                ),
                Triple("500 no body", failing(500), AppError.Server(500, null, null)),
            )
        return cases.map { (name, response, expected) ->
            DynamicTest.dynamicTest(name) { assertEquals(Outcome.Err(expected), outcome(response)) }
        }
    }

    @TestFactory
    fun `failures map without throwing`(): List<DynamicTest> =
        listOf<Pair<Throwable, AppError>>(
                SocketTimeoutException() to AppError.Network(interrupted = true),
                ConnectException() to AppError.Network(interrupted = true),
                UnknownHostException() to AppError.Network(interrupted = true),
                SocketException("Connection reset") to AppError.Network(interrupted = true),
                IOException("other") to AppError.Network(interrupted = false),
                TokenUnreadableException() to AppError.Locked,
                SerializationException("bad") to AppError.InvalidResponse,
            )
            .map { (thrown, expected) ->
                DynamicTest.dynamicTest(thrown.javaClass.simpleName) {
                    assertEquals(
                        Outcome.Err(expected),
                        kotlinx.coroutines.runBlocking { apiCall<String> { throw thrown } },
                    )
                }
            }
}
