#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AAB="$ROOT/app/build/outputs/bundle/stableRelease/app-stable-release.aab"
BUILD_FILE="$ROOT/app/build.gradle.kts"
MERGED_MANIFEST="$ROOT/app/build/intermediates/merged_manifest/stableRelease/processStableReleaseMainManifest/AndroidManifest.xml"
BUNDLE_MANIFEST="$ROOT/app/build/intermediates/bundle_manifest/stableRelease/processApplicationManifestStableReleaseForBundle/AndroidManifest.xml"
LOCAL_PROPS="$ROOT/local.properties"
JAVA_HOME_DEFAULT="/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

has_env_or_prop() {
  local key="$1"
  if [[ -n "${!key:-}" ]]; then
    return 0
  fi
  [[ -f "$LOCAL_PROPS" ]] && grep -qE "^${key}=" "$LOCAL_PROPS"
}

[[ -f "$AAB" ]] || fail "Missing stable release AAB. Run ./gradlew bundleStableRelease first."
[[ -s "$AAB" ]] || fail "Stable release AAB is empty"

grep -q 'applicationId = "com.subtranslate"' "$BUILD_FILE" ||
  fail "Stable applicationId must remain com.subtranslate"
VERSION_CODE="$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\).*/\1/p' "$BUILD_FILE" | head -1)"
VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' "$BUILD_FILE" | head -1)"
[[ -n "$VERSION_CODE" ]] || fail "Could not determine versionCode"
[[ -n "$VERSION_NAME" ]] || fail "Could not determine versionName"
grep -q 'isMinifyEnabled = true' "$BUILD_FILE" ||
  fail "Release minification should be enabled"
grep -q 'isShrinkResources = true' "$BUILD_FILE" ||
  fail "Release resource shrinking should be enabled"
grep -q 'signingConfig = signingConfigs.getByName("release")' "$BUILD_FILE" ||
  fail "Release build type must use release signing config"

[[ -f "$MERGED_MANIFEST" ]] || fail "Missing merged stable release manifest"
[[ -f "$BUNDLE_MANIFEST" ]] || fail "Missing bundle stable release manifest"
grep -q 'package="com.subtranslate"' "$BUNDLE_MANIFEST" ||
  fail "Bundle manifest package mismatch"
grep -q "android:versionCode=\"$VERSION_CODE\"" "$BUNDLE_MANIFEST" ||
  fail "Bundle manifest versionCode mismatch"
grep -q "android:versionName=\"$VERSION_NAME\"" "$BUNDLE_MANIFEST" ||
  fail "Bundle manifest versionName mismatch"
! grep -q 'com.android.vending.BILLING' "$BUNDLE_MANIFEST" ||
  fail "Bundle manifest must not include Play Billing"
grep -q 'alias(libs.plugins.google.services)' "$BUILD_FILE" ||
  fail "Release build must apply Google Services for live analytics"
grep -q 'implementation(libs.firebase.firestore)' "$BUILD_FILE" ||
  fail "Release build must package Firestore live analytics"
! grep -q 'implementation(libs.play.billing)' "$BUILD_FILE" ||
  fail "Release build must not package Google Play Billing"

has_env_or_prop KEYSTORE_PATH || fail "KEYSTORE_PATH is not configured"
has_env_or_prop KEYSTORE_PASSWORD || fail "KEYSTORE_PASSWORD is not configured"
has_env_or_prop KEY_ALIAS || fail "KEY_ALIAS is not configured"
has_env_or_prop KEY_PASSWORD || fail "KEY_PASSWORD is not configured"
JAVA_HOME_TO_USE="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
[[ -x "$JAVA_HOME_TO_USE/bin/jarsigner" ]] || fail "jarsigner not found; set JAVA_HOME"

"$JAVA_HOME_TO_USE/bin/jarsigner" -verify "$AAB" >/dev/null 2>&1 ||
  fail "AAB signature verification failed"

echo "Release readiness checks passed"
echo "AAB: $AAB"
