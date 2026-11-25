# Feature spec: checkins

> Modules `:feature:checkins:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Check-ins:** today's form (mood and energy 1–5, optional stress and sleep, tags, goal link), celebration, the coach's reply bubble; past days with swipe edit/delete; week dots. Mood can be preset from a notification or link.
- **Quick check-in** (widget or notification): refuses to save if a check-in already exists today, so it never overwrites notes written in the app.
- **Capture:** parse a sentence, preview what was read, commit. Used by Quick Add, the share target and shortcuts.
- **Inbox:** items from the share sheet or typed; swipe File as Suggested, or file to Journal, Knowledge or a goal note; Dismiss.
- **API:** check-ins list/save today/update/delete, reply to today's check-in, capture parse/commit, inbox list/add/file/dismiss.
- Send `source: android` (B4).
- **Tests to port:** check-in notification flow.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Capture/CaptureComposer.swift` | 114 |
| `K/khepri/Features/Capture/CaptureDraft.swift` | 100 |
| `K/khepri/Features/Capture/CaptureService.swift` | 39 |
| `K/khepri/Features/CheckIns/CheckInCelebration.swift` | 142 |
| `K/khepri/Features/CheckIns/CheckInPresentation.swift` | 137 |
| `K/khepri/Features/CheckIns/CheckInsScreen.swift` | 424 |
| `K/khepri/Features/CheckIns/CheckInsService.swift` | 45 |
| `K/khepri/Features/Inbox/InboxScreen.swift` | 136 |
| `K/khepri/Features/Inbox/InboxService.swift` | 76 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
