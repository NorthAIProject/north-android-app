package com.fernandocorreia.khepri.core.navigation

/**
 * A place in the app something can link to: a notification, a widget, a `khepri://` URL, or a web path the server sent.
 * A one-to-one port of the iOS enum; `Navigator.open` decides where each one lands.
 */
public sealed interface AppDestination {
    public data class Tab(val tab: AppTab) : AppDestination

    public data object Settings : AppDestination

    /** A day of the plan; the plan id in the link is informational. */
    public data class TrainingDay(val day: Int) : AppDestination

    /** A day of the plan with its workout started: the reminder's Start button. */
    public data class StartWorkout(val day: Int) : AppDestination

    /** The plan's next session, resolved once the plan loads: a widget or a shortcut that cannot know the day. */
    public data class NextWorkout(val start: Boolean) : AppDestination

    /** One of More's sections by its web name, e.g. `care`, `check-ins`. */
    public data class Section(val id: String) : AppDestination

    public data class Decision(val id: String) : AppDestination

    public data class Goal(val id: String) : AppDestination

    /** Today's check-in with its mood already chosen (1 to 5): a button under a check-in nudge. */
    public data class CheckIn(val mood: Int) : AppDestination

    /** The check-in flow as a sheet on the current tab. */
    public data object CheckInFlow : AppDestination

    /** Somebody's invite link (`/i/<code>`): kept, redeemed once there is a session, then Friends opens. */
    public data class Invite(val code: String) : AppDestination

    /** A crew's join link (`/i/c/<code>`): kept, joined once there is a session, then the crew opens. */
    public data class CrewJoin(val code: String) : AppDestination

    /** One crew's space, on one channel or its default one. Ids are lower-cased. */
    public data class CrewChannel(val crewId: String, val channelId: String?) : AppDestination

    public data class Exercise(val slug: String) : AppDestination

    /** The exercises that train one muscle region. */
    public data class Muscle(val id: String) : AppDestination

    public data object WeeklyReview : AppDestination

    public data object Horizon : AppDestination

    /** The first week's day-7 review. Opening it closes the week. */
    public data object FirstWeekReview : AppDestination

    /** A new conversation with [draft] waiting in the composer, unsent. */
    public data class AskCoach(val draft: String) : AppDestination

    /** A conversation that already exists. iOS passes the summary; the thread here loads its own header. */
    public data class Conversation(val id: String) : AppDestination
}
