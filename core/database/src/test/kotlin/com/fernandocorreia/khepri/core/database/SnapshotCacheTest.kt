package com.fernandocorreia.khepri.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.fernandocorreia.khepri.core.common.DispatcherProvider
import com.fernandocorreia.khepri.core.database.snapshot.CachedSnapshot
import com.fernandocorreia.khepri.core.database.snapshot.Prune
import com.fernandocorreia.khepri.core.database.snapshot.SnapshotCache
import com.fernandocorreia.khepri.core.database.snapshot.StoredValue
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotCacheTest {
    @Serializable data class Day(val date: String, val steps: Int)

    private val database = testDatabase()
    private val now = Instant.parse("2026-11-26T09:30:00.250Z")
    private val dispatchers =
        object : DispatcherProvider {
            override val main = Dispatchers.Default
            override val io = Dispatchers.Default
            override val default = Dispatchers.Default
        }
    private val cache =
        SnapshotCache(
            database,
            Json { ignoreUnknownKeys = true },
            Clock.fixed(now, ZoneOffset.UTC),
            dispatchers,
        )

    @After fun close() = database.close()

    @Test
    fun `a value round-trips with the clock's time`() = runTest {
        cache.putIf({ true }, "a", "today", Day("2026-11-26", 4200), Day.serializer())
        assertEquals(StoredValue(Day("2026-11-26", 4200), now), cache.observe("a", "today", Day.serializer()).first())
    }

    @Test
    fun `an undecodable row is observed as nothing and deleted`() = runTest {
        database.snapshots().put(CachedSnapshot("a", "today", """{"date":"2026-11-26"}""", now))
        cache.observe("a", "today", Day.serializer()).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(database.snapshots().observe("a", "today").first())
    }

    @Test
    fun `a write that prunes drops older days and touches no other prefix`() = runTest {
        val day = Day("x", 1)
        listOf("day:2026-11-24", "day:2026-11-25", "today").forEach {
            cache.putIf({ true }, "a", it, day, Day.serializer())
        }
        val keep = setOf("day:2026-11-26", "day:2026-11-25")
        cache.putIf({ true }, "a", "day:2026-11-26", day, Day.serializer(), Prune("day:", keep))
        val left =
            listOf("day:2026-11-24", "day:2026-11-25", "day:2026-11-26", "today").filter {
                cache.observe("a", it, Day.serializer()).first() != null
            }
        assertEquals(listOf("day:2026-11-25", "day:2026-11-26", "today"), left)
    }

    @Test
    fun `a write whose account is gone writes and prunes nothing`() = runTest {
        cache.putIf({ true }, "a", "day:2026-11-24", Day("x", 1), Day.serializer())
        val wrote =
            cache.putIf({ false }, "a", "day:2026-11-26", Day("x", 2), Day.serializer(), Prune("day:", emptySet()))
        assertEquals(false, wrote)
        assertNull(cache.observe("a", "day:2026-11-26", Day.serializer()).first())
        assertEquals(Day("x", 1), cache.observe("a", "day:2026-11-24", Day.serializer()).first()?.value)
    }

    @Test
    fun `an unreadable row is replaced by the next good write, which is shown`() = runTest {
        database.snapshots().put(CachedSnapshot("a", "today", "not json", now))
        cache.observe("a", "today", Day.serializer()).test {
            assertNull(awaitItem())
            cache.putIf({ true }, "a", "today", Day("2026-11-26", 10), Day.serializer())
            assertEquals(Day("2026-11-26", 10), awaitItem()?.value)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
