package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import java.time.Instant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionManagerTest {
    private val journal = Journal()
    private val store = FakeStore(journal, stored = session)
    private val remote = FakeRemote(journal, store)
    private val signal = UnauthorizedSignal()
    private var failBefore = false

    private fun TestScope.manager(): SessionManager {
        lateinit var manager: SessionManager
        val phase = { manager.phase.value }
        manager =
            SessionManager(
                store,
                remote,
                signal,
                setOf(
                    RecordingCleaner("push", journal, phase, failBefore),
                    RecordingCleaner("tabs", journal, phase),
                ),
                backgroundScope,
            )
        return manager
    }

    @Test
    fun `no session is signed out`() = runTest {
        store.stored = null
        val manager = manager()
        manager.start()
        assertEquals(AppPhase.SignedOut, manager.phase.value)
        assertEquals(emptyList<String>(), journal.entries)
    }

    @Test
    fun `a stored session lands on onboarding or signed in`() = runTest {
        remote.me = Outcome.Ok(false)
        val manager = manager()
        manager.start()
        assertEquals(AppPhase.Onboarding, manager.phase.value)
        remote.me = Outcome.Ok(true)
        manager.start()
        assertEquals(AppPhase.SignedIn, manager.phase.value)
    }

    @Test
    fun `a network error on me is Failed and retry recovers`() = runTest {
        remote.me = Outcome.Err(AppError.Network(interrupted = false))
        val manager = manager()
        manager.start()
        assertEquals(AppPhase.Failed(AppError.Network(interrupted = false)), manager.phase.value)
        remote.me = Outcome.Ok(true)
        manager.retry()
        assertEquals(AppPhase.SignedIn, manager.phase.value)
    }

    @Test
    fun `a locked store waits, keeps the session, and retry recovers`() = runTest {
        store.locked = true
        val manager = manager()
        manager.start()
        assertEquals(AppPhase.Locked, manager.phase.value)
        assertEquals(session, store.stored)
        manager.retry()
        assertEquals(AppPhase.Locked, manager.phase.value)
        store.locked = false
        manager.retry()
        assertEquals(AppPhase.SignedIn, manager.phase.value)
        assertFalse("clear" in journal.entries)
    }

    @Test
    fun `retry does nothing while signed in`() = runTest {
        val manager = manager()
        manager.start()
        journal.entries.clear()
        manager.retry()
        assertEquals(emptyList<String>(), journal.entries)
    }

    @Test
    fun `signing in writes the session then loads the account`() = runTest {
        store.stored = null
        remote.me = Outcome.Ok(false)
        val manager = manager()
        manager.start()
        manager.signedIn(session)
        assertEquals(listOf("write", "me"), journal.entries)
        assertEquals(AppPhase.Onboarding, manager.phase.value)
    }

    @Test
    fun `a 401 signal clears locally without calling the server`() = runTest {
        val manager = manager()
        manager.start()
        journal.entries.clear()
        signal.post()
        runCurrent()
        assertEquals(
            listOf("clear", "after:push:Unauthorized:SignedIn", "after:tabs:Unauthorized:SignedIn"),
            journal.entries,
        )
        assertEquals(AppPhase.SignedOut, manager.phase.value)
    }

    @Test
    fun `two signals together sign out once`() = runTest {
        val manager = manager()
        manager.start()
        journal.entries.clear()
        signal.post()
        signal.post()
        runCurrent()
        signal.post()
        runCurrent()
        assertEquals(1, journal.entries.count { it == "clear" })
        assertEquals(AppPhase.SignedOut, manager.phase.value)
    }

    @Test
    fun `a 401 on me signs out`() = runTest {
        remote.me = Outcome.Err(AppError.Unauthorized("expired"))
        val manager = manager()
        manager.start()
        assertEquals(AppPhase.SignedOut, manager.phase.value)
        assertNull(store.stored)
    }

    @Test
    fun `explicit sign-out runs in order`() = runTest {
        val manager = manager()
        manager.start()
        journal.entries.clear()
        manager.signOut(SignOutReason.Explicit)
        assertEquals(
            listOf(
                "before:push",
                "before:tabs",
                "logout(token=t0ken)",
                "clear",
                "after:push:Explicit:SignedIn",
                "after:tabs:Explicit:SignedIn",
            ),
            journal.entries,
        )
        assertEquals(AppPhase.SignedOut, manager.phase.value)
    }

    @Test
    fun `failing server steps do not stop the local clear`() = runTest {
        failBefore = true
        remote.logoutFails = true
        val manager = manager()
        manager.start()
        manager.signOut(SignOutReason.Explicit)
        assertNull(store.stored)
        assertEquals(AppPhase.SignedOut, manager.phase.value)
    }

    @Test
    fun `an expired expiresAt does not sign out by itself`() = runTest {
        store.stored = session.copy(expiresAt = Instant.parse("2020-01-01T00:00:00Z"))
        val manager = manager()
        manager.start()
        assertEquals(AppPhase.SignedIn, manager.phase.value)
    }

    @Test
    fun `signedIn is true exactly while the phase is SignedIn`() = runTest {
        val manager = manager()
        assertFalse(manager.signedIn.value)
        manager.start()
        assertTrue(manager.signedIn.value)
        remote.me = Outcome.Ok(false)
        manager.start()
        assertFalse(manager.signedIn.value)
        remote.me = Outcome.Ok(true)
        manager.start()
        manager.signOut(SignOutReason.Explicit)
        assertFalse(manager.signedIn.value)
    }
}
