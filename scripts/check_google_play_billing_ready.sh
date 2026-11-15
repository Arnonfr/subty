#!/usr/bin/env bash
set -euo pipefail

# The current product is free and offline-only. Billing must stay out of the app.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"
BUILD_FILE="$ROOT/app/build.gradle.kts"
BUNDLE_MANIFEST="$ROOT/app/build/intermediates/bundle_manifest/stableRelease/processApplicationManifestStableReleaseForBundle/AndroidManifest.xml"

if grep -q 'com.android.vending.BILLING' "$MANIFEST"; then
  echo 'FAIL: free offline release must not request Play Billing' >&2
  exit 1
fi
if grep -qE '^[[:space:]]*implementation\(libs\.play\.billing\)' "$BUILD_FILE"; then
  echo 'FAIL: free offline release must not package Play Billing' >&2
  exit 1
fi
if [[ -f "$BUNDLE_MANIFEST" ]] && grep -q 'com.android.vending.BILLING' "$BUNDLE_MANIFEST"; then
  echo 'FAIL: bundle contains a Play Billing permission' >&2
  exit 1
fi

echo 'Billing absence verified for the free offline release'
