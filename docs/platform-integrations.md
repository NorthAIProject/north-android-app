# Platform integrations

> Extracted from `docs/design.md` section 11. If the two disagree, fix both.


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


