# Subty Design System v1

## Core tokens
- Accent: `SubtyAccent` (`#ED5A2F`)
- Hero: `SubtyHeroBg`, `SubtyHeroMask`
- Panels: `SubtyPanelBg`, `SubtyPanelAlt`, `SubtyPanelHit`, `SubtyPanelLine`, `SubtyPanelText`

## Button system
Use `SubtyButton` only:
- `OUTLINE` for secondary actions
- `ACCENT` for primary actions
- `FILLED` reserved for inverse sections
- `MOCHA` legacy style (to be removed gradually)

## Component rules
- Season/Episode selectors must use the same chip style in Search and Results.
- Subtitle action row must keep exactly two equal-width actions: Download + Translate.
- Type badges in title lists must use accent style for `Series`, muted style for `Movie`.

## Typography rules
- Page title: `SubtyPageTitle`
- Section labels: `SubtyLabel`
- Row titles: `SubtyText` 14 semibold
- Metadata: `SubtyText` 11-12 regular/medium

## Consistency checklist
- No inline hex colors in feature screens.
- No direct Material buttons in feature screens.
- No mixed naming (`TV` vs `Series`). Always use `Series`.
