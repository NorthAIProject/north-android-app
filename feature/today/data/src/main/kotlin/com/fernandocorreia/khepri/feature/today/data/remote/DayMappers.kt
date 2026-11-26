package com.fernandocorreia.khepri.feature.today.data.remote

import com.fernandocorreia.khepri.core.api.model.DayActivityDto
import com.fernandocorreia.khepri.core.api.model.DayBodyDto
import com.fernandocorreia.khepri.core.api.model.DayFastDto
import com.fernandocorreia.khepri.core.api.model.DayFoodDto
import com.fernandocorreia.khepri.core.api.model.DayResponseDto
import com.fernandocorreia.khepri.core.api.model.DayRingDto
import com.fernandocorreia.khepri.core.api.model.DayStreakTileDto
import com.fernandocorreia.khepri.feature.today.domain.model.Activity
import com.fernandocorreia.khepri.feature.today.domain.model.BmiCategory
import com.fernandocorreia.khepri.feature.today.domain.model.Body
import com.fernandocorreia.khepri.feature.today.domain.model.Caffeine
import com.fernandocorreia.khepri.feature.today.domain.model.CheckInMoment
import com.fernandocorreia.khepri.feature.today.domain.model.CheckInTile
import com.fernandocorreia.khepri.feature.today.domain.model.Day
import com.fernandocorreia.khepri.feature.today.domain.model.DayFast
import com.fernandocorreia.khepri.feature.today.domain.model.DaySleep
import com.fernandocorreia.khepri.feature.today.domain.model.FastPhase
import com.fernandocorreia.khepri.feature.today.domain.model.Food
import com.fernandocorreia.khepri.feature.today.domain.model.MacroGoal
import com.fernandocorreia.khepri.feature.today.domain.model.Nutrients
import com.fernandocorreia.khepri.feature.today.domain.model.Ring
import com.fernandocorreia.khepri.feature.today.domain.model.TimelineEntry
import com.fernandocorreia.khepri.feature.today.domain.model.Vitals
import com.fernandocorreia.khepri.feature.today.domain.model.Water
import com.fernandocorreia.khepri.feature.today.domain.model.Workouts

internal fun DayResponseDto.toDay(): Day =
    Day(
        date = date,
        isToday = isToday,
        now = now,
        vitals = Vitals(vitals.energyPercent, vitals.daylightMinutes, vitals.screenMinutes),
        food = food.toFood(),
        water = Water(water.totalMl, water.targetMl),
        activity = activity.toActivity(),
        workouts = Workouts(workouts.count, workouts.minutes, workouts.calories, workouts.labels),
        body = body.toBody(),
        streak = streak,
        level = level,
        caffeine = Caffeine(caffeine.totalMg, caffeine.activeMg, caffeine.limitMg, caffeine.afterCutoff),
        nutrients = Nutrients(nutrients.covered, nutrients.missing, nutrients.total),
        sleep = sleep?.let { DaySleep(it.totalMinutes, it.source, it.start, it.end, it.quality) },
        fast = fast?.toFast(),
        timeline = timeline.map { TimelineEntry(it.kind, it.at, it.title, it.icon, it.detail, it.href) },
        checkIn = checkIn?.toTile(),
    )

/**
 * The response as it may be stored (design 6.5): no blood pressure, no sleep stages or blocks. Stages and blocks are
 * required fields, so they are stored empty; a sheet that needs them reads a live response.
 */
internal fun DayResponseDto.forCache(): DayResponseDto =
    copy(
        body = body.copy(bloodPressure = null),
        sleep = sleep?.copy(stages = emptyMap(), blocks = emptyList()),
    )

private fun DayFoodDto.toFood() =
    Food(calories, proteinG, carbG, fatG, goal?.let { MacroGoal(it.calories, it.proteinG, it.carbG, it.fatG) })

private fun DayActivityDto.toActivity() = Activity(move.toRing(), exercise.toRing(), stand.toRing())

private fun DayRingDto.toRing() = Ring(value, goal, percent)

private fun DayBodyDto.toBody() = Body(weightKg, heightCm, bmi, bmiCategory?.toCategory(), targetWeightKg, toGoalKg)

private fun DayBodyDto.BmiCategory.toCategory() =
    when (this) {
        DayBodyDto.BmiCategory.UNDERWEIGHT -> BmiCategory.Underweight
        DayBodyDto.BmiCategory.HEALTHY -> BmiCategory.Healthy
        DayBodyDto.BmiCategory.OVERWEIGHT -> BmiCategory.Overweight
        DayBodyDto.BmiCategory.OBESE -> BmiCategory.Obese
        DayBodyDto.BmiCategory.UNKNOWN -> BmiCategory.Unknown
    }

private fun DayFastDto.toFast() = DayFast(startedAt, targetHours, elapsedMinutes, phase.toPhase(), fraction, endedAt)

private fun DayFastDto.Phase.toPhase() =
    when (this) {
        DayFastDto.Phase.FED -> FastPhase.Fed
        DayFastDto.Phase.FASTING -> FastPhase.Fasting
        DayFastDto.Phase.FAT_BURNING -> FastPhase.FatBurning
        DayFastDto.Phase.KETOSIS -> FastPhase.Ketosis
        DayFastDto.Phase.UNKNOWN -> FastPhase.Unknown
    }

private fun DayStreakTileDto.toTile() =
    CheckInTile(streak, today?.let { CheckInMoment(it.mood, it.energy, it.updatedAt) })
