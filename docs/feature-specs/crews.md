# Feature spec: crews

> Modules `:feature:crews:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Crews list:** New Crew, join links.
- **Crew space:** Chat / Challenge switch. Chat: channel switcher, messages, replies, reactions, edits, pins, @mentions, report sheet, pins sheet, mute sheet. Rules sheet must be agreed before the first post (`terms_required`). Challenge board: challenge editor, member rows, remove or block, leave crew.
- **Live stream** with reconnect; events only trigger a refetch.
- **Sends** carry a `clientId`; pending messages show retry or discard. A reply from a notification derives its id from the notification id so retries are stored once.
- **Unread** badges on the More tab and the Crews row.
- **API:** crews list/create/join/get, member remove, challenge set/delete, moderation terms get/accept, unread, space, notifications, channel messages list/send, pins, read, message get/edit/delete, reactions, pin, report, live (SSE).
- **Cache:** channel messages.
- **Tests to port:** crew chat store, live stream, crew notifications.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Crews/Chat/ChannelView.swift` | 300 |
| `K/khepri/Features/Crews/Chat/CrewChatService.swift` | 137 |
| `K/khepri/Features/Crews/Chat/CrewChatStore.swift` | 273 |
| `K/khepri/Features/Crews/Chat/CrewLiveStream.swift` | 96 |
| `K/khepri/Features/Crews/Chat/CrewMessageActions.swift` | 116 |
| `K/khepri/Features/Crews/Chat/CrewMessageRow.swift` | 269 |
| `K/khepri/Features/Crews/Chat/CrewNotifySheet.swift` | 109 |
| `K/khepri/Features/Crews/Chat/CrewPinsSheet.swift` | 111 |
| `K/khepri/Features/Crews/Chat/CrewReportSheet.swift` | 60 |
| `K/khepri/Features/Crews/Chat/CrewRulesSheet.swift` | 33 |
| `K/khepri/Features/Crews/Chat/CrewSpaceScreen.swift` | 280 |
| `K/khepri/Features/Crews/Chat/CrewUnread.swift` | 62 |
| `K/khepri/Features/Crews/CrewsScreen.swift` | 298 |
| `K/khepri/Features/Crews/CrewsService.swift` | 73 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
