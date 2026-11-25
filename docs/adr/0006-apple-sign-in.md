# ADR 0006: Sign in with Apple on Android in the first release

- **Status:** accepted
- **Date:** 2026-11-24

## Context

The backend accepts only native iOS Apple identity tokens. People who signed up on iOS with Apple often have a relay email and no password, so they could not sign in on Android.

## Decision

Add a web flow to the backend (`/auth/apple/start`, `/auth/apple/code`, Services ID as audience), modelled on the existing Google web flow. Android opens it in a Custom Tab and receives a one-time code on `khepri://auth/apple`.

## Consequences

- Backend work (B2 in `docs/backend-prerequisites.md`) and an Apple Services ID are prerequisites for release.
- No account is locked out of Android.
