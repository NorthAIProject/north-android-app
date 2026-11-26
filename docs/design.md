# Khepri for Android: design and build plan

This document is written to be picked up cold, by you or by another model, at any phase. Section 2 says how to read it. Section 16 says what this session produces after approval: a scaffold and documents, no app code.

---

## 1. Context

Khepri (North) is an AI coach app that ships on iOS and the web. There is no Android client and no Android plan anywhere in the workspace; marketing already says "Google Play: coming soon". The goal is a native Android app with the same features and behaviour as the iOS app, on the same backend.

Stack (chosen by the owner): Kotlin, Jetpack Compose + Material 3 (no XML layouts), MVVM + Clean Architecture + unidirectional data flow, Navigation 3 with type-safe routes, ViewModel + coroutines + StateFlow, Hilt, Retrofit + OkHttp, Room, Coil, Compose animation APIs + custom Canvas (Lottie Compose only when an asset needs it), Vico charts, WorkManager, JUnit + MockK + Compose UI tests + Maestro.

Decisions made by the owner in the planning session:

| Decision | Choice |
|---|---|
| Layering | Strict Clean Architecture: data / domain / presentation with use cases, plus Room |
| Room scope | Durable local state **and** an offline read cache (asked separately, chosen explicitly) |
| Release | Full feature parity before the first release; work still ordered so the app runs early |
| 3D | Emblem, mascot and body map are all 3D on Android |
| Sign in with Apple | In the first release (needs a backend web flow) |
| Quality | Linters and good practices are a first-class requirement |
| Status on 2026-11-24 | Design approved; repo scaffolded with documents and empty module folders. No app code exists yet. This is a status, not a rule: later sessions write code. |

Approaches considered: (a) mirror iOS with no Room and thin layering, fastest and identical offline; (b) Room read cache with lighter layering; (c) strict Clean Architecture with Room. The owner chose (c). Cross-platform toolkits were not considered because native Android was requested.

---

## 2. How to use this plan

### Workspace

All paths are relative to the `north` workspace folder (the parent of this repo) unless noted.

| Path | What it is |
|---|---|
| `north-ios-app/khepri/` | iOS app (nested git repo). **Source of truth for behaviour.** Abbreviated `K/` below. |
| `K/khepri/Features/<Folder>/` | 40 feature folders |
| `K/khepri/Core/` | Network, Health, Notifications, Permissions, Security, Speech, Spotlight, UI, Background |
| `K/khepri/App/` | `AppModel.swift` (app phase), `AppRouter.swift` (destinations, deep links), `AppDelegate.swift` (push handling), `Intents/` |
| `K/NorthKit/` | Local package: theme tokens, fonts, medals, exercise art, generated API client |
| `K/Shared/`, `K/KhepriWidgets/`, `K/KhepriShare/` | Code shared with extensions, widgets, share extension |
| `K/DESIGN.md`, `K/PRODUCT.md` | Design rules and product/accessibility targets |
| `north-client/` | Go backend. Rules in `north-client/AGENTS.md`. Contract: `north-client/docs/api/openapi.yaml` |
| `north-web-app/` | SvelteKit web client. Holds the GLB models and serves `/.well-known` files |
| `../../platform/infra` | Cluster config. **Merging to its `main` deploys.** |
| `north-android-app/` | New Android repo (created by the scaffold) |

### What to trust

- **Trust the iOS code and `openapi.yaml` on `origin/main`.** Read the Swift file before porting a screen; the tables in this plan point to it.
- **Do not trust** `docs/ios/parity.md`, `docs/ios/*.md`, the root `ARCHITECTURE.md`, `.planning/codebase/*`, or `north-client/docs/AUTH_IOS_WALKTHROUGH.md`. All are out of date.
- The local `north-client` checkout is on branch `fix/audit-p2`, behind `origin/main`, and lacks `internal/aiconsent` and `internal/horizon`. Read backend code with `git -C north-client show origin/main:<path>`.
- Production runs the image SHA pinned in `platform/infra/apps/khepri/web/values-production.yaml`. Check it before assuming a route is live (`/horizon` may not be).

### Rules that apply to this work

- Infrastructure changes go only in `platform/infra`; archived `*-infra/` copies deploy nothing.
- No paid speech API. The cluster has its own transcription service; the backend already exposes `/voice/transcribe`.
- Tests never use a real API key; use stub servers.
- Sealed secrets are bound to namespace and secret name; a wrong namespace decrypts to nothing with no error.
- Deploy backend first, clients after. Never add a value to an existing response enum in the spec: shipped iOS builds fail the whole response on an unknown enum value.
- Push category strings must match the server's `nudges.Category*` constants exactly, or the buttons silently vanish.
- Library versions in this plan are deliberately absent. Look up the current stable version when adding each dependency.

### Size of the port

About 41k lines of app Swift, 13k lines of tests, 334 API operations used by iOS (390 in the spec), about 2,000 strings in four languages. No purchases, analytics, crash reporting or feature flags exist on iOS.

---

## 3. Deliberate differences from iOS

| Area | iOS | Android |
|---|---|---|
| Offline reads | Nothing cached; offline shows an error | Room caches a short list (section 6.5); offline shows last-known content with "Offline, showing HH:mm" |
| Cold launch offline with a valid token | `failed` phase | Lands on cached Today once the profile cache exists (Phase 2). In Phase 1 it shows `Failed`, as iOS does |
| Lock-screen widgets, Dynamic Island | Present | Not available. Ongoing workout notification covers the lock screen |
| Spotlight entities | Exercises and goals indexed | Dynamic launcher shortcuts for recent exercises |
| Siri phrases with confirmation | Five phrases | Launcher shortcuts through deep links; assistant voice invocation only if the spike proves it |
| Session expiry | Signs out locally at the original `expiresAt` | A 401 is the only expiry signal (the server slides the session) |
| Unknown enum values | Whole response fails | Decoded as `UNKNOWN`, UI takes a default branch |
| Sign in with Apple | Native | Web flow in a Custom Tab |
| Locked device before first unlock | `waitingForUnlock` phase | `Locked` phase kept only for Keystore read failures; must not sign out |
| Screen Time report | Dormant, not a target | Not ported; manual screen-time entry only |
| Form Check | Unreachable in the UI | Not ported; generated API types stay |
| Review prompt | StoreKit | Play In-App Review with the same rules |

---

## 4. Repo and build configuration

- Repo `north-android-app/`, its own git repo, beside `north-ios-app/`.
- Application id `com.fernandocorreia.khepri`. Debug uses the `.debug` suffix so it installs beside release. No separate `.beta` id: Play internal and closed tracks do that job.
- minSdk 28 (Health Connect and passkeys need it). targetSdk and compileSdk are the current stable level at execution time.
- Build types:

| Build | API base URL | Notes |
|---|---|---|
| debug | `http://10.0.2.2:8090` | Cleartext allowed for that host only, via network security config. Client appends `api/v1`. |
| release | `https://kheprios.com` | R8 full mode |

- Secrets (Google web client id, Firebase config) come from a git-ignored properties file; CI writes it from a secret. Nothing secret is committed.
- Edge-to-edge with insets handled on every screen. Rotation and process death keep screen state.
- Database and DataStore are excluded from cloud backup and device transfer.
- CI: GitHub Actions (the other repos use it). Play upload to the internal track after CI passes on `main`.

---

## 5. Backend and config prerequisites

A separate track with its own PRs. The Android app cannot be finished without them. Each row lists where the change lands and how to tell it is done.

### B1. FCM push (`north-client`)

- Add `internal/fcm` modelled on `internal/apns` (`apns.go`, `sender.go`, `service.go`, `api.go`).
- Device table and routes `PUT /devices/fcm` and `DELETE /devices/fcm/{token}`, added to `docs/api/openapi.yaml` (a backend test fails if a mounted route is missing from the spec: `cmd/api/api_spec_test.go`).
- Implement the existing `pusher.Send(ctx, userID, title, body, href, category)` interface (`internal/nudges/service.go`) and `SendThread` (`internal/crews/push.go`, `internal/moderation/moderation.go`). Wire in `cmd/api/routes.go` and `cmd/worker/main.go` next to APNs so a user with both device kinds gets both.
- Payload is **data-only**: `title`, `body`, `href`, `category`, `thread`. The app builds the notification, so action buttons work when the app is killed.
- Categories to carry unchanged: `CHECKIN`, `COACH_LIGHTER`, `COACH_MISSED`, `COACH_FIRST_WEEK` (`internal/nudges/nudges.go`), `CREW_MESSAGE` (`internal/crews/push.go`).
- Mark tokens failed when FCM reports them unregistered, as the APNs sender does.
- Answer 503 when FCM is not configured, as `/devices/apns` does.
- Done when: a stub-server test sends each category, and a real device receives a check-in nudge with buttons.

### B2. Sign in with Apple web flow (`north-client`)

