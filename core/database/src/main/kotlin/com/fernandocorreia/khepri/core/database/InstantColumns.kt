package com.fernandocorreia.khepri.core.database

import androidx.room3.ColumnTypeConverter
import java.time.Instant

/** Instants are stored as epoch milliseconds, the precision the server sends. */
internal object InstantColumns {
    @ColumnTypeConverter fun toMillis(instant: Instant): Long = instant.toEpochMilli()

    @ColumnTypeConverter fun fromMillis(millis: Long): Instant = Instant.ofEpochMilli(millis)
}
