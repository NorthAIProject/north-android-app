package com.fernandocorreia.khepri.core.navigation

import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QueuedNavigatorTest {
    private val links = FakePendingLinks()

    private fun TestScope.navigator() = QueuedNavigator(links, backgroundScope)

    @Test
    fun `commands queue in order and are taken once`() = runTest {
        val navigator = navigator()
        navigator.open(AppDestination.Goal("g"))
        navigator.push(Route.Section("care"))
        navigator.pop()
        runCurrent()
        assertEquals(
            listOf(NavCommand.Open(AppDestination.Goal("g")), NavCommand.Push(Route.Section("care")), NavCommand.Pop),
            navigator.takeAll(),
        )
        assertTrue(navigator.takeAll().isEmpty())
    }

    @Test
    fun `the same crew link twice is two commands`() = runTest {
        val navigator = navigator()
        val link = "/app/crews/66666666-6666-6666-6666-666666666666"
        assertTrue(navigator.open(link))
        assertTrue(navigator.open(link))
        runCurrent()
        assertEquals(2, navigator.takeAll().size)
    }

    @Test
    fun `an unknown link is refused and queues nothing`() = runTest {
        val navigator = navigator()
        assertFalse(navigator.open("https://example.com/app/today"))
        runCurrent()
        assertTrue(navigator.pending.value.isEmpty())
    }

    @Test
    fun `invite and crew join codes are kept on disk before their command is queued`() = runTest {
        val navigator = navigator()
        navigator.open(AppDestination.Invite("abc123xyz9"))
        navigator.open(AppDestination.CrewJoin("q8w3n5k2ht"))
        navigator.open(AppDestination.Goal("g"))
        runCurrent()
        assertEquals(listOf(AppDestination.Invite("abc123xyz9"), AppDestination.CrewJoin("q8w3n5k2ht")), links.kept)
        assertEquals(3, navigator.takeAll().size)
    }

    @Test
    fun `a disk failure still queues the command`() = runTest {
        links.failure = IOException("disk")
        val navigator = navigator()
        navigator.open(AppDestination.Invite("abc123xyz9"))
        runCurrent()
        assertEquals(listOf(NavCommand.Open(AppDestination.Invite("abc123xyz9"))), navigator.takeAll())
    }
}
