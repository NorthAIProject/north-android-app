# Phase 2: Reference Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** one real daily loop (see Today, check in, see Today update) built exactly to the feature template, so every later feature copies a working example.

**Architecture:** the AI consent gate joins `:feature:auth` and the app root. `:core:database` (Room) brings the snapshot cache used by the new `:feature:today` (Today and My Day, read path, cached) and `:feature:checkins` (today's check-in, write path). Writes emit on `DataChangeBus`; screens reload through `bus.reloads(...)`. `:core:charts` wraps Vico for the trends chart. Phase 2 ends by writing `docs/feature-template.md` from the code.

**Tech Stack:** Phase 1's stack plus Room (with KSP) and Vico. Both are new dependencies and need the owner's approval (see "Owner gates").

**Spec:** `docs/design.md` sections 6.3–6.5, 10.1, 10.3, 10.9, 13 (Phase 2). Feature specs: `docs/feature-specs/today.md`, `checkins.md`, `auth.md`. Rows tagged `2` in `docs/parity-checklist.md`. iOS is the source of truth for behaviour and copy (`K/` = `../north-ios-app/khepri`).

**Depends on:** Phase 1 (PR #1). Backend B4 (north-client #179, `android` check-in source) is **open, not deployed**: the pinned backend accepts `source: "android"` and records it as `ios`. Everything here works before B4 ships; only the parity row "Check-in recorded with source `android`" waits for it.

## Owner gates

Ask once, at plan handoff:

1. **Room:** 2.8.5 (`androidx.room`, the plain reading of "Room" in the design; recommended) or Room 3.0.3 (`androidx.room3`, the new line). Task 2's first step compiles a one-entity database against the pinned Kotlin 2.4.21 + KSP 2.3.14 and falls back to the other line if it fails.
2. **Vico 3.3.1** (`com.patrykandpatrick.vico:compose-m3`) for `:core:charts`. Blocks Task 6 only.
3. **B4 promotion** (north-client #179 merge + manual infra tag bump) is the owner's call whenever convenient; nothing in this plan blocks on it.

Task 1 needs no new dependency.

## Fixed interfaces

```kotlin
// :core:database
@Entity data class CachedSnapshot(val accountId: String, val key: String, val json: String, val fetchedAt: Instant)  // PK (accountId, key)
interface SnapshotDao { fun observe(accountId: String, key: String): Flow<CachedSnapshot?>; suspend fun put(s: CachedSnapshot); suspend fun clearAll() }

// :core:common
data class Cached<T>(val value: T, val fetchedAt: Instant, val refreshError: AppError?)   // refreshError != null → show the staleness line

// feature repositories that cache
fun observeToday(): Flow<Cached<Today>?>          // emits cache first, then the refreshed value
suspend fun refreshToday(): Outcome<Unit>
```

Cache keys: `today`, `day:<yyyy-MM-dd>` (today and yesterday only).

## Global Constraints

- Everything in Phase 1's Global Constraints still holds (ids, SDK levels, `java.time`, catalog-only versions, ktfmt 120, detekt and Lint with no baseline, explicit API in `core/*`, no `!!`, injected dispatchers and `Clock`, four locales, MockWebServer or fakes only).
- Rules in `CLAUDE.md` "Architecture rules" apply verbatim: `apiCall(map = …)`, scope tiers, `SignOutCleaner` for account-bound singleton state, `CollectEffects`, `bus.reloads(...)`.
- Never cached (design 6.5): blood pressure, sleep stages and blocks, health samples, trends. The cache strips `body.bloodPressure`, `sleep.stages` and `sleep.blocks` from `DayResponseDto` before `put`; a sheet that needs them shows them only from a live response.
- Copy comes from the iOS string catalogs through `tools/convert_strings.py`; Android-only copy goes in `tools/strings-android.json` with all four locales.
- Check-ins send `source: "android"`.
- Test tags match iOS accessibility identifiers where iOS has one (`ai-consent-allow`, `ai-consent-decline`), else kebab-case.
- Reduce motion is honoured by every animation added here (`LocalReduceMotion`).
- Every screen: previews and screenshot tests for loading, content, empty and failed, through the shared harness from Task 4.
- Commit after each task; Fable reviews at the end of each task (owner rule).

## Review Focus

- **Yesterday's cache after midnight:** offline at 00:30 with Today cached at 23:10 must not read as today's numbers. The staleness line shows the weekday when `fetchedAt` is not today ("Offline, showing Tue 23:10"). Test in Task 4.
- **Cached JSON that no longer decodes** (a contract pin bump changed a DTO): treated as a miss and the row deleted, never a crash or a stuck spinner. Test in Task 2.
- **Account switch during a refresh:** a refresh started for account A that answers after A signs out writes nothing, and B never sees A's row. Test in Task 3.
- **Declined consent and an AI call:** a 403 `ai_consent_required` reloads consent from the server; the gate shows only if the status comes back `unset`. A declined user's check-in saves with no reply bubble, no error and no gate. Tests in Tasks 1 and 7.
- **Failed or repeated save:** a failed check-in keeps the form filled and Today's content on screen; a double tap on Save sends one request. Test in Task 7.

---

### Task 1: AI consent gate

**Files:**
- Create in `core/common`: `ConsentSignal.kt` (interface + `SharedFlowConsentSignal`).
- Create in `core/network`: `ConsentRefusalInterceptor.kt`; register it in the OkHttp stack after the bearer interceptor.
- Create in `feature/auth/domain`: `model/AiConsent.kt`, `repository/AiConsentRepository.kt`, `usecase/LoadAiConsent.kt`, `DecideAiConsent.kt`, `ObserveAiConsent.kt`.
- Create in `feature/auth/data`: `repository/AiConsentRepositoryImpl.kt`, `remote/AiConsentMappers.kt`; bind in `di/AuthDataModule.kt` (also `@IntoSet SignOutCleaner`).
- Create in `feature/auth/ui`: `consent/AiConsentScreen.kt`, `AiConsentViewModel.kt`, `AiConsentUiState.kt`, `AiConsentDisclosure.kt`.
- Modify: `app/…/MainActivity.kt` (`AppRoot` overlays the gate), `app/…/shell/AppViewModel.kt` (loads consent on entering `SignedIn`/`Onboarding`), `maestro/flows/smoke-sign-in.yaml`.
- Create: `maestro/flows/ai-consent.yaml`.
- Test: `core/network/src/test/…/ConsentRefusalInterceptorTest.kt`, `feature/auth/data/src/test/…/AiConsentRepositoryImplTest.kt`, `feature/auth/ui/src/test/…/AiConsentViewModelTest.kt`, `AiConsentScreenScreenshotTest.kt`, `app/src/test/…/RootContentTest.kt` (gate cases).

**Interfaces:**
- Consumes: generated `AiConsentApi.getAIConsent()`, `setAIConsent(SetAIConsentRequestDto(decision))`, `apiCall(map)`, `SignOutCleaner`, `@ApplicationScope`.
- Produces:

```kotlin
// :core:common
public interface ConsentSignal { public val required: Flow<Unit>; public fun post() }

// feature:auth:domain
enum class ConsentChoice { Unknown, Unanswered, Allowed, Declined }   // status unset/granted/declined; anything else Unknown
data class AiConsent(val choice: ConsentChoice, val recipients: List<String>)
interface AiConsentRepository {
    val consent: StateFlow<AiConsent>            // starts AiConsent(Unknown, emptyList())
    suspend fun load(): Outcome<Unit>            // no call unless choice is Unknown (iOS load())
    suspend fun decide(allow: Boolean): Outcome<Unit>   // PUT decision "granted"/"declined"
    fun reset()                                  // back to Unknown, recipients cleared
}
```

- `ConsentRefusalInterceptor` posts `ConsentSignal` on a 403 whose body (`peekBody`, 16 KiB) has `error.code == "ai_consent_required"`. It never changes the response.
- `AiConsentRepositoryImpl` collects `ConsentSignal.required` on `@ApplicationScope` and runs `reset()` then `load()`; it is a `SignOutCleaner` whose `afterLogout` calls `reset()`.
- The gate shows when the phase is `SignedIn` or `Onboarding` and the choice is `Unanswered`. It covers the tabs, cannot be dismissed with Back, and has its own `testTagsAsResourceId`.
- Read first: `K/khepri/Features/AIConsent/AIConsent.swift`, `AIConsentViews.swift`, `K/khepri/khepriApp.swift` (`RootView.asksAIConsent`, load on phase change, reset on sign-out), `K/khepriTests/AIConsentTests.swift`.

- [ ] **Step 1:** `AiConsentRepositoryImplTest` (MockWebServer), the four iOS tests plus three: `unanswered account is asked` (`status: "unset"` → `Unanswered`, recipients kept); `deciding stops the question` (PUT body is exactly `{"decision":"granted"}` → `Allowed`); `a status this build does not know does not ask` (`"paused"` → `Unknown`); `a failed save keeps the question open` (500 → `Err`, still `Unanswered`); `load does nothing once known` (one request for two loads); `a consent refusal reloads and asks only when unset` (signal posted with server `declined` → `Declined`, no gate; with `unset` → `Unanswered`); `sign-out forgets the answer` (`afterLogout` → `Unknown`).
- [ ] **Step 2:** `ConsentRefusalInterceptorTest`: 403 with the code posts once; 403 with another code, 403 with no body and 200 do not; the response body is still readable by the caller.
- [ ] **Step 3:** `AiConsentViewModelTest`: Allow and Don't Allow call `DecideAiConsent` once each; buttons disabled while saving; a failed save shows the server message (`AppError.description()`) above the buttons and keeps the screen.
- [ ] **Step 4:** Implement signal, interceptor, domain, data, ViewModel. Run the three tests; expect PASS.
- [ ] **Step 5:** `AiConsentScreen` as iOS: title "Share your data with AI?", `AiConsentDisclosure` (intro, "What is sent", "Who receives it" with the server's recipients, own-provider line), bottom bar with Allow (prominent), Don't Allow, footnote. Strings through `convert_strings.py`.
- [ ] **Step 6:** `AppRoot` overlays the gate; `AppViewModel` calls `LoadAiConsent` when the phase becomes `SignedIn` or `Onboarding`. `RootContentTest`: gate shown over `SignedIn` when `Unanswered`; not shown when `Declined`, `Allowed`, `Unknown` or `SignedOut`; Back does not dismiss it.
- [ ] **Step 7:** Screenshots: gate light/dark, font scale 2.0, pt-PT, saving, failed save.
- [ ] **Step 8:** Maestro. `smoke-sign-in.yaml`: after sign-in, `runFlow` when `ai-consent-allow` is visible, tap it. New `ai-consent.yaml`: sign up a fresh account (`smoke+${Date.now()}@example.com`), assert the gate covers the onboarding screen, tap Don't Allow, assert the gate is gone. Run both against the local backend.
- [ ] **Step 9:** `./gradlew check`; commit.

---

### Task 2: `:core:database` and the snapshot cache

**Files:**
- Create: `build-logic/convention/…/RoomPlugin.kt` (`khepri.room`: KSP, Room Gradle plugin, schema dir `$projectDir/schemas`, `room.generateKotlin=true`), register it in `build-logic/convention/build.gradle.kts`.
- Create `core/database`: `build.gradle.kts`, `KhepriDatabase.kt` (version 1, `exportSchema = true`), `snapshot/CachedSnapshot.kt`, `SnapshotDao.kt`, `SnapshotCache.kt`, `DatabaseCleaner.kt`, `di/DatabaseModule.kt`, `schemas/` (committed).
- Create in `core/common`: `Cached.kt`.
- Modify: `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`.
- Test: `core/database/src/test/…/SnapshotDaoTest.kt`, `SnapshotCacheTest.kt`, `DatabaseCleanerTest.kt` (Robolectric, in-memory Room).

**Interfaces:**
- Consumes: `SignOutCleaner`, `Clock`, the network `Json` instance (`:core:network`'s qualifier, or move the shared `Json` provider to `:core:common` if `:core:database` must not see `:core:network`; follow design 6.1).
- Produces: the fixed `CachedSnapshot`, `SnapshotDao`, `Cached<T>`, plus:

```kotlin
public class SnapshotCache @Inject constructor(dao: SnapshotDao, json: Json, clock: Clock) {
    public fun <T> observe(accountId: String, key: String, serializer: KSerializer<T>): Flow<StoredValue<T>?>
    public suspend fun <T> put(accountId: String, key: String, value: T, serializer: KSerializer<T>)
    public suspend fun keepOnly(accountId: String, prefix: String, keys: Set<String>)   // prunes day:<date> rows
}
public data class StoredValue<T>(val value: T, val fetchedAt: Instant)
```

- `SnapshotDao` also gets `deleteKey(accountId, key)` and `deleteWithPrefixExcept(accountId, prefix, keep: List<String>)`.
- A row that fails to decode is deleted and observed as `null`.
- `DatabaseCleaner.afterLogout` clears every table for both reasons (only the snapshot table exists in Phase 2).
- Database file `khepri.db`; backup rules already exclude the `database` domain.

- [ ] **Step 1:** Add the owner-approved Room line to the catalog and a throwaway one-entity build of `:core:database`; run `./gradlew :core:database:assembleDebug`. If KSP fails against Kotlin 2.4.21, switch to the other approved line and record why in this task's Done notes.
- [ ] **Step 2:** `SnapshotDaoTest`: put then observe; same `(account, key)` replaces; another account's row is invisible; `clearAll` empties; `deleteWithPrefixExcept` keeps exactly the listed keys.
- [ ] **Step 3:** `SnapshotCacheTest`: round-trips a serializable value with `fetchedAt` from the injected clock; an undecodable row observes `null` and is gone afterwards; `keepOnly("day:", {today, yesterday})` drops older days and touches no other prefix.
- [ ] **Step 4:** `DatabaseCleanerTest`: rows are gone after `afterLogout(Explicit)` and after `afterLogout(Unauthorized)`.
- [ ] **Step 5:** Implement. Run; expect PASS. Schema JSON committed under `core/database/schemas`.
- [ ] **Step 6:** `HiltGraphTest`: `SnapshotCache` resolves; the cleaner set contains `DatabaseCleaner`.
- [ ] **Step 7:** `./gradlew check`; commit.

---

### Task 3: `:feature:today` domain and data, and the account-state guard

**Files:**
- Create `feature/today/domain`: `model/Today.kt`, `model/Day.kt` (and card value types), `repository/TodayRepository.kt`, `usecase/ObserveToday.kt`, `RefreshToday.kt`, `ObserveDay.kt`, `RefreshDay.kt`, `StartFast.kt`, `StopFast.kt`.
- Create `feature/today/data`: `remote/TodayMappers.kt`, `remote/DayMappers.kt`, `local/CacheKeys.kt`, `repository/TodayRepositoryImpl.kt`, `di/TodayDataModule.kt`.
- Create in `build-logic/detekt-rules`: `AccountStateNeedsCleaner.kt`, registered in `KhepriRuleSetProvider`, enabled in the detekt config.
- Modify: `settings.gradle.kts`, `app/build.gradle.kts`.
- Test: `feature/today/data/src/test/…/TodayRepositoryImplTest.kt` (MockWebServer + in-memory Room), `DayMappersTest.kt`, `feature/today/domain/src/test/…/StartFastTest.kt`, `build-logic/detekt-rules/src/test/…/AccountStateNeedsCleanerTest.kt`.

**Interfaces:**
- Consumes: `getToday()`, `getDay(date)`, fasting start/stop operations, `SnapshotCache`, `SignedInState.account`, `@AccountScope`, `DataChangeBus`, `Clock`.
- Produces:

```kotlin
interface TodayRepository {
    fun observeToday(): Flow<Cached<Today>?>
    suspend fun refreshToday(): Outcome<Unit>
    fun observeDay(date: LocalDate): Flow<Cached<Day>?>   // today and yesterday from the cache; other dates in memory only
    suspend fun refreshDay(date: LocalDate): Outcome<Unit>
    suspend fun startFast(hours: Int): Outcome<Unit>
    suspend fun stopFast(): Outcome<Unit>
}
class StartFast(repo: TodayRepository, bus: DataChangeBus) { suspend operator fun invoke(hours: Int): Outcome<Unit> }   // emits DataScope.Day on Ok
```

- Domain models hold what Phase 2 screens read: `Today(briefing, streak, checkedInToday, checkIn: TodayCheckIn?, hydration, sleep, nextStep: NextStep?)`; `Day(date, isToday, now, vitals, food, water, activity, workouts, body (no blood pressure), streak, level, caffeine, nutrients, sleep: DaySleep?, fast: DayFast?, timeline, checkIn)`. Read `K/khepri/Features/Today/TodayView.swift` and `Day/DayViews.swift`, `DayMath.swift` for which fields each card shows; leave out fields only Phase 3 cards read.
- The cache stores the response DTO (generated `@Serializable` types) and maps on read. Before `put`, `DayResponseDto` loses `body.bloodPressure`, `sleep.stages`, `sleep.blocks`.
- `refreshError` comes from an in-memory `MutableStateFlow<Map<String, AppError>>` keyed by cache key, combined with the cached flow; it clears on the next successful refresh. Days other than today and yesterday live in an in-memory map. Both are cleared by the repository's `SignOutCleaner`.
- Refreshes run as `accountScope.async { … }.await()`, capture the account id before the call, and write only if `SignedInState.account` still equals it.
- The repository does not subscribe to the bus; screens reload on `bus.reloads(DataScope.Day)` (CLAUDE.md rule; this supersedes the earlier task-list wording).
- `AccountStateNeedsCleaner`: in main sources, a class annotated `@Singleton` that declares a property initialised with `MutableStateFlow(` or `mutableMapOf(` must implement `SignOutCleaner`, or carry `@Suppress("AccountStateNeedsCleaner")` with a comment saying why the state is not account-bound. Add those suppressions to existing singletons that need them (session, navigation) in this task.

- [ ] **Step 1:** `TodayRepositoryImplTest`: `observeToday` emits the cached value first, then the refreshed one; a refresh replaces the row; a failed refresh keeps the cached value and sets `refreshError`, which the next success clears; nothing cached + failure emits `null` and returns `Err`; the stored day JSON has no `bloodPressure`, `stages` or `blocks`; refreshing today keeps only today's and yesterday's `day:` rows; a date three days back is served from memory and never written; a refresh whose account signed out mid-flight writes nothing (MockWebServer delays the body, sign-out happens, the account changes); `afterLogout` clears the in-memory errors and days.
- [ ] **Step 2:** `DayMappersTest`: a golden `DayResponse` maps every field Phase 2 reads; unknown open-string values (`bmiCategory`, timeline `kind`) map without failing.
- [ ] **Step 3:** `StartFastTest`: Ok emits `DataScope.Day` once; Err emits nothing.
- [ ] **Step 4:** `AccountStateNeedsCleanerTest`: flags a `@Singleton` with a `MutableStateFlow` and no cleaner; passes with `SignOutCleaner`; passes a non-singleton; passes with the suppression.
- [ ] **Step 5:** Implement. Run; expect PASS. `./gradlew detekt` passes on the whole project.
- [ ] **Step 6:** `./gradlew check`; commit.

---

### Task 4: Today screen and the shared screenshot harness

**Files:**
- Create in `core/testing`: `ScreenshotHarness.kt` (moved from `LoginScreenScreenshotTest`: locale, font scale, appearance, the Light-first Dark workaround, `captureRoboImage` path).
- Modify: `feature/auth/ui/src/test/…/LoginScreenScreenshotTest.kt` and `AiConsentScreenScreenshotTest.kt` to use it (screenshots must not change).
- Create `feature/today/ui`: `today/TodayScreen.kt`, `TodayViewModel.kt`, `TodayUiState.kt`, `TodayEvent.kt`, `components/TodayHeader.kt`, `StatGrid.kt`, `NextStepCard.kt`, `DateToolbar.kt`, `StalenessLine.kt`, `navigation/TodayEntries.kt` (installs the Today tab root).
- Modify: `app/…` (installer registered), `CLAUDE.md` (the `stateIn` line, see Step 3).
- Test: `feature/today/ui/src/test/…/TodayViewModelTest.kt`, `StalenessLineTest.kt`, `TodayScreenScreenshotTest.kt`, `StatGridTest.kt`.

**Interfaces:**
- Consumes: Task 3 use cases, `Navigator`, `DataChangeBus.reloads`, `CollectEffects`, `ScreenAsk.ReloadRoot(AppTab.Today)`, `Loadable`, `Clock`.
- Produces: `TodayUiState(today: Loadable<Today>, day: Loadable<Day>, date: LocalDate, stale: Staleness?)` where `Staleness(fetchedAt: Instant, showDay: Boolean)`; `TodayEvent` = `Refresh`, `PreviousDay`, `NextDay`, `GoToToday`, `OpenNextStep`, `OpenCheckIn`. Task 5 adds the My Day section to this screen.

Phase 2 subset of the column (parity rows tagged 2): date eyebrow and coach briefing; stat grid (streak, check-in, water, sleep); next-step card with the one prominent button; then the My Day section (Task 5). The first-week, recovery, outreach, lighter-day, your-week, fitness-week, goals and news cards, the nudges bell and Quick Add are Phase 3 and are not rendered.

- [ ] **Step 1:** `TodayViewModelTest`: content waits for both Today and the day, or 2 s, whichever comes first (`StandardTestDispatcher`, advance 1999 ms → Loading, 2000 ms → Content with the day still `Loading`); a failed refresh with content keeps the content and sets `stale`; nothing cached and failed → `Failed`; `PreviousDay` / `NextDay` / `GoToToday` change `date` and observe that day; `NextDay` is a no-op on today; a `DataScope.Day` change, `ScreenAsk.ReloadRoot(Today)` and returning to the foreground each refresh once.
- [ ] **Step 2:** `StalenessLineTest`: `fetchedAt` today → "Offline, showing 23:10"; yesterday → "Offline, showing Tue 23:10" (locale-aware weekday, 24 h or 12 h from the device setting).
- [ ] **Step 3:** Implement. State: private `MutableStateFlow`, exposed with `onStart { launchRefresh() }.stateIn(viewModelScope, WhileSubscribed(5_000), initial)` where `launchRefresh` launches and returns at once, so cached content reaches the screen before the network answers. Correct the `CLAUDE.md` line to say "launch, do not await". Run; expect PASS.
- [ ] **Step 4:** UI: `PullToRefreshBox`; header; `StatGrid` is two columns, one column at font scale ≥ 1.5 (`StatGridTest` asserts both); next-step card routes its action through `Navigator.open`; date toolbar (previous, next, Today) with the iOS labels. Tags: `today-screen`, `stat-streak`, `stat-check-in`, `stat-water`, `stat-sleep`, `next-step`, `day-previous`, `day-next`, `day-today`, `staleness-line`.
- [ ] **Step 5:** Harness: move the setup, migrate the two auth screenshot tests, run `verifyRoborazziDebug` on `:feature:auth:ui`; the images must not change.
- [ ] **Step 6:** Today screenshots through the harness: loading, content, empty (new account), failed, stale (yesterday); content also dark, font scale 2.0 and pt-PT.
- [ ] **Step 7:** `./gradlew check`; commit.

---

### Task 5: My Day cards and detail sheets

**Files:**
- Create in `feature/today/ui`: `myday/MyDaySection.kt`, `VitalsStrip.kt`, `DayCard.kt` (the shared card), `DayCardKind.kt`, `DayCardDetailSheet.kt`, `BodyCard.kt` (flat image), `DayFormat.kt` (port of the `DayMath` text the cards show).
- Modify: `TodayScreen.kt`, `TodayViewModel.kt` (events `OpenCard(kind)`, `StartFast(hours)`, `StopFast`), `TodayUiState.kt` (`openCard: DayCardKind?`, `fastAction: ActionState`).
- Test: `DayFormatTest.kt`, `TodayViewModelTest.kt` (fast cases), `MyDayScreenshotTest.kt`.

**Interfaces:**
- Consumes: Task 3 `Day`, `StartFast`, `StopFast`; `AppDestination.CheckInFlow`.
- Produces: `enum class DayCardKind { Fast, Food, Water, Activity, Sleep, Workouts, Nutrients, Streak }`, keyed by ordinal for `rememberSaveable` (never by class).
- Read first: `K/khepri/Features/Day/DayViews.swift` (cards, `DayCardDetail` lines 300–420), `DayMath.swift`, `DayStore.swift`.

- [ ] **Step 1:** `DayFormatTest`: port the `DayMath` functions the cards and sheets use, with iOS's values (durations, ml, kcal, macro split, caffeine active/limit, fast elapsed and remaining). Values come from `K/khepriTests` where a DayMath test exists.
- [ ] **Step 2:** `TodayViewModelTest` additions: `StartFast(16)` calls the use case once and the sheet shows `Running` then closes on Ok; a failed start shows the error inside the sheet and leaves the day on screen; `StopFast` likewise.
- [ ] **Step 3:** Implement: vitals strip (energy, daylight, screen time; hidden when all null), eight cards in iOS order, each opening `DayCardDetailSheet` (a `ModalBottomSheet` with its own `testTagsAsResourceId`). The fast sheet offers End the fast or the iOS presets; the streak sheet's Check in opens `AppDestination.CheckInFlow`; the others are read-only. The body card shows the flat body image (3D in Phase 5) with weight and BMI; the body sheet is Phase 3.
- [ ] **Step 4:** Run tests; expect PASS. Tags: `day-card-<kind>`, `day-sheet-<kind>`, `fast-start-<hours>`, `fast-stop`.
- [ ] **Step 5:** Screenshots: My Day with data, an empty day, each sheet once in light; content in dark and font scale 2.0.
- [ ] **Step 6:** `./gradlew check`; commit.

---

### Task 6: Day timeline, trends and `:core:charts`

**Files:**
- Create `core/charts`: `build.gradle.kts`, `TrendLineChart.kt` (one Vico line chart themed from `KhepriTheme` tokens), `ChartPreviews.kt`.
- Create in `feature/today` domain/data: `model/DayTrends.kt`, `usecase/LoadDayTrends.kt`, repository `trends(): Outcome<DayTrends>` (never cached).
- Create in `feature/today/ui`: `myday/DayTimeline.kt`, `TrendsSection.kt`.
- Modify: `settings.gradle.kts`, catalog (Vico, owner-approved), `TodayViewModel.kt` (`trends: Loadable<DayTrends>`, loaded with the day, kept on a failed refresh).
- Test: `core/charts/src/test/…/TrendLineChartScreenshotTest.kt`, `feature/today/data/src/test/…/TrendsTest.kt`, `DayTimelineScreenshotTest.kt`.

**Interfaces:**
- Consumes: `getDayTrends()`, `Day.timeline`.
- Produces: `@Composable fun TrendLineChart(points: List<TrendPoint>, modifier: Modifier = Modifier, unit: String? = null)` with `TrendPoint(at: LocalDate, value: Double)`; the only chart API later phases use until they need another.
- Read first: `K/khepri/Features/Day/TrendsSection.swift`, the timeline part of `DayViews.swift`.

- [ ] **Step 1:** `TrendsTest` (MockWebServer): golden trends map; a failure is `Err` and nothing is written to the database (assert the snapshot table is unchanged).
- [ ] **Step 2:** Implement `:core:charts` and the two sections. Timeline rows with an `href` open it through `Navigator.open`; rows without one are not clickable. Trends render only the series the response has.
- [ ] **Step 3:** Screenshots: chart light/dark; timeline with five kinds; trends with one and with all series.
- [ ] **Step 4:** `./gradlew check`; commit.

---

### Task 7: `:feature:checkins` write path

**Files:**
- Create `feature/checkins/domain`: `model/CheckInForm.kt`, `SavedCheckIn.kt`, `CheckInReply.kt`, `repository/CheckInRepository.kt`, `usecase/SaveTodayCheckIn.kt`, `LoadTodayCheckIn.kt`, `AskForReply.kt`, `LoadCheckInGoals.kt`.
- Create `feature/checkins/data`: `remote/CheckInMappers.kt`, `repository/CheckInRepositoryImpl.kt`, `di/CheckInsDataModule.kt`.
- Create `feature/checkins/ui`: `today/CheckInScreen.kt`, `CheckInViewModel.kt`, `CheckInUiState.kt`, `CheckInEvent.kt`, `components/MoodPicker.kt`, `EnergyPicker.kt`, `OptionalScale.kt`, `TagChips.kt`, `GoalLink.kt`, `CheckInCelebration.kt`, `CheckInReplyBubble.kt`, `navigation/CheckInsEntries.kt` (the More › Check-ins section root).
- Test: `feature/checkins/data/src/test/…/CheckInRepositoryImplTest.kt`, `feature/checkins/domain/src/test/…/SaveTodayCheckInTest.kt`, `feature/checkins/ui/src/test/…/CheckInViewModelTest.kt`, `CheckInScreenScreenshotTest.kt`.

**Interfaces:**
- Consumes: `saveTodayCheckIn(CheckInRequestDto)`, `replyToTodayCheckIn()`, the today check-in read and goals list operations iOS uses, `DataChangeBus`, `ScreenAsk.CheckInMood(mood)`.
- Produces:

```kotlin
data class GoalOption(val id: String, val title: String)
data class CheckInForm(val mood: Int, val energy: Int, val stress: Int?, val sleepQuality: Int?, val tags: List<String>, val relatedGoalId: String?, val notes: String?)
interface CheckInRepository {
    suspend fun today(): Outcome<SavedCheckIn?>
    suspend fun saveToday(form: CheckInForm): Outcome<SavedCheckIn>   // sends source = "android"
    suspend fun reply(): Outcome<CheckInReply?>                       // 403 ai_consent_required → Ok(null)
    suspend fun goals(): Outcome<List<GoalOption>>
}
class SaveTodayCheckIn(repo: CheckInRepository, bus: DataChangeBus) { suspend operator fun invoke(form: CheckInForm): Outcome<SavedCheckIn> }   // emits DataScope.Day on Ok
```

- Read first: `K/khepri/Features/CheckIns/CheckInsScreen.swift`, `CheckInCelebration.swift`, `CheckInPresentation.swift`, `CheckInsService.swift`.

- [ ] **Step 1:** `CheckInRepositoryImplTest` (MockWebServer): the request body has exactly the form's keys plus `"source":"android"` (no nulls for unset optionals unless iOS sends them); `RequestSchemaTest` key check passes; 422 maps to `FieldValidation`; reply 200 maps `kind`/`text`; reply 403 with `ai_consent_required` → `Ok(null)`; reply with blank text → `Ok(null)` (iOS `CheckInReplyNote`).
- [ ] **Step 2:** `SaveTodayCheckInTest`: Ok emits `DataScope.Day` once; Err emits nothing.
- [ ] **Step 3:** `CheckInViewModelTest`: prefills from today's existing check-in; `ScreenAsk.CheckInMood(4)` presets mood 4 and is consumed; Save is disabled until mood and energy are set; a double Save sends one request; a failed save keeps every field, shows the error inline and plays the error haptic; Ok shows the celebration (success haptic) then asks for the reply, showing the pending bubble, then the text or nothing; a declined user (`reply` → `Ok(null)`) sees no bubble and no error.
- [ ] **Step 4:** Implement. Run; expect PASS.
- [ ] **Step 5:** UI as iOS: mood and energy 1–5, optional stress and sleep quality, tag chips, goal link picker, notes, Save; celebration respects reduce motion. Tags: `check-in-mood-<n>`, `check-in-energy-<n>`, `check-in-save`, `check-in-celebration`, `check-in-reply`.
- [ ] **Step 6:** Screenshots: empty form, filled, saving, failed, celebration with reply, celebration pending; dark, font scale 2.0, pt-PT for the filled form.
- [ ] **Step 7:** `./gradlew check`; commit.

---

### Task 8: Check-in flow as a sheet

**Files:**
- Modify: `feature/checkins/ui/…/navigation/CheckInsEntries.kt` (install `Sheet.CheckInFlow`), `CheckInScreen.kt` (`presentation: Section | Sheet`; the sheet closes itself after the celebration as iOS does).
- Modify: `feature/today/ui/…/TodayViewModel.kt` (`OpenCheckIn` → `Navigator.open(AppDestination.CheckInFlow)`; the stat-grid check-in tile and the streak sheet use it).
- Test: `feature/checkins/ui/src/test/…/CheckInFlowTest.kt` (Robolectric), `core/navigation` tests unchanged.

**Interfaces:**
- Consumes: Task 7 screen, `Sheet.CheckInFlow` (already in `:core:navigation`), `AppDestination.CheckIn(mood)` → `ScreenAsk.CheckInMood`.
- Produces: nothing new for later tasks.

- [ ] **Step 1:** `CheckInFlowTest`: opening `CheckInFlow` from Today shows the sheet over the Today tab; saving closes it after the celebration; Back closes it without saving; `khepri://check-ins?mood=2` (deep link) opens More › Check-ins with mood 2 preset.
- [ ] **Step 2:** Implement. Run; expect PASS. The sheet modifier sets `testTagsAsResourceId`.
- [ ] **Step 3:** `./gradlew check`; commit.

---

### Task 9: Daily loop on a device

**Files:**
- Create: `maestro/flows/daily-loop.yaml`, `maestro/flows/offline-today.yaml`.
- Modify: `maestro/README.md` (consent step, fake AI provider note: the local backend replies with canned text when no provider key is set, which is enough for the reply bubble), `docs/parity-checklist.md`.

- [ ] **Step 1:** `daily-loop.yaml`: sign in (consent handled), Today shows the check-in tile as not done, open the check-in flow, pick mood and energy, save, see the celebration and a reply bubble, sheet closes, Today's check-in tile shows done without a manual refresh.
- [ ] **Step 2:** `offline-today.yaml` (or a scripted adb sequence if Maestro cannot toggle the network on API 37): load Today online, `adb shell cmd connectivity airplane-mode enable`, `am kill` the app, relaunch, assert Today's content and `staleness-line`; re-enable the network, pull to refresh, the line disappears.
- [ ] **Step 3:** Run both, plus `smoke-sign-in.yaml` and `ai-consent.yaml`, on the `minified` build (R8) and the debug build against the local backend at the pinned SHA. Zero StrictMode lines and no crashes in logcat.
- [ ] **Step 4:** Tick the parity rows this phase proves (rows tagged 2 in Root and shell, Today and My Day, Check-ins), each with the flow or test that proves it. Leave "Check-in recorded with source `android`" unticked until B4 is live; its unit test (Task 7 Step 1) is noted beside it.
- [ ] **Step 5:** Commit.

---

### Task 10: Freeze the template

**Files:**
- Create: `docs/feature-template.md`.
- Modify: `CLAUDE.md` (point at the template; any rule that changed in this phase).

- [ ] **Step 1:** Write the template from the code, not from the design: module and package layout (`today` and `checkins` as the examples), naming, the cache-then-refresh repository (`SnapshotCache`, account check, `refreshError`, what is never cached), the uncached repository (`trends`, `reply`), the ViewModel skeleton (private `MutableStateFlow`, `onStart { launchRefresh() }`, `stateIn`, `onEvent`, effects channel, `CollectEffects`, `bus.reloads`), write use cases emitting `DataScope`, entry installers, test file names and what each covers, the screenshot harness call.
- [ ] **Step 2:** Check every code path the template names exists (`rg` each symbol); fix the template or the code until they agree.
- [ ] **Step 3:** Commit.

## Done when

- [ ] The daily loop (`daily-loop.yaml`) passes on the emulator against the seeded local backend, on debug and `minified`.
- [ ] Offline shows cached Today with the staleness line; a failed check-in save shows inline without blanking either screen.
- [ ] `docs/feature-template.md` exists and every symbol it names exists.
- [ ] Parity rows tagged 2 for Root and shell (consent), Today and My Day (except 3D), and Check-ins are ticked, except the B4 row.
- [ ] `./gradlew check` is green locally and in CI.
