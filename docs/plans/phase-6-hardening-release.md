# Phase 6: hardening and release

**Goal:** a release build that is at parity with iOS, passes Play review, and is in closed testing.

**Spec:** `docs/design.md` sections 12, 14, 15.

**Depends on:** Phases 3, 4 and 5 complete.

**Status:** task list.

## Tasks

1. **Parity sweep.** Walk `docs/parity-checklist.md` side by side with the iOS app; every row closed by a Maestro flow or a recorded manual check.
2. **Maestro parity suite.** One flow per iOS UI test file in `docs/ios-test-inventory.md`, against the seeded backend (`K/scripts/seed-*.sh`). Debug intent extras mirror the iOS launch arguments: reset, skip tour, AI consent, demo health.
3. **Translations.** Re-run the string conversion; zero missing keys; turn `UnusedResources` on for the generated `strings.xml` and delete or exempt what is unused; review layouts with pt-PT and es at font scale 2.0.
4. **Accessibility audit.** TalkBack through every tab; 48 dp targets; contrast; font scale 200% without clipping; reduce motion; landscape.
5. **State and lifecycle.** Rotation and process death on every form and sheet ("don't keep activities"); workout killed mid-rest; coach reply interrupted by backgrounding.
6. **Performance.** Baseline profile (startup, Today scroll, open coach); startup macrobenchmark recorded; R8 full-mode release build passes the Maestro smoke flow.
7. **Security and privacy.** No secrets in the APK; backup exclusions verified; Coil disk cache off for signed URLs; logs carry no tokens or health values.
8. **Contract pin.** `contract.sha` is not ahead of the SHA production runs.
9. **Play Console.** Data-safety form (email, name, user id, health, fitness, photos/videos, other user content: app functionality only, as the iOS privacy manifest declares); health-permission and foreground-service declarations approved; store listing and screenshots in four languages.
10. **Release pipeline.** CI uploads to the internal track after `main` is green; closed testing track; a manual gate for production.
11. **Documentation.** `CLAUDE.md` commands and traps updated; ADRs added for decisions made during the build.

## Done when

- The parity checklist has no open rows other than the documented differences in `docs/design.md` section 3.
- The device-only checks in `docs/design.md` section 14 pass on a Pixel and one other manufacturer.
- A closed-testing build is live on Play.
