# Google Play release readiness — Subty

Updated: 2026-10-19

## Candidate

Package `com.subtranslate`, version **1.18 / 21**, target SDK 36, min SDK 26. Signed, minified Stable APK and AAB built successfully; release lint has no fatal/error issues. Firestore operational telemetry is packaged; Play Billing is absent. Translation uses ML Kit on-device after a model download.

## Verified

- 60 JVM tests, zero failures/errors/skips; covers translation processing, cancellation job isolation, parsers, history helpers, ZIP extraction and saving.
- Final `lintStableRelease assembleStableRelease bundleStableRelease` passed. Release signature/readiness and billing-absence checks passed.
- Signed minified Stable app on an API 35 emulator: movie and series discovery, season/episode selection, 55 Matrix results, 13 next-episode results, OpenSubtitles/SubDL downloads, movie recent-history persistence, denied-notification-permission translation and saving.
- Real 1017-cue English→Hebrew translation saved from Stable: cue IDs and timestamps preserved, no empty cues or internal marker leaks. The same flow on Lab also passed offline/background completion and cancel/restart.
- Headless software-rendered emulator isolated final QA from host-lock/window-renderer instability. No physical-phone or Play-delivered v21 install was performed this session.

## Store and privacy

- Reference-inspired folded-plane logo in existing purple/cream palette integrated into launcher and store icon.
- Four marketing screenshots composed from actual app captures, plus a 1024×500 feature graphic and 512×512 icon.
- All submitted titles, descriptions, release notes and artwork are English. The Hebrew/Israel locale intentionally mirrors English so it does not select the old Hebrew listing.
- Public privacy policy updated and verified at https://arnonfr.github.io/subty/privacy-policy.html.
- Updated Data safety declaration accepted by the API; details in `docs/store-listing/DATA_SAFETY_20261019.md`.

## Release evidence

`artifacts/release-20261019/`: test/build logs, source-input hashes, store validation, full-file translation checks, screenshots, before/after Play API snapshots, edit and commit receipts.

`release/20261019-v21/`: signed APK/AAB, R8 mapping and SHA-256 hashes. The existing dirty develop worktree was preserved; source-input hashes identify this build without claiming a clean release commit.

Production v21 / 1.18 was committed successfully; fresh API read-back confirms the release and both English listing locales, with all uploaded asset hashes matching. The public Israel page still showed the old Hebrew copy immediately after commit. Submission evidence is recorded in the Play edit/commit receipts. A completed Production track does not by itself prove Google review has finished or the update is available to every user.

## Remaining product gaps

Machine Hebrew needs human review, particularly slang/names. Process-death recovery still relies on an in-memory subtitle input. External-file VIEW handling and legacy encodings need follow-up. Some visible settings need a wiring audit. External-player rendering and real-phone/Play-delivered v21 QA remain unverified. No broad MicroDVD/binary `.sub` claim is made. The UI still has tall-screen whitespace and dense history rows; those were not release blockers.
