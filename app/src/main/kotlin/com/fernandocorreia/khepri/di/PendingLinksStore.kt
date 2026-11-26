package com.fernandocorreia.khepri.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fernandocorreia.khepri.core.navigation.AppDestination
import com.fernandocorreia.khepri.core.navigation.PendingLinks
import java.io.IOException

/**
 * Invite and crew-join codes from links, kept on disk until a screen redeems them, so a code opened while signed out
 * survives sign-up and a process kill. Each code is kept once, oldest first. Other destinations are not kept.
 */
class PendingLinksStore(private val dataStore: DataStore<Preferences>) : PendingLinks {
    override suspend fun put(destination: AppDestination) {
        val line = destination.line() ?: return
        dataStore.edit { prefs ->
            val lines = prefs[key].lines()
            if (line !in lines) prefs[key] = (lines + line).joinToString("\n")
        }
    }

    /** An unreadable file gives nothing: a lost invite code is better than a crash at sign-in. */
    override suspend fun take(): List<AppDestination> {
        var taken = emptyList<AppDestination>()
        try {
            dataStore.edit { prefs ->
                taken = prefs[key].lines().mapNotNull(::destination)
                prefs.remove(key)
            }
        } catch (expected: IOException) {
            return emptyList()
        }
        return taken
    }

    private fun String?.lines(): List<String> = this?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

    private fun AppDestination.line(): String? =
        when (this) {
            is AppDestination.Invite -> "$INVITE$code"
            is AppDestination.CrewJoin -> "$CREW$code"
            else -> null
        }

    private fun destination(line: String): AppDestination? =
        when {
            line.startsWith(INVITE) -> AppDestination.Invite(line.removePrefix(INVITE))
            line.startsWith(CREW) -> AppDestination.CrewJoin(line.removePrefix(CREW))
            else -> null
        }

    private companion object {
        val key = stringPreferencesKey("pending_links")
        const val INVITE = "invite:"
        const val CREW = "crew:"
    }
}
