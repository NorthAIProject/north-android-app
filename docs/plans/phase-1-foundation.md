# Phase 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** a debug build that signs in with email against a locally seeded backend, shows the five-tab shell, opens a `khepri://` link, and passes every quality gate in CI.

**Architecture:** multi-module Gradle project with convention plugins. This phase builds `:app`, `:core:model`, `:core:common`, `:core:api`, `:core:network`, `:core:datastore`, `:core:session`, `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:testing`, and the email slice of `:feature:auth`. `:core:database` is created in Phase 2 with its first entity (Room needs at least one). The API client is generated from the pinned, committed OpenAPI spec; SSE and multipart are hand-written.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Navigation 3, Hilt, Retrofit + OkHttp, kotlinx.serialization, `java.time`, DataStore, JUnit, MockK, MockWebServer, Robolectric, Roborazzi, Maestro, Spotless + ktfmt, detekt, Android Lint.

**Spec:** `docs/design.md` (sections 4, 6, 7, 8, 9, 12). Spike results: `docs/spikes/S1-*.md`, `docs/spikes/S2-*.md`, `docs/spikes/S9-*.md`.

**Depends on:** spikes S1 (generator options, interface grouping, enum member name, validator choice), S2 (the contract SHA) and S9 (navigation model). Dependency vetting needs the owner's Sonatype token, or the owner approving the dependency list by hand. Nothing else in Phase 0 blocks this plan.

## Amendments after Phase 0 (review 2026-11-25)

Phase 0 ended with a Fable review of the spikes. These items change the tasks below and win over older text in them.

- **Task 1.** Before any feature code, prove Hilt + KSP + Robolectric + Roborazzi build and run one test each on AGP 9.5.2, Kotlin 2.4.21 and compileSdk 37.1. These dependencies need the owner's approval by hand (no Sonatype token). `compileSdk { version = release(37) { minorApiLevel = 1 } }`, `platforms;android-37.1` installed, `google()` in both repository blocks, and `org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g` (R8 ran the daemon out of Metaspace in S9).
- **Task 4.**
  - Use the S1 generator configuration and `normalizeSpec` as they are, including retagging by first path segment (expect 68 interfaces, 378 operations, 12 operations removed; assert these counts so a spec change is noticed). There is no `core/api/templates/` directory: S1 needed no template overrides.
  - `uuid` maps to `String` (design 7.1), not `java.util.UUID` as the spike did. One malformed id must not fail a whole response, and links compare ids as text.
  - `ApiJson.default` is the spike's `ApiJson.tolerant`.
  - `UNKNOWN_ENUM = "UNKNOWN"` (set with `enumNameMappings`). Only model enums have it; the 9 endpoint (query and path) enums do not, so `EnumToleranceTest` lists the model package only.
  - Encoding `UNKNOWN` must throw: its wire value `unknown_default_open_api` is rejected by the server.
  - The `Instant`, `LocalDate` and UUID-format serializers throw `SerializationException`, never `DateTimeParseException` or `IllegalArgumentException`.
  - `RequestSchemaTest` checks keys recursively through nested objects and arrays, builds a fully populated instance of every request DTO from its `SerialDescriptor`, and resolves the `HelpfulRequest.helpful` conflict (required and nullable). Add a required-fields-only decode for every response DTO.
  - `normalizeSpec` fails on any unquoted YAML 1.1 boolean word used as a key (`on`, `off`, `yes`, `no`, `y`, `n`), and the free-form object rewrite also drops `additionalProperties: false`.
  - `OperationIndexTask` reads the spec after the `on` quoting but before operations are removed, so the event-stream and multipart operations are indexed too.
- **Task 5.** Register `QueryValueConverterFactory` (from S1) on the Retrofit instance; without it enum query values go out in upper case (`?week=NEXT`). `apiCall` maps `SerializationException` and `IllegalArgumentException` to `AppError.Decoding`.
- **Task 7.** Sign-out clears every tab's `ViewModelStore` and resets every tab's stack before the login screen shows, so the next account never sees the last one's screens.
- **Task 9.**
  - A `NavigationState` holder lives above the phase switch in `RootContent`, owns the per-tab back stacks and the per-tab `ViewModelStoreProvider`s (S9), and survives phase changes.
  - `Navigator` exposes `open`, `push`, `pop` and `popToRoot`, applied through a command queue so a link that arrives before the shell is composed is not lost.
  - When `open` replaces a stack, it calls `provider.clearKey(contentKey)` for each removed entry.
  - Prove a Hilt ViewModel with an assisted route key and a `SavedStateHandle` under the per-tab provider, including process death.
  - Consume a link once: clear `intent.data` with `setIntent` after `Navigator` takes it.
  - Evaluate Navigation 3 `runtime.deeplink` and `ResultEffect` before writing our own. Re-selecting a tab also reloads its root.
- **Task 12.** Test what was not proven in S9: sheet swipe-dismiss, a sheet restored after process death, predictive back, a minSdk 28 emulator, and an R8 release run of the process-death check. Decide what a push link does while a workout is active (recommended: hold it until the workout ends).

## Global Constraints

- Application id `com.fernandocorreia.khepri`; debug suffix `.debug`. `:app` namespace `com.fernandocorreia.khepri`; other modules `com.fernandocorreia.khepri.<module path with dots>` (`:core:network` → `com.fernandocorreia.khepri.core.network`).
- minSdk 28; targetSdk 37; compileSdk 37.1 (Compose 1.13 needs the minor level).
- Debug API base `http://10.0.2.2:8090`, release `https://kheprios.com`; the client appends `/api/v1/`. Cleartext is allowed for `10.0.2.2` only.
- Time types are `java.time` everywhere.
- Every version lives in `gradle/libs.versions.toml`. Use the versions recorded in the spike results; look up the current stable version for anything else.
- Module dependencies follow the tables in `docs/design.md` section 6.1. All are `implementation`.
- Formatting: ktfmt, width 120, indent 4. detekt zero issues, no baseline. Android Lint warnings are errors, no baseline. Generated code in `:core:api` is excluded from Spotless, detekt and explicit API mode.
- Explicit API mode is on for every `core/*` module except `:core:api` and `:core:testing`.
- No `!!`, no `GlobalScope`, dispatchers and `Clock` injected.
- User-visible text comes from string resources in four locales: `values`, `values-es`, `values-pt-rPT`, `values-pt-rBR`.
- Tests never call a real API. Use MockWebServer or fakes.
- Database and DataStore files are excluded from backup.
- Material 3 dynamic colour is off.
- Commit after each task. Never commit `google-services.json`, `secrets.properties` or keystores.

## Review Focus

Failure modes most likely to bite. Each has a test in the task that owns the code.

1. A timestamp with nanoseconds (`…:00.123456789Z`), with no fraction, or with an offset fails to decode and a whole screen breaks. → Task 4.
2. A response carries an enum value the client does not know. → Task 4.
3. A 401 on a request that sent no token, or whose token could not be read, signs the user out; or two concurrent 401s run sign-out twice. → Tasks 5 and 7.
4. A write is retried after a dropped connection and happens twice. → Task 5.
5. A link with an unexpected shape (upper-case invite code, wrong id length, unknown path, `/app/progress`) lands on the wrong screen or crashes. → Task 9.

---