- Today `internal/auth/apple.go` accepts only native iOS identity tokens (audience in `APPLE_BUNDLE_ID`, nonce check).
- Add `/auth/apple/start` and `/auth/apple/code`, copying the existing `/auth/google/start` + `/auth/google/code` pair. Accept the Apple Services ID as an audience (`APPLE_SERVICE_ID`, listed as "later" in `north-client/docs/IOS.md`).
- The app opens the start URL in a Custom Tab, Apple redirects to the backend, the backend redirects to `khepri://auth/apple?code=…`, the app exchanges the one-time code for `AuthResponse`.
- Account linking by email follows the Google web flow.
- Done when: an account created with Apple on iOS (relay email, no password) signs in on Android.

### B3. Health source for Android (`north-client`)

- `internal/health/api.go` hard-codes source `apple_health` on `/health/samples`. Add a source field (or path) accepting `health_connect`.
- `internal/awards/db/queries.sql` counts only `apple_health`; include `health_connect` so Android data earns the same awards.
- The activity-session source constraint is already relaxed (`migrations/20260815170000_relax_activity_session_source.sql`).
- Done when: an Android sync stores samples under `health_connect` and re-sending a day replaces it (no duplicates), as iOS relies on.

### B4. Check-in source (`north-client`)

- `internal/checkins/api.go` records any unknown `source` as `ios`. Add `android` to the allowed values (`internal/checkins/checkin/checkin.go`).
- Verified on `origin/main`: the response field is an open string ("clients show values they do not know generically"), so this does not break shipped iOS builds.

### B5. Digital asset links (`north-web-app`)

- Add `/.well-known/assetlinks.json` beside the AASA route (`src/routes/(public)/.well-known/apple-app-site-association/+server.ts`, built in `src/lib/features/pwa/aasa.ts`).
- Relations: `delegate_permission/common.get_login_creds` (passkeys) and `delegate_permission/common.handle_all_urls` (app links).
- Production lists only the **Play App Signing** certificate SHA-256, not the upload key and not the debug key.
- Today's universal links cover only `/i/*`. Android app links cover `/i/*` and `/app/*`.

### B6. Cluster config (`platform/infra`; merging deploys, confirm each change first)

- `WEBAUTHN_RP_ORIGINS=https://kheprios.com,android:apk-key-hash:<base64url SHA-256 of the Play signing cert>`. Setting it replaces the default, so `https://kheprios.com` must be listed.
- Add the Google **web** client id to `GOOGLE_IOS_CLIENT_ID` (a comma-separated allow-list). Android ID tokens carry the `serverClientId` as audience.
- Firebase service-account key as a sealed secret; Apple Services ID and key.
- Read `platform/infra/secrets/*/README.md` before sealing.

### Owner-only actions

Play Console app with Play App Signing, Firebase project, Google OAuth Android client (package name + SHA-1), Apple Services ID, a new Sonatype token (the dependency checker in the planning session failed with an expired token).

---

## 6. Architecture

### 6.1 Modules

```
north-android-app/
  app/                      single activity, app-phase root, tab shell, Hilt wiring, manifest, receivers
  build-logic/              convention plugins
  baselineprofile/
  maestro/                  end-to-end flows
  core/
    model/        (JVM)     shared domain types, AppError, Outcome, Session, DataScope
    common/       (JVM)     dispatchers, clock, DataChangeBus, SessionGateway, SignOutCleaner, SignedInState, TokenUnreadableException
    api/          (JVM)     generated DTOs and Retrofit interfaces only
    network/                OkHttp clients, interceptors, error mapping, SSE, multipart
    database/               Room database, DAOs, outboxes, snapshot cache
    datastore/              session token (Keystore-encrypted), preferences, drafts
    session/                session manager, app phase, sign-out orchestration
    designsystem/           tokens, fonts, components, medals, exercise art
    ui/                     Loadable helpers, markdown blocks, chat composer, haptics, reduce motion
    navigation/             AppDestination, DeepLinkParser, Navigator, route keys
    scene3d/                Filament wrapper, motion evaluators, emblem/mascot/body views
    charts/                 Vico wrappers themed to tokens
    speech/                 transcriber interface, on-device + server, text-to-speech
    health/                 Health Connect source, sync, mapping
    notifications/          channels, FCM service, action receivers, reminders
    testing/                fakes, test dispatchers, fixture loader
  feature/<name>/{domain,data,ui}
```

Convention plugins: `khepri.android.application`, `khepri.android.library`, `khepri.kotlin.jvm`, `khepri.compose`, `khepri.hilt`, `khepri.room`, `khepri.feature.domain`, `khepri.feature.data`, `khepri.feature.ui`, `khepri.quality`. One version catalog; no version outside it.

Dependency rules (enforced by the module split plus a module-graph assertion in `check`):

- `feature:x:domain` is pure Kotlin; depends only on `:core:model`, `:core:common`.
- `feature:x:data` depends on its own domain and `:core:api`, `:core:network`, `:core:database`, `:core:datastore`.
- `feature:x:ui` depends on its own domain and `:core:ui`, `:core:designsystem`, `:core:navigation`; it may add `:core:charts`, `:core:scene3d`, `:core:speech` when it uses them. It never sees `data`, `:core:api`, `:core:network` or `:core:session`, directly or transitively.
- No feature depends on another feature. Shared concepts move into `:core:model`; other features' screens are reached by destination.
- Only `:app` depends on `data` modules, to bind them with Hilt.
- The convention plugins add only the dependencies every module of that kind uses; anything else is declared in the module, so `buildHealth` stays clean. All module dependencies are `implementation`.

Core-to-core edges (anything not listed is forbidden; the module-graph assertion checks the full transitive graph):

| Module | May depend on |
|---|---|
| `:core:model` | nothing |
| `:core:common` | `:core:model` |
| `:core:api` | nothing (generated code plus serializers) |
| `:core:network` | `:core:model`, `:core:common`, `:core:api` |
| `:core:datastore` | `:core:model`, `:core:common` |
| `:core:database` | `:core:model`, `:core:common` |
| `:core:session` | `:core:model`, `:core:common`, `:core:api`, `:core:network`, `:core:datastore` |
| `:core:designsystem` | `:core:model` |
| `:core:ui` | `:core:model`, `:core:common`, `:core:designsystem` |
| `:core:navigation` | `:core:model`, `:core:common` |
| `:core:charts`, `:core:scene3d` | `:core:designsystem`, `:core:ui` |
| `:core:speech`, `:core:health`, `:core:notifications` | `:core:model`, `:core:common`, `:core:api`, `:core:network`, `:core:datastore` |
| `:core:testing` | any core module; test configurations only |

Time types are `java.time` (`Instant`, `LocalDate`, `ZoneId`, `Clock`) everywhere: available natively from API 26 and on the JVM, no extra dependency.

### 6.2 Feature modules and the iOS folders they cover

| Module | iOS folders | iOS lines (approx.) |
|---|---|---|
| auth | Auth, AIConsent | 2,300 |
| onboarding | Onboarding, Tour | 1,250 |
| today | Today, Day, Nudges, DailyPost | 3,800 |
| coach | Coach, QuickCoach, Memories, Decisions | 2,900 |
| training | Training (without `Workout/`) | 2,400 |
| workout | Training/Workout | 1,500 |
| planimport | PlanImport | 1,400 |
| nutrition | Nutrition | 2,000 |
| checkins | CheckIns, Capture, Inbox | 1,200 |
| progress | Progress, Fitness, Horizon, ProgressCheck, Goals | 6,300 |
| reviews | Weekly, Recap, Reports | 1,300 |
| wellbeing | Care, Mind, Knowledge | 1,050 |
| friends | Friends, Profile, Awards | 3,000 |
| crews | Crews | 2,200 |
| settings | Settings, Account, More, Review | 2,100 |

`Shell` goes to `:app`. `Dictation` goes to `:core:speech` and `:core:ui`. `FormCheck` is not ported.

### 6.3 The feature template

Every feature has the same shape. Phase 2 freezes it on the `today` + `checkins` slice.

```
feature/x/domain   model/  repository/ (interfaces)  usecase/ (one class per action, operator fun invoke)
feature/x/data     remote/ (DTO↔domain mappers)  local/ (entities, DAOs)  repository/ (impl)  di/
feature/x/ui       <screen>/Screen.kt  ViewModel.kt  UiState.kt   navigation/ (entry installer)
```

State contract:

```kotlin
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Content<T>(val value: T, val refreshing: Boolean, val staleSince: Instant?) : Loadable<T>
    data class Failed(val error: AppError) : Loadable<Nothing>
}
sealed interface ActionState { Idle; Running; data class Failed(val error: AppError) }

data class XUiState(val content: Loadable<X>, val save: ActionState, /* fields */)
sealed interface XEvent     // user intents, delivered through one onEvent(event)
sealed interface XEffect    // one-shot: navigate, snackbar, haptic; sent on a Channel
```

Rules, taken from iOS conventions:

