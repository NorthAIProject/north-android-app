package com.fernandocorreia.khepri.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.database.snapshot.CachedSnapshot
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseCleanerTest {
    private val database = testDatabase()
    private val cleaner = DatabaseCleaner(database)

    @After fun close() = database.close()

    private suspend fun signOutLeavesNothing(reason: SignOutReason) {
        database.snapshots().put(CachedSnapshot("a", "today", "{}", Instant.EPOCH))
        cleaner.afterLogout(reason)
        assertNull(database.snapshots().observe("a", "today").first())
    }

    @Test fun `an explicit sign-out empties the database`() = runTest { signOutLeavesNothing(SignOutReason.Explicit) }

    @Test
    fun `a sign-out from a rejected token empties it too`() = runTest {
        signOutLeavesNothing(SignOutReason.Unauthorized)
    }
}
