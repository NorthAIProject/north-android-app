# Phase 5: 3D and brand moments

**Goal:** the 3D emblem, mascot and body map, plus the remaining polish passes.

**Spec:** `docs/platform-integrations.md` (3D), ADR 0005, `docs/spikes/S3-*.md`.

**Depends on:** spike S3 passed. Can start any time after that; until it lands, flat images stand in.

**Status:** task list. Expand to a task-level plan from the S3 result.

## Tasks

1. `:core:scene3d` wrapper: Filament engine lifecycle tied to the composable, gltfio loader, transparent surface inside Compose, release on dispose. Assets copied from `north-web-app/static/assets/models/` by a sync script with a check that they match (as iOS CI checks its motion data against the web app).
2. Motion evaluators in pure Kotlin for `emblem-motion.json` and `mascot-motion.json`; tests against `emblem-motion.fixtures.json` and `mascot-motion.fixtures.json`. Read the iOS emblem and mascot motion tests.
3. Emblem view on the login screen; mascot view on the onboarding welcome step.
4. Body map view: per-muscle colour from the training-heat or soreness ramp (15 muscle keys), shell morph weights (`waist`, `hips`, `chest`, `overall`, `core`) from `GET /body/map` and `/body/current`, shell drawn twice (opaque core, rim outline with a custom material compiled at build time; read `K/khepri/Features/Day/BodyShell.metal`).
5. Tap picking → `Muscle(id)` destination. Accessibility: one element with a custom action per muscle.
6. Use the body map in My Day's body card, the Muscles screen and Horizon (with the slider scrubbing shape).
7. Fallback to flat images under reduce motion, power saving, or load failure. Screenshot tests cover the fallback; the 3D path is checked on devices.
8. Performance: frame rate on a mid-range phone; pause rendering when off screen; APK size per ABI recorded.
9. Haptics pass: check all 16 iOS call sites have their Android counterpart.
10. Motion pass: every non-informative animation respects reduce motion; numeric text transitions; coach ring states.

## Done when

The three 3D surfaces match iOS side by side on a device, the fallbacks work, and the Phase 5 rows in `docs/parity-checklist.md` are ticked.
