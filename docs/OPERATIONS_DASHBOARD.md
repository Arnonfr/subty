# Subty Operations Dashboard

## Monetization
- Free starter balance: `30` stars.
- Translation cost is star-based and depends on subtitle size and model multiplier.
- Google Play products currently wired in code:
  - `subty_stars_small` for `100` stars
  - `subty_stars_large` for `250` stars
- Current state: local star accounting is implemented, but Play Console product setup and purchase testing are still pending.
- If you want strict per-user enforcement or auto-refill, the next step is server-side entitlement verification and a refill policy decision.

## Analytics
- `search_submitted`
- `search_suggestion_selected`
- `search_history_suggestion_selected`
- `search_results_loaded`
- `download_clicked`
- `translate_clicked`
- `translation_start_clicked`
- `translation_complete`
- Key parameters: `engine`, `source_lang`, `target_lang`, `chars`, `entries`, `fallback_used`, `file_id`, `language_code`, `stars`

## Remote Config
- `app_enabled`
- `maintenance_message`
- `search_enabled`
- `translate_enabled`
- Control Panel
  - `translate_enabled=false` disables translation entry points.
  - `search_enabled=false` disables search flow.
  - `app_enabled=false` enables maintenance mode.
  - `maintenance_message` controls banner copy.

## KPI Dashboard
- Searches submitted
- Suggestion click-through
- Download clicks
- Translate clicks
- Star balance and depletion trends
- Plan conversion rate

## Next Step
- Move from local-only quota tracking to server-side entitlement tracking if you need strict per-user limits.
