# Phase 0: prerequisites and spikes

**Goal:** remove the technical unknowns before product code depends on them, and get the backend and accounts ready.

**Spec:** `docs/design.md` (sections 5, 7, 11, 13).

**Where spikes live:** `spikes/` in this repo, as one throwaway Android app with one activity per spike. It has no quality gates and is never merged into `app/`, `core/` or `feature/`. Each spike ends with `docs/spikes/Sx-<name>.md` from `docs/spikes/TEMPLATE.md`.

**What gates what:** Phase 1 waits only on S2, S1 and S9, in that order (S2 is an hour of reading and gives S1 and Phase 1 the contract SHA). Dependency vetting needs the owner's Sonatype token or the owner's manual approval. Every other spike runs beside Phase 1 and gates the phase named in its row.

## Global constraints

- Look up the current stable version of every library when adding it. Do not use versions from memory or from this plan.
- Vet each dependency before adding it (the Sonatype checker needs a new token from the owner).
- Never use a real third-party API key in a test. Spikes that need the backend run against a local `north-client` at `http://10.0.2.2:8090`.
- Nothing is merged to `platform/infra` without the owner confirming; merging there deploys.
- minSdk 28.

## Track A: owner actions

These need the owner's accounts. Nothing below can be done by an agent.

- [ ] Play Console: create the app `com.fernandocorreia.khepri`, enrol in Play App Signing, record the app-signing certificate SHA-256 and SHA-1.
- [ ] Firebase: create the project, add the Android app, download `google-services.json` (git-ignored), create a service-account key for the backend.
- [ ] Google Cloud: OAuth Android client (package name + SHA-1 of the Play signing cert, and one for the debug cert); note the existing **web** client id.
- [ ] Apple Developer: Services ID for Sign in with Apple on the web, with the backend callback URL; key for the client secret.
- [ ] Sonatype: new token for the dependency checker.
- [ ] Decide whether the internal testing track is open to testers during Phase 3.

## Track B: backend and config

One PR per item. Briefs with files and done-criteria are in `docs/backend-prerequisites.md`.

| # | Change | Repo | Needs from Track A | Gates |
|---|---|---|---|---|
| B1 | FCM sender and `/devices/fcm` | `north-client` | Firebase service-account key | Phase 4 push |
| B2 | Apple web sign-in | `north-client` | Apple Services ID | Phase 3 lane F |
| B3 | `health_connect` source and awards query | `north-client` | — | Phase 4 health |
| B4 | `android` check-in source | `north-client` | — | Phase 2 |
| B5 | `assetlinks.json` | `north-web-app` | Play signing SHA-256 | S5, Phase 3 lane F |
| B6 | `WEBAUTHN_RP_ORIGINS`, Google allow-list, Firebase and Apple secrets | `platform/infra` | all of the above | S5, Phase 3 lane F, Phase 4 |

Before each backend PR: check that no value is added to an existing response enum in `docs/api/openapi.yaml`. Read `north-client/AGENTS.md` and, for B6, `platform/infra/secrets/*/README.md`.

## Track C: spikes

### S2. Which spec is live (do first; gives S1 and Phase 1 their SHA)

- **Steps:**
  1. Read the image tag on the infra repo's `origin/main`, not the local checkout (it can be many commits behind): `git -C ../../../platform/infra fetch -q && git -C ../../../platform/infra show origin/main:apps/khepri/web/values-production.yaml`. The tag is a `north-client` commit.
  2. List operation ids in `docs/api/openapi.yaml` at that commit and at `origin/main` (`git -C ../north-client show <ref>:docs/api/openapi.yaml`); diff them.
  3. Check `/api/v1/horizon` and `/api/v1/ai-consent` on production with an unauthenticated request (401 means live, 404 means not deployed).
- **Decide the pin:** the production commit. If Android needs operations that exist only on `origin/main`, record them; they block release until the backend deploys, and the pin moves forward then.
- **Pass means:** the pin SHA, and the list of `origin/main` operations production does not serve yet.

### S1. Generated client against the real spec (gates Phase 1)

