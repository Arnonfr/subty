# Temporarily Disabled For Store v1

Date: 2026-06-01

## Disabled in this release
1. Translation actions in subtitle results UI
- Removed `Translate` button from results rows.
- Results now expose download-only action.

2. Translation entry flow
- Removed navigation from results to translate screen.

3. Translation wording in user-facing settings/history
- Removed translation-specific labels from main Settings screen.
- Removed translated-language marker from download history row.

4. Dark mode
- Removed Dark Mode toggle from Settings screen.
- App theme is forced to Light mode in `MainActivity` for this release.

## Kept in codebase (not exposed in primary UI)
- Translate screen files and related viewmodel remain in project for later re-enable.
- API keys screen route remains in nav graph but no direct entry from Settings.

## Re-enable plan later
1. Restore Settings sections (translation + API keys + dark mode toggle).
2. Reconnect Results -> Translate navigation.
3. Re-add translate action/button styling pass.
4. Remove forced light mode line in `MainActivity`.
