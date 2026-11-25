package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoredTokenSourceTest {
    private val store = FakeStore(Journal(), stored = session)
    private val signal = UnauthorizedSignal()
    private val source = StoredTokenSource(store, signal)

    @Test fun `returns the stored token`() = runTest { assertEquals("t0ken", source.token()) }

    @Test
    fun `null when signed out`() = runTest {
        store.stored = null
        assertNull(source.token())
    }

    @Test
    fun `an unreadable store propagates`() = runTest {
        store.locked = true
        assertThrows<TokenUnreadableException> { source.token() }
    }

    private suspend fun kotlinx.coroutines.test.TestScope.posts(action: suspend () -> Unit): Boolean {
        var received = false
        val job = launch { signal.events.first().also { received = true } }
        runCurrent()
        action()
        runCurrent()
        job.cancel()
        return received
    }

    @Test
    fun `a 401 for the stored token posts the signal`() = runTest {
        assertTrue(posts { source.onUnauthorized("t0ken") })
    }

    @Test
    fun `a late 401 for an older token is ignored`() = runTest {
        assertFalse(posts { source.onUnauthorized("old-token") })
    }

    @Test
    fun `a protected request with no session posts the signal`() = runTest {
        store.stored = null
        assertTrue(posts { source.token() })
    }
}
