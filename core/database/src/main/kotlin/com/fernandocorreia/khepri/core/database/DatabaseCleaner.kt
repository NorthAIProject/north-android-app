package com.fernandocorreia.khepri.core.database

import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignOutReason
import javax.inject.Inject

/** Empties every table at sign-out, whatever the reason: the database holds only account data. */
public class DatabaseCleaner @Inject constructor(private val database: KhepriDatabase) : SignOutCleaner {
    override suspend fun afterLogout(reason: SignOutReason) {
        database.clearAllTables()
    }
}
