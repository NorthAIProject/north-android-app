package com.fernandocorreia.khepri.feature.today.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * One day from `GET /day`: what iOS `DayViews` cards read in Phase 2. Blood pressure, sleep stages and blocks are left
 * out: they are never cached, and the sheets that show them read a live response.
 */
data class Day(
    val date: LocalDate,
    val isToday: Boolean,
    val now: Instant,
    val vitals: Vitals,
    val food: Food,
    val water: Water,
    val activity: Activity,
    val workouts: Workouts,
    val body: Body,
    val streak: Int,
    val level: Int,
    val caffeine: Caffeine,
    val nutrients: Nutrients,
    val sleep: DaySleep?,
    val fast: DayFast?,
    val timeline: List<TimelineEntry>,
    val checkIn: CheckInTile?,
)

data class Vitals(val energyPercent: Int?, val daylightMinutes: Int?, val screenMinutes: Int?)

data class Food(val calories: Double, val proteinG: Double, val carbG: Double, val fatG: Double, val goal: MacroGoal?)

data class MacroGoal(val calories: Double, val proteinG: Double, val carbG: Double, val fatG: Double)

data class Water(val totalMl: Int, val targetMl: Int)

data class Activity(val move: Ring, val exercise: Ring, val stand: Ring)

data class Ring(val value: Double, val goal: Double, val percent: Int)

data class Workouts(val count: Int, val minutes: Int, val calories: Double, val labels: List<String>)

data class Body(
    val weightKg: Double?,
    val heightCm: Double?,
    val bmi: Double?,
    val bmiCategory: BmiCategory?,
    val targetWeightKg: Double?,
    val toGoalKg: Double?,
)

/** [Unknown] is a category this build does not know yet. */
enum class BmiCategory {
    Underweight,
    Healthy,
    Overweight,
    Obese,
    Unknown,
}

data class Caffeine(val totalMg: Int, val activeMg: Int, val limitMg: Int, val afterCutoff: Boolean)

data class Nutrients(val covered: List<String>, val missing: List<String>, val total: Int)

data class DaySleep(
    val totalMinutes: Int,
    val source: String,
    val start: Instant?,
    val end: Instant?,
    val quality: Int?,
)

data class DayFast(
    val startedAt: Instant,
    val targetHours: Int,
    val elapsedMinutes: Int,
    val phase: FastPhase,
    val fraction: Double,
    val endedAt: Instant?,
)

/** [Unknown] is a phase this build does not know yet. */
enum class FastPhase {
    Fed,
    Fasting,
    FatBurning,
    Ketosis,
    Unknown,
}

/** A logged moment. [kind] is open: a kind this build does not know is shown with its [icon] and [title]. */
data class TimelineEntry(
    val kind: String,
    val at: Instant,
    val title: String,
    val icon: String,
    val detail: String?,
    val href: String?,
)

/** The check-in streak tile, with today's answer when there is one. */
data class CheckInTile(val streak: Int, val today: CheckInMoment?)

data class CheckInMoment(val mood: Int, val energy: Int, val updatedAt: Instant)
