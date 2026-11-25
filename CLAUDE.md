# Khepri Android

Kotlin + Jetpack Compose port of the iOS app in `../north-ios-app/khepri` (abbreviated `K/`). Design: `docs/design.md`. Plans: `docs/plans/`.

## Sources of truth

- Behaviour: the iOS Swift code. Read the file before porting a screen; `docs/feature-specs/<module>.md` lists them.
- Contract: `../north-client/docs/api/openapi.yaml` at the SHA in `core/api/contract.sha` (the newest commit deployed to production), copied into `core/api/contract/` and committed. The local `north-client` checkout may be on a stale branch; read backend code with `git -C ../north-client show origin/main:<path>`.
- Do not trust `../docs/ios/*.md`, `../ARCHITECTURE.md`, or `../.planning/`. They are out of date.

## Architecture rules

- `feature:x:domain` is pure Kotlin and sees only `:core:model` and `:core:common`.
- `feature:x:ui` never sees `feature:x:data` or `:core:api`.
- No feature depends on another feature. Reach other screens through `Navigator.open(destination)`.
- One immutable `UiState` per screen as `StateFlow`; user intents through one `onEvent`; one-shot outcomes through an `Effect` channel.
- A failed refresh keeps what is on screen. A failed save never blanks the screen.
- Every write use case emits its `DataScope` on `DataChangeBus`. A screen never emits from its own load.
- Nothing throws across a layer boundary; repositories return `Outcome<T>`.

## Traps

- The server rejects unknown request fields. Never add a field to a request type by hand.
- Dates are RFC 3339 with or without fractional seconds. Use `java.time` types and the shared `Instant` serializer (writes millisecond precision).
- A 401 signs the user out only if a token was sent. An unreadable token stops the request and does not sign out.
- Only GET and HEAD are retried, once. Never retry writes or event streams.
- Push category strings (`CHECKIN`, `COACH_LIGHTER`, `COACH_MISSED`, `COACH_FIRST_WEEK`, `CREW_MESSAGE`) must match the server exactly: the first four are `nudges.Category*` constants in `internal/nudges/nudges.go`; `CREW_MESSAGE` is a literal in `internal/crews/push.go`.
- Never add a value to an existing response enum in the spec: shipped iOS builds fail the whole response. Deploy the backend before the client.
- Infrastructure changes go only in `../../../platform/infra` (the `LuminaVaultInfra` repo); merging there deploys.
- No paid speech API. Use on-device recognition or the backend's `/voice/transcribe`.
- Tests never use a real API key; use stub servers.

## Code style

- Spotless + ktfmt, width 120, indent 4. detekt with Compose rules, zero issues, no baseline. Android Lint warnings are errors.
- Name by role, not type. Document public declarations whose behaviour is not obvious from the name; no other comments.
- No `!!`. No `GlobalScope`. Inject dispatchers. `collectAsStateWithLifecycle` in screens.
- Composables below screen level are stateless. Every screen has previews and screenshot tests for loading, content, empty and failed.
- Strings in en, es, pt-PT and pt-BR; a missing translation fails the build. No em dashes in copy.
- No version outside the version catalog. Look up the current stable version when adding a dependency, and vet it first.
- Test tags are kebab-case (`coach-orb`, `rest-countdown`).

## Commands

None yet. Phase 1 adds `./gradlew check` and `./gradlew syncContract --check`.