- A failed refresh keeps what is on screen. Only an empty screen shows the error (offline icon + Try Again). Empty states name the first action.
- A failed save never blanks the screen; it shows inline and leaves the form filled.
- `UiState` is exposed with `stateIn(WhileSubscribed(5s))`. Screens reload on first show, pull to refresh, app foreground, tab re-selection, and a change-bus event for their scope.
- Repositories return a result type; nothing throws across a layer boundary.
- `AppError` follows iOS `APIError` (`K/NorthKit/Sources/NorthAPI/APIError.swift`) and keeps the server's message wherever iOS does: `Unauthorized(message?)`, `NotFound(message?)`, `Conflict(message?, newerVersionJson?)`, `FieldValidation(message, fields)`, `Server(status, message?, code?)`, `Refused(code, message?)`, `Network(interrupted)`, `Locked`, `InvalidResponse`. `interrupted` is true for every `IOException` except the ones that prove the request never left (TLS handshake, peer unverified, protocol, unknown service, missing local file), because dropped pooled connections surface as EOF, stream resets and SSL errors. A 409 is always `Conflict`, as on iOS. `Server` covers iOS `invalidStatus` and carries a code for 5xx refusals such as `import_partial`.

### 6.4 Reload after a write

iOS bumps `AppRouter.dataVersion` after any local write and every screen showing the day reloads; a screen must not bump it from its own load. Android:

- `DataChangeBus` is a `SharedFlow<DataScope>` (no replay) with coarse scopes: `Day`, `Training`, `Nutrition`, `Social`, `Goals`, `Profile`.
- Every write use case emits its scope on success. Repositories for that scope refetch. Cached resources update the screen through the DAO `Flow`; uncached ones through an in-memory `StateFlow`.
- Widgets, notification actions and shortcuts run in the app process and emit on the same bus. After any write, widgets are asked to refresh (iOS does this too).

### 6.5 What Room stores

Durable local state (replaces iOS files and retry queues):

| Table | Behaviour to match |
|---|---|
| `workout_snapshot` | Rewritten on every change; discarded after 12 hours or on a schema change; resumed on relaunch (`K/khepri/Features/Training/Workout/WorkoutSnapshotStore.swift`) |
| `set_outbox` | Retry 2 s doubling to 60 s, 8 automatic tries, then again on foreground and before finish. Refusals (validation, not found, unauthorized, conflict) are not auto-retried; 408, 429 and 5xx are (`WorkoutSession.swift`, `isWorthRetrying`) |
| `crew_pending_message` | Keyed by `clientId` for idempotent retry; listed as pending with retry or discard |

Read cache: one `cached_snapshot` table (account id, resource key, JSON, `fetchedAt`) for Today, the day for today and yesterday, the current training plan, the exercise library, profile/settings/me, the conversation list and the last page of each opened coach thread. Crew channel messages are relational (appended by `seq`).

Never cached: session token (DataStore only), health samples, Care, Mind, blood pressure, progress photos, knowledge files, memories, reports, other people's data and leaderboards, contact hashes, partial streamed replies. Coil's disk cache is off for signed file URLs.

Clearing:

- Explicit sign-out: unregister the push device, call logout (with the token), clear every table, clear account-scoped DataStore, cancel tagged WorkManager jobs, clear Coil, remove dynamic shortcuts.
- 401: the same, but the workout snapshot and outboxes stay with their account id and are dropped if a different account signs in. iOS does this so signing back in resumes the workout.

### 6.6 Local preferences (DataStore), ported from iOS UserDefaults

| Key | Purpose |
|---|---|
| `appearance`, `appearance.pendingUpload` | System/Light/Dark; last write wins; an unsent choice is pushed on next sync |
| app language | Account setting; applied through the per-app locale API |
| `push.fcmToken` | Last registered token |
| `health.sync.enabled`, `health.sync.lastReport`, backfill flag | Health sync state |
| `workoutReminders.enabled`, `workoutReminders.leadMinutes` (default 15) | On-device reminders |
| `invite.pending`, `crew.pending` | Link tapped while signed out; redeemed after sign-in; kept again on failure |
| `review.*` | Review prompt rules |
| `guidedTour.finished` | Per device |
| `quickCoach.muted`, `quickCoach.hasHeld` | Quick coach |
| `quickCheckIn.mood`, `quickCheckIn.picked` | Half-made check-in from a widget; expires after 10 minutes or at midnight |
| wizard draft | Onboarding draft per user; resumes after the app is quit |

---

## 7. API client

### 7.1 Generation

- Source: `north-client/docs/api/openapi.yaml` (OpenAPI 3.1) at a **pinned git SHA** (`core/api/contract.sha`), read with `git show`, never from the working tree. The pin is the newest `north-client` commit that is deployed to production; it moves forward when the backend deploys. S1 runs against the same pin Phase 1 generates from.
- `syncContract` copies the spec, `docs/medals/medals.json` and every `internal/*/testdata/*.golden.json` at the pin into `core/api/contract/`, which is **committed** (as iOS commits its copies). Generation and contract tests read only the committed copies, so CI needs no access to `north-client`. `syncContract --check` needs the sibling repo: it runs locally before a PR and in the pre-release checklist; a CI job for it needs a read token for `north-client` (owner action).
- Generator: OpenAPI Generator, Kotlin, Retrofit, kotlinx.serialization, coroutines; run as a Gradle task in `:core:api`; output not committed; template overrides checked in.
- Spec profile (origin/main 6823e58): no `oneOf`/`anyOf`/discriminators, 59 enums, 21 schemas with `additionalProperties: false` (day, capture and medication inputs only), 9 multipart operations, 15 `allOf` sites, 3 `type: [x, "null"]` sites and 3 `nullable: true` sites. `nullable` has no meaning in 3.1, so normalisation turns both forms into the same nullable field.
- Every generated model gets the suffix `Dto` (the spec has schemas such as `Outcome` that would clash with domain names).
- Operations are grouped into interfaces by the first segment of their path (`MeApi`, `AuthApi`, `TrainingApi`; 68 in all), not by spec tag. S1 records the grouping; domain code never sees it, because `data` modules wrap generated calls.
- Generator traps found in S1 (`docs/spikes/S1-generated-client.md`): quote the YAML 1.1 key `on` before parsing; map `number` → `Double` and `URI` → `String`; never send `UNKNOWN` (wire value `unknown_default_open_api`); group interfaces by first path segment, not spec tag; Retrofit query and path values go through `QueryValueConverterFactory`, or enums are sent upper case.
- Type mapping: `uuid` → `String`; `date-time` → `Instant` with a serializer that reads RFC 3339 with or without fractional seconds (Go drops the fraction when zero and writes nanoseconds otherwise) and writes with; `date` → `LocalDate`.
- Enums: every generated enum gets an `UNKNOWN` member; a test asserts it. The client refuses to send `UNKNOWN` (request bodies and query values).
- Required and nullable request properties are sent as explicit `null` (`HelpfulRequest.helpful`: null clears); optional nulls are left out.
- Nullable sites: a small spec normalisation step before generation; contract tests assert them.
- Field names are camelCase except `AIConsent.decided_at` and the capture/agent fields `ingredient_id`, `matched_name`, `check_in`, `amount_ml`.
- The passkey `publicKey` field is a free-form object; keep it as raw JSON.

### 7.2 Hand-written

- **SSE** in `:core:network` on OkHttp SSE, exposed as a cold `Flow`. No automatic retry on these routes.
  - `POST /conversations/{id}/reply` and `/resume`: events `token {text}`, `status {line}` (ignored), `exercises {slugs}`, `approval` (turn paused for a tool decision), `error {message}`, `done {messageId?}` (always last). Unknown event names are skipped. No keep-alive pings. A reply cut off by backgrounding is recovered through `ConversationDetail.awaitingResume` → `POST /resume` (`K/khepri/Features/Coach/ReplyRecovery.swift`).
  - `GET /crews/{id}/live`: `: ping` every 20 s; events `message`, `update`, `channels`, `removed`, `resync` with `{type, channelId, messageId, userId, seq}`. Events are signals; the client refetches. Nothing is replayed after reconnect. Reconnect backoff 1, 2, 5, 10 s; stop on 404 or a refusal (`K/khepri/Features/Crews/Chat/CrewLiveStream.swift`).
- **Multipart** (9 operations): hand-written Retrofit interfaces with streaming bodies from content URIs. File parts must carry a real content type. Limits: JSON bodies 1 MiB, uploads about 91 MiB, chat attachments 8 MB.

### 7.3 Client behaviour to match (`K/NorthKit/Sources/NorthAPI/NorthAPI.swift`)

Interceptor order, outermost first: `Accept-Language`, bearer, long-call timeout, retry. Error mapping is not an interceptor: `apiCall {}` maps every `Response` to `Outcome`, so it sees the same raw response the iOS error middleware sees.

One generated `OperationIndex` (method, path template, operationId, isPublic, isLong) drives the bearer and timeout interceptors; requests are matched by template, so `/auth/oauth/requests/{requestID}` works.