- **Question:** does OpenAPI Generator (Kotlin, Retrofit, kotlinx.serialization, coroutines) produce code from the spec at the S2 pin that compiles and behaves?
- **Setup (step 0):** create `spikes/` as a Gradle project with one JVM module `spikes/client`. Add the generator plugin, kotlinx.serialization, Retrofit, JUnit and a JVM JSON Schema validator (2020-12 draft, able to load the spec's `components/schemas` so `$ref`s resolve). Each dependency is vetted first; if the Sonatype checker is unavailable, the owner approves the list by hand and the approval is recorded in the spike file.
- **Steps:**
  1. `mkdir -p spikes/contract/golden`; export the spec and five goldens at the pin: `git -C ../north-client show <sha>:docs/api/openapi.yaml > spikes/contract/openapi.yaml`, and the same for five `internal/<pkg>/testdata/*.golden.json` files from different packages, at least one containing timestamps and one containing an enum.
  2. Generate with model suffix `Dto`; exclude `replyInConversation`, `resumeConversation`, `streamCrewLive` and the 9 multipart operations.
  3. Build. List every schema that fails or is misnamed (15 `allOf` sites, inline enums) and pin names with schema mappings.
  4. Nullability: the spec has 3 `type: [x, "null"]` sites and 3 `nullable: true` sites (the second form means nothing in 3.1). Check each generated field is nullable; if not, write the normalisation step and re-run.
  5. Enums: turn on the unknown-default-case option. Record the member name it produces (by default it is not `UNKNOWN`); prefer renaming it to `UNKNOWN` with a template. Confirm it works with the kotlinx.serialization library option, and that decoding an unknown string yields it. If the option does not support kotlinx.serialization, write an enum template with a custom serializer.
  6. Write a minimal `Instant` serializer (`java.time`, reads with or without fractions and offsets) and decode `2026-09-24T07:15:00Z` and `2026-09-24T07:15:00.123456789Z`.
  7. Decode the five goldens with strict JSON (`ignoreUnknownKeys = false`).
  8. Requests: serialise a populated `DayInput`-style schema that has `additionalProperties: false` and a populated `CheckInRequest` (which does not). For both, fail on keys that are not declared properties, then validate against the schema. Confirm no null or default fields leak.
  9. Note which generated interface holds `getMe` and `logOut`, and how untagged operations are grouped.
- **Pass means:** builds; nullable fields correct; every enum has the unknown member; goldens decode; requests pass both checks.
- **If it fails:** heavier template overrides on the same generator. Hand-written DTOs are not the fallback.
- **Record:** generator version and options, template overrides, the normalisation script, schema-name mappings, the unknown-enum member name, the interface grouping, the validator chosen, the dependency versions used.

### S3. Filament and the GLBs (gates Phase 5)

- **Assets:** `north-web-app/static/assets/models/` (body map, emblem, mascot). They need `EXT_meshopt_compression` and `KHR_mesh_quantization`.
- **Gates, in order:**
  1. gltfio loads each file.
  2. Morph weights on the body `shell` (`waist`, `hips`, `chest`, `overall`, `core`) change the shape.
  3. Two renderables of the shell hold different weights (opaque core, rim outline).
  4. Setting base colour per muscle node (15 muscle keys) works.
  5. View picking returns the node name of the tapped muscle.
  6. A transparent Filament surface composites over Compose content, and under a bottom sheet.
  7. Frame rate on a mid-range device; APK size added per ABI.
- **Also:** port `emblem-motion.json` evaluation for one parameter and compare with `emblem-motion.fixtures.json`.
- **If gate 1 fails:** re-export uncompressed GLBs from the web app's model scripts.
- **Read first:** `K/khepri/Features/Day/BodyMapView.swift`, `BodyShell.swift`, `BodyShell.metal`.

### S4. Health Connect (gates Phase 4 health)

- **Steps:** request the read types listed in `K/khepri/Core/Permissions/Permissions.swift`; read 90 days of steps, sleep and workouts; run a periodic WorkManager read with a changes token while the app is in the background; write one exercise session and read it back.
- **Decide:** background-read permission behaviour per OS version; history permission for 90 days; HRV (Health Connect is RMSSD, iOS sends SDNN): send under a different metric name or skip; types with no match (stand hours, exercise minutes, time in daylight, walking heart rate average): drop.
- **Pass means:** a table of iOS metric → Health Connect record → decision, and a working background read.
- **If it fails:** foreground-only sync and a shorter backfill.

### S5. Passkeys and Google sign-in (gates Phase 3 lane F; needs B5 and B6)

- **Steps:** upload a build to the Play internal track; register and sign in with a passkey against the server (RP `kheprios.com`); sign in with Google through Credential Manager using the web client id as `serverClientId`; confirm the server accepts the token.
- **Pass means:** both sign-ins return `AuthResponse`.
- **Note:** the passkey origin is `android:apk-key-hash:<hash of the Play signing cert>`, not the upload key. The debug hash stays out of production config.

### S6. Alarms (gates Phase 4 reminders)

- **Steps:** schedule an exact alarm for a wall-clock time in a zone other than the device's; change the device zone; reboot; revoke the exact-alarm permission and repeat with a windowed alarm.
- **Pass means:** fires at the account-zone time after reboot and zone change; measured drift without the permission.

### S7. Speech (gates Phase 4 speech)

- **Steps:** check on-device `SpeechRecognizer` support for en, es, pt-PT, pt-BR on a Pixel and one other manufacturer; partial results; then record audio and post to `POST /voice/transcribe` on a local backend (`GET /voice` for limits).
- **Pass means:** a per-language support table and a working server round trip in a format the service accepts as-is (OGG/Opus, m4a, mp3, wav or webm; there is no transcoding in the cluster).

### S8. Workout service (gates Phase 3 lane B service work)

- **Steps:** foreground service with an ongoing notification showing a chronometer countdown; kill the app process and confirm the notification and timer survive; try the Android 16 promoted live update; identify the foreground service type Play accepts for a workout with no sensors.
- **Pass means:** the service type and manifest declaration, and a decision on live updates.

### S9. Navigation 3 (gates Phase 1)

- **Steps:** five tabs with one back stack each; a bottom-sheet entry and a full-screen entry on a tab's stack; re-select pops to root; process death restores the selected tab and each stack; a destination opened from outside builds a stack with parents (More → Goals → goal).
- **Pass means:** all of the above with the current stable Navigation 3 release.
- **If it fails:** Navigation Compose with type-safe routes.

### S10. Check-in notification (gates Phase 4 push)

- **Steps:** post a notification with three actions (Good, Okay, Low); on tap, with the app killed, post a second notification asking energy; on its tap, call `PUT /check-ins/today` from expedited work.
- **Pass means:** the check-in is filed without opening the app. Read `K/khepri/Core/Notifications/CheckInActions.swift` for the expiry rule (the day must still be today).

### S11. Assistant shortcuts (informational)

- **Steps:** static shortcuts for the five intents through deep links; test whether voice invocation works.
- **Pass means:** a yes or no. Launcher shortcuts ship either way.

## Done when

- [ ] `docs/spikes/S1…S11` exist, each with a decision.
- [ ] `docs/design.md` and the affected feature specs are updated for any decision that changed them.
- [ ] B1–B6 are merged and deployed, or have an owner and a date.
- [ ] The version catalog for Phase 1 can be written from the versions the spikes used.