### Task 1: Gradle project and convention plugins

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, Gradle wrapper
- Create: `build-logic/settings.gradle.kts` (includes `convention` and `detekt-rules`), `build-logic/convention/build.gradle.kts`
- Create: `build-logic/convention/src/main/kotlin/` with one plugin class per id: `khepri.android.application`, `khepri.android.library`, `khepri.kotlin.jvm`, `khepri.compose`, `khepri.hilt`, `khepri.room`, `khepri.core`, `khepri.feature.domain`, `khepri.feature.data`, `khepri.feature.ui`
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/com/fernandocorreia/khepri/MainActivity.kt`, `KhepriApplication.kt`, `di/AppConfigModule.kt`
- Create: `app/src/main/res/values{,-es,-pt-rPT,-pt-rBR}/strings.xml` with `app_name` only (Task 10 adds the rest)
- Create: `app/src/main/res/xml/network_security_config.xml` (no cleartext), `app/src/debug/res/xml/network_security_config.xml` (same name; adds cleartext for `10.0.2.2`)
- Create: `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`
- Delete: the placeholder `README.md` in each module directory when that module gets a `build.gradle.kts`

**Interfaces:**
- Produces: the plugin ids above. `khepri.core` = library + explicit API mode. `khepri.feature.domain` = `khepri.kotlin.jvm` + `:core:model`, `:core:common`. `khepri.feature.data` = library + hilt + own `:domain`, `:core:model`, `:core:common`, `:core:api`, `:core:network` (`:core:database`, `:core:datastore` added per module). `khepri.feature.ui` = library + compose + hilt + own `:domain`, `:core:ui`, `:core:designsystem`, `:core:navigation` (`:core:charts`, `:core:scene3d`, `:core:speech` added per module).
- Produces: `@ApiBaseUrl` qualifier (declared in `:core:common`, Task 3) provided by `:app`'s `AppConfigModule` from `BuildConfig.API_BASE_URL`. This is how library modules get the URL; they never read `:app`'s `BuildConfig`.

- [x] **Step 1:** Create the wrapper, settings, version catalog and `build-logic` with the plugin classes.
- [x] **Step 2:** Create `:app` with a Compose `MainActivity` showing `stringResource(R.string.app_name)`. Edge-to-edge enabled.
- [x] **Step 3:** Debug build type: `applicationIdSuffix ".debug"`, `buildConfigField API_BASE_URL "http://10.0.2.2:8090"`. Release: `"https://kheprios.com"`, R8 full mode. The manifest references `@xml/network_security_config`; the debug source set overrides it.
- [x] **Step 4:** Backup and data-extraction rules exclude `databases/` and `datastore/`.
- [x] **Step 5:** Run `./gradlew :app:assembleDebug :app:assembleRelease`. Expected: BUILD SUCCESSFUL.
- [x] **Step 6:** Install on an emulator; the app opens and shows "Khepri".
- [x] **Step 7:** Commit.

Done 2026-11-25. Deviations: `khepri.room` is added in Phase 2 with `:core:database` (Room is not approved yet); `di/AppConfigModule.kt` is added in Task 3 with the `@ApiBaseUrl` qualifier it provides; a `khepri.screenshot` plugin (Robolectric + Roborazzi, references in `src/test/screenshots`) was added so `khepri.feature.ui` and `:app` share it. Hilt, KSP, Robolectric (Android 17 image) and Roborazzi all build and run on AGP 9.5.2 with compileSdk 37.1.

### Task 2: Quality gates, CI and pre-commit

**Files:**
- Create: `build-logic/convention/src/main/kotlin/…/QualityConventionPlugin.kt` (id `khepri.quality`, applied by every other plugin)
- Create: `build-logic/detekt-rules/` with `NoHardcodedUiText` rule and its tests
- Create: `config/detekt/detekt.yml`, `config/lint/lint.xml`
- Create: `.github/workflows/ci.yml`, `.githooks/pre-commit`, `.github/pull_request_template.md`, `renovate.json`
- Modify: root `build.gradle.kts` (module-graph assertion, dependency-analysis), `README.md` (hook setup)

**Interfaces:**
- Produces: `./gradlew check` runs `spotlessCheck`, `detekt`, `lint`, `assertModuleGraph`, `buildHealth`, unit tests.

- [x] **Step 1:** Spotless with ktfmt (Kotlin style, width 120) for `*.kt` and `*.kts`; excludes `**/build/**`.
- [x] **Step 2:** detekt with the Compose rules plugin; no formatting rule set; `warningsAsErrors`; long method warn 100 / error 200; cyclomatic complexity ignores simple `when`; forbid `!!`.
- [x] **Step 3:** Lint: `warningsAsErrors`, `abortOnError`, no baseline; `MissingTranslation`, `ContentDescription` as errors. `UnusedResources` is an error, except that it ignores the generated `strings.xml` in `:core:ui` until Phase 6.
- [x] **Step 4:** `NoHardcodedUiText`: lint's `HardcodedText` covers XML only and this app has none, so a detekt rule fails on a string literal passed as `text` to `Text`, or as `contentDescription` or `label` anywhere. Previews (`@Preview`) and test sources are exempt. Use an equivalent rule from the Compose rules plugin if the installed version has one. Tests: a literal in `Text("Hi")` is reported; `Text(stringResource(R.string.x))` is not; a literal inside a `@Preview` function is not.
- [x] **Step 5:** Module-graph assertion over the transitive graph, with the rules in `docs/design.md` 6.1: feature domain sees only model and common; feature ui never reaches `:core:api`, `:core:network`, `:core:session` or any `data` module; no feature reaches another feature; only `:app` depends on `data`; core-to-core edges as in the table.
- [x] **Step 6:** Prove each gate bites, on a throwaway branch that is deleted afterwards: an unformatted line; a `!!`; `Text("Hi")`; a string present in `values` but missing from `values-es`; two temporary modules `:feature:a:ui` and `:feature:b:ui` with an edge between them. Confirm `./gradlew check` fails for each.
- [x] **Step 7:** `ci.yml`: on push and PR, JDK setup, Gradle cache, `./gradlew check :app:assembleRelease`. Generation reads only committed files, so CI needs no sibling repo.
- [x] **Step 8:** `.githooks/pre-commit` runs `spotlessApply` on staged Kotlin files and `detekt` on changed modules; add `git config core.hooksPath .githooks` to the repo `README.md`.
- [x] **Step 9:** PR template checklist: backend deployed first; strings in four locales; screenshots updated; parity checklist rows ticked; `syncContract --check` run.
- [x] **Step 10:** Run `./gradlew check`. Expected: BUILD SUCCESSFUL. Commit.

Done 2026-11-25. Deviations:
- detekt is 2.0.0-alpha.6 (owner's choice; compose-rules 0.6.8 needs it). Its `UnsafeCallOnNullableType` needs type resolution, which the `detekt` task does not run, so `!!` is caught by our own `NoNotNullAssertion` rule.
- `assertModuleGraph` is our own task (`ModuleGraphPlugin`): an allowlist of `from -> to` patterns with back-references, plus a transitive check that no `feature:x:ui` reaches `data`, `:core:api`, `:core:network` or `:core:session`. The jraska plugin was approved but is not used.
- Lint skips test sources: lint 9.5 crashes on Kotlin 2.4 test files (FIR resolve error). Re-check when AGP updates.
- `buildHealth` fails on unused dependencies and on runtime-only misuse. It ignores "declare this transitive dependency" advice (Compose and Dagger would need dozens of uncatalogued coordinates) and api-versus-implementation advice (design 6.1: every module edge is `implementation`). The dependency-analysis plugin must be applied in each module, not only the root, or it analyses nothing.
- Lint required a launcher icon. A placeholder adaptive icon (Solar Gold disc on Armor Navy) stands in until the brand emblem is exported for Android.
- Gates were proven in the working tree and reverted, not on a branch. Each of the five violations failed `check`.
- detekt keeps its rule set loaded in the Gradle daemon: after changing `build-logic/detekt-rules`, run `./gradlew --stop`.
- Open design conflict for the phase that adds speech: design 6.1 lets `feature:x:ui` use `:core:speech`, but `:core:speech` depends on `:core:network`, which `ui` must never reach. The transitive check will fail on that edge; decide then (likely a speech facade in `:core:ui`-level API, or `ui` → domain use case only).

### Task 3: `:core:model` and `:core:common`

**Files:**
- Create in `core/model/src/main/kotlin/com/fernandocorreia/khepri/core/model/`: `AppError.kt`, `Outcome.kt`, `Session.kt`, `DataScope.kt`
- Create in `core/common/src/main/kotlin/com/fernandocorreia/khepri/core/common/`: `DataChangeBus.kt`, `DispatcherProvider.kt`, `SessionGateway.kt`, `SignedInState.kt`, `SignOut.kt`, `TokenUnreadableException.kt`, `Qualifiers.kt`
- Create: `app/src/main/kotlin/com/fernandocorreia/khepri/di/CommonBindings.kt` (Hilt bindings for the JVM modules, which cannot hold Hilt modules themselves)
- Test: `core/common/src/test/kotlin/…/DataChangeBusTest.kt`, `core/model/src/test/kotlin/…/OutcomeTest.kt`

**Interfaces:**
- Produces:

```kotlin
// :core:model
sealed interface AppError {
    data class Unauthorized(val message: String?) : AppError
    data class NotFound(val message: String?) : AppError
    data class Conflict(val message: String?, val newerVersionJson: String?) : AppError
    data class FieldValidation(val message: String, val fields: Map<String, String>) : AppError
    data class Server(val status: Int, val message: String?, val code: String?) : AppError
    data class Refused(val code: String, val message: String?) : AppError
    data class Network(val interrupted: Boolean) : AppError
    data object Locked : AppError
    data object InvalidResponse : AppError
}
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Err(val error: AppError) : Outcome<Nothing>
}
inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R>
data class Session(val token: String, val userId: String, val expiresAt: java.time.Instant)
enum class DataScope { Day, Training, Nutrition, Social, Goals, Profile }

// :core:common
interface DataChangeBus { val changes: Flow<DataScope>; fun changed(scope: DataScope) }
interface DispatcherProvider { val main: CoroutineDispatcher; val io: CoroutineDispatcher; val default: CoroutineDispatcher }
/** How a feature reports a successful sign-in without seeing :core:session. */
interface SessionGateway { suspend fun signedIn(session: Session) }
/** True while the phase is SignedIn. Lets :core:navigation hold links without seeing :core:session. */
interface SignedInState { val signedIn: StateFlow<Boolean> }
enum class SignOutReason { Explicit, Unauthorized }
/** Multibound with Hilt; any module registers one. */
interface SignOutCleaner {
    /** Server calls that need a live token (push unregister). Run only for Explicit, before logout. */
    suspend fun beforeLogout() {}
    /** Local cleanup. Run for both reasons, after the session is gone. */
    suspend fun afterLogout(reason: SignOutReason)
}
/** A token exists but cannot be read right now (Keystore failure). The request is not sent; nobody is signed out. */
class TokenUnreadableException(cause: Throwable? = null) : java.io.IOException(cause)
@Qualifier annotation class ApiBaseUrl
@Qualifier annotation class ApplicationScope
```

- Hilt in `CommonBindings`: `SharedFlowDataChangeBus` as `DataChangeBus` (singleton), `DefaultDispatcherProvider`, `Clock.systemDefaultZone()`, an `@ApplicationScope CoroutineScope` (`SupervisorJob() + Dispatchers.Default`).

- [x] **Step 1:** Write `DataChangeBusTest`: `changed(Day)` is received by two active collectors; a collector that subscribes after the emission does not receive it; `changed` called with no collector returns immediately and does not throw.
- [x] **Step 2:** Run it; expect failure (class missing).
- [x] **Step 3:** Implement `SharedFlowDataChangeBus` (`MutableSharedFlow(replay = 0, extraBufferCapacity = 64)`, `tryEmit`).
- [x] **Step 4:** Write and pass `OutcomeTest` for `map` on `Ok` and `Err`.
- [x] **Step 5:** Add `CommonBindings` to `:app`.
- [x] **Step 6:** `./gradlew :core:model:test :core:common:test`. Expected: PASS. Commit.

Done 2026-11-25. Deviations: both modules are plain Kotlin with explicit API mode (new plugin `khepri.core.jvm`); `@ApiBaseUrl` is provided in `CommonBindings` (the Task 1 `AppConfigModule` was folded in); `javax.inject:javax.inject:1` is declared for `@Qualifier` and `@Inject` (it already ships with Hilt); `Session.toString()` leaves out the token so it never reaches a log.

### Task 4: `:core:api` — contract sync, generation, contract tests

**Files:**
- Create: `core/api/build.gradle.kts`, `core/api/contract.sha` (the SHA chosen in S2)
- Create: `core/api/contract/openapi.yaml`, `core/api/contract/medals.json`, `core/api/contract/golden/**` (written by `syncContract`, **committed**)
- Create: `build-logic/convention/src/main/kotlin/…/SyncContractTask.kt` (registers `syncContract`, with `--check`)
- Create: `build-logic/convention/src/main/kotlin/…/OperationIndexTask.kt` (generates `OperationIndex.kt` from the spec)
- Create: `core/api/normalize-spec.*` (from S1; no template overrides)
- Create: `core/api/src/main/kotlin/com/fernandocorreia/khepri/core/api/FlexibleInstantSerializer.kt`, `ApiJson.kt`, `GoldenMap.kt`
- Test: `core/api/src/test/kotlin/…/FlexibleInstantSerializerTest.kt`, `EnumToleranceTest.kt`, `GoldenContractTest.kt`, `RequestSchemaTest.kt`, `OperationIndexTest.kt`

**Interfaces:**
- Consumes: generator options, template overrides, the default-interface name, the unknown-enum member name, and the validator choice from `docs/spikes/S1-*.md`.
- Produces:
  - Generated models in `com.fernandocorreia.khepri.core.api.model`, all suffixed `Dto` (`TodayResponseDto`, `OutcomeDto`); Retrofit interfaces in `com.fernandocorreia.khepri.core.api`, every method `suspend` and returning `retrofit2.Response<T>`.
  - `object ApiJson { val default: Json; val strict: Json }`: `default` has `ignoreUnknownKeys = true`, `explicitNulls = false`, `encodeDefaults = false`; `strict` is the same with `ignoreUnknownKeys = false`.
  - `data class OperationEntry(val method: String, val pathTemplate: String, val operationId: String, val isPublic: Boolean, val isLong: Boolean)` and `object OperationIndex { val entries: List<OperationEntry>; fun find(method: String, path: String): OperationEntry? }`. `path` is the request path without `/api/v1`; templates match one path segment per `{param}`. `isPublic` = spec `security: []` except `logOut`. `isLong` = the eight operations in `docs/design.md` 7.3.
  - `const val UNKNOWN_ENUM = "UNKNOWN"` (S1: `enumNameMappings`, no template).

- [x] **Step 1:** `syncContract` reads `contract.sha` and, using `git -C ../north-client show <sha>:<path>`, writes `docs/api/openapi.yaml`, `docs/medals/medals.json` and every file matching `internal/*/testdata/*.golden.json` (all packages, not the iOS subset) into `core/api/contract/`. `--check` fails if any committed copy differs or is missing. It never reads the `north-client` working tree.
- [x] **Step 2:** Write `FlexibleInstantSerializerTest` (`java.time.Instant`):

```kotlin
@Test fun `reads whole seconds`() = assertEquals(Instant.parse("2026-09-24T07:15:00Z"), decode("\"2026-09-24T07:15:00Z\""))
@Test fun `reads nanoseconds`() = assertEquals(Instant.parse("2026-09-24T07:15:00.123456789Z"), decode("\"2026-09-24T07:15:00.123456789Z\""))
@Test fun `reads an offset`() = assertEquals(Instant.parse("2026-09-24T05:15:00Z"), decode("\"2026-09-24T07:15:00+02:00\""))
@Test fun `writes milliseconds`() = assertEquals("\"2026-09-24T07:15:00.000Z\"", encode(Instant.parse("2026-09-24T07:15:00Z")))
@Test fun `truncates to milliseconds`() = assertEquals("\"2026-09-24T07:15:00.123Z\"", encode(Instant.parse("2026-09-24T07:15:00.123456789Z")))
@Test fun `rejects a non-date`() { assertFailsWith<SerializationException> { decode("\"yesterday\"") } }
```

- [x] **Step 3:** Run; expect failure. Implement with `DateTimeFormatter.ISO_OFFSET_DATE_TIME` for reading. Run; expect PASS.
- [x] **Step 4:** Wire generation from `core/api/contract/openapi.yaml`: normalise (both nullable forms), generate with model suffix `Dto`, exclude `replyInConversation`, `resumeConversation`, every operation whose response declares `text/event-stream` and the 9 multipart operations; retag by first path segment; map `uuid` → `String`, `date-time` → `Instant` with the serializer, `date` → `LocalDate`. Output under `build/`; excluded from Spotless, detekt and explicit API.
- [x] **Step 5:** `EnumToleranceTest`: reflectively list every generated enum; assert each has a member named `UNKNOWN_ENUM`; decode `"definitely-not-a-value"` into one enum-typed field of a real response DTO and assert that member.
- [x] **Step 6:** `GoldenMap.kt`: golden file name → serializer, built from the mapping in `K/NorthKit/Tests/NorthAPITests/ContractTests.swift`, extended for the files iOS does not cover. `GoldenContractTest`: decode each committed golden file with `ApiJson.strict`; fail if a file in `contract/golden/` has no entry, or an entry has no file.
- [x] **Step 7:** `RequestSchemaTest`: for every generated request DTO, serialise a fully populated instance with `ApiJson.default`; fail on any key that is not a declared property of its schema in the spec (resolving `$ref` and `allOf`); then validate the JSON against the schema with the validator from S1. Also serialise one with optional fields null and assert no `null` values appear.
- [x] **Step 8:** `OperationIndexTest`: `find("POST", "/auth/login")?.isPublic == true`; `find("POST", "/auth/logout")?.isPublic == false`; the four passkey operations, `signUp`, `signInWithGoogle`, `signInWithApple`, `requestPasswordReset` are public; `find("GET", "/conversations/3f1c…")?.operationId == "getConversation"`; `find("POST", "/training/plans")?.isLong == true`; `find("GET", "/nope") == null`.
- [x] **Step 9:** `./gradlew syncContract --check :core:api:test`. Expected: PASS. Commit the contract copies with the code.

Done 2026-11-25 (548 tests). Deviations and findings:
- `:core:api` is a plain Kotlin module (`khepri.kotlin.jvm`); nothing in it needs Android.
- `GoldenMap.kt` is `src/test/resources/golden-map.txt` (164 entries: the 151 from iOS plus 13 found by strict-decoding each unmapped golden against every DTO). `tools` and `tools.readonly` are the MCP server's tool lists, not API responses, and are excluded by name.
- Retrofit uses our own `JsonConverterFactory` instead of the kotlinx converter, so every request passes two checks: it refuses `UNKNOWN` anywhere in the body, and it puts back `null` for properties that are required and nullable. That second one was a live bug: `HelpfulRequest.helpful` is required and `null` means "clear the vote", but `explicitNulls = false` sent `{}`.
- `RequestSchemaTest` fills every request DTO at every depth from its `SerialDescriptor` (values chosen from the schema's format, enum and minimum), checks keys recursively against the spec and validates the body; every 2xx response DTO decodes from its required properties alone.
- There are 55 generated model enums. Design 7.1's "59 enums" counted every `enum:` site, including the 9 query and path enums that live in the endpoint interfaces.
- `normalizeSpec` asserts 12 removed, 378 operations and 68 interfaces; `OperationIndex` holds all 390.
- The Gradle daemon needs `MaxMetaspaceSize=2g` once Hilt and the generator run in one build.

### Task 5: `:core:network` — clients, interceptors, error mapping

**Files:**
- Create in `core/network/src/main/kotlin/com/fernandocorreia/khepri/core/network/`: `TokenSource.kt`, `BearerInterceptor.kt`, `LanguageInterceptor.kt`, `LongCallInterceptor.kt`, `RetryInterceptor.kt`, `ApiCall.kt`, `ErrorEnvelope.kt`, `di/NetworkModule.kt`
- Test: `core/network/src/test/kotlin/…/BearerInterceptorTest.kt`, `LanguageInterceptorTest.kt`, `LongCallInterceptorTest.kt`, `RetryInterceptorTest.kt`, `ApiCallTest.kt`

**Interfaces:**
- Consumes: `OperationIndex`, `ApiJson`, `AppError`, `Outcome`, `TokenUnreadableException`, `@ApiBaseUrl`.
- Produces:

```kotlin
interface TokenSource {
    /** Null when signed out. Throws TokenUnreadableException when a token exists but cannot be read. */
    suspend fun token(): String?
    /** Called after a 401 on a request that carried a token. Must be cheap and idempotent. */
    fun onUnauthorized()
}
/** Supplies the phone's languages, most preferred first, as BCP 47 tags. */
fun interface PreferredLanguages { fun get(): List<String> }
suspend fun <T : Any> apiCall(block: suspend () -> Response<T>): Outcome<T>
suspend fun apiCallUnit(block: suspend () -> Response<Unit>): Outcome<Unit>   // 2xx with no body, e.g. 204
@Qualifier annotation class DefaultClient
@Qualifier annotation class StreamingClient
```

- Two OkHttp clients sharing one connection pool: default (connect 15 s, read 60 s) and streaming (read timeout none). Interceptors on both, outermost first: `LanguageInterceptor`, `BearerInterceptor`, `LongCallInterceptor` (default client only), `RetryInterceptor`. Retrofit at `"$apiBaseUrl/api/v1/"` on the default client, with `ApiJson.default`.
- `PreferredLanguages` default implementation reads `LocaleListCompat.getAdjustedDefault()`; tests inject a list.

- [x] **Step 1:** Write `BearerInterceptorTest` (MockWebServer):
  - `adds the token to a protected path`: header is `Bearer t0ken`.
  - `sends no token to a public path`: `POST /api/v1/auth/login` has no `Authorization`.
  - `logout carries the token`.
  - `401 with a token sent calls onUnauthorized`.
  - `401 on a public path does not call onUnauthorized`.
  - `401 when signed out does not call onUnauthorized`.
  - `unreadable token stops the request`: `token()` throws; the server receives nothing; the call fails with `TokenUnreadableException`; `onUnauthorized` is not called.
- [x] **Step 2:** Run; expect failure. Implement (look up the entry with `OperationIndex.find`). Run; expect PASS.
- [x] **Step 3:** `LanguageInterceptorTest`: `["pt-PT","en","es","fr"]` → `pt-PT, en;q=0.9, es;q=0.8`; a request that already has the header is unchanged; an empty list adds nothing.
- [x] **Step 4:** `LongCallInterceptorTest`: `POST /api/v1/training/plans` proceeds with a 300 s read timeout; `GET /api/v1/today` keeps 60 s.
- [x] **Step 5:** `RetryInterceptorTest`:
  - `GET is retried once after a dropped connection` (first response `DISCONNECT_AT_START`, second 200): 2 requests.
  - `GET is not retried twice`: two drops → failure, 2 requests.
  - `POST is never retried`: 1 request.
  - `an event stream is never retried`: `Accept: text/event-stream`, 1 request.
  - `a 500 is not retried`.
  - The delay is injected; production value 500 ms.
- [x] **Step 6:** `ApiCallTest`, in the iOS order:

| Response | Expected |
|---|---|
| 200 with body | `Ok(body)` |
| 204 via `apiCallUnit` | `Ok(Unit)` |
| 401 `{"error":{"message":"m"}}` | `Unauthorized("m")` |
| 404 | `NotFound(message)` |
| 409 with error envelope | `Conflict("m", null)` |
| 409 with a non-envelope JSON body | `Conflict(null, thatJson)` |
| 422 `{"error":{"message":"m","fields":{"email":"taken"}}}` | `FieldValidation("m", {"email":"taken"})` |
| 429 with `fields` | `FieldValidation(…)` |
| 429 without `fields` | `Server(429, "m", null)` |
| 403 `{"error":{"message":"m","code":"ai_consent_required"}}` | `Refused("ai_consent_required","m")` |
| 500 `{"error":{"message":"m","code":"import_partial"}}` | `Server(500, "m", "import_partial")` |
| 500 with no body | `Server(500, null, null)` |
| `SocketTimeoutException`, `ConnectException`, `UnknownHostException`, connection reset | `Network(interrupted = true)` |
| any other `IOException` | `Network(interrupted = false)` |
| `TokenUnreadableException` | `Locked` |
| 200 with undecodable body | `InvalidResponse` |

- [x] **Step 7:** Implement `apiCall`, `apiCallUnit` and `ErrorEnvelope`. Run; expect PASS.
- [x] **Step 8:** `NetworkModule` provides both clients, `Retrofit`, and each generated interface (`retrofit.create(...)`). Commit.

Done 2026-11-25 (35 tests). Deviations:
- `PreferredLanguages` reads `android.os.LocaleList.getAdjustedDefault()` (API 24, below minSdk 28), so `:core:network` needs no appcompat.
- The language and long-call tests share one file, `LanguageAndLongCallTest`.
- Both clients set `retryOnConnectionFailure(false)`, so `RetryInterceptor` is the only retry. It does not retry a cancelled call.
- `ApiServices` creates each generated interface once (`services.create<TrainingApi>()`); data modules bind the interfaces they use instead of `NetworkModule` providing all 68.
- `apiCall` also maps `IllegalArgumentException` (thrown by the converters, for example on `UNKNOWN`) to `InvalidResponse`.
- `khepri.hilt` no longer adds `hilt-android-testing` to every module; `:app` declares it. Dependency analysis ignores `hilt-android` as unused, because Hilt modules reach it only through generated code.
- Android library modules run JUnit 5 for unit tests.

### Task 6: `:core:network` — SSE and multipart helpers

**Files:**
- Create: `core/network/src/main/kotlin/…/sse/SseEvent.kt`, `SseClient.kt`, `multipart/UriRequestBody.kt`
- Test: `core/network/src/test/kotlin/…/sse/SseClientTest.kt`, `multipart/UriRequestBodyTest.kt`

**Interfaces:**
- Produces:

```kotlin
data class SseEvent(val name: String?, val data: String)
class SseException(val error: AppError) : Exception()
class SseClient @Inject constructor(@StreamingClient client: OkHttpClient) {
    /** Cold. Completes when the server closes the stream. Fails with SseException on a non-2xx status. */
    fun events(request: Request): Flow<SseEvent>
}
class UriRequestBody(resolver: ContentResolver, uri: Uri, contentType: MediaType) : RequestBody()
```

- [x] **Step 1:** `SseClientTest`: a body of `event: token\ndata: {"text":"Hi"}\n\n: ping\n\nevent: done\ndata: {}\n\n` yields two events (`token`, `done`) and completes; comments are skipped; a 403 with an error envelope fails with `SseException(Refused(…))`; cancelling the collector closes the connection.
- [x] **Step 2:** Run; expect failure. Implement on the OkHttp SSE artefact with `callbackFlow`. Run; expect PASS.
- [x] **Step 3:** `UriRequestBodyTest` (Robolectric, a temp file URI): streams the bytes; reports `contentLength()` when the resolver knows it, `-1` otherwise.
- [x] **Step 4:** Commit.

Done 2026-11-25 (9 tests). Deviations:
- `SseException` carries the cause. A stream that fails before a response maps like `apiCall`: an unreadable token is `Locked` and an `IOException` is `Network`.
- `SseClient` sets `Accept: text/event-stream`, so `RetryInterceptor` never retries it.
- `UriRequestBody` reads the length from `openAssetFileDescriptor` and reopens the stream on every `writeTo`.
- Robolectric tests in JUnit 5 modules run on `junit-vintage-engine`. It comes from the approved JUnit BOM and has no separate version.
- Also added `RetrofitStackTest`, which drives the production client stack through Retrofit's async suspend path. It checks that an unreadable token is `Locked` with nothing sent, that a 401 signs out, that query enums use their wire value and that an undecodable body is `InvalidResponse`.

### Task 7: `:core:datastore` and `:core:session`

**Files:**
- Create in `core/datastore/src/main/kotlin/…/core/datastore/`: `SessionStore.kt`, `TokenCipher.kt`, `KeystoreTokenCipher.kt`, `di/DataStoreModule.kt`
- Create in `core/session/src/main/kotlin/…/core/session/`: `AppPhase.kt`, `SessionRemote.kt`, `SessionRemoteImpl.kt`, `StoredTokenSource.kt`, `UnauthorizedSignal.kt`, `SessionManager.kt`, `di/SessionModule.kt`
- Test: `core/datastore/src/test/…/SessionStoreTest.kt`, `core/datastore/src/androidTest/…/KeystoreTokenCipherTest.kt`, `core/session/src/test/…/SessionManagerTest.kt`, `StoredTokenSourceTest.kt`

**Interfaces:**
- Consumes: `Session`, `SessionGateway`, `SignedInState`, `SignOutCleaner`, `SignOutReason`, `TokenUnreadableException`, `TokenSource`, `apiCall`, the generated interface holding `getMe` and `logOut` (name from S1).
- Produces:

```kotlin
// :core:datastore
interface TokenCipher { fun encrypt(plain: ByteArray): ByteArray; fun decrypt(sealed: ByteArray): ByteArray }   // AES-GCM Keystore key in production
interface SessionStore {
    /** Throws TokenUnreadableException when stored data cannot be decrypted right now. Leaves the data in place. */
    suspend fun read(): Session?
    suspend fun write(session: Session)
    suspend fun clear()
}

