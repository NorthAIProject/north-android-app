# Phase 2: reference slice

**Goal:** one real daily loop (see Today, check in, see Today update) built exactly to the feature template, so every later feature copies a working example.

**Spec:** `docs/design.md` sections 6.3–6.5, 10.1 (AI consent), 10.3, 10.9. Feature specs: `docs/feature-specs/today.md`, `checkins.md`, `auth.md`.

**Depends on:** Phase 1 complete; backend B4 (`android` check-in source) deployed.

**Status:** task list. Expand to a task-level plan (same format as `phase-1-foundation.md`) before starting; the interfaces below are fixed.

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

## Tasks

1. **AI consent gate** (`:feature:auth`). `GET/PUT /ai-consent`; full-screen Allow/Decline over every phase until answered; `AIConsentCenter` equivalent as an app-scoped state holder; 403 `ai_consent_required` from any call re-opens the gate. Port the iOS AI consent tests. Read `K/khepri/Features/AIConsent/`.
2. **Snapshot cache.** Create `:core:database` here (Room needs at least one entity, so Phase 1 does not create it): `KhepriDatabase` with schema export on, `CachedSnapshot`, DAO, a `SignOutCleaner` that clears it for both reasons. In-memory Room tests.
3. **`today` domain and data.** Models for Today and Day; mappers from the generated types; `TodayRepository` with cache-then-refresh; subscribes to `DataChangeBus` for `DataScope.Day` and refetches. MockWebServer + Room integration tests: cache first, refresh replaces, refresh failure keeps cache and sets `refreshError`.
4. **Today screen.** The column in the order given in `docs/feature-specs/today.md`. Cards appear together after at most 2 seconds. Date toolbar (previous, next, Today). Pull to refresh, reload on foreground and tab re-selection. Stat grid stacks vertically at large font scale. Staleness line "Offline, showing HH:mm".
5. **My Day dashboard.** Vitals strip, eight cards with detail sheets, day timeline, trends chart (first use of `:core:charts`; create the module here with one themed line chart). Body card shows the flat body image until Phase 5.
6. **Check-in write path** (`:feature:checkins`). Today's form (mood, energy, optional stress and sleep, tags, goal link), `PUT /check-ins/today` with `source: android`, celebration (reduce-motion aware, success haptic), coach reply bubble (`POST /check-ins/today/reply`). The use case emits `DataScope.Day`.
7. **Check-in flow as a sheet.** `AppDestination.CheckInFlow` entry on the current tab; `CheckIn(mood)` presets the mood.
8. **Quality for the slice.** Screenshot sets for Today and Check-in (loading, content, empty, failed; light/dark; font scale 1.0/2.0; en, pt-PT). Compose UI tests for semantics. Maestro flow `daily-loop.yaml`: sign in, check in, assert the Today check-in tile changed. Offline test: airplane mode, relaunch, Today shows cached content and the staleness line.
9. **Freeze the template.** Write `docs/feature-template.md` from the code: package layout, naming, the cache-then-refresh repository, the ViewModel skeleton, test file names. Update `CLAUDE.md` to point at it.

## Done when

- The daily loop works on a device against the seeded backend.
- Offline shows cached Today with the staleness line; writes fail inline without blanking the screen.
- `docs/feature-template.md` exists and matches the code.
- Parity checklist rows for Today, My Day (except 3D) and Check-ins (form path) are ticked.
