# Code quality

> Extracted from `docs/design.md` section 12. If the two disagree, fix both.


One tool per job. Everything gates CI (iOS runs SwiftLint only locally; do not repeat that).

| Job | Tool | Policy |
|---|---|---|
| Formatting | Spotless + ktfmt | Width 120, indent 4 (same as `K/.swiftformat`); `spotlessCheck` in CI |
| Kotlin analysis | detekt (no formatting rule set) | Zero issues, no baseline, warnings as errors. Function length warn 100 / error 200 and complexity ignoring simple `when` mappings, as `K/.swiftlint.yml` does. No `!!`. Unused imports and privates are errors. |
| Compose | Compose rules plugin for detekt | Stability, modifier parameter first optional, state hoisting, naming, no ViewModel passed down, previews private |
| Android and resources | Android Lint | Warnings as errors, no baseline. `MissingTranslation`, `ContentDescription`, `UnusedResources` as errors (`UnusedResources` ignores the generated `strings.xml` until Phase 6) |
| Hard-coded UI text | detekt rule | Lint's `HardcodedText` is XML-only. A detekt rule fails on a string literal used as Compose text, content description or label |
| Module boundaries | Module-graph assertion plugin | Rules in 6.1 |
| Dependencies | Version catalog, dependency-analysis `buildHealth`, Renovate | No unused or misdeclared dependency. Each new dependency is vetted before it is added |
| Dead code | R8 usage report | Weekly report, not a gate |
| Translations | Lint + translation check script | Fails on any missing key in the four locales |
| Accessibility | Lint + accessibility checks in Compose UI tests | Touch target, contrast, label findings fail |
| Screenshots | Roborazzi (JVM) | Light/dark, font scale 1.0 and 2.0, en and pt-PT |
| Runtime | StrictMode in debug | Disk/network on main thread and leaked closables crash |
| Release | R8 full mode, baseline profile, startup macrobenchmark | Release-build smoke run in CI |
| Pre-commit | Repo hook via `core.hooksPath` | Spotless on staged files, detekt on changed modules |

Coding practices:

- Kotlin explicit API mode in `core` modules. Immutable UI state. No `GlobalScope`; dispatchers injected. `collectAsStateWithLifecycle` in screens.
- Composables are stateless below the screen level; screens take state and `onEvent`.
- Name by role, not type. Document public declarations whose behaviour is not obvious from the name; no other comments.
- Every screen has a preview and a screenshot test for loading, content, empty and failed states.
- Test tags follow one style (kebab-case, as most iOS identifiers: `coach-orb`, `rest-countdown`, `day-card-<kind>`).

Test pyramid:

- **JVM unit:** use cases, mappers, ViewModels with fakes (MockK only at platform edges), deep-link tables, ports of the iOS logic tests named in section 10.
- **Contract:** golden decoding, request schema validation.
- **Integration (JVM):** repositories with MockWebServer and in-memory Room; SSE reconnect; outbox backoff.
- **Compose UI (Robolectric):** per-screen states and semantics.
- **Maestro:** end to end against a locally seeded server, reusing `K/scripts/seed-*.sh`. iOS launch arguments to mirror as debug intent extras: reset, skip tour, AI consent, demo health.

CI gates (GitHub Actions): `spotlessCheck`, `detekt`, `lint`, module graph, `buildHealth`, unit and contract tests, screenshot verify, `syncContract --check`, release assemble. Maestro nightly and before release.

Repo hygiene: `CLAUDE.md` and `AGENTS.md` with the rules in section 2, an ADR folder, `.editorconfig`, a PR template with the checklist "backend deployed first? strings in four locales? screenshots updated?".


