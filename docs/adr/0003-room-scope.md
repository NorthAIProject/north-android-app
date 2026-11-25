# ADR 0003: Room holds durable state and an offline read cache

- **Status:** accepted
- **Date:** 2026-11-24

## Context

iOS has no database and no offline read cache; offline it shows an error. It keeps only the in-progress workout, failed set uploads and pending crew messages on the phone. The owner was asked whether Room should also cache reads and chose yes.

## Decision

Room stores (1) the workout snapshot, the set outbox and pending crew messages, and (2) a read cache limited to Today, the day for today and yesterday, the current plan, the exercise library, profile/settings, the conversation list with the last page of opened threads, and crew channel messages. Everything else is memory-only. Health, Care, Mind, photos, knowledge files, memories, reports and other people's data are never cached.

## Consequences

- Offline, the cached areas show last-known content with an 'Offline, showing HH:mm' line. This differs from iOS on purpose.
- A cold launch offline with a valid token lands on cached Today instead of the failed phase.
- Explicit sign-out clears everything; a 401 keeps the workout snapshot and outboxes for the same account.