// :core:session
sealed interface AppPhase {
    data object Launching : AppPhase; data object Locked : AppPhase; data object SignedOut : AppPhase
    data object Onboarding : AppPhase; data object SignedIn : AppPhase
    data class Failed(val error: AppError) : AppPhase
}
/** Narrow port over the generated client, so SessionManager tests need no HTTP. */
interface SessionRemote {
    /** GET /me. True when onboarding is complete. */
    suspend fun onboardingComplete(): Outcome<Boolean>
    suspend fun logout(): Outcome<Unit>
}
/** onUnauthorized() posts here; SessionManager collects. Breaks the TokenSource → client → SessionManager cycle. */
class UnauthorizedSignal { val events: SharedFlow<Unit>; fun post() }
/** The only TokenSource. Depends on SessionStore and UnauthorizedSignal only. */
class StoredTokenSource(store: SessionStore, signal: UnauthorizedSignal) : TokenSource
class SessionManager(
    store: SessionStore, remote: SessionRemote, signal: UnauthorizedSignal,
    cleaners: Set<@JvmSuppressWildcards SignOutCleaner>, @ApplicationScope scope: CoroutineScope,
) : SessionGateway, SignedInState {
    val phase: StateFlow<AppPhase>
    suspend fun start()                        // read session, GET /me, decide the phase
    override suspend fun signedIn(session: Session)   // write, then GET /me to choose Onboarding or SignedIn
    suspend fun signOut(reason: SignOutReason)
    suspend fun retry()                        // from Failed or Locked
}
```

- `SessionManager` collects `UnauthorizedSignal.events` in `scope` and calls `signOut(Unauthorized)`, ignoring events while the phase is already `SignedOut` (concurrent 401s run sign-out once).
- `:app` calls `retry()` while `Locked` on each `ProcessLifecycleOwner` `ON_START` and on `Intent.ACTION_USER_UNLOCKED`, as iOS retries when protected data becomes available.

- [x] **Step 1:** `SessionStoreTest` (Robolectric, fake `TokenCipher` that XORs): write then read returns the same session; clear then read returns null; a cipher that throws on decrypt makes `read()` throw `TokenUnreadableException` and the data is still there afterwards.
- [x] **Step 2:** Implement `SessionStore` on DataStore. `KeystoreTokenCipherTest` is an instrumented test (emulator): round trip; it is not part of `check` and runs in the Phase 6 device suite.
- [x] **Step 3:** `StoredTokenSourceTest`: returns the stored token; null when signed out; propagates `TokenUnreadableException`; `onUnauthorized()` posts to the signal.
- [x] **Step 4:** `SessionManagerTest` with fakes. iOS `AppModelTests` has four cases (three locked cases, one with no session); port those, and add the rest:
  - no session → `SignedOut`
  - session, `onboardingComplete` = false → `Onboarding`; true → `SignedIn`
  - session, `/me` network error → `Failed`; `retry()` re-runs and reaches `SignedIn`
  - `read()` throws → `Locked`; the session is not cleared; `retry()` after the cipher recovers → `SignedIn`
  - `signedIn(session)` writes the session, calls `/me`, and lands on `Onboarding` or `SignedIn`
  - a posted signal → no `beforeLogout`, no logout call; store cleared; `afterLogout(Unauthorized)` on every cleaner; phase `SignedOut`
  - two signals posted together → cleaners run once
  - `signOut(Explicit)` runs in this order, asserted with a recording fake: every `beforeLogout()`, then `logout()` (the token is still readable), then store cleared, then every `afterLogout(Explicit)`, then phase `SignedOut`
  - a failing `beforeLogout()` or `logout()` does not stop the local clear
  - an `expiresAt` in the past does not sign out by itself
  - `signedIn` (the `SignedInState` flow) is true exactly while the phase is `SignedIn`
- [x] **Step 5:** Implement. `SessionModule` binds `StoredTokenSource` as `TokenSource`, `SessionManager` as `SessionGateway` and `SignedInState`, and `SessionRemoteImpl` (which wraps the generated calls with `apiCall`). Run; expect PASS. Commit.

Done 2026-11-25 (22 tests, plus the device-only `KeystoreTokenCipherTest`). Deviations:
- `SessionStoreTest` runs on the plain JVM: `DataStoreSessionStore` takes a `DataStore<Preferences>`, so the test builds one on a temp file and needs no Robolectric. It also checks that the token is not stored in the clear.
- A key that is gone for good (missing alias, or `AEADBadTagException`) throws `SealedDataLostException`. The store drops that data and reads as signed out, so a lost key cannot leave the user `Locked` forever. Every other cipher failure is `TokenUnreadableException`, and the data stays.
- `SessionManager` runs every transition under one `Mutex`. A 401 on `/me` signs out through the same path as the signal, and a signal that arrives while already `SignedOut` is ignored. The signal collector subscribes with `UNDISPATCHED` in `init`, so a 401 posted before the first suspension is not lost.
- `:app` has `SessionLifecycle`, which calls `start()` at launch and `retry()` while `Locked` on `ON_START` and on `ACTION_USER_UNLOCKED`. It was smoke-launched on the emulator with no crash.
- Sign-out clearing the tab stores (amendment) is a `SignOutCleaner` that `:core:navigation` registers in Task 9. `afterLogout` runs before the phase becomes `SignedOut`.
- `KeystoreTokenCipherTest` still needs `androidx.test:runner` (not yet approved) to run on a device. That waits for the Phase 6 device suite.

### Task 8: `:core:designsystem`, `:core:ui`, `:core:testing`

**Files:**
- Create: `tools/sync_theme.py` (reads the same web CSS source as `K/scripts/sync-theme.py`; writes `core/designsystem/src/main/kotlin/…/theme/KhepriColors.kt`)
- Create: `core/designsystem/src/main/kotlin/…/theme/KhepriTheme.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Appearance.kt`; `component/SurfaceCard.kt`, `ProminentButton.kt`, `KhepriTextField.kt`, `Eyebrow.kt`
- Create: `core/designsystem/src/main/res/font/` (Geist, Geist Mono, Marcellus copied from `K/NorthKit/Sources/NorthKit/Resources/Fonts/`, renamed to lower-case resource names)
- Create: `core/ui/src/main/kotlin/…/Loadable.kt`, `ActionState.kt`, `ReduceMotion.kt`, `Haptics.kt`, `ErrorState.kt`
- Create: `core/testing/src/main/kotlin/…/MainDispatcherRule.kt`, `TestDispatcherProvider.kt`, `FakeDataChangeBus.kt`
- Test: `core/designsystem/src/test/…/ThemeTokensTest.kt`, `ComponentScreenshotTest.kt`

**Interfaces:**
- Produces: `KhepriTheme(appearance: Appearance = Appearance.System, content: @Composable () -> Unit)`, `KhepriTheme.colors`, `.spacing`, `.shapes`; `enum class Appearance { System, Light, Dark }` (in `:core:designsystem`); `Loadable<T>` and `ActionState` as in `docs/design.md` 6.3; `LocalReduceMotion: ProvidableCompositionLocal<Boolean>` (true when the animator duration scale is 0); `enum class Haptic { Success, Selection, Warning, Error, Heavy }` with `fun View.perform(haptic: Haptic)`.

- [x] **Step 1:** Read `K/scripts/sync-theme.py` to find its input; write `tools/sync_theme.py` to emit light and dark `Color` values for every token.
- [x] **Step 2:** `ThemeTokensTest`: light Background `#F5F2EB`, dark Background `#0C111B`, light Signal `#846328`, dark Signal `#E0B86A`, light Destructive `#9E413D`, dark Destructive `#D27E7C`; radii 6, 10, 12, 16 dp; spacing 4, 8, 12, 16, 20, 24 dp.
- [x] **Step 3:** Theme: Material 3 colour scheme built from tokens; dynamic colour off; Geist for all text styles, Marcellus as `displayBrand`, Geist Mono as `mono`.
- [x] **Step 4:** Components: cards have no shadow; prominent button; text field with a trailing icon that has a content description and a 48 dp target.
- [x] **Step 5:** Roborazzi screenshots of each component: light/dark × font scale 1.0/2.0. Record and commit the goldens. Add `verifyRoborazzi` to `check`.
- [x] **Step 6:** `./gradlew :core:designsystem:check :core:ui:check`. Expected: PASS. Commit.

