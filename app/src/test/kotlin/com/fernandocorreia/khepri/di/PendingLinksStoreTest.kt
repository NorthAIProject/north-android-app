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
    fun `one code of each kind comes back once, the newest`() = runTest {
        val store = store()
        store.put(AppDestination.Invite("abc123xyz9"))
        store.put(AppDestination.CrewJoin("crew42"))
        store.put(AppDestination.Invite("zzz999aaa1"))
        repeat(50) { store.put(AppDestination.Invite("flood$it")) }
        assertEquals(listOf(AppDestination.CrewJoin("crew42"), AppDestination.Invite("flood49")), store.take())
        assertEquals(emptyList<AppDestination>(), store.take())
    }

    @Test
    fun `other destinations are not kept`() = runTest {
        val store = store()
        store.put(AppDestination.Goal("g1"))
        assertEquals(emptyList<AppDestination>(), store.take())
    }
}
