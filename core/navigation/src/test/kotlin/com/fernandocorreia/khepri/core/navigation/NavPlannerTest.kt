package com.fernandocorreia.khepri.core.navigation

import com.fernandocorreia.khepri.core.navigation.AppTab.Coach
import com.fernandocorreia.khepri.core.navigation.AppTab.More
import com.fernandocorreia.khepri.core.navigation.AppTab.Today
import com.fernandocorreia.khepri.core.navigation.AppTab.Training
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NavPlannerTest {
    private var openings = 0

    private fun plan(state: TabStacks, command: NavCommand) = NavPlanner.plan(state, command) { "o${++openings}" }

    private fun open(destination: AppDestination, state: TabStacks = TabStacks.Roots) =
        plan(state, NavCommand.Open(destination))

    private fun root(tab: AppTab) = Route.TabRoot(tab)

    private val crew = "66666666-6666-6666-6666-666666666666"

    @Test
    fun `a goal selects More and builds the stack with its parents`() {
        val plan = open(AppDestination.Goal("g1"))
        assertEquals(More, plan.state.selected)
        assertEquals(listOf(root(More), Route.Section("goals"), Route.Goal("g1")), plan.state.stack(More))
        assertEquals(listOf(root(Today)), plan.state.stack(Today))
    }

    @Test
    fun `a tab keeps its stack`() {
        val deep = open(AppDestination.Goal("g1")).state.copy(selected = Today)
        val plan = open(AppDestination.Tab(More), deep)
        assertEquals(More, plan.state.selected)
        assertEquals(deep.stack(More), plan.state.stack(More))
    }

    @Test
    fun `the check-in flow is a sheet on the current tab`() {
        val onTraining = TabStacks.Roots.copy(selected = Training)
        val plan = open(AppDestination.CheckInFlow, onTraining)
        assertEquals(Training, plan.state.selected)
        assertEquals(listOf(root(Training), Sheet.CheckInFlow), plan.state.stack(Training))
    }

    @Test
    fun `the same sheet is not stacked twice and a new sheet replaces the one on top`() {
        val once = open(AppDestination.Exercise("push-up")).state
        assertEquals(once, open(AppDestination.Exercise("push-up"), once).state)
        val replaced = open(AppDestination.WeeklyReview, once).state
        assertEquals(listOf(root(Today), Sheet.WeeklyReview), replaced.stack(Today))
    }

    @Test
    fun `settings is a sheet over More`() {
        val plan = open(AppDestination.Settings)
        assertEquals(More, plan.state.selected)
        assertEquals(listOf(root(More), Sheet.Settings), plan.state.stack(More))
    }

    @Test
    fun `a section link keeps what is open inside that section, any other replaces it`() {
        val inCrews = open(AppDestination.CrewChannel(crew, null)).state
        assertEquals(inCrews.stack(More), open(AppDestination.Section("crews"), inCrews).state.stack(More))
        assertEquals(
            listOf(root(More), Route.Section("goals")),
            open(AppDestination.Section("goals"), inCrews).state.stack(More),
        )
    }

    @Test
    fun `a crew link opens the space over Crews, each time a new screen`() {
        val first = open(AppDestination.CrewChannel(crew, "c1"))
        assertEquals(
            listOf(root(More), Route.Section("crews"), Route.CrewSpace(crew, "c1", "o1")),
            first.state.stack(More),
        )
        val other = open(AppDestination.CrewChannel("77777777-7777-7777-7777-777777777777", null), first.state)
        assertEquals(3, other.state.stack(More).size)
        assertNotEquals(first.state.stack(More).last(), other.state.stack(More).last())
    }

    @Test
    fun `a link for the crew already open asks its space to switch channel, twice is twice`() {
        val open = open(AppDestination.CrewChannel(crew, null)).state.copy(selected = Today)
        val link = AppDestination.CrewChannel(crew.uppercase(), "c2")
        val plan = open(link, open)
        assertEquals(More, plan.state.selected)
        assertEquals(open.stack(More), plan.state.stack(More))
        assertEquals(listOf(ScreenAsk.SwitchChannel(crew.uppercase(), "c2")), plan.asks)
        assertEquals(plan.asks, open(link, plan.state).asks)
    }

    @Test
    fun `one-shot payloads travel as asks, not in saved keys`() {
        val start = open(AppDestination.StartWorkout(3))
        assertEquals(listOf(root(Training), Route.TrainingDay(3)), start.state.stack(Training))
        assertEquals(listOf(ScreenAsk.StartWorkout), start.asks)
        val next = open(AppDestination.NextWorkout(start = true), start.state)
        assertEquals(listOf(root(Training)), next.state.stack(Training))
        assertEquals(listOf(ScreenAsk.NextWorkout(true)), next.asks)
        val mood = open(AppDestination.CheckIn(4))
        assertEquals(listOf(root(More), Route.Section("check-ins")), mood.state.stack(More))
        assertEquals(listOf(ScreenAsk.CheckInMood(4)), mood.asks)
        val draft = open(AppDestination.AskCoach("About today's post: "))
        assertEquals(Coach, draft.state.selected)
        assertEquals(listOf(ScreenAsk.CoachDraft("About today's post: ")), draft.asks)
    }

    @Test
    fun `invites and crew joins open their section and ask it to redeem`() {
        val invite = open(AppDestination.Invite("abc123xyz9"))
        assertEquals(listOf(root(More), Route.Section("friends")), invite.state.stack(More))
        assertEquals(listOf(ScreenAsk.RedeemInvite), invite.asks)
        assertEquals(listOf(ScreenAsk.JoinCrew), open(AppDestination.CrewJoin("abc123xyz9")).asks)
    }

    @Test
    fun `a conversation opens full screen and hides the tab bar`() {
        val plan = open(AppDestination.Conversation("t9"))
        assertEquals(listOf(root(Coach), Route.CoachThread("t9")), plan.state.stack(Coach))
        assertTrue(plan.state.stack(Coach).hidesTabBar())
        val sheetOver = open(AppDestination.Exercise("push-up"), plan.state)
        assertTrue(sheetOver.state.stack(Coach).hidesTabBar())
        assertFalse(open(AppDestination.Goal("g")).state.stack(More).hidesTabBar())
    }

    @Test
    fun `pop stops at the root, re-select pops to the root and asks for a reload`() {
        val deep = open(AppDestination.Goal("g1")).state
        val popped = plan(deep, NavCommand.Pop).state
        assertEquals(listOf(root(More), Route.Section("goals")), popped.stack(More))
        assertEquals(listOf(root(More)), plan(plan(popped, NavCommand.Pop).state, NavCommand.Pop).state.stack(More))
        val reselect = plan(deep, NavCommand.Select(More))
        assertEquals(listOf(root(More)), reselect.state.stack(More))
        assertEquals(listOf(ScreenAsk.ReloadRoot(More)), reselect.asks)
        val switch = plan(deep, NavCommand.Select(Today))
        assertEquals(deep.stack(More), switch.state.stack(More))
        assertTrue(switch.asks.isEmpty())
    }

    @Test
    fun `content keys are distinct per value and stable`() {
        assertEquals(Route.Goal("a").contentKey(), Route.Goal("a").contentKey())
        assertNotEquals(Route.Goal("a").contentKey(), Route.Decision("a").contentKey())
        assertNotEquals(Route.Section("goals").contentKey(), Route.Section("crews").contentKey())
    }
}
