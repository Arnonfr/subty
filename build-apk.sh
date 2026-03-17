#!/bin/bash
# build-apk.sh — בנה APK ב-debug עם Android Studio bundled JDK

set -e

# מצא את ה-JDK של Android Studio
JDKS=(
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  "/Applications/Android Studio.app/Contents/jre/Contents/Home"
  "$HOME/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/*/jbr/Contents/Home"
)

JAVA_HOME_FOUND=""
for JDK in "${JDKS[@]}"; do
  for DIR in $JDK; do
    if [ -f "$DIR/bin/java" ]; then
      JAVA_HOME_FOUND="$DIR"
      break 2
    fi
  done
done

if [ -z "$JAVA_HOME_FOUND" ]; then
  echo "❌ לא נמצא JDK. פתח את Android Studio ולחץ Build → Build APK"
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_FOUND"
echo "✅ Java: $JAVA_HOME"

chmod +x ./gradlew
./gradlew assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  echo ""
  echo "✅ APK נבנה בהצלחה:"
  echo "   $PWD/$APK"
  echo ""
  echo "להעתיק לטלפון:"
  echo "   adb install $APK"
fi
