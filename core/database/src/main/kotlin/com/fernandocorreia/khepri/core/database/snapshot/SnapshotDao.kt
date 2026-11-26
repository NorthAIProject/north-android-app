package com.fernandocorreia.khepri.core.database.snapshot

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
public interface SnapshotDao {
    @Query("SELECT * FROM snapshots WHERE accountId = :accountId AND `key` = :key")
    public fun observe(accountId: String, key: String): Flow<CachedSnapshot?>

    @Upsert public suspend fun put(snapshot: CachedSnapshot)

    /** Deletes the row only if it still holds [json]: a row written since it was read is kept. */
    @Query("DELETE FROM snapshots WHERE accountId = :accountId AND `key` = :key AND json = :json")
    public suspend fun deleteIfUnchanged(accountId: String, key: String, json: String)

    /** Deletes the account's rows whose key starts with [prefix], except [keep]. */
    @Query(
        "DELETE FROM snapshots WHERE accountId = :accountId AND substr(`key`, 1, length(:prefix)) = :prefix " +
            "AND `key` NOT IN (:keep)"
    )
    public suspend fun deleteWithPrefixExcept(accountId: String, prefix: String, keep: List<String>)

    @Query("DELETE FROM snapshots") public suspend fun clearAll()
}
