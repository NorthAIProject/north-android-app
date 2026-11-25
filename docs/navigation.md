# Navigation

> Extracted from `docs/design.md` section 8. If the two disagree, fix both.


### 8.1 Root

`MainActivity` renders the app phase (`K/khepri/App/AppModel.swift`): `Launching`, `Locked`, `SignedOut`, `Onboarding`, `SignedIn`, `Failed` (Try Again, Sign Out). The AI-consent gate is drawn above everything until the account answers. When the account declines, the Coach tab shows an "AI is off" view and the coach orb is hidden.

### 8.2 Tabs and stacks

Five tabs: Today, Coach, Training, Progress, More. One saved back stack per tab (Navigation 3). Re-selecting a tab pops to its root and reloads. System back pops the stack, then returns to Today, then exits. More carries a badge with the crew unread count. The tab bar is hidden inside a coach thread and in full-screen flows.

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


