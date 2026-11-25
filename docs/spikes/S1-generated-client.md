# S1: Generated client against the real spec

- **Date:** 2026-11-25
- **Device / emulator, Android version:** none (JVM-only module `spikes/client`)
- **Library versions used:**
  - Gradle 9.9.2, JDK Temurin 21.0.13.1, Kotlin 2.4.21, openapi-generator (Gradle plugin `org.openapi.generator`) 7.27.0
  - kotlinx-serialization-json 1.13.0, kotlinx-coroutines 1.11.0, Retrofit 3.0.0 with its `converter-kotlinx-serialization` 3.0.0, OkHttp 5.5.1
  - JUnit Jupiter 6.1.3, networknt json-schema-validator 3.0.9 (pulls Jackson 3 `tools.jackson` transitively), jackson-dataformat-yaml 2.23.0
- **Dependency vetting:** there is no Sonatype token, so the owner approved the list above by hand on 2026-11-25 in the Claude Code session that ran this spike.

## Question

Does OpenAPI Generator (Kotlin, Retrofit, kotlinx.serialization, coroutines) produce code from the spec at the S2 pin that compiles and behaves? It has to compile, make nullable fields nullable, give every enum an unknown member, decode the goldens and pass both request checks.

## What was tried

Everything is in `spikes/client/build.gradle.kts` and `spikes/client/src/`.

1. **Contract copy.** `spikes/contract/` holds `openapi.yaml` and all 166 `internal/*/testdata/*.golden.json` files, both taken at `6823e58c`.
   - Golden names follow the iOS sync script. `internal/profiles/testdata/*` gets a `profiles-` prefix, because `profile.golden.json` exists in both `settings` and `profiles`.
   - `golden-map.txt` maps golden names to schema names. It has 151 entries, extracted from `K/NorthKit/Tests/NorthAPITests/ContractTests.swift`.
2. **Normalisation** (`normalizeSpec` task, run before generation). It changes the following:
   - **YAML 1.1 trap:** a bare `on` is read as boolean `true`. The spec uses `on` as a property name in `FitnessHub` and `InsightsLifting`, which made the generator reject the spec ("required is not of type string"). The task quotes `on` keys and `on` items in `required: [...]` before parsing. Phase 1 must keep this step.
   - It removes 12 operations: the event streams `replyInConversation`, `resumeConversation` and `streamCrewLive`, and every operation with a `multipart/form-data` body (exactly 9). The task log lists them.
   - It rewrites `nullable: true` + `allOf: [$ref]` (3 sites: `CrewMessage.author`, `CrewMessage.replyTo`, `MealPlanIntakeState.intake`) into `anyOf: [$ref, {type: null}]`.
   - It turns free-form objects (`type: object`, `additionalProperties: true`, no properties; 3 sites) into plain `type: object`, so they generate as `JsonObject`.
3. **Generator options:**
   - `generatorName = kotlin` and `library = jvm-retrofit2`.
   - `serializationLibrary = kotlinx_serialization`, `useCoroutines`, `dateLibrary = java8`.
   - `enumUnknownDefaultCase = true` and `enumPropertyNaming = UPPERCASE`.
   - `modelNameSuffix = Dto`.
   - Packages: `com.fernandocorreia.khepri.core.api{,.endpoint,.model}`.
   - `typeMappings`:
     - `DateTime → java.time.Instant`
     - `AnyType → JsonElement`, `object → JsonObject` (with `importMappings`)
     - `number → kotlin.Double` (iOS uses Double; `BigDecimal` cannot be a `Map` value under kotlinx)
     - `URI → kotlin.String`
   - `enumNameMappings`: `"" → EMPTY` (the `DecisionInput.held` enum has an empty value) and `unknown_default_open_api → UNKNOWN`.
   - `.openapi-generator-ignore` drops `infrastructure/ApiClient.kt` and `auth/**`. The OkHttp and Retrofit setup is hand-written (design 7.2). The generated `ApiClient` needs the logging and scalars converters, and it imports `HttpBearerAuth` twice.
   - No template overrides were needed.
