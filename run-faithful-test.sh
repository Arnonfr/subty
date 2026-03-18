#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

AVD_NAME="${AVD_NAME:-subty_api35}"
API_LEVEL="${API_LEVEL:-35}"
ABI="${ABI:-arm64-v8a}"
IMG_PACKAGE="system-images;android-${API_LEVEL};google_apis;${ABI}"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
APP_ID="com.subtranslate"

detect_sdk_dir() {
  local from_local=""
  if [[ -f "$ROOT_DIR/local.properties" ]]; then
    from_local="$(grep -E '^sdk.dir=' "$ROOT_DIR/local.properties" | head -n1 | sed 's/^sdk.dir=//' || true)"
  fi
  if [[ -n "$from_local" ]]; then
    echo "$from_local"
    return
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "$ANDROID_SDK_ROOT"
    return
  fi
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    echo "$ANDROID_HOME"
    return
  fi
  echo ""
}

SDK_DIR="$(detect_sdk_dir)"
if [[ -z "$SDK_DIR" || ! -d "$SDK_DIR" ]]; then
  echo "❌ Android SDK not found."
  echo "Set sdk.dir in local.properties or ANDROID_SDK_ROOT."
  exit 1
fi

export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$SDK_DIR/platform-tools:$SDK_DIR/emulator:$SDK_DIR/cmdline-tools/latest/bin:$PATH"

JDKS=(
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  "/Applications/Android Studio.app/Contents/jre/Contents/Home"
  "$HOME/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/*/jbr/Contents/Home"
)
JAVA_HOME_FOUND=""
for JDK in "${JDKS[@]}"; do
  for DIR in $JDK; do
    if [[ -x "$DIR/bin/java" ]]; then
      JAVA_HOME_FOUND="$DIR"
      break 2
    fi
  done
done
if [[ -n "$JAVA_HOME_FOUND" ]]; then
  export JAVA_HOME="$JAVA_HOME_FOUND"
fi

require_tool() {
  local name="$1"
  if ! command -v "$name" >/dev/null 2>&1; then
    echo "❌ Missing tool: $name"
    exit 1
  fi
}

require_tool adb
require_tool sdkmanager
require_tool avdmanager

if ! command -v emulator >/dev/null 2>&1; then
  echo "⚠️ emulator binary not found in SDK."
  echo "Installing required SDK packages..."
  yes | sdkmanager --licenses >/dev/null || true
  sdkmanager "emulator" "platform-tools" "platforms;android-${API_LEVEL}" "$IMG_PACKAGE"
fi

echo "🔧 Ensuring Android platform + system image are installed..."
yes | sdkmanager --licenses >/dev/null || true
sdkmanager "platform-tools" "platforms;android-${API_LEVEL}" "$IMG_PACKAGE" >/dev/null

if ! avdmanager list avd | grep -q "Name: ${AVD_NAME}"; then
  echo "📱 Creating AVD: ${AVD_NAME}"
  echo "no" | avdmanager create avd -n "${AVD_NAME}" -k "${IMG_PACKAGE}" >/dev/null
fi

echo "🏗️ Building debug APK..."
chmod +x ./gradlew
./gradlew assembleDebug

if [[ ! -f "$APK_PATH" ]]; then
  echo "❌ APK not found at $APK_PATH"
  exit 1
fi

if ! adb devices | grep -q "emulator-"; then
  echo "🚀 Starting emulator: ${AVD_NAME}"
  nohup emulator -avd "${AVD_NAME}" -netdelay none -netspeed full >/tmp/subty-emulator.log 2>&1 &
fi

echo "⏳ Waiting for emulator..."
adb wait-for-device
until adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q '^1$'; do
  sleep 2
done

echo "📦 Installing app..."
adb install -r "$APK_PATH" >/dev/null

echo "▶️ Launching app..."
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null

echo ""
echo "✅ Real app is running on Android emulator (not web preview)."
echo "   App ID: $APP_ID"
echo "   APK: $ROOT_DIR/$APK_PATH"
echo ""
echo "Tips:"
echo "1) To capture logs: adb logcat | rg subtranslate"
echo "2) To reinstall quickly after changes: ./gradlew assembleDebug && adb install -r $APK_PATH"
