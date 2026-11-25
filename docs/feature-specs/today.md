# Feature spec: today

> Modules `:feature:today:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Today is one scrolling column**, in this order: date eyebrow and coach briefing; first-week card ("Day N of 7", Done, review from day 7); stat grid (streak, check-in, water, sleep); next-step card with the one prominent button; recovery card (only with watch data); outreach card (coach speaking first, answer buttons); lighter-day card (take today lighter at about 60% of sets, or keep the plan); your-week card (opens Recap); fitness-week card (opens Fitness); My Day dashboard; goals card; "Recently" timeline while My Day loads; news headlines (only if the ticker is on).
- **Toolbar:** previous/next day and Today, nudges bell (sheet; each nudge routes through the navigator), "+" Quick Add.
- **Loading:** cards appear together, after at most 2 seconds of waiting.
- **My Day** (`Day/DayViews.swift`, `DayStore`) is embedded in Today, not a screen: vitals strip; eight cards (fast, food, water, activity, sleep, workouts, nutrients, streak), each opening a detail sheet; body card (3D figure, training heat or soreness, tap a muscle → Muscle destination, soreness strip, link to Horizon); body sheet (weight, height, soreness, target weight, blood pressure); day timeline; trends (charts).
- **Quick Add** (`Day/DaySheets.swift`): "Log anything" (capture), water presets, caffeine presets or custom mg, supplements, screen time, "months since" trackers, links out.
- **Coach orb** bottom-trailing: tap opens Coach; hold dictates into the quick coach overlay (in the coach module).
- **Daily post:** one AI-written post a day, kudos, "Ask the coach about this" (AskCoach).
- **API:** `getToday`, lighter day get/choose, first-week get/done/finish, outreach today/answer, `getNews`, `getDay`, `getDayTrends`, `getBodyMap`, `getBodyCurrent`, soreness get/history/set/clear, `logCaffeine`, fasting start/stop, `logSupplement`, `setScreenTime`, trackers create/done, `recordBloodPressure`, `setTargetWeight`, `logWater`, nudges list/open/read/dismiss, daily posts + kudos.
- **Cache:** Today, and the day for today and yesterday.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/DailyPost/DailyPostScreen.swift` | 150 |
| `K/khepri/Features/DailyPost/DailyPostService.swift` | 60 |
| `K/khepri/Features/Day/AmountField.swift` | 37 |
| `K/khepri/Features/Day/BodyMapView.swift` | 407 |
| `K/khepri/Features/Day/BodyShell.swift` | 186 |
| `K/khepri/Features/Day/DayActions.swift` | 71 |
| `K/khepri/Features/Day/DayMath.swift` | 246 |
| `K/khepri/Features/Day/DaySheets.swift` | 235 |
| `K/khepri/Features/Day/DayStore.swift` | 142 |
| `K/khepri/Features/Day/DayViews.swift` | 765 |
| `K/khepri/Features/Day/ScreenTimeReport.swift` | 67 |
| `K/khepri/Features/Day/SorenessFigure.swift` | 88 |
| `K/khepri/Features/Day/TrendsSection.swift` | 97 |
| `K/khepri/Features/Nudges/NudgesBell.swift` | 120 |
| `K/khepri/Features/Today/FirstWeekCard.swift` | 157 |
| `K/khepri/Features/Today/FirstWeekReview.swift` | 111 |
| `K/khepri/Features/Today/LighterDayCard.swift` | 150 |
| `K/khepri/Features/Today/NewsSection.swift` | 50 |
| `K/khepri/Features/Today/OutreachCard.swift` | 125 |
| `K/khepri/Features/Today/TodayScreen.swift` | 134 |
| `K/khepri/Features/Today/TodayView.swift` | 420 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