4. **Hand-written code:**
   - `FlexibleInstantSerializer` reads RFC 3339 through `ISO_OFFSET_DATE_TIME` and writes UTC with milliseconds.
   - `ApiJson.strict`: `ignoreUnknownKeys = false`, `explicitNulls = false`, `encodeDefaults = false`, with contextual `Instant`, `UUID` and `LocalDate`.
   - `ApiJson.tolerant`: the same, but with unknown keys ignored, for production decoding.
5. **Tests:** `spikes/client/src/test/.../S1ContractTest.kt`, run with `./gradlew -p spikes :client:test`.

## Result

**Pass.** 159 of 159 tests pass.

- **Builds:** 575 models, 0 compiler errors.
- **Nullability:** all 6 nullable sites are nullable Kotlin properties: the 3 above, plus `TargetWeight.targetWeightKg`, `HelpfulRequest.helpful` and `ReportHelpfulRequest.helpful`.
- **Enums:**
  - Unknown values decode to `UNKNOWN`, both for nested enums (`ExecutionDto.Outcome`) and top-level ones (`MealPlanTypeDto`).
  - Each enum gets a generated custom `KSerializer` that falls back to `UNKNOWN`. This works with the kotlinx library option.
- **Instants:** decode correctly with `Z`, with nanosecond fractions and with an offset. Encoding writes `.SSSZ`.
- **Goldens:** all 151 mapped goldens decode with strict JSON. A golden with an extra key fails strict decoding and passes tolerant decoding.
- **Requests:**
  - `LiftSetInput` (`additionalProperties: false`) and `CheckInRequest` (open) serialise only declared keys, with no nulls, and validate against their 2020-12 schema.
  - An undeclared key fails the key check.
  - A missing required field fails the schema validator. This proves the validator actually catches errors.

**Interface grouping:** tagged operations go to one interface per tag:

| Interface | Operations |
|---|---|
| `AwardsApi` | 4 |
| `CrewsApi` | 27 |
| `DailyApi` | 3 |
| `DashboardApi` | 2 |
| `InboxApi` | 4 |
| `ModerationApi` | 5 |
| `NewsApi` | 4 |
| `PushApi` | 3 |
| `RecapApi` | 2 |
| `SettingsApi` | 27 |
| `SocialApi` | 34 |
| `TrainingApi` | 7 |
| `WeeklyApi` | 2 |

The 254 untagged operations are in `DefaultApi`, and that includes `getMe` and `logOut`. All functions are `suspend` and return `Response<T>`.

## Decision

Phase 1 Task 4 uses this generator configuration and normalisation as they are. Hand-written DTOs are not needed.

Rules for the code that wraps the generated client:

- Never use the generated `infrastructure.Serializer.kotlinxSerializationJson`. It sets `encodeDefaults = true` and `ignoreUnknownKeys = true`.
- Never send `UNKNOWN` back to the server. Its wire value is `unknown_default_open_api`, and the server rejects it. For example, `DecisionInputDto.Held` is a request enum.
- `DefaultApi` is the only interface large enough to matter. The `:data` modules call it through `:core:api`, and per-feature facades are optional.

## Follow-ups

- **Phase 1 Task 4:**
  - Port the `normalizeSpec` task, including the `on` quoting, the type and enum mappings, and the ignore file.
  - Extend `golden-map.txt` to cover the 15 goldens iOS does not decode: `fitness-activities`, `fitness-hub`, `insights-cardio-page`, `insights-patterns-page`, `insights-sleep-page`, `insights-sleep-page-empty`, `lift_import_partial`, `media-link`, `medications`, `nutrition-log-meal-options`, `overview`, `overview-version`, `strava-connect-result`, `tools`, `tools.readonly`.
- `docs/design.md` 7.1: record the `on` trap, the `number → Double` and `URI → String` mappings, and the `UNKNOWN` wire-value rule.
- `explicitNulls = false` drops nulls when encoding. If the server ever needs an explicit `null` to clear a field, that request needs its own encoder setting. Check this when porting editing flows (for example the `held` field, "Empty or absent clears it").
