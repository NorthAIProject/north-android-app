# S2: Which spec is live

- **Date:** 2026-11-25
- **Device / emulator, Android version:** none (reading only)
- **Library versions used:** none

## Question

Which `north-client` commit is in production, and which `origin/main` operations does production not serve yet?

## What was tried

1. `git -C ../../../platform/infra fetch && git show origin/main:apps/khepri/web/values-production.yaml`: the tag is `6823e58c7f673060a21683ab3f5de50a98ebcbd1`, the same for `worker`. That commit was promoted by infra #716 on 2026-11-19. The local infra checkout was 63 commits behind and showed `5b6e9138`. Always read `origin/main`.
2. Listed the `operationId`s in `docs/api/openapi.yaml` at `6823e58c` and at north-client `origin/main` (`45b98b9e`, 2026-11-24). There are 390 at each, and the sets are identical. The spec file is unchanged between the two commits.
3. Unauthenticated `GET https://kheprios.com/api/v1/horizon` returns 401, and `/api/v1/ai-consent` returns 401. Both are live.

## Result

Pass. The pin is `6823e58c7f673060a21683ab3f5de50a98ebcbd1`. No operations exist only on `origin/main`.

Spec profile at the pin:

- OpenAPI 3.1.0, 17.5k lines, 477 schemas, 390 operations.
- 15 `allOf` sites and no `oneOf`, `anyOf` or discriminators.
- 3 `nullable: true` sites and 3 `type: [x, "null"]` sites.
- 59 enums and 21 `additionalProperties: false` schemas.
- 9 multipart operations, 111 `date-time` fields and 47 `date` fields.

Tags are written flow-style (`tags: [settings]`) on 127 operations. The other ~263 are untagged, so the generator groups them into one default interface. S1 records the exact grouping.

`text/event-stream` appears in two places. One is `streamCrewLive`. The other is the shared `ReplyStream` response used by `replyInConversation` and `resumeConversation`. `parseMealFoods` is plain JSON, despite a misleading grep hit.

## Decision

`core/api/contract.sha` = `6823e58c7f673060a21683ab3f5de50a98ebcbd1`. S1 generates from the spec at this commit.

## Follow-ups

- `docs/plans/phase-0-spikes.md` S2 step 1 now reads infra `origin/main` (done).
- Re-check the pin before Phase 1 Task 4 commits the spec; production may have moved.