Done 2026-11-25 (11 tests, 26 screenshots). Deviations:
- Colours are **Display P3**: `Color(r, g, b, a, ColorSpaces.DisplayP3)`, with the same components iOS writes into its colour sets. The plan's hex values are those P3 components. `tools/sync_theme.py` uses the iOS script's OKLCH → P3 conversion and copies the three iOS-pinned tokens (BodyIdle, BodyShell, HeatLow) from the iOS colour sets.
- Material 3 mapping: `primary` = `signal` and `onPrimary` = `primaryForeground`. iOS tints controls with signal, and the web `--primary` (navy/cream) becomes M3 `secondary`.
- `Spacing` (4/8/12/16/20/24) is new; iOS has no spacing enum. `Radii` mirrors `NorthRadius`.
- `KhepriTextStyles` holds `displayBrand` (Marcellus), `mono` (Geist Mono) and `eyebrow`. Geist and Geist Mono are variable fonts, loaded at weights 300 to 700 through `FontVariation`. The OFL licences are in `core/designsystem/licenses/`.
- `TrailingAction` takes a `Painter` (material-icons is not a dependency). `clearIcon()` is the shared clear glyph.
- `:core:testing` has `MainDispatcherExtension` (JUnit 5) instead of a JUnit 4 rule, because unit tests run on JUnit 5.
- Screenshot tests render one composition per test and switch variants through state, capturing `onRoot()`. Separate `captureRoboImage {}` calls in one test sometimes recorded blank frames.
- `khepri.screenshot` makes `check` depend on `verifyRoborazziDebug` and adds the vintage engine with the JUnit BOM. Dependency analysis treats the roborazzi group as one bundle.
- `:core:ui` `ErrorState` has its own strings in the four locales ("Offline" and "Try Again" reuse the iOS translations). It is tested with two screenshots and a click.

