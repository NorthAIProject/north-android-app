package com.fernandocorreia.khepri.core.database.snapshot

import androidx.room3.Entity
import java.time.Instant

/** One cached server response, as the JSON it was decoded from, for one account. */
@Entity(tableName = "snapshots", primaryKeys = ["accountId", "key"])
public data class CachedSnapshot(val accountId: String, val key: String, val json: String, val fetchedAt: Instant)
