# ADR 0001: Native Kotlin and Jetpack Compose

- **Status:** accepted
- **Date:** 2026-11-24

## Context

Khepri ships on iOS (SwiftUI) and the web. Android needs the same features and behaviour. The owner asked for native Android on Google's Compose-first stack.

## Decision

Kotlin, Jetpack Compose + Material 3, no XML layouts. Navigation 3, Hilt, Retrofit + OkHttp, Room, Coil, Vico, WorkManager. Cross-platform toolkits were not considered.

## Consequences

- No code is shared with iOS; parity is kept by the shared OpenAPI contract, shared golden fixtures, shared theme tokens and the parity checklist.
- Lottie Compose is added only when a designed animation asset needs it; iOS ships none.
