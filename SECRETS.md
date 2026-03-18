# Subty — Secrets Management

## פיתוח מקומי

הוסף לקובץ `local.properties` (נמצא בשורש הפרויקט, לא עולה לגיט):

```
sdk.dir=/Users/<שם-משתמש>/Library/Android/sdk

OPENSUBTITLES_API_KEY=<המפתח שלך>
GEMINI_API_KEY=<המפתח שלך>
GOOGLE_TRANSLATE_API_KEY=<המפתח שלך>
SUBDL_API_KEY=<המפתח שלך>
```

## GitHub Actions (CI/CD)

### Secrets שחייבים להגדיר ב-GitHub:

עבור לריפו → **Settings → Secrets and variables → Actions → New repository secret**

| שם Secret | מה למלא | מאיפה מקבלים |
|-----------|---------|---------------|
| `OPENSUBTITLES_API_KEY` | מפתח OpenSubtitles | opensubtitles.com/consumer |
| `OPENSUBTITLES_USERNAME` | **שם המשתמש / אימייל ב-opensubtitles.com** | חשבון אישי — נדרש לקבלת JWT לצורך הורדה |
| `OPENSUBTITLES_PASSWORD` | **סיסמת opensubtitles.com** | חשבון אישי — ללא זה: 5 הורדות/יום בלבד |
| `GEMINI_API_KEY` | מפתח Gemini | aistudio.google.com |
| `GOOGLE_TRANSLATE_API_KEY` | מפתח Google Translate | console.cloud.google.com |
| `SUBDL_API` | מפתח SubDL | subdl.com |
| `KEYSTORE_B64` | keystore בקידוד base64 | ראה למטה |
| `KEYSTORE_B64` | keystore בקידוד base64 | ראה למטה |
| `KEYSTORE_PASS` | סיסמת keystore | שלך |
| `KEY_ALIAS` | alias של המפתח | שלך |
| `KEY_PASS` | סיסמת המפתח | שלך |

### יצירת Keystore לחתימה:

```bash
keytool -genkey -v -keystore subty-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias subty

# המרה ל-base64 להעלאה כ-secret:
base64 -i subty-release.jks | pbcopy   # מעתיק ל-clipboard
```

## איך APK נבנה בצינור CI/CD

```
Push to main
    ↓
GitHub Actions runner (Ubuntu)
    ↓
כותב local.properties מה-Secrets (בזיכרון בלבד, לא נשמר בגיט)
    ↓
./gradlew assembleDebug
    ↓
APK מצורף כ-Artifact ← ניתן להורדה מ-GitHub Actions
```

## Release APK

```bash
git tag v1.0.0
git push origin v1.0.0
```
→ GitHub Actions בונה Release APK חתום ומפרסם אוטומטית ב-GitHub Releases.

## מה מוסתר ב-APK המהודר?

ב-`release` build:
- `ProGuard` מסיר שמות משתנים ומבנה קוד
- `R8` מכווץ ומצפין את הבייטקוד
- המפתחות מוטמעים כמחרוזות בתוך הקוד המהודר

> ⚠️ זה לא הצפנה מלאה. למי שרוצה אבטחה מקסימלית — אפשר בהמשך להעביר
> את הקריאות ל-API דרך backend (proxy server) שלך.
