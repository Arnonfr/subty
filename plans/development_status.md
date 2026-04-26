# Subty — Development Status & Handoff Guide

**עודכן לאחרונה:** 2026-04-26  
**ענף פעיל:** `develop`  
**גרסה נוכחית:** 1.6 build 9

---

## סטטוס Google Play Console

| פריט | מצב |
|------|-----|
| Closed Testing (Alpha) | **פעיל** — build 8 |
| 12 טסטרים opt-in | ✅ הושלם |
| 14 ימים בבדיקה | ✅ הושלם |
| גישה ל-Production | **זמינה** — לחיצה על "הגשת בקשה לגישה לסביבת הייצור" |
| Firebase Distribution | build 9 בתהליך הפצה |

**לינק ישיר לדשבורד:**  
`https://play.google.com/console/u/0/developers/6556694888792594977/app/4972254507162504539/app-dashboard`

---

## מה שנעשה בסשן הנוכחי (build 9)

### 1. Browse Episodes מההיסטוריה
**קבצים שהשתנו:**
- `SearchSession.kt` — נוספו `pendingBrowseTitle`, `pendingBrowseLangs`
- `HistoryViewModel.kt` — נוסף `prepareSeriesBrowse()`
- `HistoryScreen.kt` — פריטי TV מציגים קישור "Browse other episodes →"
- `SearchViewModel.kt` — `init {}` קורא מה-session ומאכלס חיפוש מוכן
- `NavGraph.kt` — `onBrowseEpisodes` מנווט ל-SearchScreen עם `popUpTo`

**Flow:**
```
היסטוריה (TV item) → "Browse other episodes →"
→ prepareSeriesBrowse() → pendingBrowseTitle = "Project Runway"
→ navigate to SearchScreen (recreated)
→ SearchViewModel.init() קורא pendingBrowseTitle
→ fetchSuggestions("Project Runway")
→ משתמש בוחר עונה + פרק ומחפש
```

### 2. MyMemory במקום Google Translate
**קבצים שהשתנו:**
- `MyMemoryTranslationService.kt` — **קובץ חדש**, API חינמי ללא מפתח
- `TranslationRepositoryImpl.kt` — מחליף GoogleTranslateService ב-MyMemoryTranslationService
- `TranslateScreen.kt` — נוסף "MyMemory (Free, Basic)" לרשימת המודלים
- `TranslateViewModel.kt` — `hasTranslateApiKey()` מחזיר `true` עבור MyMemory

**הגבלות MyMemory:**
- 500 תווים לבקשה (הקוד מפצל אוטומטית)
- ~1000 מילים/יום בחינם (אנונימי)
- לא מתאים לקבצי כתוביות גדולים מאוד — Gemini עדיף לשימוש רציני

### 3. תיקוני UI
- **Arrow בין שפות** — הוסרו border ורקע; נשאר רק "→" ב-SubtyMocha
- **כפתורי חיפוש חיצוניים** — הוסרו Podnapisi / Addic7ed / OpenSubtitles / YIFY ממסך התוצאות

---

## ארכיטקטורה — מפת הקוד

```
app/src/main/java/com/subtranslate/
├── data/
│   ├── local/
│   │   ├── dao/          — SearchHistoryDao, HistoryDao
│   │   ├── datastore/    — SettingsDataStore (geminiApiKey, targetLang, etc.)
│   │   └── entity/       — SearchHistoryEntity, HistoryItemEntity
│   ├── remote/
│   │   ├── opensubtitles/ — OpenSubtitlesApi (search + download)
│   │   ├── subdl/         — SubDLApi (fallback subtitle source)
│   │   ├── tmdb/          — SearchSession (singleton shared state)
│   │   ├── config/        — AppConfig (Remote Config kill-switch)
│   │   └── translation/
│   │       ├── GeminiTranslationService.kt   — AI, context-aware, בקשות API
│   │       ├── MyMemoryTranslationService.kt — חינמי, לא AI, ללא מפתח
│   │       └── TranslationPromptBuilder.kt   — בונה prompt לGemini
│   └── repository/
│       ├── TranslationRepositoryImpl.kt — מנתב ל-Gemini / MyMemory לפי modelId
│       └── ...
├── domain/
│   ├── model/    — SubtitleEntry, SubtitleFile, HistoryItem, TranslationProgress
│   ├── repository/ — interfaces
│   └── usecase/  — Download, Save, Translate, GetHistory
├── presentation/
│   ├── search/   — SearchScreen + SearchViewModel (autocomplete, TMDB, history)
│   ├── results/  — ResultsScreen + ResultsViewModel (dual-source: OS + SubDL)
│   ├── translate/ — TranslateScreen + TranslateViewModel + foreground service
│   ├── history/  — HistoryScreen + HistoryViewModel
│   ├── settings/ — SettingsScreen + SettingsViewModel
│   ├── navigation/ — NavGraph, Screen (routes)
│   └── theme/    — SubtyComponents, Theme, Colors
└── service/
    └── TranslationForegroundService.kt — translate in background
```

