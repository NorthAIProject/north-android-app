package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `onUnauthorized posts the signal`() = runTest {
        var received = false
        val job = launch { signal.events.first().also { received = true } }
        runCurrent()
        source.onUnauthorized()
        runCurrent()
        assertTrue(received)
        job.cancel()
    }
}
