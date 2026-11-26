package com.fernandocorreia.khepri.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.fernandocorreia.khepri.core.database.snapshot.CachedSnapshot
import com.fernandocorreia.khepri.core.database.snapshot.SnapshotCache
import com.fernandocorreia.khepri.core.database.snapshot.StoredValue
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
    private val cache =
        SnapshotCache(database.snapshots(), Json { ignoreUnknownKeys = true }, Clock.fixed(now, ZoneOffset.UTC))

    @After fun close() = database.close()

    @Test
    fun `a value round-trips with the clock's time`() = runTest {
        cache.put("a", "today", Day("2026-11-26", 4200), Day.serializer())
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
    fun `keepOnly drops older days and touches no other prefix`() = runTest {
        val day = Day("x", 1)
        listOf("day:2026-11-24", "day:2026-11-25", "day:2026-11-26", "today").forEach {
            cache.put("a", it, day, Day.serializer())
        }
        cache.keepOnly("a", "day:", setOf("day:2026-11-26", "day:2026-11-25"))
        val left =
            listOf("day:2026-11-24", "day:2026-11-25", "day:2026-11-26", "today").filter {
                cache.observe("a", it, Day.serializer()).first() != null
            }
        assertEquals(listOf("day:2026-11-25", "day:2026-11-26", "today"), left)
    }
}