---

## CI/CD

| Workflow | טריגר | מה קורה |
|----------|--------|---------|
| Firebase App Distribution | push ל-`develop` או `main` | בונה stable APK ומפיץ לטסטרים |
| Play Store | push ל-`main` | (נבדק) builds release AAB |

**הרצה ידנית:**
```bash
JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home ./gradlew :app:compileStableDebugKotlin
JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home ./gradlew bundleRelease
```

---

## בעיות ידועות שממתינות לטיפול

### 1. Gemini איטי
- **מצב:** משתמשים מדווחים שהתרגום לוקח זמן רב
- **מיקום:** `GeminiTranslationService.kt` — BATCH_SIZE=80, MAX_CONCURRENT=6
- **רעיונות לשיפור:**
  - להגדיל BATCH_SIZE ל-120 (פחות קריאות API)
  - להגדיל MAX_CONCURRENT ל-8
  - לנסות streaming API במקום generateContent

### 2. "Scattered search" UX — פרטים ממתינים
- **מצב:** משתמש דיווח על בעיה אבל לא שלח פרטים
- **לא לגעת** עד קבלת פרטים מדויקים / צילום מסך

### 3. UI/padding — פרטים ממתינים
- **מצב:** משתמש ציין בעיות padding אבל לא שלח צילום מסך
- **לא לגעת** עד קבלת צילום מסך

### 4. SubDL language matching — FIXED בdevelop
- `subdlLangToIsoCode()` ב-`SubtitleRepositoryImpl.kt` ממיר "Hebrew" → "he"

---

## מה הצעד הבא — Production

**לאחר שהטסטרים מאשרים שbuild 9 תקין:**

1. פתח [Play Console Dashboard](https://play.google.com/console/u/0/developers/6556694888792594977/app/4972254507162504539/app-dashboard)
2. לחץ **"הגשת בקשה לגישה לסביבת הייצור"**
3. בנה release AAB:
   ```bash
   JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home ./gradlew bundleRelease
   ```
   פלט: `app/build/outputs/bundle/stableRelease/app-stable-release.aab`
4. העלה ל-Production Track בקונסול
5. **אל תגע ב-`main`** עד שה-AAB הועלה ואושר ידנית

---

## כללי עבודה חשובים

| כלל | סיבה |
|-----|------|
| **עבודה תמיד על `develop`** | `main` = גרסה תחת בדיקה ב-Alpha, אסור לשנות |
| **versionCode חייב לעלות בכל build** | Play Console דוחה versionCode שכבר בשימוש |
| **לא להסיר Google Translate imports עד בדיקה** | `GoogleTranslateService.kt` עדיין קיים — לא נמחק, רק לא בשימוש |
| **MyMemory: מגבלת 1000 מילים/יום** | לא מתאים לאינטנסיביות — להמליץ Gemini למשתמשים כבדים |
| **JAVA_HOME נדרש** | אין Java ב-PATH; תמיד להוסיף את הprefix לפקודות gradle |

---

## secrets נדרשים ב-GitHub

| Secret | שימוש |
|--------|--------|
| `FIREBASE_SERVICE_ACCOUNT_B64` | Firebase App Distribution |
| `GOOGLE_SERVICES_JSON_B64` | google-services.json לFirebase |
| `KEYSTORE_B64` | חתימת AAB לRelease |
| `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASS` | פרמטרי keystore |
| `GEMINI_API_KEY` | BuildConfig.GEMINI_API_KEY (בשימוש ב-CI) |
