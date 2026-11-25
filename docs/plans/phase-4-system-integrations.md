# Phase 4: system integrations

**Goal:** everything that lives outside the app's own screens: push, reminders, widgets, share target, shortcuts, Health Connect, speech.

**Spec:** `docs/platform-integrations.md` (design section 11).

**Depends on:** Phase 1; the feature each integration touches; backend B1 (push) and B3 (health); spikes S4, S6, S7, S8, S10. Can run beside Phase 3.

**Status:** task lists. Expand each track to a task-level plan before starting, using its spike result.

## Track P: push and notification actions (`:core:notifications`)

1. Notification channels: coach, check-in, crew, workout, reminders.
2. FCM service: register the token after sign-in and on each launch (`PUT /devices/fcm`), unregister before sign-out; a `SignOutCleaner`.
3. Build notifications from data messages (`title`, `body`, `href`, `category`, `thread`); group by thread.
4. Foreground rule: show banners while the app is open, except rest-over and messages for the crew channel on screen.
5. Tap: `POST /nudges/{id}/open?from=push`, then `Navigator.open(href)`.
6. Actions through a receiver and expedited work:
   - `CHECKIN`: Good (4), Okay (3), Low (2) → second local notification asks energy → file the check-in; expire if the day changed; refuse if a check-in already exists today. Port the iOS check-in notification tests.
   - `COACH_LIGHTER`, `COACH_MISSED`: answer buttons (read `K/khepri/Core/Notifications/CoachActions.swift`).
   - `COACH_FIRST_WEEK`: Done.
   - `CREW_MESSAGE`: inline reply with `RemoteInput`; the message id derives from the notification id so a retry is stored once.
7. Each action emits the right `DataScope` and refreshes widgets.
8. Device test with the app killed, on a Pixel and one other manufacturer.

## Track R: reminders

1. Schedule from the plan (weekly) or the server's two-week schedule (up to 14 dated), in the account time zone, with lead time (default 15 minutes; may cross midnight).
2. Reschedule when Training loads, on boot, on time change and on zone change.
3. Exact alarms when permitted; windowed otherwise; ask for the permission once.
4. `WORKOUT_REMINDER` notification with Start Workout → `StartWorkout(day)`.
5. Settings: on-device reminders toggle and lead time. Port the iOS reminder tests.

## Track W: widgets, share target, shortcuts

1. Glance Today widget (small, medium): streak, check-in, water with "log 250 ml" running in place, next step. "Offline" when empty. Refresh every 30 minutes and after any write.
2. Glance Check-in widget: mood then energy; the half-made check-in expires after 10 minutes or at midnight. Read `K/Shared/QuickCheckIn.swift`.
3. Share target activity: text, one URL, or one page → save to Knowledge, save to Inbox, or parse and log with a preview. Read `K/KhepriShare/ShareView.swift`.
4. Launcher shortcuts: capture, log check-in, start today's workout, log water (static); recent exercises (dynamic, cleared at sign-out).
5. Widgets and the share target reuse the session through the app process; signed out, they show a sign-in prompt.

## Track H: Health Connect (`:core:health`)

1. Permission request from onboarding and Settings; "allow new data types"; stop, or stop and delete (`DELETE /health/samples`).
2. Source: read the record types decided in S4; build the payload to match `K/khepri/Core/Health/HealthPayload.swift` (metric names, daily totals keyed by start of day, sleep nights and stages, workouts with heart rate, distance, climb).
3. Activity mapping from `K/khepri/Core/Health/HealthActivityMapping.swift`.
4. Sync rules: first sync 30 days; later syncs from 2 days before the last; one-off 90-day backfill; on foreground, from Settings, and periodic background work with change tokens. Upload with source `health_connect`.
5. Write finished strength workouts as exercise sessions; avoid re-uploading the app's own writes.
6. Fitness screen switches from the demo source to Health Connect.
7. Play health-permission declaration: submit as soon as the flow is testable. Port the iOS health tests.

## Track S: speech (`:core:speech`)

1. `Transcriber` interface with on-device and server implementations; choose per language from the S7 table; server path records to a format the service takes as-is and posts to `/voice/transcribe`.
2. Dictation button with the primer before the system permission prompt.
3. Wire into: chat composer, capture composer, quick coach (hold the orb), speak your meal, training intake, onboarding, coaching style.
4. Text-to-speech of coach replies sentence by sentence (ICU `BreakIterator`), audio focus with ducking, mute setting.

## On foreground

One coordinator in `:app` that, each time the app comes forward: cleans up a leftover workout notification, refreshes crew unread and awards, runs a health sync, retries failed sets, refreshes shortcuts.

## Done when

All Phase 4 rows in `docs/parity-checklist.md` are ticked, including the device-only checks in `docs/design.md` section 14.