- **Bearer:** sent on every operation except those the spec marks `security: []`. Logout is the exception: it sends the token it revokes. Generate the public set from the spec.
- **401:** signs the user out only if a token was actually sent.
- **Unreadable token** (Keystore failure): the request is not sent and the user is not signed out (`Locked`).
- **`Accept-Language`:** the phone's top three languages with falling weights (`pt-PT, en;q=0.9, es;q=0.8`). Only matters before sign-in.
- **Retry:** GET and HEAD only, once, after 500 ms, when the connection was lost, timed out or offline. Never writes, never event streams.
- **Errors,** in this order, as iOS checks them: body `{"error":{"message","fields"?,"code"?}}`. 401 → Unauthorized; 404 → NotFound; 409 with the envelope → Conflict; any other status whose envelope has `fields` → FieldValidation (this includes the 429 quota refusal, which carries `fields`); 403 with `code` → Refused; anything else → Server(status, message, code). A non-2xx JSON body that is not the error envelope (a 409 carrying the newer plan) becomes `Conflict(newerVersionJson = body)`, which the repository decodes as the operation's documented type; that is the Android form of iOS passing it through.
- **Codes to handle** (sent by the server; not all are declared in the spec): `ai_consent_required` (403, `internal/shared/httpx/httpx.go`), `terms_required` and `suspended` (403, `internal/crews/chat_api.go`), `import_partial` (500 with a code, so `Server.code`), `invalid_reset_token`. Also statuses 402 (quota), 413, 422, 429 (`Retry-After: 60` on sign-in routes, `1` on signed-in routes), 503.
- **Unknown request fields are rejected by the server.** Only 21 schemas say `additionalProperties: false`, so schema validation alone would miss most. The test serialises a fully populated instance of every request type and fails on any key that is not a declared property of its schema, then also validates against the schema.
- **Clients:** two OkHttp clients sharing one pool: default (60 s read timeout) and streaming (300 s read timeout, as the iOS generation client, so a half-open stream after a network switch fails and reconnects; a stream with server pings, such as crew live every 20 s, sets 60 s per call; used only by `SseClient`). Long calls stay on the default client; an interceptor raises the read timeout to 300 s with `chain.withReadTimeout` for operations whose `OperationIndex` entry is long.
- **300 s operations:** `createPlan`, `parseWorkoutImport`, `parseMealImport`, `generateMealPlan`, `estimateProgressCheck`, `compareProgressChecks`, `decideToolCalls`, `uploadChatAttachment`.
- **Long calls in the background:** coach replies, plan approval (1–3 minutes), attachments and crew replies must survive the app going to the background (iOS uses `BackgroundActivity.run`); use expedited work or a short foreground service.

### 7.4 Auth contract

- Opaque bearer token, no cookies, no JWT, no refresh endpoint. Every sign-in returns `AuthResponse {token, expiresAt, user}`. 30-day sliding expiry on the server.
- Store token, user id and expiry in DataStore, encrypted with a Keystore key behind a `TokenCipher` interface (a fake cipher in JVM tests; the real one is covered by an instrumented test).
- `GET /auth/options` returns `{google, passkey, passwordReset}`; `google` reflects the web flow, not native.
- `AI_CONSENT_REQUIRED=true` in production: until the account answers through `GET/PUT /ai-consent` (`unset`, `granted`, `declined`), AI calls return 403 `ai_consent_required`.
- `/capture/*` accepts the session token.

### 7.5 Contract tests

- Decode each golden fixture with its generated type using a strict `Json` (`ignoreUnknownKeys = false`), as `K/NorthKit/Tests/NorthAPITests/ContractTests.swift` does. A test fails if a golden file has no mapping entry.
- Interceptor behaviour is tested with MockWebServer.

---

## 8. Navigation

### 8.1 Root

`MainActivity` renders the app phase (`K/khepri/App/AppModel.swift`): `Launching`, `Locked`, `SignedOut`, `Onboarding`, `SignedIn`, `Failed` (Try Again, Sign Out). The AI-consent gate is drawn above everything until the account answers. When the account declines, the Coach tab shows an "AI is off" view and the coach orb is hidden.

### 8.2 Tabs and stacks

Five tabs: Today, Coach, Training, Progress, More. One saved back stack per tab (Navigation 3). Re-selecting a tab pops to its root and reloads. System back pops the stack, then returns to Today, then exits. More carries a badge with the crew unread count. The tab bar is hidden inside a coach thread and in full-screen flows.

How it is built (proven in `docs/spikes/S9-navigation3.md`):
- The stacks, the selected tab and the per-tab ViewModel stores live in `NavigationState`, an activity-scoped ViewModel above the phase switch, saved in `SavedStateHandle` (not `rememberNavBackStack`). Commands go through the pure `NavPlanner`; one-shot payloads (start the workout, preset mood, coach draft, crew channel switch, re-select reload) are `ScreenAsk`s consumed once, never route fields.
- Each tab has one `NavDisplay`, under `key(tab)` and a `SaveableStateHolder`.
- Each tab has one `ViewModelStoreProvider`, held in `NavigationState` and passed to `rememberViewModelStoreNavEntryDecorator(provider)`. The default decorator clears every entry's ViewModels on a tab switch.
- Sheets use our own `BottomSheetSceneStrategy` (Navigation 3 ships only dialogs).

### 8.3 Destinations

`AppDestination` is a one-to-one port of the iOS enum (`K/khepri/App/AppRouter.swift`):

| Destination | Lands on |
|---|---|
| `Tab(tab)` | That tab |
| `Settings` | More, Settings sheet |
| `TrainingDay(day)` | Training, that plan day |
| `StartWorkout(day)` | Training, that day, workout started |
| `NextWorkout(start)` | Training, next session resolved once the plan loads |
| `Section(id)` | More section by web name: goals, check-ins, progress-check, reports, memories, knowledge, nutrition, care, mind, decisions, friends, crews, inbox, daily, awards |
| `Decision(id)`, `Goal(id)` | More → list → that item |
| `CheckIn(mood)` | More → Check-ins with the mood preset |
| `CheckInFlow`, `WeeklyReview`, `Horizon`, `FirstWeekReview` | Sheet over the current tab |
| `Invite(code)`, `CrewJoin(code)` | Code persisted, redeemed once signed in; opens Friends or Crews |
| `CrewChannel(crewId, channelId?)` | More → Crews → that space; if already open, moves to the channel |
| `Exercise(slug)`, `Muscle(id)` | Sheet over whatever is open |
| `AskCoach(draft)` | Coach, new conversation with the draft in the composer, unsent |
| `Conversation(id)` | Coach, that thread (iOS passes the summary; Android passes the id and the thread loads its own header) |

### 8.4 Deep links

`DeepLinkParser` ports `destination(for:)` as a pure function with the same test table (iOS tests: `MorePathTests`, `ShellTests`). It accepts `khepri://…`, `https://kheprios.com/app/…` and bare server paths.

| Path | Destination |
|---|---|
| (empty), `today` | Today |
| `chat`, `coach` | Coach |
| `training`, `workouts`, `exercises`, `activity` | Training |
| `training/next`, `training/next/start` | NextWorkout |
| `training/<plan>/<day>`, `…/start` | TrainingDay / StartWorkout (plan id is informational) |
| `insights`, `fitness` | Progress tab |
| `progress` (web path only) | Section `progress-check` |
| `check-ins?mood=1..5` | CheckIn(mood) |
| `decisions/<uuid>`, `goals/<uuid>` | Decision / Goal |
| `crews/<uuid>`, `crews/<uuid>/c/<uuid>` | CrewChannel |
| `i/<10 alphanumerics>` | Invite (lower-cased) |
| `i/c/<10 alphanumerics>` | CrewJoin (lower-cased) |
| `first-week/review` | FirstWeekReview; `first-week` alone → Today |
| `exercises/<slug>` | Exercise |
| `settings`, `weekly`, `horizon`, `more` | Settings, WeeklyReview, Horizon, More |
| any section id | Section |

Unknown links return nothing so the caller can open the browser. OAuth returns use `khepri://fitness/strava`, `khepri://friends/facebook`, and the new `khepri://auth/apple`.

### 8.5 One entry point

`Navigator.open(destination)` is called by deep links, push taps, notification buttons, widgets, shortcuts and in-app links. It selects the tab and builds the stack with parents in place. Destinations are commands, so tapping the same crew push twice still lands. A destination arriving before sign-in is held and replayed.

Sheets are entries on the current stack, so back dismisses them. Full-screen flows (workout, wizard, import review, awards celebration) hide the tab bar. Each `feature:x:ui` contributes its entries through a Hilt multibound installer; route keys live in `:core:navigation`.

---

## 9. Design system

