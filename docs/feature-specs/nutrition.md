# Feature spec: nutrition

> Modules `:feature:nutrition:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- Segmented sections: eaten today against the goal, delete entry, Log Food sheet (ingredient by grams, or a meal from a plan); ingredients with New Ingredient sheet; meal plans and one plan (day targets, portions, alternatives, portion sheet, day target sheet, plan settings sheet); Speak Meal sheet (add a meal by voice, `POST /capture/foods`); Body & Goal (macro calculator, also linked from Settings and Fitness).
- **API:** ingredients search/create/delete, plan options, meal plans (15 operations), food log get/log/log meal/delete, calculator get/biometrics/plan.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Nutrition/BodyAndGoalScreen.swift` | 165 |
| `K/khepri/Features/Nutrition/MealPlanOptionViews.swift` | 120 |
| `K/khepri/Features/Nutrition/MealPlanSheets.swift` | 212 |
| `K/khepri/Features/Nutrition/MealPlanStore.swift` | 270 |
| `K/khepri/Features/Nutrition/MealPlansView.swift` | 212 |
| `K/khepri/Features/Nutrition/MealPlanView.swift` | 355 |
| `K/khepri/Features/Nutrition/NutritionScreen.swift` | 332 |
| `K/khepri/Features/Nutrition/NutritionService.swift` | 172 |
| `K/khepri/Features/Nutrition/SpeakMealSheet.swift` | 174 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
