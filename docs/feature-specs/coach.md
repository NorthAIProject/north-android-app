# Feature spec: coach

> Modules `:feature:coach:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Conversation list:** normal or reflection threads, swipe to delete, empty state "Start a Conversation", custom header with the animated ring.
- **Thread** (`ConversationStore`): streamed reply; inline exercise cards (open the exercise sheet); tool approval card (approve or deny, 300 s call); thumbs up/down; attachments (photo, PDF, docx, xlsx, csv, txt, json, up to 8 MB); dictation; stop button; interrupted replies recovered; a reflection thread can end.
- **Quick coach:** hold the orb, speak, a rising card shows the reply, read aloud sentence by sentence with other audio ducked (`ReplySpeaker.swift`), approvals, "Open in chat". Mute setting.
- **Memories:** keep or discard proposals; pinned / normal / excluded; edit; forget; add.
- **Decisions:** list, new decision sheet, outcome "Did it hold?", deep link to one decision, calibration.
- **API:** conversations list/start/get/delete, reply and resume (SSE), attachments, `decideToolCalls`, `rateMessage`, `getExercise`, `searchExercises`, memories (8), decisions (6).
- **Cache:** conversation list and the last page of each opened thread.
- **Tests to port:** SSE decoding, reply recovery.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Coach/ChatAttachmentViews.swift` | 99 |
| `K/khepri/Features/Coach/CoachActivity.swift` | 98 |
| `K/khepri/Features/Coach/CoachHeader.swift` | 300 |
| `K/khepri/Features/Coach/CoachScreen.swift` | 152 |
| `K/khepri/Features/Coach/CoachService.swift` | 173 |
| `K/khepri/Features/Coach/ConversationStore.swift` | 394 |
| `K/khepri/Features/Coach/ConversationView.swift` | 326 |
| `K/khepri/Features/Coach/ExerciseViews.swift` | 179 |
| `K/khepri/Features/Coach/ReplyRecovery.swift` | 54 |
| `K/khepri/Features/Decisions/DecisionsScreen.swift` | 217 |
| `K/khepri/Features/Memories/MemoriesScreen.swift` | 251 |
| `K/khepri/Features/QuickCoach/CoachOrb.swift` | 105 |
| `K/khepri/Features/QuickCoach/QuickCoachOverlay.swift` | 291 |
| `K/khepri/Features/QuickCoach/QuickCoachSession.swift` | 246 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
