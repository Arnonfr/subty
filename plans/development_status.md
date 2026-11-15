# Subty - Development Status & Handoff Guide

**Base version on device:** `v1.11` (`versionCode=14`)

## Current Status
- Search flow is being reshaped from the installed `v1.11` baseline.
- The search screen now uses a dark hero section for the title, query field, advanced-search control, and recent posters.
- Recent posters are clickable and render with a bottom fade and title overlay.
- Season and episode controls are hidden until a specific series is selected.
- The build currently passes `:app:compileLabDebugKotlin` after the latest search-screen rewrite.

## Monetization
- Free starter balance: `30` stars.
- Star-based charging is implemented locally.
- Google Play product IDs currently wired in code:
  - `subty_stars_small`
  - `subty_stars_large`
- Play Console setup and purchase testing still need to be completed.
- Auto-refill / subscription policy has not been finalized.

## Remaining Work
- Install the rebuilt APK on the phone and rerun the smoke + screenshot set.
- Review the visual results against the dark-hero search design.
- Finish the Play Console purchase setup.
- Decide whether to keep simple star packs or move to an auto-refill/subscription flow later.

## Notes
- The device currently showed the installed `v1.11` APK before this rewrite.
- If you need strict per-user enforcement, server-side entitlement verification is still the next step.
