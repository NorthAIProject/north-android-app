# Feature spec: workout

> Modules `:feature:workout:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


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


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Training/Workout/ActivityHistoryScreen.swift` | 170 |
| `K/khepri/Features/Training/Workout/ActivityService.swift` | 59 |
| `K/khepri/Features/Training/Workout/LiftService.swift` | 71 |
| `K/khepri/Features/Training/Workout/LoggedSetsList.swift` | 85 |
| `K/khepri/Features/Training/Workout/SetEntrySheet.swift` | 158 |
| `K/khepri/Features/Training/Workout/SetKind.swift` | 45 |
| `K/khepri/Features/Training/Workout/WorkoutLiveActivityController.swift` | 115 |
| `K/khepri/Features/Training/Workout/WorkoutLoggedSet.swift` | 50 |
| `K/khepri/Features/Training/Workout/WorkoutRecap.swift` | 266 |
| `K/khepri/Features/Training/Workout/WorkoutSession.swift` | 489 |
| `K/khepri/Features/Training/Workout/WorkoutSession+Derived.swift` | 114 |
| `K/khepri/Features/Training/Workout/WorkoutSession+Live.swift` | 65 |
| `K/khepri/Features/Training/Workout/WorkoutSessionView.swift` | 383 |
| `K/khepri/Features/Training/Workout/WorkoutSnapshot.swift` | 52 |
| `K/khepri/Features/Training/Workout/WorkoutSnapshotStore.swift` | 75 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
