# ADR 0004: Full parity before the first release

- **Status:** accepted
- **Date:** 2026-11-24

## Context

The app is about 41k lines of Swift across 40 feature folders. A staged release (core loop first) was offered.

## Decision

Nothing ships to users until every reachable iOS feature is ported. Work is still ordered so the app runs early: foundation, a reference slice, then parallel feature lanes.

## Consequences

- Longest wait before real users; internal and closed Play tracks are used throughout to find device problems.
- Form Check (unreachable on iOS) and the Screen Time report (not an iOS target) are not ported.
- Lock-screen widgets, Dynamic Island and Spotlight entities have no Android equivalent and are replaced as described in the design.
