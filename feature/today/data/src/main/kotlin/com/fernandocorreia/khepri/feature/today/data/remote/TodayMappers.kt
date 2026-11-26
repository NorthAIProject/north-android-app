package com.fernandocorreia.khepri.feature.today.data.remote

import com.fernandocorreia.khepri.core.api.model.NextStepDto
import com.fernandocorreia.khepri.core.api.model.TodayCheckInDto
import com.fernandocorreia.khepri.core.api.model.TodayResponseDto
import com.fernandocorreia.khepri.feature.today.domain.model.Hydration
import com.fernandocorreia.khepri.feature.today.domain.model.NextStep
import com.fernandocorreia.khepri.feature.today.domain.model.SleepSummary
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.model.TodayCheckIn

internal fun TodayResponseDto.toToday(): Today =
    with(snapshot) {
        Today(
            briefing = briefing,
            streak = streak,
            checkedInToday = checkedInToday,
            checkIn = todayCheckIn?.toCheckIn(),
            hydration = Hydration(hydration.todayML, hydration.targetML, hydration.percent),
            sleep = SleepSummary(sleep.logged, sleep.durationMinutes, sleep.quality),
            nextStep = nextStep?.toNextStep(),
        )
    }

private fun TodayCheckInDto.toCheckIn() = TodayCheckIn(mood, energy, stress, sleepQuality, at)

private fun NextStepDto.toNextStep() = NextStep(kind, eyebrow, title, body, cta, href)
