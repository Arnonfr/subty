# Handoff: Hidden Web-Hunt Subtitle Search (No-API Flow)

## What was added

1. A **hidden feature** on the Home/Search page, separate from the regular search flow:
- Trigger: tap the `Subty` title **5 times quickly**.
- Behavior: toggles a hidden panel called **Web Hunt**.

2. A dedicated scrape endpoint in preview server:
- `GET /webhunt/search?query=<text>`
- Returns aggregated results from website scraping providers (no official API flow).

3. UI + logic for hidden search:
- Input and button inside hidden panel.
- Results are shown in a separate list with direct links to provider pages.
- This does **not** modify normal `doSearch()` / API subtitle search behavior.

## Files changed

- `/Users/hyh/Documents/translate-sub/preview/index.html`
  - Added hidden panel styles (`.stealth-*`, `.webhunt-*`).
  - Added hidden panel HTML in Home/Search expanded section.
  - Added JS:
    - `onHomeTitleTap()` (unlock/toggle hidden panel)
    - `runWebHunt()` (calls backend scrape route)
    - `escHtml()` utility
  - Added state:
    - `homeTapCount`, `homeTapTimer`, `webHuntUnlocked`

- `/Users/hyh/Documents/translate-sub/preview/server.js`
  - Added scrape utilities:
    - `fetchText()`, `stripTags()`, `decodeEntities()`, `toAbs()`
    - `extractAnchors()`, `scrapeProvider()`, `runWebHunt()`
  - Added route:
    - `/webhunt/search`
  - Kept existing `/osapi`, `/sdapi`, `/sddl` behavior intact.

## Scrape providers currently wired

- YIFY (`yifysubtitles.org`)
- Addic7ed (`addic7ed.com`)
- TVSubtitles (`tvsubtitles.net`)
- Podnapisi (`podnapisi.net`)

## Important caveats for next developer

1. HTML scraping is brittle.
- If provider markup changes, anchor extraction can break.
- Matchers are regex-based and intentionally lightweight.

2. Some sites may block/bot-protect requests.
- Current behavior on failure: provider returns empty list (silent fallback).
- Consider adding per-provider error diagnostics in JSON for observability.

3. No dedupe by normalized title yet.
- Current dedupe is by final URL only.
- Could improve by title normalization + language extraction.

4. This flow intentionally stays separate.
- Do not merge into `doSearch()` unless product asks for unification.

## Suggested next steps

1. Add optional filters in hidden panel:
- language code
- provider toggle chips

2. Improve parser quality:
- provider-specific DOM parsing with stable selectors (or Cheerio if allowed)
- extract language/year/season/episode when available

3. Add telemetry/debug mode:
- response metadata per provider: status, latency, parse count

4. Add legal/compliance gate:
- feature flag + disclaimer in hidden panel for scraping mode

## Quick manual test

1. Start preview server:
- `node /Users/hyh/Documents/translate-sub/preview/server.js`

2. Open preview app and go to Search screen.
3. Tap `Subty` title 5 times quickly.
4. Enter query in hidden panel, click `Scan Sites`.
5. Verify links open and regular search still works unchanged.
