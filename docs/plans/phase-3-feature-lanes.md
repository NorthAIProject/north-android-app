# Phase 3: feature lanes

**Goal:** every in-app feature, built to the frozen template. System integrations (push, widgets, health) are Phase 4; 3D is Phase 5.

**Spec:** `docs/design.md` section 10; `docs/feature-specs/<module>.md` per module; `docs/feature-template.md` (from Phase 2).

**Depends on:** Phase 2 complete. Lanes are independent and can run in parallel.

**Status:** task lists. Before starting a lane, expand it to a task-level plan (format of `phase-1-foundation.md`): read every Swift file listed in the module's feature spec first.

## Per-module definition of done

Same for every module (also at the bottom of each feature spec):

- Every screen exists with loading, content, empty and failed states.
- The iOS tests named in the spec are ported.
- Screenshots recorded; a Maestro flow covers the main path; parity rows ticked; strings in four locales.

## Lane A: coach

1. Conversation list: normal and reflection, swipe to delete, empty state, custom header with the animated ring (idle, listening, working, celebrating).
2. Thread: streamed reply on `SseClient` (`token`, `exercises`, `approval`, `error`, `done`; skip unknown events; ignore `status`), stop button, inline exercise cards, thumbs.
3. Tool approval card (`decideToolCalls`, generation client).
4. Attachments: picker (photo, PDF, docx, xlsx, csv, txt, json), 8 MB limit, multipart upload on the generation client.
5. Reply recovery: `awaitingResume` → `POST /resume`; keep the stream alive in the background (expedited work or a short foreground service). Port the reply-recovery and SSE-decoding tests.
6. Cache: conversation list and the last page of opened threads.
7. Coach orb on Today and the quick coach overlay (dictation arrives in Phase 4; until then the overlay takes typed input behind a debug flag).
8. Memories. Decisions (list, new, outcome, single-decision deep link, calibration).
9. `AskCoach(draft)` and `Conversation(id)` destinations. "AI is off" view when consent is declined.

## Lane B: training, workout, planimport

1. Training screen: states, plan overview, week section, plan days, problems section, toolbar menu.
2. Week editor sheet. Plan intake sheet and `createPlan` (300 s, survives backgrounding).
3. Day view: start time, exercise rows with swipe and menu actions, add exercise picker, prescription sheet; 409 adopts the newer version and shows a notice. Only the followed plan is editable.
4. Exercise library, exercise sheet (`Exercise(slug)` destination), muscles screen (flat figure until Phase 5), activity history and log-activity sheet.
5. Cache: current plan, exercise library.
6. **Workout engine** (`:feature:workout:domain`, pure Kotlin): port `WorkoutSession.swift` as a state machine with injected clock and delay. Port the iOS workout tests first, then implement.
7. Room: `workout_snapshot` (12-hour expiry) and `set_outbox` (2 s doubling to 60 s, 8 automatic tries, non-retryable refusals wait for foreground or finish).
8. Workout screen: full-screen entry, exercise art, set entry sheet, rest countdown (+15 s, Skip, heavy haptic), pause/resume, end options, recap with chart.
9. Restore after process death; resumed-workout full-screen entry at launch.
10. Foreground service and ongoing notification (per S8). Rest-over alert.
11. Plan import: source picker (camera, photo, file), parse (300 s multipart), draft review, commit; `import_partial`.

## Lane C: progress, reviews

1. `:core:charts`: the chart shapes the 14 iOS chart screens need (line with range band, bar, stacked bar, fan chart, sparkline, 12-week grid on Canvas).
2. Insights screen and range picker; areas; domain views (body, mind, progress, training, nutrition, sleep, cardio, patterns, timeline, coach, spend); metric detail; consistency page; stats pages; lifting sections.
3. Goals: list, detail with milestones and notes, form sheet; `Goal(id)` destination.
4. Horizon sheet with slider and range bars; lift detail; share card (Compose to bitmap, 4:5, always dark, FileProvider). Flat figure until Phase 5.
5. Progress check: photos (camera and picker), measurements, estimate and compare on the generation client, cadence, delete.
6. Weekly review flow (sheet). Recap (sheet) with share card and post to friends. Reports with markdown blocks, helpful, archive.
7. Fitness screen: build against an interface with a demo source; the Health Connect source arrives in Phase 4.

## Lane D: nutrition, check-ins (rest), wellbeing, settings

1. Nutrition: food log, log-food sheet, ingredients, meal plans and plan detail with all sheets, Body & Goal calculator. Speak-meal sheet takes typed text until Phase 4.
2. Check-ins: past days with swipe edit/delete, week dots. Capture composer (parse, preview, commit). Inbox.
3. Quick Add sheet on Today (capture, water, caffeine, supplements, screen time, trackers).
4. Care, Mind (journal), Knowledge (search, note, file upload, re-read, document view with heading jumps).
5. More tab with all sections and the crew unread badge slot.
6. Settings sheet: account, coach (style, tone, AI provider and key sheet, AI data sharing), connections (Strava via Custom Tab returning to `khepri://fitness/strava`, agents with one-time token sheet, Telegram, calendar; the Health row is wired in Phase 4), app (language through the per-app locale API, appearance with pending-upload flag, notification settings, units, diets, export zip to the share sheet, acknowledgements), sign out, delete account.
7. Review prompt rules (port `ReviewPrompter` and its tests; Play In-App Review call).
8. Nudges bell and Daily Post.

## Lane E: friends, crews

1. Friends: profile and leaderboard, feed with kudos, invite link share, handle editor, sharing toggles, follow by handle, lists with swipe actions; `Invite(code)` redemption with the persisted pending code.
2. Phone number verification and Facebook friends (shown only when the server says they are configured; Facebook via Custom Tab returning to `khepri://friends/facebook`).
3. Find contacts: `READ_CONTACTS`, normalise, SHA-256 on the device, send hashes only. Port the contact-matching and phone-number tests first.
4. Profiles: profile, workouts, awards, workout detail.
5. Awards: screen, detail sheet, monthly challenge card, coin medals on Canvas from `medals.json` (port the medal tests), unlock celebration full-screen entry.
6. Crews list, new crew, join by link (`CrewJoin(code)`).
7. Crew space: chat with channels, replies, reactions, edits, pins, mentions, report, mute; rules sheet before first post (`terms_required`); challenge board.
8. Room: `crew_pending_message` (clientId) and the relational message cache. Live stream with reconnect 1, 2, 5, 10 s; events trigger refetch only. Port the crew store and live-stream tests.
9. Unread counts and badges; `CrewChannel` destination, including the "already open" case.

## Lane F: rest of auth, onboarding, tour

Depends on backend B2, B5, B6 and spike S5.

1. Google sign-in with Credential Manager.
2. Passkey sign-in and registration with Credential Manager.
3. Apple sign-in: Custom Tab to `/auth/apple/start`, return on `khepri://auth/apple`, exchange the code.
4. Onboarding wizard, six steps, draft persistence, notification-permission primer (Android 13+), Health primer (the permission request itself is wired in Phase 4). Flat mascot until Phase 5.
5. Guided tour overlay (Canvas cut-out from layout bounds) and "Show Me Around".

## Done when

Every module meets the per-module definition of done, and `docs/parity-checklist.md` has only Phase 4 and Phase 5 rows open.
