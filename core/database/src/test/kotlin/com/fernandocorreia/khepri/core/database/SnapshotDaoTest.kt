package com.fernandocorreia.khepri.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.database.snapshot.CachedSnapshot
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDaoTest {
    private val database = testDatabase()
    private val dao = database.snapshots()
    private val at = Instant.parse("2026-11-26T10:00:00.123Z")

    @After fun close() = database.close()

    private fun row(account: String, key: String, json: String = "{}") = CachedSnapshot(account, key, json, at)

    private suspend fun keys(account: String, vararg candidates: String) = candidates.filter {
        dao.observe(account, it).first() != null
    }

    @Test
    fun `a stored row is observed with its fetch time to the millisecond`() = runTest {
        dao.put(row("a", "today", """{"x":1}"""))
        assertEquals(row("a", "today", """{"x":1}"""), dao.observe("a", "today").first())
    }

    @Test
    fun `the same account and key replaces the row`() = runTest {
        dao.put(row("a", "today", "1"))
        dao.put(row("a", "today", "2"))
        assertEquals("2", dao.observe("a", "today").first()?.json)
    }

    @Test
    fun `another account's row is invisible`() = runTest {
        dao.put(row("a", "today"))
        assertNull(dao.observe("b", "today").first())
    }

    @Test
    fun `clearAll empties the table`() = runTest {
        dao.put(row("a", "today"))
        dao.put(row("b", "day:2026-11-25"))
        dao.clearAll()
        assertEquals(emptyList<String>(), keys("a", "today") + keys("b", "day:2026-11-25"))
    }

    @Test
    fun `deleteWithPrefixExcept keeps exactly the listed keys, and only under the prefix and account`() = runTest {
        listOf("day:2026-11-24", "day:2026-11-25", "day:2026-11-26", "today", "dayz").forEach { dao.put(row("a", it)) }
        dao.put(row("b", "day:2026-11-24"))
        dao.deleteWithPrefixExcept("a", "day:", listOf("day:2026-11-25", "day:2026-11-26"))
        assertEquals(
            listOf("day:2026-11-25", "day:2026-11-26", "today", "dayz"),
            keys("a", "day:2026-11-24", "day:2026-11-25", "day:2026-11-26", "today", "dayz"),
        )
        assertEquals(listOf("day:2026-11-24"), keys("b", "day:2026-11-24"))
    }

    @Test
    fun `an empty keep list deletes every row under the prefix`() = runTest {
        dao.put(row("a", "day:2026-11-24"))
        dao.put(row("a", "today"))
        dao.deleteWithPrefixExcept("a", "day:", emptyList())
        assertEquals(listOf("today"), keys("a", "day:2026-11-24", "today"))
    }

    @Test
    fun `a prefix with LIKE wildcards is matched literally`() = runTest {
        dao.put(row("a", "d_y:1"))
        dao.put(row("a", "day:1"))
        dao.deleteWithPrefixExcept("a", "d_y:", emptyList())
        assertEquals(listOf("day:1"), keys("a", "d_y:1", "day:1"))
    }

    @Test
    fun `deleteIfUnchanged keeps a row rewritten since it was read`() = runTest {
        dao.put(row("a", "today", "old"))
        dao.put(row("a", "today", "new"))
        dao.deleteIfUnchanged("a", "today", "old")
        assertEquals("new", dao.observe("a", "today").first()?.json)
        dao.deleteIfUnchanged("a", "today", "new")
        assertNull(dao.observe("a", "today").first())
    }
}
