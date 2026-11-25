# Feature spec: auth

> Modules `:feature:auth:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Login screen:** 3D emblem, "Khepri" wordmark in Marcellus, tagline, Sign In / Sign Up segmented switch with a sliding indicator. Error and info banners are tinted, announced to the screen reader, with an error haptic.
- **Sign in:** email, password, Forgot Password sheet, passkey, Apple, Google.
- **Sign up:** name, email, password with strength meter, confirm with inline mismatch error, Create Account, "create account with a passkey", Apple, Google. Sign-up sends the device time zone.
- **Password reset** finishes on the web from the emailed link.
- **Google:** Credential Manager; send the ID token to `POST /auth/google`.
- **Passkeys:** Credential Manager; `…/passkey/login/begin|finish`, `…/passkey/register/begin|finish`; RP id `kheprios.com`; credential JSON with base64url fields.
- **Apple:** Custom Tab to `/auth/apple/start` (B2).
- **AI consent:** full-screen Allow / Decline, recipient list from the server; settings row to change it.
- **API:** `signUp`, `logIn`, `signInWithGoogle`, `logOut`, `requestPasswordReset`, passkey begin/finish ×2, `getMe`, `get/setAIConsent`.
- **Tests to port:** AppModel phases, AI consent, Keychain behaviour (as DataStore/Keystore).


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/AIConsent/AIConsent.swift` | 83 |
| `K/khepri/Features/AIConsent/AIConsentViews.swift` | 135 |
| `K/khepri/Features/Auth/Services/AppleSignInCoordinator.swift` | 158 |
| `K/khepri/Features/Auth/Services/AuthService.swift` | 209 |
| `K/khepri/Features/Auth/Services/AuthSessionManager.swift` | 110 |
| `K/khepri/Features/Auth/Services/OAuthWebAuthenticator.swift` | 119 |
| `K/khepri/Features/Auth/Services/PasskeyCoordinator.swift` | 221 |
| `K/khepri/Features/Auth/UI/Components/PasswordStrengthMeter.swift` | 53 |
| `K/khepri/Features/Auth/UI/Components/SocialAuthButton.swift` | 82 |
| `K/khepri/Features/Auth/UI/Components/SocialAuthSection.swift` | 55 |
| `K/khepri/Features/Auth/UI/Components/VaultTextField.swift` | 123 |
| `K/khepri/Features/Auth/UI/Emblem3D/Emblem3DView.swift` | 172 |
| `K/khepri/Features/Auth/UI/Emblem3D/EmblemMotion.swift` | 98 |
| `K/khepri/Features/Auth/UI/ForgotPasswordSheet.swift` | 84 |
| `K/khepri/Features/Auth/UI/LoginScreen.swift` | 152 |
| `K/khepri/Features/Auth/UI/LoginViewModel.swift` | 170 |
| `K/khepri/Features/Auth/UI/SignInView.swift` | 91 |
| `K/khepri/Features/Auth/UI/SignUpView.swift` | 132 |
| `K/khepri/Features/Auth/Validation/AuthValidation.swift` | 46 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
