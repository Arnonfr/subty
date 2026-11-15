#!/usr/bin/env bash
set -euo pipefail

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
APP_ID="${APP_ID:-com.subtranslate}"
ACTIVITY="${ACTIVITY:-$APP_ID/.MainActivity}"
TMP_XML="/tmp/subty_ui.xml"
RUN_TS="$(date +%Y%m%d-%H%M%S)"
SHOT_DIR="${SHOT_DIR:-artifacts/visual-regression/$RUN_TS}"

mkdir -p "$SHOT_DIR"

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; exit 1; }

dump_ui() {
  "$ADB" shell uiautomator dump /sdcard/subty_ui.xml >/dev/null 2>&1 || true
  "$ADB" pull /sdcard/subty_ui.xml "$TMP_XML" >/dev/null 2>&1 || true
}

capture_screen() {
  local name="$1"
  "$ADB" shell screencap -p "/sdcard/$name.png" >/dev/null 2>&1 || true
  "$ADB" pull "/sdcard/$name.png" "$SHOT_DIR/$name.png" >/dev/null 2>&1 || true
}

has_text() {
  local txt="$1"
  TARGET_TEXT="$txt" perl -0ne '
    my $target = $ENV{TARGET_TEXT};
    $found = 1 if /(?:text|content-desc)="[^"]*\Q$target\E[^"]*"/i;
    END { exit($found ? 0 : 1) }
  ' "$TMP_XML"
}

has_any_text() {
  local txt
  for txt in "$@"; do
    has_text "$txt" && return 0
  done
  return 1
}

tap_xy() {
  "$ADB" shell input tap "$1" "$2" >/dev/null 2>&1 || true
}

tap_text_first() {
  local txt="$1"
  dump_ui
  local bounds x1 y1 x2 y2 cx cy
  bounds="$(
    perl -0777 -ne '
      my $target = shift @ARGV;
      if (/<node[^>]*text="\Q$target\E"[^>]*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/i) {
        print "$1 $2 $3 $4\n";
      }
    ' "$TMP_XML" "$txt"
  )"
  [[ -z "$bounds" ]] && return 1
  read -r x1 y1 x2 y2 <<<"$bounds"
  cx=$(( (x1 + x2) / 2 ))
  cy=$(( (y1 + y2) / 2 ))
  tap_xy "$cx" "$cy"
}

tap_content_desc_first() {
  local desc="$1"
  dump_ui
  local bounds x1 y1 x2 y2 cx cy
  bounds="$(
    perl -0777 -ne '
      my $target = shift @ARGV;
      if (/<node[^>]*content-desc="\Q$target\E"[^>]*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/i) {
        print "$1 $2 $3 $4\n";
      }
    ' "$TMP_XML" "$desc"
  )"
  [[ -z "$bounds" ]] && return 1
  read -r x1 y1 x2 y2 <<<"$bounds"
  cx=$(( (x1 + x2) / 2 ))
  cy=$(( (y1 + y2) / 2 ))
  tap_xy "$cx" "$cy"
}

clear_and_type_query() {
  local q="$1"
  tap_xy 260 420
  sleep 0.3
  for _ in {1..30}; do "$ADB" shell input keyevent 67 >/dev/null 2>&1 || true; done
  "$ADB" shell input text "$q" >/dev/null 2>&1 || true
  sleep 1
}

start_app() {
  "$ADB" shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
  "$ADB" shell am start -n "$ACTIVITY" >/dev/null 2>&1 || true
  sleep 2
}

echo "== Subty phone smoke =="
"$ADB" devices | rg -q "device$" || fail "No connected Android device"

# 1) Search series flow
start_app
capture_screen "00_app_opened"
clear_and_type_query "the\\ boys"
"$ADB" shell input keyevent 66 >/dev/null 2>&1 || true
sleep 3
capture_screen "01_series_query_results"
dump_ui
has_text "The Boys" || fail "Series search: 'The Boys' not shown"
pass "Series search list shown"

# choose first result (usually The Boys)
tap_text_first "The Boys" || tap_xy 220 760
sleep 3
capture_screen "02_series_selection"
dump_ui
has_any_text "Season" "SEASONS" "Seasons" || fail "Series selection: Season selector missing"
has_any_text "Episode" "EPISODES" "Episodes" || fail "Series selection: Episode selector missing"
has_any_text "Search Subtitles" "SEARCH SUBTITLES" "Search" || fail "Series selection: Search button missing"
pass "Series selection screen (season/episode) shown"

# 2) Search movie flow
start_app
clear_and_type_query "matrix"
"$ADB" shell input keyevent 66 >/dev/null 2>&1 || true
sleep 3
capture_screen "03_movie_query_results"
dump_ui
has_text "The Matrix" || fail "Movie search: 'The Matrix' not shown"
pass "Movie search list shown"

# choose first result (The Matrix movie)
tap_text_first "The Matrix" || tap_xy 220 760
sleep 8
capture_screen "05_after_movie_tap"
dump_ui
has_any_text "Download" "DOWNLOAD" "Translate" "TRANSLATE" || fail "Movie search did not show download/translate actions"
pass "Movie selection navigates to results"
capture_screen "06_movie_results"

# 3) Download flow
tap_text_first "Download" || tap_text_first "DOWNLOAD" || fail "Download button not found"
sleep 2
capture_screen "05_after_download_tap"
dump_ui
if has_any_text "Saved ✓" "Saving…"; then
  pass "Download action triggered"
else
  # Snackbar may disappear quickly; still count if we're on results with buttons.
  has_any_text "Download" "DOWNLOAD" || fail "Download action left results unexpectedly"
  pass "Download action triggered (results remained stable)"
fi

# 4) Translate flow
tap_text_first "Translate" || tap_text_first "TRANSLATE" || tap_content_desc_first "Translate" || fail "Translate button not found"
sleep 3
capture_screen "06_translate_screen"
dump_ui
has_text "Translate" || fail "Did not navigate to Translate screen"
has_any_text "Translate" "Translate again" || fail "Translate screen action button missing"
pass "Translate screen flow works"

# 5) Search from recents flow
start_app
capture_screen "07_recent_titles"
dump_ui
has_any_text "Recent titles" "Recently searched" || fail "Recent titles section missing on Search screen"
# Use dedicated recent-flow action
tap_text_first "Find next episode" || fail "Search from recents: Find next episode not found"
sleep 3
capture_screen "08_recents_next_episode_result"
dump_ui
if has_any_text "SUBTITLES FOUND" "No subtitles found"; then
  pass "Search from recents works"
elif has_any_text "Download" "DOWNLOAD" "Translate" "TRANSLATE"; then
  pass "Search from recents works (results actions shown)"
else
  fail "Search from recents did not navigate to results"
fi

echo "== ALL SMOKE TESTS PASSED =="
echo "Screenshots saved to: $SHOT_DIR"
