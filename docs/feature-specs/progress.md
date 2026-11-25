# Feature spec: progress

> Modules `:feature:progress:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Insights (Progress tab):** range picker, awards card, "N of M on track", recovery today, areas (By Week → Areas; each score → domain view for body, mind, progress, training, nutrition, sleep, cardio, patterns, timeline, coach, spend), Worth Watching and health metrics → metric detail, highlights, links to Horizon, the 12-week consistency grid and the stats pages (sleep, cardio, cardio by kind, eating, patterns). Lifting sections with exercise detail.
- **Fitness:** week, workouts, weight, steps and VO2 max trends read directly from Health Connect on the phone.
- **Horizon (sheet):** body figure with a slider from today to 12 weeks out; each forecast as a range bar; lift detail (fan chart, track record); share card image at 4:5.
- **Progress check:** front/side/back photos and measurements, camera, estimates (300 s), reminder cadence, first-vs-latest comparison (300 s), delete.
- **Goals:** list, detail (milestones add/edit/re-date/delete, progress notes, status, delete), goal form sheet.
- **API:** insights (14), stats (5), horizon (3), progress checks (7), goals and milestones (11), `getXP`.
- 14 screens use charts; build them on `:core:charts`.
- **Tests to port:** Horizon, progress check.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Fitness/DemoFitnessSource.swift` | 31 |
| `K/khepri/Features/Fitness/FitnessData.swift` | 306 |
| `K/khepri/Features/Fitness/FitnessScreen.swift` | 392 |
| `K/khepri/Features/Fitness/FitnessStore.swift` | 100 |
| `K/khepri/Features/Fitness/FitnessWeekCard.swift` | 122 |
| `K/khepri/Features/Goals/GoalsScreen.swift` | 514 |
| `K/khepri/Features/Goals/GoalsService.swift` | 128 |
| `K/khepri/Features/Horizon/HorizonLift.swift` | 88 |
| `K/khepri/Features/Horizon/HorizonLiftDetailScreen.swift` | 250 |
| `K/khepri/Features/Horizon/HorizonModel.swift` | 177 |
| `K/khepri/Features/Horizon/HorizonScreen.swift` | 441 |
| `K/khepri/Features/Horizon/HorizonService.swift` | 47 |
| `K/khepri/Features/Horizon/HorizonShareCard.swift` | 132 |
| `K/khepri/Features/Horizon/HorizonStore.swift` | 70 |
| `K/khepri/Features/Progress/ActivityKindPage.swift` | 204 |
| `K/khepri/Features/Progress/AreasScreen.swift` | 113 |
| `K/khepri/Features/Progress/ConsistencyPage.swift` | 148 |
| `K/khepri/Features/Progress/InsightsDomainService.swift` | 99 |
| `K/khepri/Features/Progress/InsightsDomainView.swift` | 601 |
| `K/khepri/Features/Progress/InsightsScreen.swift` | 369 |
| `K/khepri/Features/Progress/InsightsService.swift` | 82 |
| `K/khepri/Features/Progress/LiftingSections.swift` | 204 |
| `K/khepri/Features/Progress/MetricDetailView.swift` | 155 |
| `K/khepri/Features/Progress/RecoveryViews.swift` | 79 |
| `K/khepri/Features/Progress/StatsViews.swift` | 508 |
| `K/khepri/Features/ProgressCheck/NewProgressCheckSheet.swift` | 287 |
| `K/khepri/Features/ProgressCheck/ProgressCheckDraft.swift` | 170 |
| `K/khepri/Features/ProgressCheck/ProgressCheckScreen.swift` | 209 |
| `K/khepri/Features/ProgressCheck/ProgressCheckService.swift` | 113 |
| `K/khepri/Features/ProgressCheck/ProgressCheckStore.swift` | 85 |
| `K/khepri/Features/ProgressCheck/ProgressComparisonSection.swift` | 66 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
