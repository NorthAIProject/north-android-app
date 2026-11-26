package com.fernandocorreia.khepri.core.database.snapshot

import com.fernandocorreia.khepri.core.common.WireJson
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** A decoded cached value and when it was fetched. */
public data class StoredValue<T>(val value: T, val fetchedAt: Instant)

/**
 * Server responses kept per account as the JSON they were decoded from, so a cold launch shows the last good answer. A
 * row this build cannot decode (an older shape, a corrupted write) is deleted and observed as no value.
 */
public class SnapshotCache
@Inject
constructor(private val dao: SnapshotDao, @WireJson private val json: Json, private val clock: Clock) {
    public fun <T> observe(accountId: String, key: String, serializer: KSerializer<T>): Flow<StoredValue<T>?> =
        dao.observe(accountId, key).map { row -> row?.let { decode(it, serializer) } }

    /** Stores [value] as fetched now. */
    public suspend fun <T> put(accountId: String, key: String, value: T, serializer: KSerializer<T>) {
        dao.put(CachedSnapshot(accountId, key, json.encodeToString(serializer, value), clock.instant()))
    }

    /** Deletes the account's rows under [prefix] (`day:`) except [keys]; rows under other prefixes are untouched. */
    public suspend fun keepOnly(accountId: String, prefix: String, keys: Set<String>) {
        dao.deleteWithPrefixExcept(accountId, prefix, keys.toList())
    }

    private suspend fun <T> decode(row: CachedSnapshot, serializer: KSerializer<T>): StoredValue<T>? =
        try {
            StoredValue(json.decodeFromString(serializer, row.json), row.fetchedAt)
        } catch (expected: IllegalArgumentException) {
            // Unreadable (SerializationException is an IllegalArgumentException): the next fetch writes a fresh row.
            dao.deleteKey(row.accountId, row.key)
            null
        }
}
