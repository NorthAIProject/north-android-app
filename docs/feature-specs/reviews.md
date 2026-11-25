# Feature spec: reviews

> Modules `:feature:reviews:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Weekly review (sheet):** The Week, Priorities, Goals in Order, Training.
- **Recap (sheet):** 7 or 30 days, totals against the previous period, stacked day bars, share card image, "post to friends".
- **Reports:** weekly reports and morning briefings rendered from markdown blocks; rate helpful; archive.
- **API:** weekly review/focus, recap get/share, reports list/get/archive/rate.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Recap/PeriodRecapService.swift` | 135 |
| `K/khepri/Features/Recap/RecapBars.swift` | 126 |
| `K/khepri/Features/Recap/RecapScreen.swift` | 228 |
| `K/khepri/Features/Recap/RecapShareCard.swift` | 97 |
| `K/khepri/Features/Recap/YourWeekCard.swift` | 54 |
| `K/khepri/Features/Reports/ReportsScreen.swift` | 354 |
| `K/khepri/Features/Weekly/WeeklyReviewFlow.swift` | 222 |
| `K/khepri/Features/Weekly/WeeklyService.swift` | 91 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
