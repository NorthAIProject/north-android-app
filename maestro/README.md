# UI flows

[Maestro](https://maestro.mobile.dev) flows run against an emulator and a local backend.
They are not part of `./gradlew check`.

## Smoke: sign in, tabs, a link, sign out

1. Start the backend at the pinned contract SHA (`core/api/contract.sha`) on port 8090, from its own worktree, not the working `north-client` checkout:

   ```sh
   git -C ../north-client worktree add --detach ../north-client-pinned "$(cat core/api/contract.sha)"
   (cd ../north-client-pinned && docker compose up -d postgres)
   ```

   Then run `./cmd/api` there with `DATABASE_URL=postgres://north:north@localhost:5434/north?sslmode=disable`.
   In development the backend refuses to start without `POSTHOG_API_KEY`. Give it a dummy key and point `POSTHOG_HOST` at a closed local port, so no events leave the machine.

2. Seed a throwaway account. The script generates the password into the file and never prints it:

   ```sh
   ../north-ios-app/khepri/scripts/seed-review-account.sh http://localhost:8090 <throwaway-email> <password-file>
   ```

   The model-backed steps answer 503 without AI keys. The smoke flow does not need them.

3. Install a build that points at `10.0.2.2:8090`. These builds declare `ACCESS_LOCAL_NETWORK`: from API 37 an app cannot reach a private address without it, and the connection times out silently. The flow grants it at launch; by hand, run `adb shell pm grant <app id> android.permission.ACCESS_LOCAL_NETWORK`. Builds: `debug` (`com.fernandocorreia.khepri.debug`) or `minified` (`com.fernandocorreia.khepri.minified`, release code under R8).

   Install only that one build. With two apps claiming `khepri://`, the link step stops at Android's "Open with" chooser.

4. Run:

   ```sh
   MAESTRO_CLI_NO_ANALYTICS=1 maestro test \
     -e APP_ID=com.fernandocorreia.khepri.minified \
     -e EMAIL=<throwaway-email> -e PASSWORD="$(cat <password-file>)" \
     maestro/flows/smoke-sign-in.yaml
   ```

Never commit credentials, and never point a flow at production.
