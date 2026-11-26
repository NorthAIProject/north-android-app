package com.fernandocorreia.khepri.feature.auth.data

import com.fernandocorreia.khepri.core.api.endpoint.AuthApi
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import com.fernandocorreia.khepri.core.network.khepriRetrofit
import com.fernandocorreia.khepri.feature.auth.data.repository.AuthRepositoryImpl
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthRepositoryImplTest {
    private val server = MockWebServer()
    private lateinit var repository: AuthRepositoryImpl

    @BeforeEach
    fun start() {
        server.start()
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        repository = AuthRepositoryImpl(khepriRetrofit(server.url("/").toString(), client).create(AuthApi::class.java))
    }

    @AfterEach fun stop() = server.close()

    private fun answer(code: Int, body: String) =
        server.enqueue(
            MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()
        )

    private fun sent(): Pair<String, JsonObject> {
        val request = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        return request.url.encodedPath to Json.parseToJsonElement(request.body?.utf8().orEmpty()).jsonObject
    }

    private val authResponse =
        """{"token":"t0ken","expiresAt":"2030-01-01T00:00:00.5Z","user":{"id":"u1","email":"ana@example.com",""" +
            """"displayName":"Ana","timezone":"Europe/Lisbon","needsOnboarding":false}}"""

    @Test
    fun `a sign-in maps the response to a session and sends only email and password`() = runTest {
        answer(200, authResponse)
        assertEquals(
            Outcome.Ok(Session("t0ken", "u1", Instant.parse("2030-01-01T00:00:00.5Z"))),
            repository.signIn("ana@example.com", "pw"),
        )
        val (path, body) = sent()
        assertEquals("/api/v1/auth/login", path)
        assertEquals(setOf("email", "password"), body.keys)
    }

    @Test
    fun `a 422 with fields is a field validation error`() = runTest {
        answer(422, """{"error":{"message":"Email is taken","fields":{"email":"taken"}}}""")
        assertEquals(
            Outcome.Err(AppError.FieldValidation("Email is taken", mapOf("email" to "taken"))),
            repository.signIn("ana@example.com", "pw"),
        )
    }

    @Test
    fun `a 429 without fields is a server error with its message`() = runTest {
        answer(429, """{"error":{"message":"Too many attempts"}}""")
        val result = repository.signIn("ana@example.com", "pw")
        assertEquals(429, ((result as Outcome.Err).error as AppError.Server).status)
        assertEquals("Too many attempts", (result.error as AppError.Server).message)
    }

    @Test
    fun `sign-up sends the zone id and the confirmation, and leaves out a blank name`() = runTest {
        answer(201, authResponse)
        val form = SignUpForm("  ", "ana@example.com", "Secret12", "Secret12", ZoneId.of("Europe/Lisbon"))
        assertEquals("u1", (repository.signUp(form) as Outcome.Ok).value.userId)
        val (path, body) = sent()
        assertEquals("/api/v1/auth/signup", path)
        assertEquals(setOf("email", "password", "passwordConfirmation", "timezone"), body.keys)
        assertEquals("Europe/Lisbon", body.getValue("timezone").jsonPrimitive.content)
        assertEquals("Secret12", body.getValue("passwordConfirmation").jsonPrimitive.content)
    }

    @Test
    fun `sign-up sends a trimmed name`() = runTest {
        answer(201, authResponse)
        repository.signUp(SignUpForm(" Ana ", "ana@example.com", "Secret12", "Secret12", ZoneId.of("UTC")))
        assertEquals("Ana", sent().second.getValue("displayName").jsonPrimitive.content)
    }

    @Test
    fun `a reset request answers ok with no body`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertEquals(Outcome.Ok(Unit), repository.requestPasswordReset("ana@example.com"))
        val (path, body) = sent()
        assertEquals("/api/v1/auth/forgot-password", path)
        assertEquals(setOf("email"), body.keys)
    }
}
