package com.fernandocorreia.khepri.core.navigation

import com.fernandocorreia.khepri.core.navigation.AppDestination.CheckIn
import com.fernandocorreia.khepri.core.navigation.AppDestination.CrewChannel
import com.fernandocorreia.khepri.core.navigation.AppDestination.CrewJoin
import com.fernandocorreia.khepri.core.navigation.AppDestination.Decision
import com.fernandocorreia.khepri.core.navigation.AppDestination.Exercise
import com.fernandocorreia.khepri.core.navigation.AppDestination.FirstWeekReview
import com.fernandocorreia.khepri.core.navigation.AppDestination.Goal
import com.fernandocorreia.khepri.core.navigation.AppDestination.Horizon
import com.fernandocorreia.khepri.core.navigation.AppDestination.Invite
import com.fernandocorreia.khepri.core.navigation.AppDestination.NextWorkout
import com.fernandocorreia.khepri.core.navigation.AppDestination.Section
import com.fernandocorreia.khepri.core.navigation.AppDestination.Settings
import com.fernandocorreia.khepri.core.navigation.AppDestination.StartWorkout
import com.fernandocorreia.khepri.core.navigation.AppDestination.Tab
import com.fernandocorreia.khepri.core.navigation.AppDestination.TrainingDay
import com.fernandocorreia.khepri.core.navigation.AppDestination.WeeklyReview
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * The iOS router table (`khepriTests/ShellTests.swift` `mapsLinksToDestinations`, `leavesForeignLinksAlone`,
 * `crewChannelLinkRoutes`, `crewLinkRoutes`, `eachButtonOpensTodaysCheckInWithItsMood`) first, then the plan's rows and
 * the parser traps. Where they disagree, the iOS test wins.
 */
class DeepLinkParserTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("links")
    fun `maps links to destinations`(link: String, expected: AppDestination?) {
        assertEquals(expected, DeepLinkParser.parse(link))
    }

    companion object {
        private const val UPPER = "5A5A5A5A-1111-2222-3333-444444444444"
        private const val LOWER = "5a5a5a5a-1111-2222-3333-444444444444"
        private const val CREW = "66666666-6666-6666-6666-666666666666"
        private const val CHANNEL = "77777777-7777-7777-7777-777777777777"

        private fun row(link: String, expected: AppDestination?) = Arguments.of(link, expected)

        @JvmStatic
        fun links(): List<Arguments> =
            ios() + crews() + plan() + traps() + (1..5).map { row("khepri://check-ins?mood=$it", CheckIn(it)) }

        private fun ios() =
            listOf(
                row("khepri://today", Tab(AppTab.Today)),
                row("khepri://coach", Tab(AppTab.Coach)),
                row("khepri://settings", Settings),
                row("/app/check-ins", Section("check-ins")),
                row("khepri://care", Section("care")),
                row("khepri://more", Tab(AppTab.More)),
                row("khepri://training/next", NextWorkout(start = false)),
                row("khepri://training/next/start", NextWorkout(start = true)),
                row("/app/chat/33333333-3333-3333-3333-333333333333", Tab(AppTab.Coach)),
                row("https://kheprios.com/app/training/abc", Tab(AppTab.Training)),
                row("khepri://training/plan-1/2", TrainingDay(2)),
                row("khepri://training/plan-1/2/start", StartWorkout(2)),
                row(
                    "/app/decisions/44444444-4444-4444-4444-444444444444",
                    Decision("44444444-4444-4444-4444-444444444444"),
                ),
                row("/app/decisions/not-an-id", Section("decisions")),
                row("https://kheprios.com/i/k7m2p9qx4r", Invite("k7m2p9qx4r")),
                row("khepri://i/K7M2P9QX4R", Invite("k7m2p9qx4r")),
                row("khepri://friends", Section("friends")),
                row("khepri://weekly", WeeklyReview),
                row("/app/weekly", WeeklyReview),
                row("/app/first-week/review", FirstWeekReview),
                row("https://kheprios.com/app/first-week/review", FirstWeekReview),
                row("/app/first-week", Tab(AppTab.Today)),
                row("https://kheprios.com/i/c/q8w3n5k2ht", CrewJoin("q8w3n5k2ht")),
                row("khepri://crews", Section("crews")),
                row("khepri://exercises/push-up", Exercise("push-up")),
                row("/app/exercises/goblet-squat", Exercise("goblet-squat")),
                row("khepri://exercises", Tab(AppTab.Training)),
                row(
                    "khepri://goals/55555555-5555-5555-5555-555555555555",
                    Goal("55555555-5555-5555-5555-555555555555"),
                ),
                row("khepri://goals/not-an-id", Section("goals")),
                row("khepri://check-ins?mood=4", CheckIn(4)),
                row("/app/check-ins?mood=2", CheckIn(2)),
                row("khepri://check-ins?mood=9", Section("check-ins")),
                row("/app/daily", Section("daily")),
                row("khepri://daily", Section("daily")),
                row("/app/progress", Section("progress-check")),
                row("https://kheprios.com/app/progress", Section("progress-check")),
                row("khepri://progress", Tab(AppTab.Progress)),
                row("khepri://progress-check", Section("progress-check")),
                row("/app/insights", Tab(AppTab.Progress)),
                row("https://example.com/app/chat", null),
                row("khepri://nowhere", null),
            )

        private fun crews() =
            listOf(
                row("/app/crews/$CREW/c/$CHANNEL", CrewChannel(CREW, CHANNEL)),
                row("khepri://crews/$CREW/c/$CHANNEL", CrewChannel(CREW, CHANNEL)),
                row("https://kheprios.com/app/crews/$CREW", CrewChannel(CREW, null)),
                row("/app/crews/not-an-id", Section("crews")),
                row("/app/crews/$CREW/c/nope", Section("crews")),
                row("/app/crews/$UPPER", CrewChannel(LOWER, null)),
                row("/app/crews/$UPPER/c/$UPPER", CrewChannel(LOWER, LOWER)),
                row("/app/crews/$UPPER/x/$UPPER", Section("crews")),
            )

        private fun plan() =
            listOf(
                row("/app", Tab(AppTab.Today)),
                row("/app/today", Tab(AppTab.Today)),
                row("khepri://chat", Tab(AppTab.Coach)),
                row("/app/coach/abc", Tab(AppTab.Coach)),
                row("khepri://training", Tab(AppTab.Training)),
                row("/app/workouts", Tab(AppTab.Training)),
                row("/app/activity", Tab(AppTab.Training)),
                row("khepri://training/plan1/3", TrainingDay(3)),
                row("khepri://training/plan1/3/start", StartWorkout(3)),
                row("khepri://training/plan1/x", Tab(AppTab.Training)),
                row("/app/fitness", Tab(AppTab.Progress)),
                row("khepri://check-ins", Section("check-ins")),
                row("khepri://decisions/$UPPER", Decision(UPPER)),
                row("khepri://decisions/not-a-uuid", Section("decisions")),
                row("khepri://goals/$UPPER", Goal(UPPER)),
                row("https://kheprios.com/i/AbC123xYz9", Invite("abc123xyz9")),
                row("https://kheprios.com/i/short", null),
                row("https://kheprios.com/i/c/AbC123xYz9", CrewJoin("abc123xyz9")),
                row("khepri://horizon", Horizon),
                row("khepri://nonsense", null),
                row("https://example.com/app/today", null),
                row("mailto:x@y.z", null),
            )

        private fun traps() =
            listOf(
                row("khepri://goals/1-1-1-1-1", Section("goals")),
                row("khepri://decisions/$UPPER/extra", Section("decisions")),
                row("khepri://check-ins?mood=0", Section("check-ins")),
                row("khepri://check-ins?mood=", Section("check-ins")),
                row("khepri://check-ins?other=1&mood=3", CheckIn(3)),
                row("khepri://training/next/start/now", NextWorkout(start = false)),
                row("khepri://i/c/AbC123xYz", null),
                row("khepri://i/AbC123-Yz9", null),
                row("https://kheprios.com", Tab(AppTab.Today)),
                row("https://kheprios.com/progress", Section("progress-check")),
                row("khepri://progress/x", Tab(AppTab.Progress)),
                row("not a link", null),
                row("khepri:today", null),
                row("", null),
                row("   ", null),
            )
    }
}
