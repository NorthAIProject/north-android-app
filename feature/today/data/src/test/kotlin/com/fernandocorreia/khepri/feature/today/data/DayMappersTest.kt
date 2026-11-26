package com.fernandocorreia.khepri.feature.today.data

import com.fernandocorreia.khepri.core.api.ApiJson
import com.fernandocorreia.khepri.core.api.model.DayResponseDto
import com.fernandocorreia.khepri.core.api.model.TodayResponseDto
import com.fernandocorreia.khepri.feature.today.data.remote.forCache
import com.fernandocorreia.khepri.feature.today.data.remote.toDay
import com.fernandocorreia.khepri.feature.today.data.remote.toToday
import com.fernandocorreia.khepri.feature.today.domain.model.Activity
import com.fernandocorreia.khepri.feature.today.domain.model.BmiCategory
import com.fernandocorreia.khepri.feature.today.domain.model.Body
import com.fernandocorreia.khepri.feature.today.domain.model.Caffeine
import com.fernandocorreia.khepri.feature.today.domain.model.CheckInMoment
import com.fernandocorreia.khepri.feature.today.domain.model.CheckInTile
import com.fernandocorreia.khepri.feature.today.domain.model.DayFast
import com.fernandocorreia.khepri.feature.today.domain.model.DaySleep
import com.fernandocorreia.khepri.feature.today.domain.model.FastPhase
import com.fernandocorreia.khepri.feature.today.domain.model.Food
import com.fernandocorreia.khepri.feature.today.domain.model.Hydration
import com.fernandocorreia.khepri.feature.today.domain.model.MacroGoal
import com.fernandocorreia.khepri.feature.today.domain.model.NextStep
import com.fernandocorreia.khepri.feature.today.domain.model.Ring
import com.fernandocorreia.khepri.feature.today.domain.model.SleepSummary
import com.fernandocorreia.khepri.feature.today.domain.model.TimelineEntry
import com.fernandocorreia.khepri.feature.today.domain.model.Today
import com.fernandocorreia.khepri.feature.today.domain.model.TodayCheckIn
import com.fernandocorreia.khepri.feature.today.domain.model.Vitals
import com.fernandocorreia.khepri.feature.today.domain.model.Water
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DayMappersTest {
    @Test
    fun `a golden day maps every field Phase 2 reads`() {
        val day = Goldens.day().toDay()
        assertEquals(LocalDate.parse("2026-09-26"), day.date)
        assertTrue(day.isToday)
        assertEquals(Instant.parse("2026-09-26T14:46:00Z"), day.now)
        assertEquals(Vitals(32, 84, 248), day.vitals)
        assertEquals(Food(1750.0, 107.0, 133.0, 80.0, MacroGoal(2200.0, 160.0, 220.0, 75.0)), day.food)
        assertEquals(Water(1550, 2500), day.water)
        assertEquals(Activity(Ring(501.0, 500.0, 100), Ring(62.0, 30.0, 207), Ring(12.0, 12.0, 100)), day.activity)
        assertEquals(2, day.workouts.count)
        assertEquals(62, day.workouts.minutes)
        assertEquals(410.0, day.workouts.calories)
        assertEquals("Dog walk", day.workouts.labels.first())
        assertEquals(Body(83.9, 181.0, 25.6, BmiCategory.Overweight, 80.0, 3.9), day.body)
        assertEquals(91, day.streak)
        assertEquals(21, day.level)
        assertEquals(Caffeine(172, 72, 400, afterCutoff = false), day.caffeine)
        assertEquals(listOf("omega3"), day.nutrients.covered)
        assertEquals("vitamin_a", day.nutrients.missing.first())
        assertEquals(3, day.nutrients.total)
        val sleepStart = Instant.parse("2026-09-25T23:00:00Z")
        assertEquals(DaySleep(433, "apple_health", sleepStart, Instant.parse("2026-09-26T07:13:00Z"), 4), day.sleep)
        val fastStart = Instant.parse("2026-09-26T02:00:00Z")
        assertEquals(DayFast(fastStart, 16, 766, FastPhase.FatBurning, 0.8, null), day.fast)
        val at = Instant.parse("2026-09-26T15:30:00Z")
        val entry = TimelineEntry("food", at, "Chicken rice bowl", "utensils", "650 kcal", "/app/nutrition/log")
        assertEquals(listOf(entry), day.timeline)
        assertEquals(CheckInTile(91, CheckInMoment(4, 3, Instant.parse("2026-09-26T08:00:00Z"))), day.checkIn)
    }

    @Test
    fun `values this build does not know map without failing`() {
        val day =
            Goldens.day { root ->
                    val entry = root.getValue("timeline").jsonArray.first().jsonObject
                    root
                        .edit("body") { it.with("bmiCategory", JsonPrimitive("athletic")) }
                        .edit("fast") { it.with("phase", JsonPrimitive("autophagy")) }
                        .with("timeline", JsonArray(listOf(entry.with("kind", JsonPrimitive("meditation")))))
                }
                .toDay()
        assertEquals(BmiCategory.Unknown, day.body.bmiCategory)
        assertEquals(FastPhase.Unknown, day.fast?.phase)
        assertEquals("meditation", day.timeline.single().kind)
    }

    @Test
    fun `the cached shape has no blood pressure, sleep stages or blocks, and still maps`() {
        val stored = Goldens.day().forCache()
        assertNull(stored.body.bloodPressure)
        assertEquals(emptyMap<String, Int>(), stored.sleep?.stages)
        assertEquals(0, stored.sleep?.blocks?.size)
        assertEquals(Goldens.day().toDay(), stored.toDay())
    }

    @Test
    fun `a golden today maps what the Today screen shows`() {
        val expected =
            Today(
                briefing = "Easy day. Hydrate before the evening session.",
                streak = 6,
                checkedInToday = true,
                checkIn = TodayCheckIn(4, 3, 2, 4, Instant.parse("2026-09-24T07:15:00Z")),
                hydration = Hydration(1250, 2500, 50),
                sleep = SleepSummary(logged = true, durationMinutes = 452, quality = 4),
                nextStep =
                    NextStep(
                        "check_in",
                        "Today",
                        "How are you arriving?",
                        "A 20-second check-in keeps your streak.",
                        "Check in",
                        "/app/check-ins",
                    ),
            )
        assertEquals(expected, Goldens.today().toToday())
    }
}

/** The contract's golden responses, decoded as the app decodes them. */
internal object Goldens {
    fun text(name: String): String = requireNotNull(javaClass.getResource("/$name.golden.json")) { name }.readText()

    fun day(edit: (JsonObject) -> JsonObject = { it }): DayResponseDto =
        ApiJson.default.decodeFromJsonElement(DayResponseDto.serializer(), edit(json("day")))

    fun today(edit: (JsonObject) -> JsonObject = { it }): TodayResponseDto =
        ApiJson.default.decodeFromJsonElement(TodayResponseDto.serializer(), edit(json("today")))

    fun json(name: String): JsonObject = ApiJson.default.parseToJsonElement(text(name)).jsonObject
}

internal fun JsonObject.with(key: String, value: JsonElement): JsonObject = JsonObject(this + (key to value))

internal fun JsonObject.edit(key: String, change: (JsonObject) -> JsonObject): JsonObject =
    with(key, change(getValue(key).jsonObject))
