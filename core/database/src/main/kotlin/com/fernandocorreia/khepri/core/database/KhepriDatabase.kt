package com.fernandocorreia.khepri.core.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.fernandocorreia.khepri.core.database.snapshot.CachedSnapshot
import com.fernandocorreia.khepri.core.database.snapshot.SnapshotDao

/** The app's one database, `khepri.db`. Every table holds account data and is emptied at sign-out. */
@Database(entities = [CachedSnapshot::class], version = 1, exportSchema = true)
@ColumnTypeConverters(InstantColumns::class)
public abstract class KhepriDatabase : RoomDatabase() {
    public abstract fun snapshots(): SnapshotDao

    public companion object {
        public const val FILE_NAME: String = "khepri.db"
    }
}