- **Tokens:** generated from the web CSS by `K/scripts/sync-theme.py` into `K/NorthKit/Sources/NorthKit/Resources/Theme.xcassets`. Extend the script (or add a sibling) to emit Compose colours for light and dark. Core tokens: Background, Foreground, Signal/Accent, Agent, Ember, Destructive, Primary, PrimaryForeground, Hairline, Border, Ring, Card/Surface, Muted; day colours (Water, Food, Move, Sleep, Protein, Carb, Fat, Caffeine, Exercise, Stand, Sun, Screen, Now); body, heat and soreness ramps; sport colours.
- **Material 3 dynamic colour is off.** The palette is the brand.
- **Radii:** 6, 10, 12, 16. **Spacing:** 4/8/12/16/20/24 (a convention in `K/DESIGN.md`; make it a token on Android).
- **Fonts** (`K/NorthKit/Sources/NorthKit/Resources/Fonts/`): Geist (UI, scales with font size setting), Geist Mono (timers), Marcellus (brand moments only).
- **Rules from `K/DESIGN.md`:** no shadows on cards, one prominent button per screen, no em dashes in copy.
- **Components to port:** surface card, card header, big value, inset list, eyebrow, display number, prominent button, day gauges, text field with trailing icon, social auth button, password strength meter, chat composer, markdown blocks, icon badge, trend chart, wrapping layout, exercise sheet, import source picker, weekday picker, coach header ring (idle, listening, working, celebrating), exercise art (SVG path frames cross-fading), coin medals drawn from `medals.json`.
- **Icons:** iOS uses SF Symbols. Build one mapping table to Material Symbols, reviewed once.
- **Motion:** Compose animation APIs and Canvas; every non-informative animation checks reduce motion (animator scale). Numbers animate their digits. iOS ships no Lottie assets.
- **Haptics:** one enum mapped to platform constants: success (set done, check-in, kudos, award), selection, warning, error (login), heavy (rest over).
- **Strings:** a script converts `K/khepri/Resources/Localizable.xcstrings` (1,951 keys), the widget catalog (70), the share catalog (31) and the NorthAPI catalog (6) to `strings.xml` for `values`, `values-es`, `values-pt-rPT`, `values-pt-rBR`, with generated stable keys and plurals. A check fails the build on any missing translation (port of `K/scripts/check-translations.py`). Some text arrives already localised from the server.
- **Accessibility targets (`K/PRODUCT.md`):** WCAG 2.2 AA contrast, font scale to 200% without clipping, labels on every control, 48 dp targets, reduce motion honoured, layouts survive strings 30% longer. Today's stat grid and the training week switch layout at large text sizes.
- **Appearance:** System/Light/Dark saved to the account and kept on the phone. Widgets and the workout notification follow the system. Share cards are always dark.

---

## 10. Feature specifications

For each feature: the iOS source to read, what to build, the API it uses, and the behaviours that are easy to miss. Endpoint paths are relative to `/api/v1`.

### 10.1 auth (`K/khepri/Features/Auth`, `AIConsent`)

- **Login screen:** 3D emblem, "Khepri" wordmark in Marcellus, tagline, Sign In / Sign Up segmented switch with a sliding indicator. Error and info banners are tinted, announced to the screen reader, with an error haptic.
- **Sign in:** email, password, Forgot Password sheet, passkey, Apple, Google.
- **Sign up:** name, email, password with strength meter, confirm with inline mismatch error, Create Account, "create account with a passkey", Apple, Google. Sign-up sends the device time zone.
- **Password reset** finishes on the web from the emailed link.
- **Google:** Credential Manager; send the ID token to `POST /auth/google`.
- **Passkeys:** Credential Manager; `…/passkey/login/begin|finish`, `…/passkey/register/begin|finish`; RP id `kheprios.com`; credential JSON with base64url fields.
- **Apple:** Custom Tab to `/auth/apple/start` (B2).
- **AI consent:** full-screen Allow / Decline, recipient list from the server; settings row to change it.
- **API:** `signUp`, `logIn`, `signInWithGoogle`, `logOut`, `requestPasswordReset`, passkey begin/finish ×2, `getMe`, `get/setAIConsent`.
- **Tests to port:** AppModel phases, AI consent, Keychain behaviour (as DataStore/Keystore).

### 10.2 onboarding (`Onboarding/Wizard`, `Tour`)

- **Wizard, six steps** (`WizardModel.swift`): welcome (3D mascot), focus areas (fitness, health, work, learning, personal), coaching style (direct, supportive, socratic, or custom text), first goal, Health permission primer, notifications primer. Draft saved per user; resumes after quit. Sign Out available.
- **Tour:** three steps (Today, Coach, Training) as a spotlight cut-out drawn with Canvas. Completion per device; Settings → "Show Me Around" restarts it.
- **API:** `completeOnboarding`.

### 10.3 today (`Today`, `Day`, `Nudges`, `DailyPost`)

- **Today is one scrolling column**, in this order: date eyebrow and coach briefing; first-week card ("Day N of 7", Done, review from day 7); stat grid (streak, check-in, water, sleep); next-step card with the one prominent button; recovery card (only with watch data); outreach card (coach speaking first, answer buttons); lighter-day card (take today lighter at about 60% of sets, or keep the plan); your-week card (opens Recap); fitness-week card (opens Fitness); My Day dashboard; goals card; "Recently" timeline while My Day loads; news headlines (only if the ticker is on).
- **Toolbar:** previous/next day and Today, nudges bell (sheet; each nudge routes through the navigator), "+" Quick Add.
- **Loading:** cards appear together, after at most 2 seconds of waiting.
- **My Day** (`Day/DayViews.swift`, `DayStore`) is embedded in Today, not a screen: vitals strip; eight cards (fast, food, water, activity, sleep, workouts, nutrients, streak), each opening a detail sheet; body card (3D figure, training heat or soreness, tap a muscle → Muscle destination, soreness strip, link to Horizon); body sheet (weight, height, soreness, target weight, blood pressure); day timeline; trends (charts).
- **Quick Add** (`Day/DaySheets.swift`): "Log anything" (capture), water presets, caffeine presets or custom mg, supplements, screen time, "months since" trackers, links out.
- **Coach orb** bottom-trailing: tap opens Coach; hold dictates into the quick coach overlay (in the coach module).
- **Daily post:** one AI-written post a day, kudos, "Ask the coach about this" (AskCoach).
- **API:** `getToday`, lighter day get/choose, first-week get/done/finish, outreach today/answer, `getNews`, `getDay`, `getDayTrends`, `getBodyMap`, `getBodyCurrent`, soreness get/history/set/clear, `logCaffeine`, fasting start/stop, `logSupplement`, `setScreenTime`, trackers create/done, `recordBloodPressure`, `setTargetWeight`, `logWater`, nudges list/open/read/dismiss, daily posts + kudos.
- **Cache:** Today, and the day for today and yesterday.

### 10.4 coach (`Coach`, `QuickCoach`, `Memories`, `Decisions`)

- **Conversation list:** normal or reflection threads, swipe to delete, empty state "Start a Conversation", custom header with the animated ring.
- **Thread** (`ConversationStore`): streamed reply; inline exercise cards (open the exercise sheet); tool approval card (approve or deny, 300 s call); thumbs up/down; attachments (photo, PDF, docx, xlsx, csv, txt, json, up to 8 MB); dictation; stop button; interrupted replies recovered; a reflection thread can end.
- **Quick coach:** hold the orb, speak, a rising card shows the reply, read aloud sentence by sentence with other audio ducked (`ReplySpeaker.swift`), approvals, "Open in chat". Mute setting.
- **Memories:** keep or discard proposals; pinned / normal / excluded; edit; forget; add.
- **Decisions:** list, new decision sheet, outcome "Did it hold?", deep link to one decision, calibration.
- **API:** conversations list/start/get/delete, reply and resume (SSE), attachments, `decideToolCalls`, `rateMessage`, `getExercise`, `searchExercises`, memories (8), decisions (6).
- **Cache:** conversation list and the last page of each opened thread.
- **Tests to port:** SSE decoding, reply recovery.

### 10.5 training (`Training`)

- **Training screen:** loading, failed, empty (Create a Plan, Import a Plan); plan overview with this week's sessions and Edit Week, plan header with rationale, plan days, "Worth a look" problems.
- **Menu:** Exercise Library, Muscles This Week, Activity History, Follow Another Plan, New Plan (intake sheet), Import Plan.
- **Week editor sheet:** this or next week, the weekday each session falls on, "Back to Your Usual Week".
- **Day view:** Start Workout; start time (drives the reminder); exercise rows with swipe Swap/Remove and a menu for sets and reps, swap, move up/down, remove; Add Exercise (suggestions and search); tap opens the exercise sheet. Only the followed plan is editable. A 409 on an edit returns the newest version and shows a notice.
- **Activity history** with a sheet to log one after the fact. **Exercise library** with search and muscle filter. **Muscles screen** with the body map for this week.
- Workout reminders are rescheduled every time Training loads (`TrainingStore.swift`).
- **API:** plans list/create/get/activate, latest intake, training week get/set/reset/suggest, plan-day editing (start time, suggestions, add/swap/remove/move, prescription, replacements), activity list/log/delete.
- **Cache:** current plan, exercise library.

### 10.6 workout (`Training/Workout`)

