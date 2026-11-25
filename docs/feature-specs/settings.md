# Feature spec: settings

> Modules `:feature:settings:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **More tab:** top section (Plan the Week, Horizon), Growth (Daily Post, Goals, Check-ins, Progress check, Reports, Memories, Knowledge, Awards, Friends, Crews, Inbox), Life (Nutrition, Care, Mind, Decisions), Settings as a sheet.
- **Settings:** Account (name, time zone, "use this phone's"); Coach (coaching style with dictation, tone, AI provider with bring-your-own-key, AI data sharing); Connections (Health Connect: toggle, sync now, allow new data types, stop or stop-and-delete; Strava OAuth connect/sync/disconnect; Agents: create with one-time token sheet, revoke, activity log; Telegram link; Calendar); App (language, appearance, notifications, units, diets, Body & Goal, export as a zip to the share sheet, Show Me Around, review rows, Discord link, acknowledgements); Sign Out with confirmation; Delete Account (type to confirm).
- **Notifications settings:** missed check-in, goal deadlines, training reminders, coach activity, "let my coach message me first", "lighten a session when recovery is low", daily briefing and hour, weekly report, stats digest, evening reflection and hour, quiet hours, on-device workout reminders and lead time, progress-photo cadence.
- **Review prompt** (`Review/`): after successes on 3 different days, never in the first 24 hours, at most once per 120 days, switchable in Settings.
- **Pattern:** load, edit, save with a toolbar save action and inline error rows.
- **API:** settings profile, preferences, notifications, AI, connections, activity, Telegram, diets, calendar; Strava; account export and delete.
- **Tests to port:** review prompter, appearance, language.



## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Account/ExportRow.swift` | 49 |
| `K/khepri/Features/More/MoreScreen.swift` | 269 |
| `K/khepri/Features/Review/ReviewPrompter.swift` | 115 |
| `K/khepri/Features/Review/ReviewPromptPresenter.swift` | 53 |
| `K/khepri/Features/Review/ReviewSettingsRows.swift` | 19 |
| `K/khepri/Features/Settings/Acknowledgements.swift` | 19 |
| `K/khepri/Features/Settings/AppearancePicker.swift` | 32 |
| `K/khepri/Features/Settings/ConnectionSettings.swift` | 554 |
| `K/khepri/Features/Settings/FitnessSettings.swift` | 252 |
| `K/khepri/Features/Settings/SettingsScreen.swift` | 613 |
| `K/khepri/Features/Settings/SettingsService.swift` | 149 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
