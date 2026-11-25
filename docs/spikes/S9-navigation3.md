# S9: Navigation 3

- **Date:** 2026-11-25
- **Device / emulator, Android version:** emulator AVD `khepri37` (Pixel 8 profile, `system-images;android-37.0;google_apis_playstore;arm64-v8a`), Android 17 / API 37, headless on an M4 Pro.
- **Library versions used** (approved by the owner by hand on 2026-11-25, with no Sonatype token):

  | Library | Version |
  |---|---|
  | AGP | 9.5.2 (built-in Kotlin) |
  | Kotlin + Compose compiler plugin | 2.4.21 |
  | Compose BOM | 2026.11.01 (Material 3 1.5.0, Compose 1.13.0) |
  | `androidx.navigation3:navigation3-runtime` / `-ui` | 1.2.2 |
  | `androidx.lifecycle:lifecycle-viewmodel-navigation3` / `lifecycle-runtime-compose` | 2.11.1 |
  | `androidx.activity:activity-compose` | 1.13.0 |
  | kotlinx-serialization | 1.13.0 |

  The SDK settings were `compileSdk` 37.1 (`release(37) { minorApiLevel = 1 }`), `targetSdk` 37 and `minSdk` 28.

## Question

Five tabs with one back stack each; a bottom-sheet entry and a full-screen entry on a tab's stack; re-select pops to root; process death restores the selected tab and each stack; a destination opened from outside builds a stack with parents (More → Goals → goal). **Pass means:** all of the above with the current stable Navigation 3 release.

## What was tried

Spike app: `spikes/nav3` (module `:nav3` in the `spikes/` Gradle build). Driver: `spikes/nav3/s9-check.py` (adb + uiautomator; it prints PASS/FAIL per check).

The shape that passed:

- **Route keys.** They are `@Serializable` and implement `NavKey`. A sealed `Route` holds `TabRoot(tab)`, `Detail`, `Goals`, `Goal(id)`, `SettingsSheet`, `Workout` and `Thread(id)`.
- **One `NavBackStack` per tab.** Each comes from `rememberNavBackStack(TabRoot(tab))`, which is saveable, so it survives process death. The selected tab is held in `rememberSaveable`.
- **One `NavDisplay` per tab.** Only the selected tab's display is composed. It is wrapped in `key(selected) { saveableStateHolder.SaveableStateProvider(tab.name) { NavDisplay(...) } }`.
  - `key()` gives each tab its own composition group.
  - The outer `SaveableStateHolder` keeps the tab's `rememberSaveable` state while the tab is not shown.
  - Inside it, `rememberSaveableStateHolderNavEntryDecorator()` keeps the state of each entry.
- **Entry ViewModels.**
  - Use `rememberViewModelStoreNavEntryDecorator(provider)` with **one `ViewModelStoreProvider` per tab**.
  - Each provider is held in an activity-scoped ViewModel (`TabStores`), so a ViewModel lives until its entry is popped. It survives tab switches and rotation.
  - **Trap:** the default `rememberViewModelStoreNavEntryDecorator()` remembers its provider in composition and releases it on dispose. That cleared every entry's ViewModels on every tab switch (seen as `vm#4 -> vm#7`), and `key()` alone did not fix it. The decorator's `onPop` calls `provider.clearKey(contentKey)`, so popped entries are still cleared.
- **Sheets.**
  - Navigation 3 1.2.2 ships only `DialogSceneStrategy`, so `BottomSheetSceneStrategy` is ours: about 30 lines, an `OverlayScene` wrapping `ModalBottomSheet`.
  - An entry opts in with `metadata = BottomSheetSceneStrategy.bottomSheet()`.
  - Back and swipe-dismiss both pop the entry.
- **Full-screen.** The top route decides whether the bottom bar shows (`Route.hidesTabBar()`: `Workout`, `Thread`).
- **Back.**
  - `NavDisplay.onBack` pops when the stack has more than one entry, and otherwise exits.
  - A `BackHandler(enabled = stack.size == 1 && selected != Today)` placed before it returns to Today.
  - The result is the design 8.2 order: pop, then Today, then exit.