Read `WorkoutSession.swift` in full before starting; it is the specification.

- **The workout never waits on the network.** It starts locally on tap; the server session is joined when it answers. If the server refuses, the workout continues and the finish says why it was not recorded. If start fails but a session is already open on the account, join it and say so.
- **Phases:** ready, working, resting(until), finished; plus paused. Rest is measured by the clock, so a rest that ran out while the app was dead is over on resume.
- **Screen (full-screen):** animated exercise art, "exercise N of M", timer, "Done · Set N" → set entry sheet (weight stepper, reps, set kind, RIR picker, suggestion from Horizon's next sets, last time's numbers). Rest countdown with +15 s and Skip; heavy haptic when rest ends. Pause/resume. End offers Finish and Save, Discard, Keep Going. Recap with a chart.
- **Sets:** a warm-up is logged but does not advance the plan. Exercises use this week's prescription and skip those with zero sets. Weight ≥ 0, reps ≥ 1, RIR 0–10.
- **Finish:** stop the service, cancel the rest alert, clear the snapshot, refresh widgets, write the workout to Health Connect, wait for the server session, resend failed sets, then stop the session. Sets the server refused are counted as "unsaved" in the summary.
- **Discard:** delete the saved sets on the server and cancel the session.
- **Restore after process death:** resume from the snapshot, re-attach the ongoing notification, rejoin the open server session (never start a new one: it would time the workout from the wrong moment), keep last time's numbers as they were, resend pending and failed sets.
- **Ongoing notification / foreground service** with exercise, set x of y, a system-run rest countdown and moving time; leftovers cleaned at launch.
- **API:** activity start/pause/resume/stop/cancel and open session, lifts sets log/delete, `lifts/last`, `lifts/stats`, session recap, `horizon/next-sets`.
- **Tests to port:** workout session, snapshots, set retries, rest notifications, recap.

### 10.7 planimport (`PlanImport`)

- Import a training plan or a meal plan from a photo, camera, or file: parse (300 s, multipart), review the draft, commit. Meal plans can also be built with AI from a form. `import_partial` is a refusal code to surface.
- **API:** `parseWorkoutImport`, `commitWorkoutImport`, `parseMealImport`, preview, import, meal intake, `generateMealPlan`.

### 10.8 nutrition (`Nutrition`)

- Segmented sections: eaten today against the goal, delete entry, Log Food sheet (ingredient by grams, or a meal from a plan); ingredients with New Ingredient sheet; meal plans and one plan (day targets, portions, alternatives, portion sheet, day target sheet, plan settings sheet); Speak Meal sheet (add a meal by voice, `POST /capture/foods`); Body & Goal (macro calculator, also linked from Settings and Fitness).
- **API:** ingredients search/create/delete, plan options, meal plans (15 operations), food log get/log/log meal/delete, calculator get/biometrics/plan.

### 10.9 checkins (`CheckIns`, `Capture`, `Inbox`)

- **Check-ins:** today's form (mood and energy 1–5, optional stress and sleep, tags, goal link), celebration, the coach's reply bubble; past days with swipe edit/delete; week dots. Mood can be preset from a notification or link.
- **Quick check-in** (widget or notification): refuses to save if a check-in already exists today, so it never overwrites notes written in the app.
- **Capture:** parse a sentence, preview what was read, commit. Used by Quick Add, the share target and shortcuts.
- **Inbox:** items from the share sheet or typed; swipe File as Suggested, or file to Journal, Knowledge or a goal note; Dismiss.
- **API:** check-ins list/save today/update/delete, reply to today's check-in, capture parse/commit, inbox list/add/file/dismiss.
- Send `source: android` (B4).
- **Tests to port:** check-in notification flow.

### 10.10 progress (`Progress`, `Fitness`, `Horizon`, `ProgressCheck`, `Goals`)

- **Insights (Progress tab):** range picker, awards card, "N of M on track", recovery today, areas (By Week → Areas; each score → domain view for body, mind, progress, training, nutrition, sleep, cardio, patterns, timeline, coach, spend), Worth Watching and health metrics → metric detail, highlights, links to Horizon, the 12-week consistency grid and the stats pages (sleep, cardio, cardio by kind, eating, patterns). Lifting sections with exercise detail.
- **Fitness:** week, workouts, weight, steps and VO2 max trends read directly from Health Connect on the phone.
- **Horizon (sheet):** body figure with a slider from today to 12 weeks out; each forecast as a range bar; lift detail (fan chart, track record); share card image at 4:5.
- **Progress check:** front/side/back photos and measurements, camera, estimates (300 s), reminder cadence, first-vs-latest comparison (300 s), delete.
- **Goals:** list, detail (milestones add/edit/re-date/delete, progress notes, status, delete), goal form sheet.
- **API:** insights (14), stats (5), horizon (3), progress checks (7), goals and milestones (11), `getXP`.
- 14 screens use charts; build them on `:core:charts`.
- **Tests to port:** Horizon, progress check.

### 10.11 reviews (`Weekly`, `Recap`, `Reports`)

- **Weekly review (sheet):** The Week, Priorities, Goals in Order, Training.
- **Recap (sheet):** 7 or 30 days, totals against the previous period, stacked day bars, share card image, "post to friends".
- **Reports:** weekly reports and morning briefings rendered from markdown blocks; rate helpful; archive.
- **API:** weekly review/focus, recap get/share, reports list/get/archive/rate.

### 10.12 wellbeing (`Care`, `Mind`, `Knowledge`)

- **Care:** water presets with undo, last night's sleep sheet, habits and reminders (add sheets with weekday picker), This Week chart.
- **Mind:** journal with mood trend and a Write sheet.
- **Knowledge:** searchable library, Write a Note, Add a File, Re-read Everything, document text view with heading jumps.
- **API:** care (10), journal get/write, knowledge (8, one multipart).

### 10.13 friends (`Friends`, `Profile`, `Awards`)

- **Friends:** your profile and leaderboard (four boards, XP and level); feed with kudos, workout cards and awards; invite link (share); handle editor; phone number with a texted code (only if the server says `phone.configured`); "What Followers See" toggles; follow by handle; find contacts (emails and phone numbers hashed with SHA-256 on the phone, only hashes sent: `ContactMatching.swift`); Facebook friends (only if `facebook.configured`); Following, Followers, Blocked with swipe actions.
- **Profiles:** profile, their workouts, their awards, workout detail.
- **Awards:** awards screen, detail sheet, monthly challenge card, unlock celebration (full-screen, pages through new badges, coin flip, "View on Profile"). Coins are drawn on Canvas from `medals.json`.
- **API:** social get, handle, feed, sharing, invites redeem, follows, followers, blocks, friends match, phone verification, Facebook, profiles (3), awards + unseen + seen, kudos, leaderboard.
- **Tests to port:** contact matching, phone numbers, medals.

### 10.14 crews (`Crews`)

- **Crews list:** New Crew, join links.
- **Crew space:** Chat / Challenge switch. Chat: channel switcher, messages, replies, reactions, edits, pins, @mentions, report sheet, pins sheet, mute sheet. Rules sheet must be agreed before the first post (`terms_required`). Challenge board: challenge editor, member rows, remove or block, leave crew.
- **Live stream** with reconnect; events only trigger a refetch.
- **Sends** carry a `clientId`; pending messages show retry or discard. A reply from a notification derives its id from the notification id so retries are stored once.
- **Unread** badges on the More tab and the Crews row.
- **API:** crews list/create/join/get, member remove, challenge set/delete, moderation terms get/accept, unread, space, notifications, channel messages list/send, pins, read, message get/edit/delete, reactions, pin, report, live (SSE).
- **Cache:** channel messages.
- **Tests to port:** crew chat store, live stream, crew notifications.

### 10.15 settings (`Settings`, `Account`, `More`, `Review`)

- **More tab:** top section (Plan the Week, Horizon), Growth (Daily Post, Goals, Check-ins, Progress check, Reports, Memories, Knowledge, Awards, Friends, Crews, Inbox), Life (Nutrition, Care, Mind, Decisions), Settings as a sheet.
- **Settings:** Account (name, time zone, "use this phone's"); Coach (coaching style with dictation, tone, AI provider with bring-your-own-key, AI data sharing); Connections (Health Connect: toggle, sync now, allow new data types, stop or stop-and-delete; Strava OAuth connect/sync/disconnect; Agents: create with one-time token sheet, revoke, activity log; Telegram link; Calendar); App (language, appearance, notifications, units, diets, Body & Goal, export as a zip to the share sheet, Show Me Around, review rows, Discord link, acknowledgements); Sign Out with confirmation; Delete Account (type to confirm).
- **Notifications settings:** missed check-in, goal deadlines, training reminders, coach activity, "let my coach message me first", "lighten a session when recovery is low", daily briefing and hour, weekly report, stats digest, evening reflection and hour, quiet hours, on-device workout reminders and lead time, progress-photo cadence.
- **Review prompt** (`Review/`): after successes on 3 different days, never in the first 24 hours, at most once per 120 days, switchable in Settings.
- **Pattern:** load, edit, save with a toolbar save action and inline error rows.
- **API:** settings profile, preferences, notifications, AI, connections, activity, Telegram, diets, calendar; Strava; account export and delete.
- **Tests to port:** review prompter, appearance, language.

