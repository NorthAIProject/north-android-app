package com.fernandocorreia.khepri.feature.auth.ui.login

import app.cash.turbine.test
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.testing.MainDispatcherExtension
import com.fernandocorreia.khepri.core.ui.Haptic
import com.fernandocorreia.khepri.feature.auth.domain.usecase.RequestPasswordReset
import com.fernandocorreia.khepri.feature.auth.domain.usecase.SignInWithEmail
import com.fernandocorreia.khepri.feature.auth.domain.usecase.SignUpWithEmail
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class LoginViewModelTest {
    @JvmField @RegisterExtension val main = MainDispatcherExtension()

    private val repository = FakeAuthRepository()
    private val gateway = FakeGateway()
    private val lisbon = ZoneId.of("Europe/Lisbon")
    private val model =
        LoginViewModel(
            SignInWithEmail(repository, gateway),
            SignUpWithEmail(repository, gateway),
            RequestPasswordReset(repository),
            Clock.fixed(Instant.parse("2026-11-26T10:00:00Z"), lisbon),
        )

    private fun type(vararg events: LoginEvent) = events.forEach(model::onEvent)

    private val state
        get() = model.state.value

    @Test
    fun `switching mode keeps the email and clears the banner`() {
        repository.result = Outcome.Err(AppError.Unauthorized("no"))
        type(LoginEvent.EmailChanged("ana@example.com"), LoginEvent.PasswordChanged("pw"), LoginEvent.Submit)
        type(LoginEvent.ModeChanged(LoginMode.SignUp))
        assertEquals("ana@example.com", state.email)
        assertEquals(LoginMode.SignUp, state.mode)
        assertNull(state.banner)
    }

    @Test
    fun `a sign-in sends the trimmed email and hands the session to the gateway`() {
        type(LoginEvent.EmailChanged(" ana@example.com "), LoginEvent.PasswordChanged(" pw "), LoginEvent.Submit)
        assertEquals(listOf("ana@example.com" to " pw "), repository.signIns)
        assertEquals(1, gateway.kept.size)
        assertFalse(state.running)
    }

    @Test
    fun `mismatched passwords show the inline error and make no call`() {
        type(
            LoginEvent.ModeChanged(LoginMode.SignUp),
            LoginEvent.NameChanged("Ana"),
            LoginEvent.EmailChanged("ana@example.com"),
            LoginEvent.PasswordChanged("Secret12"),
            LoginEvent.ConfirmationChanged("Secret13"),
        )
        assertTrue(state.passwordsDiffer)
        assertFalse(state.canSubmit)
        type(LoginEvent.Submit)
        assertEquals(emptyList<Any>(), repository.signUps)
    }

    @Test
    fun `sign-up needs a name and eight characters, and sends the phone's zone`() {
        type(
            LoginEvent.ModeChanged(LoginMode.SignUp),
            LoginEvent.EmailChanged("ana@example.com"),
            LoginEvent.PasswordChanged("Secret1"),
            LoginEvent.ConfirmationChanged("Secret1"),
        )
        assertFalse(state.canSubmit)
        type(LoginEvent.NameChanged(" Ana "))
        assertFalse(state.canSubmit)
        type(LoginEvent.PasswordChanged("Secret12"), LoginEvent.ConfirmationChanged("Secret12"), LoginEvent.Submit)
        val form = repository.signUps.single()
        assertEquals("Ana", form.name)
        assertEquals(lisbon, form.timezone)
        assertEquals("Secret12", form.confirmation)
    }

    @Test
    fun `a failed sign-in keeps the form and plays the error haptic`() = runTest {
        repository.result = Outcome.Err(AppError.Unauthorized("Invalid email or password"))
        model.effects.test {
            type(LoginEvent.EmailChanged("ana@example.com"), LoginEvent.PasswordChanged("pw"), LoginEvent.Submit)
            assertEquals(LoginEffect.Play(Haptic.Error), awaitItem())
        }
        assertEquals("ana@example.com", state.email)
        assertEquals("pw", state.password)
        assertEquals(LoginBanner.Failed(AppError.Unauthorized("Invalid email or password")), state.banner)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `submit is disabled while a request runs and a second tap sends nothing`() {
        repository.gate = CompletableDeferred()
        type(LoginEvent.EmailChanged("ana@example.com"), LoginEvent.PasswordChanged("pw"), LoginEvent.Submit)
        assertTrue(state.running)
        assertFalse(state.canSubmit)
        type(LoginEvent.Submit)
        assertEquals(1, repository.signIns.size)
        repository.gate?.complete(Unit)
        assertFalse(state.running)
    }

    @Test
    fun `the reset sheet starts from the login email and closes with a confirmation once sent`() {
        type(LoginEvent.EmailChanged("ana@example.com"), LoginEvent.ForgotPasswordOpened)
        assertEquals(ForgotPasswordState("ana@example.com"), state.forgotPassword)
        type(LoginEvent.ResetEmailChanged("ana@exam"))
        assertEquals(false, state.forgotPassword?.canSend)
        type(LoginEvent.ResetRequested)
        assertEquals(emptyList<String>(), repository.resets)
        type(LoginEvent.ResetEmailChanged("bea@example.com"), LoginEvent.ResetRequested)
        assertEquals(listOf("bea@example.com"), repository.resets)
        assertNull(state.forgotPassword)
        assertEquals(LoginBanner.ResetSent, state.banner)
    }

    @Test
    fun `a failed reset shows its error in the sheet, not under it`() = runTest {
        repository.resetResult = Outcome.Err(AppError.Network(interrupted = false))
        model.effects.test {
            type(LoginEvent.EmailChanged("ana@example.com"), LoginEvent.ForgotPasswordOpened, LoginEvent.ResetRequested)
            assertEquals(LoginEffect.Play(Haptic.Error), awaitItem())
        }
        assertEquals(
            ForgotPasswordState("ana@example.com", error = AppError.Network(interrupted = false)),
            state.forgotPassword,
        )
        assertNull(state.banner)
        type(LoginEvent.ResetEmailChanged("bea@example.com"))
        assertNull(state.forgotPassword?.error)
    }

    @Test
    fun `the reset sheet starts from the trimmed login email and sends it trimmed`() {
        type(LoginEvent.EmailChanged("ana@example.com "), LoginEvent.ForgotPasswordOpened)
        assertEquals("ana@example.com", state.forgotPassword?.email)
        type(LoginEvent.ResetEmailChanged(" bea@example.com "))
        assertEquals(true, state.forgotPassword?.canSend)
        type(LoginEvent.ResetRequested)
        assertEquals(listOf("bea@example.com"), repository.resets)
    }

    @Test
    fun `dismissing the sheet while sending cancels the request, so it never closes a sheet opened later`() {
        repository.gate = CompletableDeferred()
        type(LoginEvent.EmailChanged("ana@example.com"), LoginEvent.ForgotPasswordOpened, LoginEvent.ResetRequested)
        type(LoginEvent.ForgotPasswordDismissed, LoginEvent.ForgotPasswordOpened)
        repository.gate?.complete(Unit)
        assertEquals(ForgotPasswordState("ana@example.com"), state.forgotPassword)
        assertNull(state.banner)
    }

    @Test
    fun `nothing typed outlives a sign-in, and the password manager is told to save`() = runTest {
        model.effects.test {
            type(
                LoginEvent.ModeChanged(LoginMode.SignUp),
                LoginEvent.NameChanged("Ana"),
                LoginEvent.EmailChanged("ana@example.com"),
                LoginEvent.PasswordChanged("Secret12"),
                LoginEvent.ConfirmationChanged("Secret12"),
                LoginEvent.Submit,
            )
            assertEquals(LoginEffect.CommitAutofill, awaitItem())
        }
        assertEquals(LoginUiState(), state)
    }

    @Test
    fun `events never print the password`() {
        assertFalse("hunter22" in LoginEvent.PasswordChanged("hunter22").toString())
        assertFalse("hunter22" in LoginEvent.ConfirmationChanged("hunter22").toString())
    }

    @Test
    fun `the state never prints the password`() {
        type(LoginEvent.PasswordChanged("hunter22"), LoginEvent.ConfirmationChanged("hunter22"))
        assertFalse("hunter22" in state.toString())
    }
}
