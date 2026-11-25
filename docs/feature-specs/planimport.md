# Feature spec: planimport

> Modules `:feature:planimport:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- Import a training plan or a meal plan from a photo, camera, or file: parse (300 s, multipart), review the draft, commit. Meal plans can also be built with AI from a form. `import_partial` is a refusal code to surface.
- **API:** `parseWorkoutImport`, `commitWorkoutImport`, `parseMealImport`, preview, import, meal intake, `generateMealPlan`.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/PlanImport/CameraCapture.swift` | 36 |
| `K/khepri/Features/PlanImport/EditableMealDraft.swift` | 171 |
| `K/khepri/Features/PlanImport/EditableWorkoutDraft.swift` | 71 |
| `K/khepri/Features/PlanImport/Error+ImportMessage.swift` | 13 |
| `K/khepri/Features/PlanImport/ImportFile.swift` | 81 |
| `K/khepri/Features/PlanImport/ImportLabeledField.swift` | 16 |
| `K/khepri/Features/PlanImport/ImportProgressOverlay.swift` | 21 |
| `K/khepri/Features/PlanImport/ImportSourcePicker.swift` | 108 |
| `K/khepri/Features/PlanImport/MealGenerateForm.swift` | 119 |
| `K/khepri/Features/PlanImport/MealImportDaySection.swift` | 82 |
| `K/khepri/Features/PlanImport/MealImportDayTotals.swift` | 24 |
| `K/khepri/Features/PlanImport/MealImportFoodRow.swift` | 85 |
| `K/khepri/Features/PlanImport/MealImportReview.swift` | 37 |
| `K/khepri/Features/PlanImport/MealImportSettingsSection.swift` | 48 |
| `K/khepri/Features/PlanImport/MealImportSheet.swift` | 181 |
| `K/khepri/Features/PlanImport/PlanImportService.swift` | 69 |
| `K/khepri/Features/PlanImport/PlanImportServicing.swift` | 42 |
| `K/khepri/Features/PlanImport/WorkoutImportDaySection.swift` | 42 |
| `K/khepri/Features/PlanImport/WorkoutImportExerciseRow.swift` | 36 |
| `K/khepri/Features/PlanImport/WorkoutImportReview.swift` | 45 |
| `K/khepri/Features/PlanImport/WorkoutImportSheet.swift` | 98 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
