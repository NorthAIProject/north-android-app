# ADR 0002: Strict Clean Architecture with three modules per feature

- **Status:** accepted
- **Date:** 2026-11-24

## Context

iOS uses observable stores that call a service wrapping the generated client, with no use-case layer. Three options were considered: mirror iOS, a lighter layering with a Room cache, or strict Clean Architecture. The owner chose the strict option.

## Decision

Each feature is split into `:domain` (pure Kotlin: models, repository interfaces, one use case per action), `:data` (mappers, Room, repository implementations) and `:ui` (screens, ViewModels, UiState). Features never depend on each other. A module-graph assertion enforces the rules.

## Consequences

- More mapping code than iOS; the frozen feature template and convention plugins keep it uniform.
- The UI cannot see API types, so a spec change is absorbed in `:data`.
- Cross-feature navigation goes through destinations in `:core:navigation`.
