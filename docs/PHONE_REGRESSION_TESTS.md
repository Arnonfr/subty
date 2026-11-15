# Phone Regression Smoke Tests (Stable)

This is the fixed baseline smoke suite we run on a connected Android phone before release.

## Run command

```bash
cd /Users/hyh/Documents/translate-sub
scripts/phone_regression_smoke.sh
```

The script now also captures screenshots automatically for visual QA:
- Output path: `artifacts/visual-regression/<timestamp>/`
- Review checklist: `docs/VISUAL_QA_CHECKLIST.md`

## Covered scenarios

1. Series search flow
- Search query: `the boys`
- Expect: titles list appears
- Select result
- Expect: season/episode selection screen appears

2. Movie search flow
- Search query: `matrix`
- Expect: titles list appears
- Select result
- Expect: results screen with `DOWNLOAD` and `TRANSLATE` actions

3. Download flow
- Tap `DOWNLOAD` on results
- Expect: action triggers successfully (`Saved ✓` / stable results state)

4. Translation flow
- Tap `TRANSLATE` on results
- Expect: Translate screen opens and shows translation action button

5. Search from recents flow
- Open Search screen
- Use `Find next episode` from Recent titles
- Expect: navigates to subtitle results

## Current status (2026-06-01)

- Passed:
  - Series search
  - Movie search
  - Download
  - Translation screen entry
  - Search from recents

## Notes

- This suite is device-level smoke, not instrumentation tests.
- It uses ADB + UI dump parsing and is intended to catch regression in critical user flow.
- Visual consistency is now a release gate via screenshot review.
