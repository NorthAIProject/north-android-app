# Feature spec: onboarding

> Modules `:feature:onboarding:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Wizard, six steps** (`WizardModel.swift`): welcome (3D mascot), focus areas (fitness, health, work, learning, personal), coaching style (direct, supportive, socratic, or custom text), first goal, Health permission primer, notifications primer. Draft saved per user; resumes after quit. Sign Out available.
- **Tour:** three steps (Today, Coach, Training) as a spotlight cut-out drawn with Canvas. Completion per device; Settings → "Show Me Around" restarts it.
- **API:** `completeOnboarding`.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Onboarding/Wizard/Mascot3D/Mascot3DView.swift` | 194 |
| `K/khepri/Features/Onboarding/Wizard/Mascot3D/MascotMotion.swift` | 150 |
| `K/khepri/Features/Onboarding/Wizard/WizardModel.swift` | 259 |
| `K/khepri/Features/Onboarding/Wizard/WizardSteps.swift` | 295 |
| `K/khepri/Features/Onboarding/Wizard/WizardView.swift` | 140 |
| `K/khepri/Features/Tour/GuidedTour.swift` | 92 |
| `K/khepri/Features/Tour/TourSpotlight.swift` | 105 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
