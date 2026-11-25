# API client

> Extracted from `docs/design.md` section 7. If the two disagree, fix both.


### 7.1 Generation

- Source: `north-client/docs/api/openapi.yaml` (OpenAPI 3.1) at a **pinned git SHA** (`core/api/contract.sha`), read with `git show`, never from the working tree. The pin is the newest `north-client` commit that is deployed to production; it moves forward when the backend deploys. S1 runs against the same pin Phase 1 generates from.
- `syncContract` copies the spec, `docs/medals/medals.json` and every `internal/*/testdata/*.golden.json` at the pin into `core/api/contract/`, which is **committed** (as iOS commits its copies). Generation and contract tests read only the committed copies, so CI needs no access to `north-client`. `syncContract --check` needs the sibling repo: it runs locally before a PR and in the pre-release checklist; a CI job for it needs a read token for `north-client` (owner action).
- Generator: OpenAPI Generator, Kotlin, Retrofit, kotlinx.serialization, coroutines; run as a Gradle task in `:core:api`; output not committed; template overrides checked in.
- Spec profile (origin/main 6823e58): no `oneOf`/`anyOf`/discriminators, 59 enums, 21 schemas with `additionalProperties: false` (day, capture and medication inputs only), 9 multipart operations, 15 `allOf` sites, 3 `type: [x, "null"]` sites and 3 `nullable: true` sites. `nullable` has no meaning in 3.1, so normalisation turns both forms into the same nullable field.
- Every generated model gets the suffix `Dto` (the spec has schemas such as `Outcome` that would clash with domain names).
- Untagged operations (most of them, including `/me` and `/auth/*`) land in one default interface. S1 records the grouping; domain code never sees it, because `data` modules wrap generated calls.
- Type mapping: `uuid` → `String`; `date-time` → `Instant` with a serializer that reads RFC 3339 with or without fractional seconds (Go drops the fraction when zero and writes nanoseconds otherwise) and writes with; `date` → `LocalDate`.
- Enums: every generated enum gets an `UNKNOWN` member; a test asserts it.
- Nullable sites: a small spec normalisation step before generation; contract tests assert them.
- Field names are camelCase except `AIConsent.decided_at` and the capture/agent fields `ingredient_id`, `matched_name`, `check_in`, `amount_ml`.
- The passkey `publicKey` field is a free-form object; keep it as raw JSON.

### 7.2 Hand-written

- **SSE** in `:core:network` on OkHttp SSE, exposed as a cold `Flow`. No automatic retry on these routes.
  - `POST /conversations/{id}/reply` and `/resume`: events `token {text}`, `status {line}` (ignored), `exercises {slugs}`, `approval` (turn paused for a tool decision), `error {message}`, `done {messageId?}` (always last). Unknown event names are skipped. No keep-alive pings. A reply cut off by backgrounding is recovered through `ConversationDetail.awaitingResume` → `POST /resume` (`K/khepri/Features/Coach/ReplyRecovery.swift`).
  - `GET /crews/{id}/live`: `: ping` every 20 s; events `message`, `update`, `channels`, `removed`, `resync` with `{type, channelId, messageId, userId, seq}`. Events are signals; the client refetches. Nothing is replayed after reconnect. Reconnect backoff 1, 2, 5, 10 s; stop on 404 or a refusal (`K/khepri/Features/Crews/Chat/CrewLiveStream.swift`).
- **Multipart** (9 operations): hand-written Retrofit interfaces with streaming bodies from content URIs. File parts must carry a real content type. Limits: JSON bodies 1 MiB, uploads about 91 MiB, chat attachments 8 MB.

### 7.3 Client behaviour to match (`K/NorthKit/Sources/NorthAPI/NorthAPI.swift`)

Interceptor order, outermost first: `Accept-Language`, bearer, long-call timeout, retry. Error mapping is not an interceptor: `apiCall {}` maps every `Response` to `Outcome`, so it sees the same raw response the iOS error middleware sees.

One generated `OperationIndex` (method, path template, operationId, isPublic, isLong) drives the bearer and timeout interceptors; requests are matched by template, so `/auth/oauth/requests/{requestID}` works.

- **Bearer:** sent on every operation except those the spec marks `security: []`. Logout is the exception: it sends the token it revokes. Generate the public set from the spec.
- **401:** signs the user out only if a token was actually sent.
- **Unreadable token** (Keystore failure): the request is not sent and the user is not signed out (`Locked`).
- **`Accept-Language`:** the phone's top three languages with falling weights (`pt-PT, en;q=0.9, es;q=0.8`). Only matters before sign-in.
- **Retry:** GET and HEAD only, once, after 500 ms, when the connection was lost, timed out or offline. Never writes, never event streams.
- **Errors,** in this order, as iOS checks them: body `{"error":{"message","fields"?,"code"?}}`. 401 → Unauthorized; 404 → NotFound; 409 with the envelope → Conflict; any other status whose envelope has `fields` → FieldValidation (this includes the 429 quota refusal, which carries `fields`); 403 with `code` → Refused; anything else → Server(status, message, code). A non-2xx JSON body that is not the error envelope (a 409 carrying the newer plan) becomes `Conflict(newerVersionJson = body)`, which the repository decodes as the operation's documented type; that is the Android form of iOS passing it through.
- **Codes to handle** (sent by the server; not all are declared in the spec): `ai_consent_required` (403, `internal/shared/httpx/httpx.go`), `terms_required` and `suspended` (403, `internal/crews/chat_api.go`), `import_partial` (500 with a code, so `Server.code`), `invalid_reset_token`. Also statuses 402 (quota), 413, 422, 429 (`Retry-After: 60` on sign-in routes, `1` on signed-in routes), 503.
- **Unknown request fields are rejected by the server.** Only 21 schemas say `additionalProperties: false`, so schema validation alone would miss most. The test serialises a fully populated instance of every request type and fails on any key that is not a declared property of its schema, then also validates against the schema.
- **Clients:** two OkHttp clients sharing one pool: default (60 s read timeout) and streaming (no read timeout, used only by `SseClient`). Long calls stay on the default client; an interceptor raises the read timeout to 300 s with `chain.withReadTimeout` for operations whose `OperationIndex` entry is long.
- **300 s operations:** `createPlan`, `parseWorkoutImport`, `parseMealImport`, `generateMealPlan`, `estimateProgressCheck`, `compareProgressChecks`, `decideToolCalls`, `uploadChatAttachment`.
- **Long calls in the background:** coach replies, plan approval (1–3 minutes), attachments and crew replies must survive the app going to the background (iOS uses `BackgroundActivity.run`); use expedited work or a short foreground service.

### 7.4 Auth contract

- Opaque bearer token, no cookies, no JWT, no refresh endpoint. Every sign-in returns `AuthResponse {token, expiresAt, user}`. 30-day sliding expiry on the server.
- Store token, user id and expiry in DataStore, encrypted with a Keystore key behind a `TokenCipher` interface (a fake cipher in JVM tests; the real one is covered by an instrumented test).
- `GET /auth/options` returns `{google, passkey, passwordReset}`; `google` reflects the web flow, not native.
- `AI_CONSENT_REQUIRED=true` in production: until the account answers through `GET/PUT /ai-consent` (`unset`, `granted`, `declined`), AI calls return 403 `ai_consent_required`.
- `/capture/*` accepts the session token.

### 7.5 Contract tests

- Decode each golden fixture with its generated type using a strict `Json` (`ignoreUnknownKeys = false`), as `K/NorthKit/Tests/NorthAPITests/ContractTests.swift` does. A test fails if a golden file has no mapping entry.
- Interceptor behaviour is tested with MockWebServer.


