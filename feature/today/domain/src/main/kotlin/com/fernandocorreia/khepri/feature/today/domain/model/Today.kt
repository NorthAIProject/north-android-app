package com.fernandocorreia.khepri.feature.today.domain.model

import java.time.Instant

/** What the Today screen shows from `GET /today`: iOS `TodayView`'s header, stat grid and next step. */
data class Today(
    val briefing: String?,
    val streak: Int,
    val checkedInToday: Boolean,
    val checkIn: TodayCheckIn?,
    val hydration: Hydration,
    val sleep: SleepSummary,
    val nextStep: NextStep?,
)

/** Today's check-in, when there is one. Scores are 1 to 5. */
data class TodayCheckIn(val mood: Int, val energy: Int, val stress: Int?, val sleepQuality: Int?, val at: Instant)

data class Hydration(val todayMl: Int, val targetMl: Int, val percent: Int)

data class SleepSummary(val logged: Boolean, val durationMinutes: Int, val quality: Int?)

/** The server's suggestion for what to do next. [href] is a web path the app maps to a screen. */
data class NextStep(
    val kind: String,
    val eyebrow: String,
    val title: String,
    val body: String,
    val cta: String,
    val href: String,
)
