package com.fernandocorreia.khepri.core.navigation

/** What `Navigator` queues for `NavigationState`. */
public sealed interface NavCommand {
    public data class Open(val destination: AppDestination) : NavCommand

    public data class Push(val route: Route) : NavCommand

    /** Back inside the selected tab; does nothing at its root. */
    public data object Pop : NavCommand

    public data object PopToRoot : NavCommand

    /** A tab bar tap. On the selected tab it pops to the root and asks the root to reload. */
    public data class Select(val tab: AppTab) : NavCommand
}

/**
 * A one-shot ask for a screen that a command put on a stack, as the iOS router's flags. The screen consumes it with
 * `NavigationState.consume`. Asks are not saved across process death.
 */
public sealed interface ScreenAsk {
    /** For the `TrainingDay` on top of Training: start its workout. */
    public data object StartWorkout : ScreenAsk

    /** For Training's root: open the plan's next session once the plan loads, and start it when [start]. */
    public data class NextWorkout(val start: Boolean) : ScreenAsk

    /** For the check-ins section: today's check-in with [mood] chosen. */
    public data class CheckInMood(val mood: Int) : ScreenAsk

    /** For Coach's root: a new conversation with [draft] in the composer. */
    public data class CoachDraft(val draft: String) : ScreenAsk

    /** For Friends: an invite link was kept in `PendingLinks`; redeem it. */
    public data object RedeemInvite : ScreenAsk

    /** For More's root: a crew join link was kept in `PendingLinks`; join it and open the crew. */
    public data object JoinCrew : ScreenAsk

    /** For the open space of [crewId]: move to [channelId] (its default when null) and to Chat. */
    public data class SwitchChannel(val crewId: String, val channelId: String?) : ScreenAsk

    /** For [tab]'s root: the tab was re-selected; reload. */
    public data class ReloadRoot(val tab: AppTab) : ScreenAsk
}

/** A [ScreenAsk] with an id, so the same ask twice is two asks. */
public data class ScreenRequest(val id: Long, val ask: ScreenAsk)
