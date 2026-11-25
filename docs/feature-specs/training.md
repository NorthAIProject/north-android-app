# Feature spec: training

> Modules `:feature:training:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Training screen:** loading, failed, empty (Create a Plan, Import a Plan); plan overview with this week's sessions and Edit Week, plan header with rationale, plan days, "Worth a look" problems.
- **Menu:** Exercise Library, Muscles This Week, Activity History, Follow Another Plan, New Plan (intake sheet), Import Plan.
- **Week editor sheet:** this or next week, the weekday each session falls on, "Back to Your Usual Week".
- **Day view:** Start Workout; start time (drives the reminder); exercise rows with swipe Swap/Remove and a menu for sets and reps, swap, move up/down, remove; Add Exercise (suggestions and search); tap opens the exercise sheet. Only the followed plan is editable. A 409 on an edit returns the newest version and shows a notice.
- **Activity history** with a sheet to log one after the fact. **Exercise library** with search and muscle filter. **Muscles screen** with the body map for this week.
- Workout reminders are rescheduled every time Training loads (`TrainingStore.swift`).
- **API:** plans list/create/get/activate, latest intake, training week get/set/reset/suggest, plan-day editing (start time, suggestions, add/swap/remove/move, prescription, replacements), activity list/log/delete.
- **Cache:** current plan, exercise library.


## iOS source files

Activity history and the log-activity sheet belong to this module, but their Swift files sit under `Training/Workout/`: read `K/khepri/Features/Training/Workout/ActivityHistoryScreen.swift` and `ActivityService.swift` (listed in `workout.md`). The workout session itself is the `workout` module.

| File | Lines |
|---|---|
| `K/khepri/Features/Training/DayView.swift` | 315 |
| `K/khepri/Features/Training/ExerciseLibrary.swift` | 136 |
| `K/khepri/Features/Training/IntakeSheet.swift` | 153 |
| `K/khepri/Features/Training/MusclesScreen.swift` | 66 |
| `K/khepri/Features/Training/TrainingScreen.swift` | 235 |
| `K/khepri/Features/Training/TrainingService.swift` | 267 |
| `K/khepri/Features/Training/TrainingStore.swift` | 211 |
| `K/khepri/Features/Training/WeekEditorSheet.swift` | 252 |
| `K/khepri/Features/Training/WeekSection.swift` | 116 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