### Fable review of Tasks 4-7 (2026-11-25)

The owner's rule is a Fable review after every task. This review covered Tasks 4-7. It made 11 findings, and all were fixed in one commit:

1. A storage error at launch crashed the app. The session DataStore now has a `ReplaceFileCorruptionHandler`, any `IOException` in `restore()` moves to `Locked`, and `signedIn` returns `Outcome<Unit>` (`Err(Locked)` when the session cannot be saved; `SessionGateway` changed).
2. Sign-out could be cancelled halfway when a cleaner tore down the calling ViewModel. Every `SessionManager` transition now runs in the application scope (`scope.async { lock.withLock { } }.await()`). A test cancels the caller from inside a cleaner.
3. An expired URI grant crashed OkHttp's dispatcher with a `SecurityException`. `UriRequestBody` now turns every open failure into an `IOException`.
4. The token was sent to any URL. `BearerInterceptor` now sends it only to spec operations on the `@ApiBaseUrl` origin.
5. Real dropped connections (EOF, stream reset, SSL) were mapped to `interrupted = false`. The rule is now inverted; see design 7.3.
6. A half-open stream never failed. The streaming client now has a 300 s read timeout, the same as iOS.
7. A failed body read in `SseClient.onFailure` hung the flow. The read is now wrapped, and a test covers a stream that drops mid-way.
8. A late 401 for an old token could sign out a new session. `TokenSource.onUnauthorized(sentToken)` now signs out only while that token is still stored.
9. A 409 with no JSON body became `Server(409)`. A 409 is now always `Conflict`, and an empty `fields` or `code` is ignored, as on iOS.
10. A key lost mid-run left a signed-in shell sending bare requests. `StoredTokenSource` now posts the signal when a protected request finds no session.
11. `ApiServices` is now `@Singleton`. The `ACTION_USER_UNLOCKED` receiver was removed: the app is not direct-boot aware, so it never fired, and `ON_START` covers the retry.

### Fable review of Task 8 (2026-11-25)

