package com.fernandocorreia.khepri.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fernandocorreia.khepri.core.navigation.AppDestination
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PendingLinksStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private fun TestScope.store(name: String = "links") =
        PendingLinksStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "$name.preferences_pb") }
        )

    @Test
    fun `codes come back once, oldest first, without duplicates`() = runTest {
        val store = store()
        store.put(AppDestination.Invite("abc123xyz9"))
        store.put(AppDestination.CrewJoin("crew42"))
        store.put(AppDestination.Invite("abc123xyz9"))
        assertEquals(listOf(AppDestination.Invite("abc123xyz9"), AppDestination.CrewJoin("crew42")), store.take())
        assertEquals(emptyList<AppDestination>(), store.take())
    }

    @Test
    fun `other destinations are not kept`() = runTest {
        val store = store()
        store.put(AppDestination.Goal("g1"))
        assertEquals(emptyList<AppDestination>(), store.take())
    }
}
