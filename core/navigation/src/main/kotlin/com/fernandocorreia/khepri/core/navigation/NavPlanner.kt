package com.fernandocorreia.khepri.core.navigation

/** The selected tab and every tab's stack, root first. */
public data class TabStacks(val selected: AppTab, val stacks: Map<AppTab, List<Route>>) {
    public fun stack(tab: AppTab): List<Route> = stacks[tab] ?: listOf(Route.TabRoot(tab))

    internal fun with(tab: AppTab, stack: List<Route>, select: Boolean = true): TabStacks =
        TabStacks(if (select) tab else selected, stacks + (tab to stack))

    public companion object {
        public val Roots: TabStacks =
            TabStacks(AppTab.Today, AppTab.entries.associateWith { listOf(Route.TabRoot(it)) })
    }
}

/** The stacks after a command, and the asks it leaves for the screens on them. */
public data class NavPlan(val state: TabStacks, val asks: List<ScreenAsk> = emptyList())

/** Pure: how each command changes the stacks. Follows iOS `AppRouter.open(_:)` and `MorePath`. */
internal object NavPlanner {
    fun plan(state: TabStacks, command: NavCommand, newOpening: () -> String): NavPlan =
        leaveSheets(state.selected, plain(state, command, newOpening))

    /** iOS presents sheets from the tab view and never brings one back: a tab that is left loses its sheet. */
    private fun leaveSheets(from: AppTab, plan: NavPlan): NavPlan {
        val next = plan.state
        if (next.selected == from || next.stack(from).last() !is Sheet) return plan
        return plan.copy(state = next.with(from, next.stack(from).dropLastWhile { it is Sheet }, select = false))
    }

    private fun plain(state: TabStacks, command: NavCommand, newOpening: () -> String): NavPlan =
        when (command) {
            is NavCommand.Open -> open(state, command.destination, newOpening)
            is NavCommand.Push -> NavPlan(push(state, state.selected, command.route))
            NavCommand.Pop ->
                NavPlan(state.with(state.selected, state.stack(state.selected).let { it.take(maxOf(1, it.size - 1)) }))
            NavCommand.PopToRoot -> NavPlan(state.with(state.selected, root(state.selected)))
            is NavCommand.Select ->
                if (command.tab == state.selected) {
                    NavPlan(state.with(command.tab, root(command.tab)), listOf(ScreenAsk.ReloadRoot(command.tab)))
                } else {
                    NavPlan(state.copy(selected = command.tab))
                }
        }

    @Suppress("CyclomaticComplexMethod")
    private fun open(state: TabStacks, destination: AppDestination, newOpening: () -> String): NavPlan =
        when (destination) {
            is AppDestination.Tab -> NavPlan(state.copy(selected = destination.tab))
            AppDestination.Settings -> NavPlan(push(state, AppTab.More, Sheet.Settings))
            is AppDestination.TrainingDay -> NavPlan(trainingDay(state, destination.day))
            is AppDestination.StartWorkout ->
                NavPlan(trainingDay(state, destination.day), listOf(ScreenAsk.StartWorkout))
            is AppDestination.NextWorkout ->
                NavPlan(
                    state.with(AppTab.Training, root(AppTab.Training)),
                    listOf(ScreenAsk.NextWorkout(destination.start)),
                )
            is AppDestination.Section -> NavPlan(section(state, destination.id))
            is AppDestination.Decision -> NavPlan(more(state, "decisions", Route.Decision(destination.id)))
            is AppDestination.Goal -> NavPlan(more(state, "goals", Route.Goal(destination.id)))
            is AppDestination.CheckIn ->
                NavPlan(section(state, "check-ins"), listOf(ScreenAsk.CheckInMood(destination.mood)))
            is AppDestination.Invite -> NavPlan(section(state, "friends"), listOf(ScreenAsk.RedeemInvite))
            is AppDestination.CrewJoin -> NavPlan(section(state, "crews"), listOf(ScreenAsk.JoinCrew))
            is AppDestination.CrewChannel -> crewChannel(state, destination, newOpening)
            is AppDestination.AskCoach ->
                NavPlan(state.with(AppTab.Coach, root(AppTab.Coach)), listOf(ScreenAsk.CoachDraft(destination.draft)))
            is AppDestination.Conversation ->
                NavPlan(state.with(AppTab.Coach, root(AppTab.Coach) + Route.CoachThread(destination.id)))
            AppDestination.CheckInFlow -> NavPlan(push(state, state.selected, Sheet.CheckInFlow))
            AppDestination.WeeklyReview -> NavPlan(push(state, state.selected, Sheet.WeeklyReview))
            AppDestination.Horizon -> NavPlan(push(state, state.selected, Sheet.Horizon))
            AppDestination.FirstWeekReview -> NavPlan(push(state, state.selected, Sheet.FirstWeekReview))
            is AppDestination.Exercise -> NavPlan(push(state, state.selected, Sheet.Exercise(destination.slug)))
            is AppDestination.Muscle -> NavPlan(push(state, state.selected, Sheet.Muscle(destination.id)))
        }

    /**
     * Pushes [route] on [tab] and selects it. The same route already on top is not pushed twice, and a sheet replaces a
     * sheet on top: a sheet under a sheet would show as a full page.
     */
    private fun push(state: TabStacks, tab: AppTab, route: Route): TabStacks {
        val stack = state.stack(tab)
        val base = if (route is Sheet && stack.last() is Sheet) stack.dropLast(1) else stack
        return state.with(tab, if (stack.last() == route) stack else base + route)
    }

    private fun trainingDay(state: TabStacks, day: Int): TabStacks =
        state.with(AppTab.Training, root(AppTab.Training) + Route.TrainingDay(day))

    /** iOS `MorePath.opening(section:)`: the section alone, unless More already starts there; then nothing moves. */
    private fun section(state: TabStacks, id: String): TabStacks {
        val stack = state.stack(AppTab.More)
        return state.with(AppTab.More, if (stack.getOrNull(1) == Route.Section(id)) stack else sectionStack(id))
    }

    private fun more(state: TabStacks, section: String, route: Route): TabStacks =
        state.with(AppTab.More, sectionStack(section) + route)

    /**
     * iOS `CrewLinkLanding`: a link for the crew whose space is on top of More moves that space to the channel; any
     * other replaces the open space, so there is one space and Back goes to the Crews list.
     */
    private fun crewChannel(state: TabStacks, link: AppDestination.CrewChannel, newOpening: () -> String): NavPlan {
        val open = state.stack(AppTab.More).lastOrNull { it !is Sheet } as? Route.CrewSpace
        return if (open?.crewId.equals(link.crewId, ignoreCase = true)) {
            NavPlan(state.copy(selected = AppTab.More), listOf(ScreenAsk.SwitchChannel(link.crewId, link.channelId)))
        } else {
            NavPlan(more(state, "crews", Route.CrewSpace(link.crewId, link.channelId, newOpening())))
        }
    }

    private fun root(tab: AppTab): List<Route> = listOf(Route.TabRoot(tab))

    private fun sectionStack(id: String): List<Route> = root(AppTab.More) + Route.Section(id)
}
