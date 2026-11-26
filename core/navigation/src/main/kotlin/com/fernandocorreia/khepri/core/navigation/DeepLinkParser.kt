package com.fernandocorreia.khepri.core.navigation

import java.net.URI

/**
 * Maps `khepri://…` links, `https://kheprios.com/app/…` URLs and bare server paths onto a destination, line by line
 * after iOS `AppRouter.destination(for:)`. Returns null for links this build does not understand, so the caller can
 * open the browser instead.
 */
public object DeepLinkParser {
    /** More's sections, by the path the web uses for them. */
    public val sections: Set<String> =
        setOf(
            "goals",
            "check-ins",
            "progress-check",
            "reports",
            "memories",
            "knowledge",
            "nutrition",
            "care",
            "mind",
            "decisions",
            "friends",
            "crews",
            "inbox",
            "daily",
            "awards",
        )

    private const val SCHEME = "khepri"
    private const val WEB_HOST = "kheprios.com"
    private const val PROGRESS_CHECK = "progress-check"
    private val moods = 1..5

    public fun parse(link: String): AppDestination? {
        val uri = link.takeIf { it.isNotBlank() }?.let { runCatching { URI(it) }.getOrNull() }
        val parts = uri?.let(::parts)
        val mood = uri?.let(::mood)?.takeIf { it in moods }
        return when {
            uri == null || parts == null -> null
            uri.scheme != SCHEME && parts.firstOrNull() == "progress" -> AppDestination.Section(PROGRESS_CHECK)
            parts == listOf("check-ins") && mood != null -> AppDestination.CheckIn(mood)
            else -> PathRules.forPath(parts)
        }
    }

    /**
     * The path as segments. For `khepri://` the host is the first segment; for a web URL or a bare path, leading `app`
     * segments are dropped. Anything else (another host, `mailto:`) is not ours.
     */
    private fun parts(uri: URI): List<String>? {
        val segments = uri.path?.split('/')?.filter { it.isNotEmpty() }
        return when {
            uri.isOpaque || segments == null -> null
            uri.scheme == SCHEME -> listOfNotNull(uri.authority) + segments
            uri.scheme == null || uri.host == WEB_HOST -> segments.dropWhile { it == "app" }
            else -> null
        }
    }

    private fun mood(uri: URI): Int? =
        uri.query
            ?.split('&')
            ?.firstOrNull { it.substringBefore('=') == "mood" }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.toIntOrNull()
}

/** The path rules of iOS `destination(forPath:)`, in its order. Indices name path segments. */
private object PathRules {
    private const val THIRD = 2
    private const val FOURTH = 3
    private const val CODE_LENGTH = 10
    private val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    fun forPath(parts: List<String>): AppDestination? =
        training(parts)
            ?: record(parts)
            ?: crew(parts)
            ?: invite(parts)
            ?: firstWeek(parts)
            ?: exercise(parts)
            ?: byFirst(parts.firstOrNull())

    private fun training(parts: List<String>): AppDestination? {
        val day = parts.getOrNull(THIRD)?.toIntOrNull()
        return when {
            parts.firstOrNull() != "training" || parts.size < 2 -> null
            parts[1] == "next" ->
                AppDestination.NextWorkout(start = parts.lastIndex == THIRD && parts[THIRD] == "start")
            day == null -> null
            parts.lastIndex == FOURTH && parts[FOURTH] == "start" -> AppDestination.StartWorkout(day)
            parts.lastIndex == THIRD -> AppDestination.TrainingDay(day)
            else -> null
        }
    }

    private fun record(parts: List<String>): AppDestination? {
        val id = parts.getOrNull(1)?.takeIf { parts.size == 2 && isUuid(it) }
        return when {
            id == null -> null
            parts[0] == "decisions" -> AppDestination.Decision(id)
            parts[0] == "goals" -> AppDestination.Goal(id)
            else -> null
        }
    }

    /** `crews/<crew>` or `crews/<crew>/c/<channel>` with real ids. Anything else under `crews` is the Crews list. */
    private fun crew(parts: List<String>): AppDestination? {
        val crewId = parts.getOrNull(1)?.takeIf { parts[0] == "crews" && isUuid(it) }?.lowercase()
        val channelId = parts.getOrNull(FOURTH)?.takeIf { parts[THIRD] == "c" && isUuid(it) }?.lowercase()
        return when {
            crewId == null -> null
            parts.size == 2 -> AppDestination.CrewChannel(crewId, null)
            parts.lastIndex == FOURTH -> channelId?.let { AppDestination.CrewChannel(crewId, it) }
            else -> null
        }
    }

    private fun invite(parts: List<String>): AppDestination? =
        when {
            parts.firstOrNull() != "i" -> null
            parts.lastIndex == THIRD && parts[1] == "c" -> code(parts[THIRD])?.let(AppDestination::CrewJoin)
            parts.size == 2 -> code(parts[1])?.let(AppDestination::Invite)
            else -> null
        }

    /** `/app/first-week/review` is where the day-7 coach message leads; `/app/first-week` alone is a card on Today. */
    private fun firstWeek(parts: List<String>): AppDestination? =
        if (parts.firstOrNull() != "first-week") {
            null
        } else if (parts.size == 2 && parts[1] == "review") {
            AppDestination.FirstWeekReview
        } else {
            AppDestination.Tab(AppTab.Today)
        }

    private fun exercise(parts: List<String>): AppDestination? =
        parts.getOrNull(1)?.takeIf { parts.size == 2 && parts[0] == "exercises" }?.let(AppDestination::Exercise)

    private fun byFirst(first: String?): AppDestination? =
        when (first) {
            null,
            "today" -> AppDestination.Tab(AppTab.Today)
            "chat",
            "coach" -> AppDestination.Tab(AppTab.Coach)
            "training",
            "workouts",
            "exercises",
            "activity" -> AppDestination.Tab(AppTab.Training)
            "insights",
            "fitness",
            "progress" -> AppDestination.Tab(AppTab.Progress)
            "settings" -> AppDestination.Settings
            "weekly" -> AppDestination.WeeklyReview
            "horizon" -> AppDestination.Horizon
            "more" -> AppDestination.Tab(AppTab.More)
            else -> first?.takeIf(DeepLinkParser.sections::contains)?.let(AppDestination::Section)
        }

    private fun code(raw: String): String? =
        raw.takeIf { it.length == CODE_LENGTH && it.all(Char::isLetterOrDigit) }?.lowercase()

    /** Strict 8-4-4-4-12 hex, as Foundation's `UUID(uuidString:)`; `java.util.UUID.fromString` accepts `1-1-1-1-1`. */
    private fun isUuid(text: String): Boolean = uuid.matches(text)
}
