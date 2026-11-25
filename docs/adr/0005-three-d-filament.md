# ADR 0005: All three 3D surfaces in 3D, on Filament

- **Status:** accepted
- **Date:** 2026-11-24

## Context

iOS renders a 3D emblem (login), a 3D mascot (onboarding) and a tappable muscle body map with RealityKit and a Metal shader. Android cannot load USDZ. The web app has GLBs of the same meshes in `north-web-app/static/assets/models/`.

## Decision

Build all three in 3D with Filament (gltfio plus a small wrapper in `:core:scene3d`), using the web GLBs. Motion comes from the shared `emblem-motion.json` and `mascot-motion.json`, evaluated in pure Kotlin and tested against the shared fixtures.

## Consequences

- Largest single technical risk; spike S3 gates it. If the loader rejects meshopt or quantization, re-export uncompressed GLBs.
- Flat image fallback under reduce motion, power saving or load failure, as on iOS.
- Native libraries per ABI add to APK size; measured in the spike.
