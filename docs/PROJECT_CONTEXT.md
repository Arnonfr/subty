# Subty project context

Subty searches for and downloads subtitles, then translates and saves them on Android. This source snapshot uses version name `1.19` and version code `22`.

- The Android app and unit tests live under `app/`. The `stable` flavor is the production package, and `lab` uses a separate application ID for testing.
- TMDB powers title and season lookup. Subtitle results can come from OpenSubtitles, SubDL, and optionally Screwzira when configured.
- The app's translation flow uses on-device ML Kit. Cloud provider implementations and the LibreTranslate server tools are kept in the source tree; the translation server and model plans describe experiments, not current product requirements.
- Optional operational telemetry, its Firestore rules, and the dashboard are covered in [Operations Dashboard](OPERATIONS_DASHBOARD.md) and the [privacy policy](privacy-policy.html).
- [Development status](../plans/development_status.md), [phone regression tests](PHONE_REGRESSION_TESTS.md), and [Fastlane instructions](../fastlane/README.md) cover development, QA, and store operations. Store listing source and graphics live under `fastlane/metadata/`.

Run the Android unit tests with `./gradlew testStableDebugUnitTest testLabDebugUnitTest` after configuring the Android SDK and local Firebase configuration. Use `.env.example` for provider variable names and keep real values in the local environment or the CI secret store. Signed APKs and AABs, raw QA captures, and local logs are generated outside the tracked source snapshot.
