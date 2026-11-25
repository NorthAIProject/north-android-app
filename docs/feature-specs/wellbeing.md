# Feature spec: wellbeing

> Modules `:feature:wellbeing:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Care:** water presets with undo, last night's sleep sheet, habits and reminders (add sheets with weekday picker), This Week chart.
- **Mind:** journal with mood trend and a Write sheet.
- **Knowledge:** searchable library, Write a Note, Add a File, Re-read Everything, document text view with heading jumps.
- **API:** care (10), journal get/write, knowledge (8, one multipart).


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Care/CareScreen.swift` | 378 |
| `K/khepri/Features/Knowledge/KnowledgeDocumentView.swift` | 184 |
| `K/khepri/Features/Knowledge/KnowledgeScreen.swift` | 274 |
| `K/khepri/Features/Knowledge/KnowledgeService.swift` | 67 |
| `K/khepri/Features/Mind/JournalScreen.swift` | 138 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
