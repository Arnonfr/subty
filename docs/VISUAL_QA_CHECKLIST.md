# Visual QA Checklist (Mandatory)

Run after every smoke test:

```bash
cd /Users/hyh/Documents/translate-sub
scripts/phone_regression_smoke.sh
```

Then review screenshots under:

`artifacts/visual-regression/<timestamp>/`

## Required screen set
- `01_series_query_results.png`
- `02_series_selection.png`
- `04_movie_results.png`
- `06_translate_screen.png`
- `08_recents_next_episode_result.png`

## Design-system checks (pass/fail)
1. Button consistency
- Season, episode, and language selectors must use one visual family:
  - same border width
  - same selected state color
  - same text weight
  - same vertical rhythm

2. Color consistency
- Only approved tokens should appear for panel/action states:
  - `SubtyAccent`
  - `SubtyPanelBg`
  - `SubtyPanelAlt`
  - `SubtyPanelHit`
  - `SubtyPanelLine`

3. Typography consistency
- Same entity naming everywhere: `Series` only (no mixed `TV`/`series` variants).
- Section labels and action labels must keep fixed casing style.

4. Spacing consistency
- Same horizontal padding in equivalent panels.
- No crowded chips or uneven vertical gaps between sections.

5. Interaction visibility
- Selected season/episode/language must be visually obvious.
- Primary action must be the same style across Search and Results.

## Release gate
- If any checklist item fails, release is blocked.
- Fix UI, rebuild, rerun smoke + screenshot review.
