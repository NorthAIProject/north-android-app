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
- Nothing throws across a layer boundary; repositories return `Outcome<T>`. Map a response with `apiCall(map = { it.toX() }) { ... }` so a mapper that throws becomes `InvalidResponse`; never `apiCall { }.map { }`.
- Coroutine scopes: a screen's work on `viewModelScope`; work that outlives a screen but belongs to the account on `@AccountScope` (cancelled at sign-out and at the next sign-in); only sign-out and retry on `@ApplicationScope`.
- Account-bound state held in a singleton (a cache, an in-memory `StateFlow`) registers a `SignOutCleaner` that clears it. Cancelling `@AccountScope` stops work; it does not clear memory.
- A screen collects its `Effect` channel with `CollectEffects` and reloads on `bus.reloads(scope, ...)`, never on raw `DataChangeBus.changes`.
- `UiState` lives in a private `MutableStateFlow` (or is combined from the repository's flows). A screen that loads exposes it with `onStart { launchRefresh() }.stateIn(viewModelScope, WhileSubscribed(5_000), initial)`, where `launchRefresh` launches and returns at once (never await in `onStart`: stored content must reach the screen before the network answers); a form with no load (sign-in) exposes it as is.
- A tab root is drawn by its feature's `EntryInstaller.roots`, never by registering `Route.TabRoot`. A screen takes its `ScreenAsk`s (`ReloadRoot`, payloads) with `ConsumeAsks`.
- Screenshot tests draw through `ScreenshotHarness` (`:core:screenshot`, test-only): one `show` per test, then change settings or state and `capture`.

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
- Room is Room 3 (`androidx.room3`): `ColumnTypeConverter`, not `TypeConverter`, and a driver is required (`AndroidSQLiteDriver`). Every table holds account data; `DatabaseCleaner` empties them all at sign-out.
- A refresh captures `SignedInState.account` before the call and writes only if it is unchanged: on disk through `SnapshotCache.putIf` (checked inside the write transaction), in memory under the lock its `SignOutCleaner` clears under. Sign-out clears `account` before the cleaners run. A `SignOutCleaner` takes `Provider<SignedInState>`: the session holds the cleaner set, so a direct injection is a Hilt cycle.
- A ViewModel test that relies on `stateIn` or `viewModelScope.launch` runs as a plain test, not inside `runTest`: under `runTest` the test Main queues those launches and they never run before the assertions.

## Code style

- Spotless + ktfmt, width 120, indent 4. detekt with Compose rules, zero issues, no baseline. Android Lint warnings are errors.
- Name by role, not type. Document public declarations whose behaviour is not obvious from the name; no other comments.
- No `!!`. No `GlobalScope`. Inject dispatchers. `collectAsStateWithLifecycle` in screens.
- Composables below screen level are stateless. Every screen has previews and screenshot tests for loading, content, empty and failed.
- Strings in en, es, pt-PT and pt-BR; a missing translation fails the build. No em dashes in copy.
- No version outside the version catalog. Look up the current stable version when adding a dependency, and vet it first.
- Test tags are kebab-case (`coach-orb`, `rest-countdown`).

## Commands

- `./gradlew check` runs everything CI runs, including `checkTranslations`, the `tools/` tests, Roborazzi verification, `buildHealth` and `verifyReleaseManifest`.
- `./gradlew syncContract [--check]` copies the spec, medals and golden fixtures at `core/api/contract.sha` from `../north-client`; `--check` fails if the committed copies differ.
- `./gradlew :<module>:recordRoborazziDebug` re-records a module's screenshots after an intended visual change.
- `./gradlew :app:assembleDebug`, `:app:assembleMinified` (release code under R8, debug-signed, local backend) and `:app:assembleRelease`.
- `maestro/README.md`: the smoke flow against a local backend at the pinned SHA. Never with real credentials or production.
- `python3 tools/convert_strings.py [--check]` regenerates `core/ui` strings from the iOS catalogs. Never edit the generated `strings.xml`; Android-only copy goes in `tools/strings-android.json`.
- `python3 tools/sync_theme.py` copies colours from the iOS asset catalog; `python3 tools/emblem_to_vector.py` regenerates the emblem drawables from the web's SVGs.

## Known limitations

- Phase 1: a cold launch with no connection shows the `Failed` screen (Try Again, Sign Out) until the Phase 2 cache exists.
- An explicit sign-out gives the server steps (push unregister, logout) 5 s, then signs out locally; on a dead network the token stays valid on the server until it expires. iOS waits for the request instead.
- Local builds (`debug`, `minified`) need `ACCESS_LOCAL_NETWORK` granted on API 37+ to reach `10.0.2.2`; without it calls time out silently.
