package com.fernandocorreia.khepri.spike.nav3

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
enum class Tab { Today, Coach, Training, Progress, More }

@Serializable sealed interface Route : NavKey

@Serializable data class TabRoot(val tab: Tab) : Route

@Serializable data class Detail(val tab: Tab, val depth: Int) : Route

@Serializable data object Goals : Route

@Serializable data class Goal(val id: String) : Route

/** Shown as a bottom sheet over the current tab. */
@Serializable data object SettingsSheet : Route

/** Full-screen flow: the tab bar is hidden. */
@Serializable data object Workout : Route

/** Coach thread: the tab bar is hidden. */
@Serializable data class Thread(val id: String) : Route

fun Route.isSheet(): Boolean = this is SettingsSheet

/** Decided by the top entry that is not a sheet, so a sheet over a full-screen flow keeps the tab bar hidden. */
fun List<Route>.hidesTabBar(): Boolean = lastOrNull { !it.isSheet() }.let { it is Workout || it is Thread }

/**
 * Parses the spike's deep links into a tab and the stack to put on it, parents first.
 * `khepri://goals/<id>` → More: [root, Goals, Goal(id)]; `khepri://more` → More root; `khepri://coach/<id>` → Coach thread.
 */
fun parseLink(uri: String): Pair<Tab, List<Route>>? {
    val parts = uri.removePrefix("khepri://").trim('/').split('/').filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() || parts == listOf("today") -> Tab.Today to listOf(TabRoot(Tab.Today))
        parts == listOf("more") -> Tab.More to listOf(TabRoot(Tab.More))
        parts.size == 2 && parts[0] == "goals" -> Tab.More to listOf(TabRoot(Tab.More), Goals, Goal(parts[1]))
        parts.size == 2 && parts[0] == "coach" -> Tab.Coach to listOf(TabRoot(Tab.Coach), Thread(parts[1]))
        parts == listOf("settings") -> Tab.More to listOf(TabRoot(Tab.More), SettingsSheet)
        else -> null
    }
}
