package com.fernandocorreia.khepri.core.database.snapshot

import androidx.room3.withWriteTransaction
import com.fernandocorreia.khepri.core.common.DispatcherProvider
import com.fernandocorreia.khepri.core.common.WireJson
import com.fernandocorreia.khepri.core.database.KhepriDatabase
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** A decoded cached value and when it was fetched. */
public data class StoredValue<T>(val value: T, val fetchedAt: Instant)

/** After a write, delete the account's rows under [prefix] (`day:`) except [keep]; other prefixes are untouched. */
public data class Prune(val prefix: String, val keep: Set<String>)

/**
 * Server responses kept per account as the JSON they were decoded from, so a cold launch shows the last good answer. A
 * row this build cannot decode (an older shape, a corrupted write) is deleted and observed as no value.
 */
public class SnapshotCache
@Inject
constructor(
    private val database: KhepriDatabase,
    @WireJson private val json: Json,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) {
    /**
     * Emits the stored value and every change to it. Room re-queries on any write to the table, so unchanged rows are
     * dropped before decoding, and decoding runs off the collector's thread. An unreadable row and its deletion emit
     * one `null`, not two.
     */
    public fun <T> observe(accountId: String, key: String, serializer: KSerializer<T>): Flow<StoredValue<T>?> =
        dao.observe(accountId, key)
            .distinctUntilChanged()
            .map { row -> row?.let { decode(it, serializer) } }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)

    private val dao: SnapshotDao = database.snapshots()

    /**
     * Stores [value] as fetched now, then applies [prune], in one write transaction and only if [stillCurrent] holds
     * inside it (the account the value was fetched for is still signed in). Sign-out clears the tables in a write
     * transaction after the account is gone, so a write either lands first and is wiped, or is dropped. Returns whether
     * it wrote.
     */
    public suspend fun <T> putIf(
        stillCurrent: () -> Boolean,
        accountId: String,
        key: String,
        value: T,
        serializer: KSerializer<T>,
        prune: Prune? = null,
    ): Boolean {
        val encoded = withContext(dispatchers.default) { json.encodeToString(serializer, value) }
        return database.withWriteTransaction {
            if (!stillCurrent()) return@withWriteTransaction false
            dao.put(CachedSnapshot(accountId, key, encoded, clock.instant()))
            prune?.let { dao.deleteWithPrefixExcept(accountId, it.prefix, it.keep.toList()) }
            true
        }
    }

    private suspend fun <T> decode(row: CachedSnapshot, serializer: KSerializer<T>): StoredValue<T>? =
        try {
            StoredValue(json.decodeFromString(serializer, row.json), row.fetchedAt)
        } catch (expected: IllegalArgumentException) {
            // Unreadable (SerializationException is an IllegalArgumentException): the next fetch writes a fresh row.
            dao.deleteIfUnchanged(row.accountId, row.key, row.json)
            null
        }
}
