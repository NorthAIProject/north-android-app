package com.fernandocorreia.khepri.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A screen on a tab's back stack. Saved across process death, so it names only what the screen shows; one-shot asks
 * (start the workout, preset the mood, a draft for the coach) travel as [ScreenAsk] instead.
 */
@Serializable
public sealed interface Route : NavKey {
    @Serializable public data class TabRoot(val tab: AppTab) : Route

    /** One of More's sections by its web name. */
    @Serializable public data class Section(val id: String) : Route

    @Serializable public data class Decision(val id: String) : Route

    @Serializable public data class Goal(val id: String) : Route

    /**
     * One crew's space over Crews. [opening] is new for every opening, so the same crew opened again is a new screen.
     */
    @Serializable public data class CrewSpace(val crewId: String, val channelId: String?, val opening: String) : Route

    @Serializable public data class TrainingDay(val day: Int) : Route

    @Serializable public data class CoachThread(val conversationId: String) : FullScreen
}

/** A route that hides the tab bar. */
@Serializable public sealed interface FullScreen : Route

/** A route shown as a bottom sheet over the screen below it. Back and swipe-down dismiss it. */
@Serializable
public sealed interface Sheet : Route {
    @Serializable public data object Settings : Sheet

    @Serializable public data object CheckInFlow : Sheet

    @Serializable public data object WeeklyReview : Sheet

    @Serializable public data object Horizon : Sheet

    @Serializable public data object FirstWeekReview : Sheet

    @Serializable public data class Exercise(val slug: String) : Sheet

    @Serializable public data class Muscle(val id: String) : Sheet
}

/**
 * The key a [Route]'s entry is stored under, for its saved state and its ViewModels. Installers register entries with
 * [screen], which uses it, so `NavigationState` can clear exactly what a replaced entry left behind.
 */
public fun Route.contentKey(): String = toString()

/** Decided by the top entry that is not a sheet, so a sheet over a full-screen flow keeps the tab bar hidden. */
public fun List<Route>.hidesTabBar(): Boolean = lastOrNull { it !is Sheet } is FullScreen
