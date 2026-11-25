# Backend and config prerequisites

> Extracted from `docs/design.md` section 5. If the two disagree, fix both.


A separate track with its own PRs. The Android app cannot be finished without them. Each row lists where the change lands and how to tell it is done.

### B1. FCM push (`north-client`)

- Add `internal/fcm` modelled on `internal/apns` (`apns.go`, `sender.go`, `service.go`, `api.go`).
- Device table and routes `PUT /devices/fcm` and `DELETE /devices/fcm/{token}`, added to `docs/api/openapi.yaml` (a backend test fails if a mounted route is missing from the spec: `cmd/api/api_spec_test.go`).
- Implement the existing `pusher.Send(ctx, userID, title, body, href, category)` interface (`internal/nudges/service.go`) and `SendThread` (`internal/crews/push.go`, `internal/moderation/moderation.go`). Wire in `cmd/api/routes.go` and `cmd/worker/main.go` next to APNs so a user with both device kinds gets both.
- Payload is **data-only**: `title`, `body`, `href`, `category`, `thread`. The app builds the notification, so action buttons work when the app is killed.
- Categories to carry unchanged: `CHECKIN`, `COACH_LIGHTER`, `COACH_MISSED`, `COACH_FIRST_WEEK` (`internal/nudges/nudges.go`), `CREW_MESSAGE` (`internal/crews/push.go`).
- Mark tokens failed when FCM reports them unregistered, as the APNs sender does.
- Answer 503 when FCM is not configured, as `/devices/apns` does.
- Done when: a stub-server test sends each category, and a real device receives a check-in nudge with buttons.

### B2. Sign in with Apple web flow (`north-client`)

- Today `internal/auth/apple.go` accepts only native iOS identity tokens (audience in `APPLE_BUNDLE_ID`, nonce check).
- Add `/auth/apple/start` and `/auth/apple/code`, copying the existing `/auth/google/start` + `/auth/google/code` pair. Accept the Apple Services ID as an audience (`APPLE_SERVICE_ID`, listed as "later" in `north-client/docs/IOS.md`).
- The app opens the start URL in a Custom Tab, Apple redirects to the backend, the backend redirects to `khepri://auth/apple?code=…`, the app exchanges the one-time code for `AuthResponse`.
- Account linking by email follows the Google web flow.
- Done when: an account created with Apple on iOS (relay email, no password) signs in on Android.

### B3. Health source for Android (`north-client`)

- `internal/health/api.go` hard-codes source `apple_health` on `/health/samples`. Add a source field (or path) accepting `health_connect`.
- `internal/awards/db/queries.sql` counts only `apple_health`; include `health_connect` so Android data earns the same awards.
- The activity-session source constraint is already relaxed (`migrations/20260815170000_relax_activity_session_source.sql`).
- Done when: an Android sync stores samples under `health_connect` and re-sending a day replaces it (no duplicates), as iOS relies on.

### B4. Check-in source (`north-client`)

- `internal/checkins/api.go` records any unknown `source` as `ios`. Add `android` to the allowed values (`internal/checkins/checkin/checkin.go`).
- Verified on `origin/main`: the response field is an open string ("clients show values they do not know generically"), so this does not break shipped iOS builds.

### B5. Digital asset links (`north-web-app`)

- Add `/.well-known/assetlinks.json` beside the AASA route (`src/routes/(public)/.well-known/apple-app-site-association/+server.ts`, built in `src/lib/features/pwa/aasa.ts`).
- Relations: `delegate_permission/common.get_login_creds` (passkeys) and `delegate_permission/common.handle_all_urls` (app links).
- Production lists only the **Play App Signing** certificate SHA-256, not the upload key and not the debug key.
- Today's universal links cover only `/i/*`. Android app links cover `/i/*` and `/app/*`.

### B6. Cluster config (`platform/infra`; merging deploys, confirm each change first)

- `WEBAUTHN_RP_ORIGINS=https://kheprios.com,android:apk-key-hash:<base64url SHA-256 of the Play signing cert>`. Setting it replaces the default, so `https://kheprios.com` must be listed.
- Add the Google **web** client id to `GOOGLE_IOS_CLIENT_ID` (a comma-separated allow-list). Android ID tokens carry the `serverClientId` as audience.
- Firebase service-account key as a sealed secret; Apple Services ID and key.
- Read `platform/infra/secrets/*/README.md` before sealing.

### Owner-only actions

Play Console app with Play App Signing, Firebase project, Google OAuth Android client (package name + SHA-1), Apple Services ID, a new Sonatype token (the dependency checker in the planning session failed with an expired token).