- **Re-select.** Tapping the selected tab pops its stack to the root.
- **Links.**
  - `parseLink` returns `(tab, routes with parents)`. The stack is replaced (`clear()` + `addAll`) and the tab is selected.
  - A cold start reads `intent.data` only when there is no saved state and the intent is not `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`. A warm start arrives through `onNewIntent` (`launchMode="singleTask"`).
  - Links are commands, so the same link twice lands twice.

## Result

**Pass.** 32/32 checks green on the emulator, on both the debug build and the R8-minified release build (`NavKey` state is restored through serialization, so R8 had to be proven):

- five tabs, with separate stacks kept across switches;
- back pops within a tab;
- per-entry saveable state is kept;
- an entry's ViewModel survives a tab switch, and a popped entry gets a fresh one;
- re-select pops to root;
- the sheet shows over More root, and back dismisses it;
- a full-screen entry hides the tab bar, and the bar comes back after leaving it;
- a sheet over a full-screen entry keeps the tab bar hidden (the bar follows the top entry that is not a sheet);
- back from a tab root returns to Today, and back at Today root leaves the app;
- rotation keeps the stack and the ViewModel;
- **process death** (`am kill` after HOME, `pidof` empty) restores the selected tab, both stacks and the saveable entry state;
- a link that arrives after process death lands, and the other tabs keep their restored stacks;
- a cold `khepri://goals/g42` builds More → Goals → Goal g42, and back walks up the parents and then to Today;
- a warm `khepri://coach/t9` lands on the thread with the tab bar hidden;
- the same link twice still lands.

Rerun with:

```sh
./gradlew -p spikes :nav3:assembleDebug
adb install -r spikes/nav3/build/outputs/apk/debug/nav3-debug.apk
python3 -I spikes/nav3/s9-check.py
```

For the release build, run `:nav3:assembleRelease` and install `nav3-release.apk` (signed with the debug key). The spikes need `org.gradle.jvmargs=-Xmx4g` in `spikes/gradle.properties`, or R8 runs the daemon out of Metaspace.

## Decision

Navigation 3 1.2.2 stays. The Navigation Compose fallback is not needed. `:core:navigation` copies the spike's shape:

- one saveable back stack and one `ViewModelStoreProvider` per tab;
- one `NavDisplay` per tab under `key(tab)` and a `SaveableStateHolder`;
- our own `BottomSheetSceneStrategy`;
- `Navigator.open` replaces the target tab's stack with the parents in place and selects the tab.

## Follow-ups

- **Phase 1 version catalog:**
  - `compileSdk` must be **37.1** (`minorApiLevel = 1`), because Compose 1.13 refuses to build against 37.0 (`checkDebugAarMetadata`).
  - Install `platforms;android-37.1`.
  - Add `google()` to both repository blocks.
- **`Navigator.open` cleanup.** When it replaces a stack that is not composed (another tab), the replaced entries never get `onPop`. Their ViewModels stay in that tab's provider until the activity dies. `Navigator` must call `provider.clearKey(entry.contentKey)` for each replaced key. That is a small leak, not a correctness bug, and it was not exercised by the spike.
- **Link replay after force-stop.** A force-stop followed by a launcher start re-delivers the task's original VIEW intent with no saved state, so the old link is applied again. This is seen only after force-stop, never after process death. The `LAUNCHED_FROM_HISTORY` guard covers recents. Phase 1 should consume the link in `Navigator` and clear `intent.data` (`setIntent`). The force-stop case is accepted.
- **Not proven yet (Phase 1):** swipe-dismiss of a sheet, a sheet restored after process death, predictive back, a minSdk 28 device, and what a push link does while a workout is active.
- **design.md 8.2** now records the per-tab shape (done in this change).
- The spike checks rely on uiautomator text. Phase 1 replaces them with Compose UI tests plus one instrumented process-death test (`ActivityScenario.recreate` cannot kill a process; use `am kill` from a test-orchestrator script, or `StateRestorationTester` for the saveable part).
