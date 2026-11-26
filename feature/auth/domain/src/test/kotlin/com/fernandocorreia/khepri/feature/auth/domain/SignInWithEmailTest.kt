package com.fernandocorreia.khepri.feature.auth.domain

import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm
import com.fernandocorreia.khepri.feature.auth.domain.usecase.SignInWithEmail
import com.fernandocorreia.khepri.feature.auth.domain.usecase.SignUpWithEmail
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SignInWithEmailTest {
    private val repository = FakeAuthRepository()
    private val gateway = FakeGateway()

    @Test
    fun `a session is handed to the gateway once`() = runTest {
        assertEquals(Outcome.Ok(Unit), SignInWithEmail(repository, gateway)("ana@example.com", "pw"))
        assertEquals(listOf(session), gateway.kept)
    }

    @Test
    fun `a failed sign-in never reaches the gateway`() = runTest {
        repository.result = Outcome.Err(AppError.Unauthorized("Wrong email or password"))
        assertEquals(repository.result, SignInWithEmail(repository, gateway)("ana@example.com", "pw"))
        assertEquals(emptyList<Any>(), gateway.kept)
    }

    @Test
    fun `a session the phone cannot keep is the gateway's error`() = runTest {
        gateway.result = Outcome.Err(AppError.Locked)
        assertEquals(Outcome.Err(AppError.Locked), SignInWithEmail(repository, gateway)("ana@example.com", "pw"))
    }

    @Test
    fun `sign-up hands its session to the gateway too`() = runTest {
        val form = SignUpForm("Ana", "ana@example.com", "Secret12", "Secret12", ZoneId.of("Europe/Lisbon"))
        assertEquals(Outcome.Ok(Unit), SignUpWithEmail(repository, gateway)(form))
        assertEquals(listOf(form), repository.signUps)
        assertEquals(listOf(session), gateway.kept)
    }
}