Eleven findings, all fixed (11 after the owner's verdict).

1. The card edge replaced the hairline alpha instead of scaling it. It was 8 to 10 times stronger than iOS `hairline.opacity(0.6)`; it now multiplies.
2. The text field outline was `border` (about 1.4:1). It is now the web's `--input` token (3.3:1 light, 3.4:1 dark). `sync_theme.py` emits `input`.
3. Nine Material slots were left at baseline purple (`surfaceTint`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, `tertiaryContainer`, `errorContainer` and the rest). Every slot is now mapped from tokens.
4. `outlineVariant` was the 10 to 12% hairline, which made the outlined button edge invisible. It is now `border`.
5. `ErrorState` scrolls, so Try Again stays reachable at 200% font. A blank server message falls back to the default copy.
6. Screenshots were 320 px at mdpi. They are now 360 dp at xhdpi. Added: `ErrorState` at 2x font and in pt-PT.
7. `ThemeTokensTest` now checks WCAG AA on the Material scheme: 4.5:1 for every text pair and 3:1 for `outline`, in both modes. `sync_theme.py --check` detects a hand-edited or stale `KhepriColors.kt`. It needs the web checkout, so it runs locally, not in CI.
8. The two colour schemes are built once, not on every recomposition.
9. `KhepriTextField` takes `visualTransformation`, `keyboardActions` and `enabled`, which the sign-in screen needs. `TrailingAction` is a data class.
10. `:core:testing` exposes coroutines-test and JUnit Jupiter as `api`; `:core:ui` exposes `:core:model`.
11. Material's baseline letter spacing (0.5 sp on `bodyLarge`, tuned for Roboto) was applied to Geist, where iOS uses none. On the owner's verdict (2026-11-26) every Material style now has 0 sp; only the eyebrow keeps its 1.5 sp, as on iOS.

Note: Material 3 1.5 outlined buttons label in `onSurfaceVariant` (muted), not `primary`. That is the library default and is kept.

### Task 9: `:core:navigation` — destinations and deep links

**Files:**
- Create: `core/navigation/src/main/kotlin/…/AppTab.kt`, `AppDestination.kt`, `DeepLinkParser.kt`, `Navigator.kt`, `PendingLinks.kt`, `EntryInstaller.kt`
- Test: `core/navigation/src/test/…/DeepLinkParserTest.kt`, `NavigatorTest.kt`

**Interfaces:**
- Consumes: `SignedInState`; the back-stack model from `docs/spikes/S9-*.md`.
- Produces:

```kotlin
enum class AppTab { Today, Coach, Training, Progress, More }
sealed interface AppDestination {
    data class Tab(val tab: AppTab) : AppDestination; data object Settings : AppDestination
    data class TrainingDay(val day: Int) : AppDestination; data class StartWorkout(val day: Int) : AppDestination
    data class NextWorkout(val start: Boolean) : AppDestination
    data class Section(val id: String) : AppDestination; data class Decision(val id: String) : AppDestination
    data class Goal(val id: String) : AppDestination
    data class CheckIn(val mood: Int) : AppDestination; data object CheckInFlow : AppDestination
    data class Invite(val code: String) : AppDestination; data class CrewJoin(val code: String) : AppDestination
    data class CrewChannel(val crewId: String, val channelId: String?) : AppDestination
    data class Exercise(val slug: String) : AppDestination; data class Muscle(val id: String) : AppDestination
    data object WeeklyReview : AppDestination; data object Horizon : AppDestination; data object FirstWeekReview : AppDestination
    data class AskCoach(val draft: String) : AppDestination; data class Conversation(val id: String) : AppDestination
}
object DeepLinkParser { fun parse(link: String): AppDestination?; val sections: Set<String> }
interface PendingLinks { suspend fun put(destination: AppDestination); suspend fun take(): List<AppDestination> }  // DataStore-backed in :app
interface Navigator { fun open(destination: AppDestination); fun open(link: String): Boolean }
```

- `sections` = goals, check-ins, progress-check, reports, memories, knowledge, nutrition, care, mind, decisions, friends, crews, inbox, daily, awards.

- [x] **Step 1:** Write `DeepLinkParserTest` as a table. First port every case from the iOS router tests (`K/khepriTests`: the router, shell and `MorePath` tests). The rows below were derived by reading `destination(for:)` in `K/khepri/App/AppRouter.swift`, not by running it; if an iOS test disagrees with a row, the iOS test wins and this table is corrected. (U = a valid UUID in upper case; u = the same lower-cased.)

| Link | Expected |
|---|---|
| `khepri://today`, `/app`, `/app/today` | `Tab(Today)` |
| `khepri://chat`, `/app/coach/abc` | `Tab(Coach)` |
| `khepri://training`, `/app/workouts`, `/app/activity` | `Tab(Training)` |
| `khepri://training/next` | `NextWorkout(false)` |
| `khepri://training/next/start` | `NextWorkout(true)` |
| `khepri://training/plan1/3` | `TrainingDay(3)` |
| `khepri://training/plan1/3/start` | `StartWorkout(3)` |
| `khepri://training/plan1/x` | `Tab(Training)` |
| `/app/insights`, `/app/fitness` | `Tab(Progress)` |
| `khepri://progress` | `Tab(Progress)` |
| `/app/progress`, `https://kheprios.com/app/progress` | `Section("progress-check")` |
| `khepri://check-ins?mood=4` | `CheckIn(4)` |
| `khepri://check-ins?mood=9`, `khepri://check-ins` | `Section("check-ins")` |
| `khepri://decisions/U` | `Decision(U)` |
| `khepri://decisions/not-a-uuid` | `Section("decisions")` |
| `khepri://goals/U` | `Goal(U)` |
| `/app/crews/U` | `CrewChannel(u, null)` |
| `/app/crews/U/c/U2` | `CrewChannel(u, u2)` |
| `/app/crews/U/x/U2` | `Section("crews")` |
| `https://kheprios.com/i/AbC123xYz9` | `Invite("abc123xyz9")` |
| `https://kheprios.com/i/short` | `null` |
| `https://kheprios.com/i/c/AbC123xYz9` | `CrewJoin("abc123xyz9")` |
| `/app/first-week/review` | `FirstWeekReview` |
| `/app/first-week` | `Tab(Today)` |
| `khepri://exercises/push-up` | `Exercise("push-up")` |
| `khepri://exercises` | `Tab(Training)` |
| `khepri://settings` | `Settings` |
| `khepri://weekly`, `khepri://horizon`, `khepri://more` | `WeeklyReview`, `Horizon`, `Tab(More)` |
| `khepri://care` | `Section("care")` |
| `khepri://nonsense`, `https://example.com/app/today`, `mailto:x@y.z` | `null` |

- [x] **Step 2:** Run; expect failure. Implement as a pure function following `destination(for:)` line by line. Run; expect PASS.
- [x] **Step 3:** `NavigatorTest`: `open(Goal(id))` selects More and produces the stack [More root, Goals, Goal(id)]; `open(CheckInFlow)` keeps the tab and adds a sheet entry; opening the same `CrewChannel` twice delivers two commands; a destination opened while `SignedInState.signedIn` is false is held and delivered once it turns true; `Invite` and `CrewJoin` go through `PendingLinks.put` so they survive process death and sign-up.
- [x] **Step 4:** Implement `Navigator` on the back-stack model from S9 (per-tab stacks; re-select pops to root).
- [x] **Step 5:** `./gradlew :core:navigation:test`. Expected: PASS. Commit.

**Done (2026-11-26).** 121 tests in `:core:navigation`: 105 parser rows (every iOS `ShellTests` row, the plan rows, the traps), 13 planner, 5 queue, 9 state, 3 Robolectric `TabHost`. The design went to the advisor (Fable was blocked by the context-mode hook). Deviations:

- **Stacks live in an activity-scoped Hilt ViewModel, `NavigationState`,** not in `rememberNavBackStack`. It holds a `SnapshotStateList<Route>` per tab, the selected tab and one `ViewModelStoreProvider` per tab, and saves JSON in `SavedStateHandle`; unreadable state starts from the roots. Being a ViewModel puts it above the phase switch by construction.
- **One-shot payloads are `ScreenAsk`s, not route fields,** as the iOS router's flags (`startsWorkout`, `checkInMood`, `coachDraft`, `opensNextWorkout`, `crewChannelRequest`, invite and crew-join counters, plus re-select reload). Asks are consumed once and not saved, so a restored stack never replays one.
- **`NavPlanner` is a pure function of (stacks, command)** and follows `AppRouter.open(_:)`, `MorePath.opening(section:)` and `CrewLinkLanding`: a link for the crew whose space is on top of More asks that space to switch channel; any other crew replaces the space, with a fresh `opening` id. Sheets are pushed on the current tab, never twice, and a sheet replaces a sheet on top. The membership check iOS does before opening a space belongs to the crews feature.
- **Gating:** `QueuedNavigator` keeps a pending list. `NavigationState` takes it whole while signed in. Invite and crew-join codes are written to `PendingLinks` before their command is queued; a disk failure still queues it.
- **Sign-out:** the `SignOutCleaner` resets on the main thread before the phase changes (stacks to roots, every store cleared, asks and queue dropped, `epoch` bumped so `TabHost` drops all composed state). The signed-in true-to-false edge resets again, for a screen recreated in between.
- **Entries replaced on a tab that is not composed** get `clearKey` at once, and their saved state is removed from the tab's `SaveableStateHolder` when the tab shows again (`TabHostTest`). The composed tab is left to the Navigation 3 decorators, which clear after the exit animation. Every route registers through `screen<T>()`, which uses `Route.contentKey()` and gives `Sheet` routes the bottom-sheet metadata.
- **Navigation 3 1.2.2 ships `runtime.deeplink` (URI pattern matchers) and `runtime.result` (`ResultEffect`).** Neither is used: patterns cannot express "a malformed id falls back to the section", and iOS is the source of truth. `ResultEffect` waits for the first screen that needs a result from another.
- **Moved to Task 12:** the shell around `TabHost` (Scaffold, tab bar, `hidesTabBar`), the DataStore `PendingLinks`, link consumption with `setIntent`, and the Hilt ViewModel with an assisted route key and `SavedStateHandle` under the per-tab provider, including process death on the emulator.
- **Build logic:** `compose-ui-tooling-preview` is no longer added by `khepri.compose`; `:app`, `:core:designsystem` and `:core:ui` declare it.
- **Fixed on the way:** `error_state_server.png` had been re-recorded as an empty window in 6725eab. Dark `ErrorState` captures were intermittently blank under Robolectric when the composition started in Dark; the test now draws a Light frame first, as `ComponentScreenshotTest` does (6 of 6 recordings stable).


**Fable review of Task 9 (2026-11-26).** The parser and planner match iOS for every ASCII link. All seven lifecycle findings are fixed:

1. Saved stacks now carry their owner. `SignedInState` exposes `account` (the user id while signed in) in place of a boolean. Stacks restored for another account start from the roots, and links queued meanwhile still land. This covers a sign-out written after `onSaveInstanceState` and a token rejected at launch.
2. A tab that leaves composition is settled: entries it showed and no longer holds are cleared and marked stale. This catches a pop and a tab change in the same frame. `TabHost` reports `composing(tab)` and `leaving(tab)`, so a late dispose cannot unsettle the tab now shown.
3. `QueuedNavigator` stamps each command with a generation. `drop()` also discards commands still in the intake or waiting on `PendingLinks`.
4. A tab that is left loses its sheet, as on iOS. Sheets are never saved.
5. The parser reads the raw query and decodes each item, accepts ASCII digits only (Swift `Int()`), and percent-encodes illegal characters before giving up (`khepri://exercises/bench press`).
6. `epoch` is saved.
7. The sign-out cleaner suspends `TabHost` until the next sign-in, so no screen loads with a session being torn down.

Also: the entry provider is remembered per installer set, so the sheet scene keeps its identity across recompositions.

### Task 10: Strings pipeline

**Files:**
- Create: `tools/convert_strings.py`, `tools/check_translations.py`, `tools/strings-keys.json` (stable key map, committed), `tools/test_convert_strings.py`
- Create: `core/ui/src/main/res/values{,-es,-pt-rPT,-pt-rBR}/strings.xml` (generated, committed)

**Interfaces:**
- Produces: `python3 tools/convert_strings.py` regenerates the four files from `K/khepri/Resources/Localizable.xcstrings`, `K/KhepriWidgets/Localizable.xcstrings`, `K/KhepriShare/Localizable.xcstrings` and `K/NorthKit/Sources/NorthAPI/Resources/Localizable.xcstrings`; `python3 tools/check_translations.py` exits non-zero when any key is missing from any locale in any module.

- [x] **Step 1:** Tests for the converter: a plain key; a key with one `%@` and one `%lld` becomes `%1$s` and `%2$d`; a plural variation becomes `<plurals>`; apostrophes and `&` are escaped; a key already in `strings-keys.json` keeps its name when the English text changes; a new key gets a generated snake-case name.
- [x] **Step 2:** Implement. Run on the real catalogs. Expected: four files with the same key set.
- [x] **Step 3:** Wire `check_translations.py` into `check` and CI. (Lint `MissingTranslation` also catches it; the script covers the per-module case and gives a readable list.)
- [x] **Step 4:** Commit.

**Done (2026-11-26).** 1,999 strings and 2 plurals per locale from 2,080 catalog keys (75 shared between catalogs, emitted once, preferring a translated copy). 19 tool tests run in `check` as `testStringTools`; `checkTranslations` runs there too, so CI covers both. Deviations:

- **`tools/strings-android.json` holds copy only Android has.** iOS shows raw error text and has no localized error screen, so Task 8's five `error_*` strings live there and keep their names.
- **Left out and listed by the converter:** 4 keys iOS has not translated (`High`, `Likely`, `Line`, the long body-figure credit) and 4 keys marked stale (a stale copy no longer hides a live one in another catalog). iOS falls back to English for the first four; Android would fail `MissingTranslation`. Translate them in the iOS catalog, then regenerate.
- **A string with no arguments is literal** (`formatted="false"`), as SwiftUI shows `About 60% of the sets`. A translation whose arguments differ from the English is rejected, not emitted (none today). Spanish and Portuguese plurals get `many` copied from `other` (CLDR).
- **Lint's `PluralsCandidate`, `TypographyDashes`, `Typos` and `UnusedResources` are off in the generated files only.** The two typos are false positives (pt-PT `quilómetro`, "check-in in one tap"). The 82 plural candidates are real ("Check-in streak, 1 days") and exist on iOS too: fix them as plural variations in the iOS catalog and they regenerate as `<plurals>`. The 5 em dashes in iOS copy are kept as is.
- `convert_strings.py --check` needs the iOS checkout beside this repo, so it is not in CI; run it after pulling iOS copy changes.


**Fable review of Task 10 (2026-11-26).** Fixed:

- A stale copy no longer shadows a live one in another catalog; this restores 3 widget strings.
- The key map is never pruned, so a key that drops out for a run gets its old name back and no other key takes it.
- A run with a rejected key writes nothing.
- The tools import on macOS system Python 3.9 (`from __future__ import annotations`).
- An Android-only name that equals a mapped name fails.
- Android-only strings must cover every locale with the same arguments; English-only entries are emitted as `translatable="false"`.
- Literal strings reject unsupported specifiers (`%02d`, `%s`) and translations that add arguments.
- Empty keys, empty values and plurals without `other` are rejected.
- Whitespace-only strings escape correctly.
- `check_translations.py` also catches a kind change, an empty translation and a plural without `other`, in every source set.

32 tool tests. Open: whether iOS groups `%lld` digits (12,345) in `Text` interpolation. 38 keys pair a number with a unit; check on a device before porting the first of those screens, and emit `%N$,d` if it does.

### Task 11: `:feature:auth` first slice — email sign-in and sign-up

**Files:**
- Create in `feature/auth/domain`: `model/SignUpForm.kt`, `repository/AuthRepository.kt`, `usecase/SignInWithEmail.kt`, `SignUpWithEmail.kt`, `RequestPasswordReset.kt`
- Create in `feature/auth/data`: `repository/AuthRepositoryImpl.kt`, `remote/AuthMappers.kt`, `di/AuthDataModule.kt`
- Create in `feature/auth/ui`: `login/LoginScreen.kt`, `LoginViewModel.kt`, `LoginUiState.kt`, `ForgotPasswordSheet.kt`, `PasswordStrengthMeter.kt`, `navigation/AuthEntries.kt`
- Test: `feature/auth/domain/src/test/…/SignInWithEmailTest.kt`, `feature/auth/data/src/test/…/AuthRepositoryImplTest.kt`, `feature/auth/ui/src/test/…/LoginViewModelTest.kt`, `LoginScreenScreenshotTest.kt`

**Interfaces:**
- Consumes: generated auth operations, `apiCall`, `SessionGateway`.
- Produces:

```kotlin
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Outcome<Session>
    suspend fun signUp(form: SignUpForm): Outcome<Session>          // SignUpForm(name, email, password, confirmation, timezone: ZoneId)
    suspend fun requestPasswordReset(email: String): Outcome<Unit>
}
class SignInWithEmail(repo: AuthRepository, gateway: SessionGateway) { suspend operator fun invoke(email: String, password: String): Outcome<Unit> }
```

- The use cases call `SessionGateway.signedIn` on success, so the `ui` module never sees `:core:session`.
- Read first: `K/khepri/Features/Auth/UI/LoginViewModel.swift`, `LoginScreen.swift`, `SignInView.swift`, `SignUpView.swift`, `Services/AuthService.swift`.

- [x] **Step 1:** `AuthRepositoryImplTest` (MockWebServer): login success maps `AuthResponseDto` to `Session`; 422 with `fields` → `FieldValidation`; 429 without `fields` → `Server(429, …)`; sign-up sends `timezone` (zone id string) and `passwordConfirmation`; every request body passes the `RequestSchemaTest` key check.
- [x] **Step 2:** `SignInWithEmailTest`: success calls the gateway once; failure does not call it.
- [x] **Step 3:** `LoginViewModelTest`: mode switch keeps the email; sign-up with mismatched passwords shows the inline error and makes no call; a failed sign-in keeps the form filled and emits the error-haptic effect; submit is disabled while running.
- [x] **Step 4:** Implement domain, data, ViewModel. Run; expect PASS.
- [x] **Step 5:** `LoginScreen`: wordmark in Marcellus, tagline, Sign In / Sign Up segmented switch with a sliding indicator that respects reduce motion, banners announced with a live region, flat emblem image (3D arrives in Phase 5). Passkey, Google and Apple buttons are not rendered until lane F.
- [x] **Step 6:** Screenshots: sign-in and sign-up, light/dark, font scale 1.0/2.0, en and pt-PT.
- [x] **Step 7:** Commit.

**Done (2026-11-26).** 33 new tests: 7 domain (use cases and the iOS strength scale), 6 data (MockWebServer), 9 ViewModel, 4 Robolectric with 21 screenshots, 7 for the error copy. Deviations:

- **`AuthValidation` is ported to the domain,** with iOS's email pattern and five-step strength; the password length counts code points, as Swift's `count` comes closer to that than UTF-16 units.
- **The request key check needs no new test.** The repository only builds generated request types, and `RequestSchemaTest` already checks every one of them against its schema; the data test asserts the exact keys sent.
- **Sign-up takes the zone from the injected `Clock`,** so the test pins it.
- **`AppError.description()` lives in `:core:ui`.** It ports iOS `APIError.errorDescription`: the server's message wins, and each case has iOS's fallback copy. A dropped connection uses the offline copy, where iOS shows the system's text.
- **The emblem is a vector drawable** generated by `tools/emblem_to_vector.py` from the web's solar SVGs, one per appearance (not a `-night` pair: the app's setting can differ from the system's). `KhepriEmblem` picks one by theme.
- **The brand name and tagline are English-only** (`translatable="false"` in `tools/strings-android.json`), as iOS keeps them unlocalized.
- **`ProminentButton` gained `loading`,** a spinner in the disabled button, as iOS.
- **Not ported yet:** the banner icons and the sheet's lock symbol (no icon set until Phase 2), the banner's slide-in (it appears without animation), and passkey, Google and Apple (lane F). iOS's unreachable "Please enter a valid email address" path for the reset sheet is dropped: its button is disabled until the address is valid.
- **The convention plugins add less.** `feature.data` no longer adds `:core:common` and `feature.ui` no longer adds `:core:navigation`; dependency analysis rejects them where unused, so each module declares them. Data modules get Retrofit, since the generated interfaces return `retrofit2.Response`.

**Fable review of Task 11 (2026-11-26).** No high findings. Fixed:

- Nothing typed outlives a sign-in: on success the ViewModel resets its state and asks for an autofill commit; the signed-out screens' ViewModels also live in a store cleared when the phase leaves `SignedOut` (Task 12's `AppViewModel.signedOutStore`).
- Dismissing the reset sheet cancels a reset still in flight, so a late answer never lands on a newer sheet.
- A failed reset shows its error in the sheet (with the error haptic), not in a banner hidden behind it.
- Autofill: the email field is `Username` + `EmailAddress`, and a successful sign-in or sign-up commits the autofill session.
- Password events print `***`; the reset sends the trimmed email and the sheet opens prefilled with it; Cancel hides the sheet with its animation before closing it.
- The reset test answers 202, as the server does.