---

## 11. Platform integrations

### 11.1 Push and notification actions (`:core:notifications`; iOS: `K/khepri/App/AppDelegate.swift`, `K/khepri/Core/Notifications/`)

- Register the FCM token after sign-in and on each launch; unregister before sign-out.
- Data-only messages; the app builds the notification from `title`, `body`, `href`, `category`, `thread`.
- Banners show while the app is open, except rest-over and messages for the crew channel on screen.
- A tap calls `POST /nudges/{id}/open?from=push` and routes the `href` that comes back.
- Actions run without opening the app, through a receiver that enqueues expedited work:

| Category | Buttons |
|---|---|
| `CHECKIN` | Mood: Good (4), Okay (3), Low (2). Then a second local notification asks energy and files the check-in. Expires if the day changes. |
| `COACH_LIGHTER`, `COACH_MISSED` | Coach answer buttons (`CoachActions.swift`) |
| `COACH_FIRST_WEEK` | Done (`FirstWeekActions.swift`) |
| `CREW_MESSAGE` | Inline text reply (`CrewNotifications.swift`) |
| `WORKOUT_REMINDER` (local) | Start Workout |

- Android 13+ needs the notification permission; ask in the onboarding primer.

### 11.2 Reminders (`K/khepri/Core/Notifications/WorkoutReminders.swift`, `RestNotifications.swift`)

- Weekly from the plan, or up to 14 dated ones from the server's two-week schedule. Fire in the **account's** time zone (07:00 at home still fires at home-07:00 while travelling). Lead time can push a reminder into the previous day.
- AlarmManager; reschedule on boot, time change and zone change, and whenever Training loads.
- Without the exact-alarm permission, fire within a few minutes; ask for the permission once.
- Rest-over alert: scheduled only if notification permission is granted; hidden while the app is in front.

### 11.3 Health Connect (`:core:health`; iOS: `K/khepri/Core/Health/`)

- Read about 30 types (list in `K/khepri/Core/Permissions/Permissions.swift`): daily totals or averages, sleep nights and stages, blood pressure, workouts with heart rate, distance and climb. Port the payload shape and metric names from `HealthPayload.swift` and the activity mapping from `HealthActivityMapping.swift`.
- Sync rules: first sync reads 30 days; later syncs start 2 days before the last; one-off 90-day backfill. Run on foreground, from Settings, and in the background (periodic WorkManager with change tokens).
- Each daily total is keyed by its start of day, so re-sending replaces it.
- Write finished strength workouts as exercise sessions.
- The Fitness screen reads Health Connect directly.
- Known mismatches to settle in the spike: HRV is RMSSD in Health Connect (iOS sends SDNN); stand hours, exercise minutes, time in daylight and walking heart rate average have no match.

### 11.4 Widgets, share target, shortcuts

- **Widgets (Glance):** Today (small and medium; "log 250 ml water" runs in place) and Check-in (two taps, mood then energy). Show "Offline" when there is nothing. Refresh after any write and every 30 minutes. iOS reference: `K/KhepriWidgets/`, `K/Shared/Glance.swift`, `QuickCheckIn.swift`, `CheckInStreak.swift`.
- **Share target:** accepts text, one URL or one web page; save to Knowledge, save to Inbox, or parse and log with a preview first (`K/KhepriShare/ShareView.swift`).
- **Shortcuts:** capture, log check-in, start today's workout, open exercise, log water, routed through deep links (`K/khepri/App/Intents/`).

### 11.5 Speech (`:core:speech`; iOS: `K/khepri/Core/Speech/`, `Features/Dictation`)

- Dictation in: chat composer, quick capture, quick coach, speak your meal, training intake, onboarding, coaching style. A primer alert comes before the system permission prompt.
- On-device `SpeechRecognizer` where the language is supported; otherwise record and send to the backend's `POST /voice/transcribe` (`GET /voice` gives availability and limits).
- Text-to-speech reads coach replies sentence by sentence, ducking other audio; split sentences with ICU `BreakIterator`.

### 11.6 3D (`:core:scene3d`; iOS: `K/khepri/Features/Day/BodyMapView.swift`, `BodyShell.swift`, `BodyShell.metal`, `Emblem3DView`, `Mascot3DView`)

- Filament directly (gltfio plus a small wrapper), with the web app's GLBs in `north-web-app/static/assets/models/` (body map, emblem, mascot). They require `EXT_meshopt_compression` and `KHR_mesh_quantization`.
- Body GLB: one node per muscle key (15 muscles) plus `shell`; the shell has five morph targets (`waist`, `hips`, `chest`, `overall`, `core`).
- Per-muscle colour by material instance; tap picking resolved to the node name; morph weights for body shape; the shell drawn twice (opaque core, rim outline with a custom material).
- Emblem and mascot motion: pure-Kotlin evaluators driven by `emblem-motion.json` and `mascot-motion.json` (copies of the web files), tested against the existing motion fixtures. iOS CI checks these stay in step with `north-web-app`; do the same.
- Flat image fallback under reduce motion, power saving, or load failure.
- Accessibility: one element with a custom action per muscle.

### 11.7 Other

| iOS | Android |
|---|---|
| Photo/camera/file pickers | Photo Picker, `TakePicture`, `OpenDocument` |
| Share cards via image renderer | Compose to bitmap, shared through FileProvider |
| Strava, Facebook OAuth sheet | Custom Tabs returning to `khepri://` |
| Background time for long calls | Expedited WorkManager or short foreground service |
| On foreground | Clean up leftover workout notification, refresh crew unread and awards, run health sync, refresh shortcuts |

---

## 12. Code quality

One tool per job. Everything gates CI (iOS runs SwiftLint only locally; do not repeat that).

| Job | Tool | Policy |
|---|---|---|
| Formatting | Spotless + ktfmt | Width 120, indent 4 (same as `K/.swiftformat`); `spotlessCheck` in CI |
| Kotlin analysis | detekt (no formatting rule set) | Zero issues, no baseline, warnings as errors. Function length warn 100 / error 200 and complexity ignoring simple `when` mappings, as `K/.swiftlint.yml` does. No `!!`. Unused imports and privates are errors. |
| Compose | Compose rules plugin for detekt | Stability, modifier parameter first optional, state hoisting, naming, no ViewModel passed down, previews private |
| Android and resources | Android Lint | Warnings as errors, no baseline. `MissingTranslation`, `ContentDescription`, `UnusedResources` as errors (`UnusedResources` ignores the generated `strings.xml` until Phase 6) |
| Hard-coded UI text | detekt rule | Lint's `HardcodedText` is XML-only. A detekt rule fails on a string literal used as Compose text, content description or label |
| Module boundaries | Module-graph assertion plugin | Rules in 6.1 |
| Dependencies | Version catalog, dependency-analysis `buildHealth`, Renovate | No unused or misdeclared dependency. Each new dependency is vetted before it is added |
| Dead code | R8 usage report | Weekly report, not a gate |
| Translations | Lint + translation check script | Fails on any missing key in the four locales |
| Accessibility | Lint + accessibility checks in Compose UI tests | Touch target, contrast, label findings fail |
| Screenshots | Roborazzi (JVM) | Light/dark, font scale 1.0 and 2.0, en and pt-PT |
| Runtime | StrictMode in debug | Disk/network on main thread and leaked closables crash |
| Release | R8 full mode, baseline profile, startup macrobenchmark | Release-build smoke run in CI |
| Pre-commit | Repo hook via `core.hooksPath` | Spotless on staged files, detekt on changed modules |

Coding practices:

- Kotlin explicit API mode in `core` modules. Immutable UI state. No `GlobalScope`; dispatchers injected. `collectAsStateWithLifecycle` in screens.
- Composables are stateless below the screen level; screens take state and `onEvent`.
- Name by role, not type. Document public declarations whose behaviour is not obvious from the name; no other comments.
- Every screen has a preview and a screenshot test for loading, content, empty and failed states.
- Test tags follow one style (kebab-case, as most iOS identifiers: `coach-orb`, `rest-countdown`, `day-card-<kind>`).

Test pyramid:

- **JVM unit:** use cases, mappers, ViewModels with fakes (MockK only at platform edges), deep-link tables, ports of the iOS logic tests named in section 10.
- **Contract:** golden decoding, request schema validation.
- **Integration (JVM):** repositories with MockWebServer and in-memory Room; SSE reconnect; outbox backoff.
- **Compose UI (Robolectric):** per-screen states and semantics.
- **Maestro:** end to end against a locally seeded server, reusing `K/scripts/seed-*.sh`. iOS launch arguments to mirror as debug intent extras: reset, skip tour, AI consent, demo health.

