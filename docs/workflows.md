# Subty — Workflow Guide

---

## 1. דחיפת גרסה יציבה ל-Play Store (Production / Alpha)

### מתי
רק כשה-`develop` עבר בדיקות ואתה מוכן לשחרר.

### שלבים

```bash
# 1. עדכן versionCode + versionName ב-build.gradle.kts
#    versionCode: מספר שלם עולה (8 → 9)
#    versionName: "1.6" → "1.7"

# 2. מיזוג develop → main
git checkout main
git merge develop
git push origin main

# 3. בנה AAB
JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home \
  ./gradlew bundleStableRelease

# 4. קובץ יצא ב:
# app/build/outputs/bundle/stableRelease/app-stable-release.aab
```

### העלאה ל-Play Console
1. play.google.com/console → Subty
2. Testing → Closed testing → Alpha → Create new release
3. Upload AAB
4. Save & Review → Rollout

> **חשוב:** `main` היא תמיד הגרסה שנמצאת בטסט. אל תדחוף ישירות ל-`main` בלי לבדוק.

---

## 2. דחיפת גרסה לבדיקה ב-Firebase App Distribution

### מתי
כל פעם שרוצים לשלוח build לטסטרים **בלי** להשפיע על Play Store.

### שלבים

```bash
# עבוד על develop
git checkout develop

# ... עשה שינויים ...

git add <files>
git commit -m "תיאור השינוי"
git push origin develop
# ← GitHub Actions מופעל אוטומטית
```

### מה קורה ב-CI
| Branch | Stable → Firebase | Lab → Firebase | Play Store |
|--------|-------------------|----------------|------------|
| `develop` | ✅ | ❌ | ❌ |
| `main` | ✅ | ✅ | ❌ (ידני) |

הטסטרים מקבלים התראה ב-Firebase App Distribution באופן אוטומטי.

### טריגר ידני (בלי push)
ניתן להפעיל את ה-workflow ידנית:
```
GitHub → Actions → Firebase App Distribution → Run workflow → develop
```

---

## 3. הרצת אמולטור + דיבוג חי

### הפעלת האמולטור

```bash
# הפעל
/Users/hyh/.local/android-sdk/emulator/emulator -avd subty_api35 -no-snapshot-load &

# המתן לאתחול
/Users/hyh/.local/android-sdk/platform-tools/adb wait-for-device
# בדוק שעלה:
/Users/hyh/.local/android-sdk/platform-tools/adb shell getprop sys.boot_completed
# → צריך להחזיר "1"
```

### בנייה והתקנה

```bash
# Build
JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home \
  ./gradlew assembleStableDebug

# התקנה
APK=app/build/outputs/apk/stable/debug/app-stable-debug.apk
/Users/hyh/.local/android-sdk/platform-tools/adb install -r "$APK"

# הפעלה
/Users/hyh/.local/android-sdk/platform-tools/adb shell am start -n com.subtranslate/.MainActivity
```

### Logcat — דיבוג חי

```bash
# קבל PID של האפליקציה
PID=$(/Users/hyh/.local/android-sdk/platform-tools/adb shell pidof com.subtranslate | tr -d '\r')

# נקה ופתח logcat מסונן לאפליקציה בלבד
/Users/hyh/.local/android-sdk/platform-tools/adb logcat -c
/Users/hyh/.local/android-sdk/platform-tools/adb logcat --pid="$PID" -v brief | tee /tmp/subty_live.log
```

### Tags שימושיים לסינון

| Tag | מה רואים |
|-----|----------|
| `SubtitleRepo` | בקשות SubDL, קודי שפה, downloads |
| `SearchViewModel` | שגיאות autocomplete, parsing |
| `okhttp.OkHttpClient` | כל בקשות ה-HTTP |
| `AndroidRuntime` | crashes |

```bash
# סינון ל-tags ספציפיים בלבד
/Users/hyh/.local/android-sdk/platform-tools/adb logcat \
  SubtitleRepo:D SearchViewModel:E AndroidRuntime:E \
  -v time > /tmp/subty_live.log &
```

### Build → Install → Watch בפקודה אחת

```bash
JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home ./gradlew assembleStableDebug && \
APK=$(find app/build/outputs/apk/stable/debug -name "*.apk" | head -1) && \
/Users/hyh/.local/android-sdk/platform-tools/adb install -r "$APK" && \
/Users/hyh/.local/android-sdk/platform-tools/adb shell am start -n com.subtranslate/.MainActivity && \
PID=$(/Users/hyh/.local/android-sdk/platform-tools/adb shell pidof com.subtranslate | tr -d '\r') && \
/Users/hyh/.local/android-sdk/platform-tools/adb logcat -c && \
/Users/hyh/.local/android-sdk/platform-tools/adb logcat --pid="$PID" -v brief | tee /tmp/subty_live.log
```

---

## מידע כללי

| פרמטר | ערך |
|-------|-----|
| JAVA_HOME | `/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home` |
| AVD name | `subty_api35` |
| Package (stable) | `com.subtranslate` |
| Package (lab) | `com.subtranslate.lab` |
| versionCode נוכחי | `8` (build.gradle.kts) |
| Branch production | `main` |
| Branch פיתוח | `develop` |