Open: the banner's live region is not yet checked with TalkBack on a device.

### Task 12: `:app` — phase root, tab shell, links, smoke flow

**Files:**
- Modify: `app/src/main/kotlin/com/fernandocorreia/khepri/MainActivity.kt`, `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/…/RootContent.kt`, `shell/MainTabs.kt`, `shell/PlaceholderScreen.kt`, `FailedScreen.kt`, `LockedScreen.kt`, `LockedRetry.kt`, `di/PendingLinksStore.kt`, `debug/StrictModeInitializer.kt`
- Create: `maestro/flows/smoke-sign-in.yaml`, `maestro/README.md`
- Test: `app/src/test/…/RootContentTest.kt`, `app/src/test/…/HiltGraphTest.kt`

**Interfaces:**
- Consumes: `SessionManager.phase`, `Navigator`, the `EntryInstaller` set, `LoginScreen`.

- [x] **Step 1:** `RootContentTest` (Robolectric Compose): each `AppPhase` renders its screen; `Failed` shows Try Again and Sign Out; `Locked` never shows the login form.
- [x] **Step 2:** `HiltGraphTest` (Robolectric + Hilt test runner): the application component builds; `TokenSource`, both OkHttp clients, `SessionManager` and `Navigator` resolve (no dependency cycle).
- [x] **Step 3:** Implement `RootContent`. `Onboarding` shows a placeholder with Sign Out until Phase 3. `LockedRetry` calls `SessionManager.retry()` on `ON_START` and `ACTION_USER_UNLOCKED` while `Locked`.
- [x] **Step 4:** `MainTabs`: five tabs with labels from strings, one stack each, placeholder roots; More shows Sign Out with a confirmation dialog.
- [x] **Step 5:** Manifest intent filters: scheme `khepri`; `https://kheprios.com` paths `/app` and `/i` with `autoVerify`. `MainActivity` passes incoming links to `Navigator.open(link)` and opens the browser when it returns false.
- [x] **Step 6:** StrictMode in debug: thread policy (disk, network) and VM policy (leaked closables) with `penaltyDeath`.
- [x] **Step 7:** Start the backend at the pinned SHA, not from the working checkout: `git -C ../north-client worktree add ../north-client-pinned $(cat core/api/contract.sha)`, then run it as `../north-client/AGENTS.md` describes, on port 8090. Seed it (read `K/scripts/seed-review-account.sh` for the account).
- [x] **Step 8:** Write `smoke-sign-in.yaml`: launch, sign in, assert the Today tab, tap each tab, open `khepri://more` and assert the More tab, sign out, assert the login screen.
- [x] **Step 9:** Run `maestro test maestro/flows/smoke-sign-in.yaml`. Expected: PASS.
- [x] **Step 10:** Run `./gradlew check :app:assembleRelease`. Expected: BUILD SUCCESSFUL. Push; CI green. Commit.