CI gates (GitHub Actions): `spotlessCheck`, `detekt`, `lint`, module graph, `buildHealth`, unit and contract tests, screenshot verify, `syncContract --check`, release assemble. Maestro nightly and before release.

Repo hygiene: `CLAUDE.md` and `AGENTS.md` with the rules in section 2, an ADR folder, `.editorconfig`, a PR template with the checklist "backend deployed first? strings in four locales? screenshots updated?".

---

## 13. Build order

Each phase lists tasks and what "done" means. Phase 1 waits only on spikes S1, S2 and S9; the other spikes run beside Phase 1 and gate the phase that uses them.

### Phase 0: prerequisites and spikes

Spikes live in a throwaway app (`north-android-app/spikes/`, never merged into the product modules). Each ends with a written result in `docs/spikes/Sx-<name>.md`: what was tried, pass or fail, the decision.

| # | Spike | Pass means | If it fails |
|---|---|---|---|
| S1 | Generated client against the real spec | Compiles; nullable fields correct; every enum has `UNKNOWN`; a populated request validates against the schema | Heavier template overrides on the same generator |
| S2 | Which spec is live | Pinned production SHA compared with `origin/main`; list of routes not yet deployed | Pin the contract to the production SHA |
| S3 | Filament + GLBs | Loads meshopt/quantized files; morph weights apply; two shell renderables hold different weights; tap picks the right muscle; transparent surface composites over Compose; APK size impact measured | Re-export uncompressed GLBs from the web app's model scripts |
| S4 | Health Connect | Background reads work; 90-day history is readable; HRV and unmatched types listed with a decision each | Foreground-only sync; shorter backfill |
| S5 | Passkeys and Google | Passkey sign-in on a Play-signed internal build; Google ID token accepted by the server | Fix origins / allow-list (B5, B6) |
| S6 | Alarms | Exact alarm fires in the account time zone; survives reboot and zone change | Windowed alarms |
| S7 | Speech | On-device recognition availability for en, es, pt-PT, pt-BR; `/voice/transcribe` round trip | Server transcription only |
| S8 | Workout service | Foreground service type accepted for a workout with no sensors; Android 16 live update tried | Plain ongoing notification |
| S9 | Navigation 3 | Per-tab stacks, sheets and full-screen entries, state restored after process death | Navigation Compose with type-safe routes |
| S10 | Check-in notification | Mood buttons then energy step work with the app killed | Open the app on the mood tap |
| S11 | Assistant shortcuts | Whether voice invocation is achievable | Launcher shortcuts only |

Also in Phase 0: owner account setup (section 5), backend PRs B1–B6, dependency vetting.

### Phase 1: foundation

1. Gradle project, version catalog, `build-logic` convention plugins.
2. All quality gates wired and green on an empty app; pre-commit hook; CI workflow.
3. `:core:model`, `:core:common`.
4. `:core:api` with `syncContract`, generation and contract tests.
5. `:core:network`: three clients, interceptors, error mapping, SSE and multipart helpers, MockWebServer tests for every rule in 7.3.
6. `:core:datastore` and `:core:session` with the app phase (`:core:database` comes in Phase 2 with its first entity).
7. `:core:designsystem`: tokens from the theme script, fonts, base components, icon mapping, screenshot tests.
8. String conversion script and translation check.
9. `:core:navigation`: `AppDestination`, `DeepLinkParser` with the ported table tests, `Navigator`.
10. `:app`: phase root, email sign-in and sign-up, five-tab shell with placeholders, sign-out.

Done when: a debug build signs in against a locally seeded server, switches tabs, opens a `khepri://` link, and `./gradlew check` is green in CI.

### Phase 2: reference slice (freezes the template)

1. AI consent gate.
2. `today` read path: Today cards and My Day, with cache and the staleness line.
3. `checkins` write path: today's check-in, celebration, coach reply bubble; change bus refreshes Today.
4. One Maestro flow (sign in, check in, see Today update); one screenshot set; one integration test with MockWebServer + Room.
5. Write `docs/feature-template.md` from what was built.

Done when: the daily loop works on a device, offline shows cached Today, and the template document exists.

### Phase 3: feature lanes (parallel after Phase 2)

| Lane | Modules | Depends on |
|---|---|---|
| A | coach | SSE helper |
| B | training, workout, planimport | S8 for the service |
| C | progress, reviews | `:core:charts` |
| D | nutrition, rest of checkins, wellbeing, settings | — |
| E | friends, crews | SSE helper |
| F | rest of auth (Google, passkeys, Apple), onboarding, tour | B2, B5, B6, S5 |

Each module is done when: every screen in its section 10 entry exists with loading, content, empty and failed states; its listed tests are ported; screenshots are recorded; a Maestro flow covers its main path; its parity checklist rows are ticked.

### Phase 4: system integrations (parallel with Phase 3 once B1 and B3 land)

Push and actions, reminders, workout foreground service, widgets, share target, shortcuts, Health Connect, dictation and text-to-speech. Submit Play declarations for health permissions and the foreground service type as soon as they are testable; review can take weeks.

### Phase 5: 3D and brand moments

Emblem, mascot, body map (Today, Muscles, Horizon), share cards, haptics pass. Can start any time after S3 passes; until then the flat fallbacks stand in.

### Phase 6: hardening and release

Translation completeness, accessibility audit (font scale 200%, screen reader, reduce motion), baseline profile, R8 release smoke, full parity Maestro suite, Play data-safety form, internal then closed testing, store listing in four languages.

---

## 14. Verification

- **Every change:** `./gradlew check` (Spotless, detekt, lint, module graph, `buildHealth`, unit, contract, screenshots).
- **Contract:** `./gradlew syncContract --check`.
- **End to end:** run `north-client` locally (`http://localhost:8090`), seed with the iOS seed scripts, install the debug build on an emulator (it uses `10.0.2.2:8090`), run the Maestro suite.
- **Parity:** `docs/parity-checklist.md` has one row per iOS screen and behaviour from section 10 and 11. Each row is closed by a Maestro flow or a side-by-side manual check against the iOS app.
- **On a real device only:** push actions with the app killed, reminders after reboot and after a time-zone change, Health Connect background sync, passkeys on the Play-signed build, 3D frame rate on a mid-range phone, workout survives the process being killed mid-rest.

---

## 15. Risks

- Play review of health permissions, background health reads and the foreground service type can take weeks.
- The passkey origin hash differs between debug, upload and Play-signed builds.
- FCM delivery is unreliable in the background on some manufacturers; action notifications may arrive late.
- iOS string keys are English source text; without generated stable keys and one conversion script the two apps drift.
- Filament adds native libraries per ABI; measure size in S3.
- The spec can run ahead of production; keep the contract pin honest.
- Strict layering across 15 features is a lot of mapping code; the frozen template and convention plugins keep it uniform.

---

## 16. What this session produces after approval

A scaffold and documents. No Kotlin, no Gradle build logic, no dependencies added (those are Phase 1, after versions are looked up and dependencies vetted).

Created under `north-android-app/` (new git repo, `git init`, nothing committed unless you ask):

```
README.md                       what the repo is, how the docs are organised
CLAUDE.md, AGENTS.md            the rules from section 2 and the coding practices from section 12
.gitignore, .editorconfig
docs/
  design.md                     this document
  backend-prerequisites.md      section 5 as a standalone brief per PR (B1–B6) with files and done-criteria
  feature-specs/<module>.md     one file per feature module from section 10, expanded with the iOS file list
  platform-integrations.md      section 11
  api-client.md                 section 7
  navigation.md                 section 8 (the deep-link test table itself is in phase-1-foundation.md, Task 9)
  quality.md                    section 12
  parity-checklist.md           one row per iOS screen and behaviour
  plans/phase-0-spikes.md       task-level plan with a protocol per spike
  plans/phase-1-foundation.md   task-level plan, ordered, with done-criteria
  plans/phase-2-reference-slice.md, phase-3-feature-lanes.md, phase-4-system-integrations.md,
        phase-5-3d-and-brand.md, phase-6-hardening-release.md   task lists per phase
  ios-test-inventory.md         iOS tests to port and core sources to read
  spikes/                       empty, one template file
  adr/0001 … 0006               the decisions in section 1 and section 3
app/, build-logic/, core/<module>/, feature/<module>/{domain,data,ui}/
                                empty directories with a one-line README each naming the module's job
```

Phase 0 and Phase 1 get task-level plans in this session. Later phases get task lists now and task-level plans once the spikes report.

Nothing is changed in `north-client`, `north-web-app`, `north-ios-app` or `platform/infra`.

---

## 17. Open points

These are my calls; say if any should change.

- Repo name `north-android-app`, application id `com.fernandocorreia.khepri`, minSdk 28, no `.beta` id.
- Form Check not ported; lock-screen widgets dropped.
- Android health data counts toward the same awards.
- Without the exact-alarm permission, reminders fire within a few minutes of the set time.
- The scaffold stops at documents and empty module folders; the Gradle skeleton is the first task of Phase 1.
