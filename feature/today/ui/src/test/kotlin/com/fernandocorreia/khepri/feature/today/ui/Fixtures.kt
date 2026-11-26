package com.fernandocorreia.khepri.feature.today.ui

import com.fernandocorreia.khepri.feature.today.domain.model.Activity
import com.fernandocorreia.khepri.feature.today.domain.model.Body
import com.fernandocorreia.khepri.feature.today.domain.model.Caffeine
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.Food
import com.fernandocorreia.khepri.feature.today.domain.model.Hydration
import com.fernandocorreia.khepri.feature.today.domain.model.NextStep
import com.fernandocorreia.khepri.feature.today.domain.model.Nutrients
import com.fernandocorreia.khepri.feature.today.domain.model.Ring
import com.fernandocorreia.khepri.feature.today.domain.model.SleepSummary
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.model.TodayCheckIn
import com.fernandocorreia.khepri.feature.today.domain.model.Vitals
import com.fernandocorreia.khepri.feature.today.domain.model.Water
import com.fernandocorreia.khepri.feature.today.domain.model.Workouts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 2026-11-26 09:30 UTC, a Thursday. */
val NOW: Instant = Instant.parse("2026-11-26T09:30:00Z")
val ZONE: ZoneOffset = ZoneOffset.UTC
val TODAY: LocalDate = LocalDate.of(2026, 11, 26)

val today =
    Today(
        briefing = "A steady day: you slept well, so the planned session stays as it is.",
        streak = 12,
        checkedInToday = true,
        checkIn = TodayCheckIn(mood = 4, energy = 3, stress = null, sleepQuality = null, at = NOW),
        hydration = Hydration(todayMl = 1_250, targetMl = 2_500, percent = 50),
        sleep = SleepSummary(logged = true, durationMinutes = 445, quality = 4),
        nextStep =
            NextStep(
                kind = "workout",
                eyebrow = "Next step",
                title = "Upper body, 45 min",
                body = "Push day: bench, rows and presses.",
                cta = "Start workout",
                href = "/app/training/next",
            ),
    )

/** A new account: nothing logged yet. */
val emptyToday =
    Today(
        briefing = null,
        streak = 0,
        checkedInToday = false,
        checkIn = null,
        hydration = Hydration(todayMl = 0, targetMl = 2_500, percent = 0),
        sleep = SleepSummary(logged = false, durationMinutes = 0, quality = null),
        nextStep =
            NextStep(
                kind = "check_in",
                eyebrow = "Start here",
                title = "How are you today?",
                body = "Two taps: mood and energy.",
                cta = "Check in",
                href = "/app/check-ins",
            ),
    )

fun day(date: LocalDate = TODAY): Day =
    Day(
        date = date,
        isToday = date == TODAY,
        now = NOW,
        vitals = Vitals(energyPercent = null, daylightMinutes = null, screenMinutes = null),
        food = Food(calories = 0.0, proteinG = 0.0, carbG = 0.0, fatG = 0.0, goal = null),
        water = Water(totalMl = 0, targetMl = 2_500),
        activity = Activity(Ring(0.0, 500.0, 0), Ring(0.0, 30.0, 0), Ring(0.0, 12.0, 0)),
        workouts = Workouts(count = 0, minutes = 0, calories = 0.0, labels = emptyList()),
        body = Body(null, null, null, null, null, null),
        streak = 12,
        level = 1,
        caffeine = Caffeine(totalMg = 0, activeMg = 0, limitMg = 400, afterCutoff = false),
        nutrients = Nutrients(covered = emptyList(), missing = emptyList(), total = 0),
        sleep = null,
        fast = null,
        timeline = emptyList(),
        checkIn = null,
    )