**Done (2026-11-26).** 1,018 tests green in `check`. The smoke flow passes against the backend at `6823e58c` on the `minified` build (release code under R8; not true at first, see the review below) and on `debug` (StrictMode with `penaltyDeath`, no violations). On the emulator, three taps on a placeholder's `SavedStateHandle` counter survive `adb shell am kill`, along with the selected tab and the session. Deviations and findings:

- **The app had no `INTERNET` permission.** No earlier task made a call on a device, so nothing failed. `:core:network` now declares it, and `ManifestTest` checks the merged manifest.
- **API 37 blocks private addresses** unless the app holds `ACCESS_LOCAL_NETWORK` (a runtime permission). Without it, `10.0.2.2` times out after 15 s with no log. Only the `debug` and `minified` manifests declare it, since release talks only to `kheprios.com`. The flow grants it with `permissions: all: allow`.
- **The phase root keyed its screens by class.** A `KClass` hashes differently in each process, so after process death nothing saved under the signed-in screen was found: the counter and every `rememberSaveable` came back empty, while the tab (held in a ViewModel) survived. Phases are now keyed by fixed strings (`AppPhase.stateKey()`).
- **A `minified` build type** (release with debug signing, app id `.minified`, `10.0.2.2:8090`, cleartext only to that host) runs the smoke flow under R8 without touching production.
- **Test tags are resource ids** (`testTagsAsResourceId`) at the activity root, and again on the sign-out dialog and the reset sheet, which are separate windows.
- **Links:** used once (`savedInstanceState == null`, then `onNewIntent`), replays from recents ignored, and the intent cleared. An unknown `https` link opens in a browser through a host-less `https:` selector, so it cannot come back to this app once the domain is verified. `autoVerify` cannot pass until `kheprios.com` serves `assetlinks.json` for the release key; the flow proves links with `khepri://`.
- **No separate `LockedRetry`.** `SessionLifecycle` already retries a locked session on every `ON_START`, and unlocking a phone that shows the app starts it. `ACTION_USER_UNLOCKED` is not registered.
- **Pending links** (invite and crew codes received while signed out) persist in a Preferences DataStore (`PendingLinksStore`).
- **Local backend:** in development it refuses to boot without `POSTHOG_API_KEY`. The run used a dummy key with `POSTHOG_HOST` on a closed local port, so no events left the machine. The seeded account's model steps answer 503 without AI keys; the flow does not need them. See `maestro/README.md`.
- **Emulator:** only one build may be installed, or `khepri://` stops at the "Open with" chooser.

**Fable review of Task 12 (2026-11-26).** Fixed:

- **High: `minified` had no R8.** It copied `release` before `release` enabled minification, so the first smoke run was not under R8 (3 dex files, 32.7 MB, no mapping). `minified` now follows `release`, and configuration fails if either type stops minifying. On the real R8 build (1 dex, 4.2 MB, mapping written) the smoke flow, the process-death check (tab and `SavedStateHandle` counter restored after `am kill`) and back (another tab, then Today, then out of the app) all pass.
- The signed-out store was cleared on the phase change, which cancelled the login ViewModel before its success branch, so the autofill commit never ran. It is now cleared when the signed-out screens leave composition (not on rotation). The password-save prompt itself is not checked on the emulator (no password manager signed in).
- A `SavedStateHandle` under `ScopedViewModels` is now held by the scoped owner, so it goes with the store instead of passing to the next account's ViewModel. It is not saved across process death.
- Pending codes: one per kind, the newest, as iOS keeps one pending invite, so links fired at the app cannot grow the file. Like iOS, they are not cleared on sign-out.
- Only `kheprios.com` links go on to a browser: the activity is exported, and another app could otherwise use it to open any site.
- `verifyReleaseManifest` (in `check`) fails if release asks for `ACCESS_LOCAL_NETWORK` or allows cleartext. Shown to fail on a violation.
- Link use is a tested function (`LinkIntent.consume`): Robolectric `ActivityScenario` never goes idle while the launch spinner animates, so the activity itself is not launched in tests.

Not changed: `LockedScreen` has no retry button, as on iOS. `SessionLifecycle` retries on every `ON_START`.

Deferred from the Task 12 amendments (Fable item 7):
- **Sheet swipe-dismiss, and a sheet restored after process death:** to the first navigation sheet route (Phase 2). The reset sheet is not a route and is not restored by design.
- **A minSdk 28 emulator run:** to the end-of-phase pass; it needs an API 28 system image.
- **What a push link does during a workout:** to the phase that ports the workout. Recommendation unchanged: hold it until the workout ends.

---

## Done when

- [x] A debug build signs in against a locally seeded server, switches tabs, opens a `khepri://` link, signs out.
- [x] `./gradlew check` is green locally and in CI; `./gradlew syncContract --check` is green locally.
- [x] Every gate in Task 2 was shown to fail on a deliberate violation. (Task 12 added `verifyReleaseManifest`, also shown to fail.)
- [x] Contract tests decode every committed golden fixture: 164 of 166. The other two, `tools` and `tools.readonly`, are MCP tool lists, not API responses (Task 4).
- [x] `CLAUDE.md` "Commands" section lists the real commands.
- [x] Known Phase 1 limitation recorded in `CLAUDE.md`: offline cold launch shows `Failed` until the Phase 2 cache exists.
