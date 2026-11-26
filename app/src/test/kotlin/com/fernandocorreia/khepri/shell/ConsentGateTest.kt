package com.fernandocorreia.khepri.shell

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.feature.auth.domain.model.AiConsent
import com.fernandocorreia.khepri.feature.auth.domain.model.ConsentChoice
import com.fernandocorreia.khepri.feature.auth.domain.repository.AiConsentRepository
import com.fernandocorreia.khepri.feature.auth.domain.usecase.LoadAiConsent
import com.fernandocorreia.khepri.feature.auth.domain.usecase.ObserveAiConsent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentGateTest {
    private class FakeConsent : AiConsentRepository {
        val flow = MutableStateFlow(AiConsent.Unknown)
        override val consent: StateFlow<AiConsent> = flow
        var loads = 0
        var answer = AiConsent(ConsentChoice.Unanswered, listOf("Anthropic"))

        override suspend fun load(): Outcome<Unit> {
            loads++
            flow.value = answer
            return Outcome.Ok(Unit)
        }

        override suspend fun decide(allow: Boolean): Outcome<Unit> = Outcome.Ok(Unit)

        override fun reset() {
            flow.value = AiConsent.Unknown
        }
    }

    private val repository = FakeConsent()
    private val phase = MutableStateFlow<AppPhase>(AppPhase.Launching)

    private fun kotlinx.coroutines.test.TestScope.gate() =
        ConsentGate(
            phase,
            ObserveAiConsent(repository),
            LoadAiConsent(repository),
            kotlinx.coroutines.CoroutineScope(
                backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)
            ),
        )

    @Test
    fun `entering the app reads the answer once and asks an unanswered account`() = runTest {
        val gate = gate()
        assertEquals(0, repository.loads)
        phase.value = AppPhase.Onboarding
        phase.value = AppPhase.SignedIn
        assertEquals(1, repository.loads)
        assertTrue(gate.asks.value)
    }

    @Test
    fun `an answered account is not asked`() = runTest {
        repository.answer = AiConsent(ConsentChoice.Declined, emptyList())
        val gate = gate()
        phase.value = AppPhase.SignedIn
        assertFalse(gate.asks.value)
    }

    @Test
    fun `signing out hides the question, and the next sign-in reads again`() = runTest {
        val gate = gate()
        phase.value = AppPhase.SignedIn
        phase.value = AppPhase.SignedOut
        repository.reset()
        assertFalse(gate.asks.value)
        phase.value = AppPhase.SignedIn
        assertEquals(2, repository.loads)
        assertTrue(gate.asks.value)
    }

    @Test
    fun `coming to the foreground reads again, but only over the app or onboarding`() = runTest {
        val gate = gate()
        phase.value = AppPhase.SignedOut
        gate.foreground()
        assertEquals(0, repository.loads)
        phase.value = AppPhase.SignedIn
        gate.foreground()
        assertEquals(2, repository.loads)
    }
}
