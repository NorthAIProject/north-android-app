# :core:navigation

`AppDestination` and `DeepLinkParser` (a port of iOS `AppRouter.destination(for:)`), route keys, the pure
`NavPlanner`, the `Navigator` command queue, and `NavigationState`: the per-tab stacks and ViewModel stores, held in an
activity-scoped ViewModel above the phase switch. See `docs/design.md` section 8.
