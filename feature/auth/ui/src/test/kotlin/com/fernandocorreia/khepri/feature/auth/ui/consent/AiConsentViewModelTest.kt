package com.fernandocorreia.khepri.feature.auth.ui.consent

import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.testing.MainDispatcherExtension
import com.fernandocorreia.khepri.feature.auth.domain.model.AiConsent
import com.fernandocorreia.khepri.feature.auth.domain.model.ConsentChoice
import com.fernandocorreia.khepri.feature.auth.domain.repository.AiConsentRepository
import com.fernandocorreia.khepri.feature.auth.domain.usecase.DecideAiConsent
import com.fernandocorreia.khepri.feature.auth.domain.usecase.ObserveAiConsent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AiConsentViewModelTest {
    @JvmField @RegisterExtension val main = MainDispatcherExtension()

    private class FakeConsent : AiConsentRepository {
        val flow = MutableStateFlow(AiConsent(ConsentChoice.Unanswered, listOf("Anthropic")))
        override val consent: StateFlow<AiConsent> = flow
        val decisions = mutableListOf<Boolean>()
        var result: Outcome<Unit> = Outcome.Ok(Unit)
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun load(): Outcome<Unit> = Outcome.Ok(Unit)

        override suspend fun decide(allow: Boolean): Outcome<Unit> {
            decisions += allow
            gate?.await()
            return result
        }

        override fun reset() = Unit
    }

    private val repository = FakeConsent()

    /** Built inside each test, after the extension has set Main: `viewModelScope` binds Main when it is created. */
    private val model by lazy { AiConsentViewModel(ObserveAiConsent(repository), DecideAiConsent(repository)) }

    private val watching = CoroutineScope(SupervisorJob())

    /** The state is shared while watched, as a screen would. */
    private fun watch() {
        watching.launch(main.dispatcher) { model.state.collect {} }
    }

    @AfterEach fun stopWatching() = watching.cancel()

    @Test
    fun `shows the server's recipients`() {
        watch()
        assertEquals(listOf("Anthropic"), model.state.value.recipients)
    }

    @Test
    fun `allow and decline each answer once`() {
        watch()
        model.onEvent(AiConsentEvent.Allow)
        model.onEvent(AiConsentEvent.Decline)
        assertEquals(listOf(true, false), repository.decisions)
    }

    @Test
    fun `answers are ignored while one is saving`() {
        watch()
        repository.gate = CompletableDeferred()
        model.onEvent(AiConsentEvent.Allow)
        assertTrue(model.state.value.saving)
        model.onEvent(AiConsentEvent.Decline)
        model.onEvent(AiConsentEvent.Allow)
        repository.gate?.complete(Unit)
        assertEquals(listOf(true), repository.decisions)
        assertFalse(model.state.value.saving)
    }

    @Test
    fun `a failed save shows the error and keeps the question, and the next answer clears it`() {
        watch()
        val error = AppError.Server(500, "Try again", null)
        repository.result = Outcome.Err(error)
        model.onEvent(AiConsentEvent.Allow)
        assertEquals(error, model.state.value.error)
        assertEquals(listOf("Anthropic"), model.state.value.recipients)
        repository.result = Outcome.Ok(Unit)
        model.onEvent(AiConsentEvent.Allow)
        assertNull(model.state.value.error)
    }
}
