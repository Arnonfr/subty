# Google Play Release Workflow — Subty (com.subtranslate)

מסמך זה מתעד את כל הצעדים, הבעיות, והפתרונות לשחרור האפליקציה ל-Google Play.

---

## מידע קבוע

| פריט | ערך |
|------|-----|
| App ID | `com.subtranslate` |
| Developer ID | `6556694888792594977` |
| App ID בקונסול | `4972254507162504539` |
| Track ID (Closed Testing / Alpha) | `4699340228558431350` |
| Keystore | `android/cookit-release.jks` (משותף עם cookit) |
| Internal Testing opt-in link | `https://play.google.com/apps/internaltest/4701543569363115155` |
| Google Group (בודקים) | `testersfrielabs@googlegroups.com` |

---

## בנייה — Release AAB

```bash
cd /Users/hyh/Documents/translate-sub/app  # (או הנתיב הנכון לפרויקט)
JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home ./gradlew bundleRelease
```

**פלט:** `app/build/outputs/bundle/release/app-stable-release.aab`

### בעיות שנתקלנו

#### בעיה 1: "Unable to locate a Java Runtime"
- **סיבה:** JDK לא בנתיב ברירת מחדל
- **פתרון:** `JAVA_HOME=/Users/hyh/.local/jdk21/zulu-21.jdk/Contents/Home ./gradlew bundleRelease`

#### בעיה 2: קובץ ה-AAB חתום במפתח שגוי
- **שגיאה:** "קובץ ה-Android App Bundle חתום במפתח שגוי"
- **SHA1 צפוי:** `5B:EB:4C:3B:2E:E7:AB:E2:EC:31:35:49:04:1F:95:23:15:4E:A3:59`
- **פתרון:** לוודא ש-`gradle.properties` או `key.properties` מצביע לקובץ ה-keystore הנכון עם ה-alias הנכון

#### בעיה 3: versionCode כבר בשימוש
- **שגיאה:** "כבר נעשה שימוש בקוד הגרסה X, יש לנסות קוד גרסה אחר"
- **פתרון:** להגדיל את `versionCode` ב-`build.gradle` לפני בנייה מחדש

---

## העלאה ל-Play Console — Closed Testing (Alpha)

### קישור ישיר לדף
```
https://play.google.com/console/u/0/developers/6556694888792594977/app/4972254507162504539/tracks/4699340228558431350/releases/1/prepare
```

### צעדים
1. **נווט לדף לעיל** (Closed Testing → Alpha track)
2. **העלה AAB** — לחץ "העלאה" ובחר את ה-AAB, **אל תגרור** (אין אזור גרירה אמיתי)
3. **מלא "נתוני גרסה"** בפורמט:
   ```
   <en-US>
   Bug fixes and performance improvements.
   </en-US>
   ```
   ⚠️ חשוב: שורות נפרדות לפני ואחרי התוכן, אחרת שגיאת validation
4. לחץ **"הבא"** → מגיע לדף review

---

## שגיאות בדף Review

### שגיאה: FOREGROUND_SERVICE_DATA_SYNC
- **השירות:** `TranslationForegroundService` עם `foregroundServiceType="dataSync"`
- **פתרון:** מלא את טופס ההצהרה:
  - נווט: `app-content/foreground-services?releaseId=X&trackId=X`
  - סמן: "עיבוד מקומי" → "יבוא, ייצוא" (מתאים כי האפליקציה מורידה ומייבאת כתוביות)
  - **חובה:** קישור YouTube לסרטון המדגים את שימוש האפליקציה בשירות הרקע
  - לאחר מילוי: לחץ "שמירה" → חזור לדף הגרסה

### אזהרה: (צפויה אחרי פתרון השגיאה)
- אזהרות בדרך כלל לא מונעות שחרור לבדיקות סגורות

---

## לאחר שחרור Closed Testing

### דרישות לגישה ל-Production
- **12+ בודקים** שנתנו הסכמה (opted-in) ב-Closed Testing
- **14 יום** של בדיקות פעילות
- לשתף עם בודקים: לינק ה-opt-in מהקונסול (לא לינק הגוגל גרופ)

### Google Group
- שם: `testersfrielabs@googlegroups.com`
- הגדרות: open join (כל אחד יכול להצטרף)
- הוסף לרשימת הבודקים ב-Internal Testing ← "Subty Testers"

---

## Internal Testing

- הוסיפו את הגוגל גרופ לרשימת הבודקים
- opt-in link: `https://play.google.com/apps/internaltest/4701543569363115155`
- **חשוב:** "0 הביעו הסכמה" בדאשבורד = מד Closed Testing בלבד, לא Internal Testing

---

## סדר עדיפויות לפתרון בעיות

1. שגיאת חתימה → בדוק keystore + alias ב-gradle
2. versionCode כבר בשימוש → הגדל ב-`build.gradle`
3. FOREGROUND_SERVICE → מלא טופס + העלה סרטון YouTube
4. "0 הביעו הסכמה" → צריך Closed Testing, לא Internal Testing
