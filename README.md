# Khepri for Android

Native Android client for Khepri (North), with the same features and behaviour as the iOS app in `../north-ios-app/khepri`, on the same backend (`../north-client`).

**Status:** scaffold only. There is no Gradle project and no Kotlin yet. The module folders are empty placeholders; Phase 1 creates the build.

## Start here

| If you want to | Read |
|---|---|
| Understand the whole design | `docs/design.md` |
| Start work | `docs/plans/phase-0-spikes.md`, then `docs/plans/phase-1-foundation.md` |
| Build a feature | `docs/feature-specs/<module>.md` and `docs/design.md` section 6.3 |
| Change the backend for Android | `docs/backend-prerequisites.md` |
| Check what is left | `docs/parity-checklist.md` |
| Know why something was decided | `docs/adr/` |

## Documents

- `docs/design.md`: the approved design. Everything else is derived from it.
- `docs/api-client.md`, `docs/navigation.md`, `docs/platform-integrations.md`, `docs/quality.md`: sections of the design as standalone references.
- `docs/feature-specs/`: one file per feature module, with the iOS source files to read.
- `docs/ios-test-inventory.md`: iOS tests to port and core sources to read.
- `docs/plans/`: one plan per phase. Phases 0 and 1 are task-level; later phases are task lists until the spikes report.
- `docs/spikes/`: one result file per spike.
- `docs/adr/`: decisions.

## Layout

```
app/  build-logic/  baselineprofile/  maestro/  spikes/
core/<module>/
feature/<module>/{domain,data,ui}/
```

Stack: Kotlin, Jetpack Compose + Material 3, Clean Architecture with use cases, Navigation 3, Hilt, Retrofit + OkHttp, Room, Coil, Vico, WorkManager.
