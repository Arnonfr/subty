# Translate-Sub 🎬

פרויקט לחיפוש והורדת כתוביות לסרטים וסדרות.

## APIs & שירותים בשימוש

### 1. OpenSubtitles.com
השירות המרכזי למציאת כתוביות.
- **הרשמה**: [OpenSubtitles API Consumers](https://www.opensubtitles.com/en/consumers)
- **דוקומנטציה**: [OpenSubtitles.com API Docs](https://opensubtitles.stoplight.io/docs/opensubtitles-api/)
- **שיטות חיפוש**: חיפוש טקסטואלי, לפי שם קובץ, או לפי Hash (הכי מדויק).

### 2. TMDB (The Movie Database)
משמש להשלמה אוטומטית (Autocomplete) של שמות סרטים וחילוץ פוסטרים.
- **הרשמה**: [TMDB Settings - API](https://www.themoviedb.org/settings/api)
- **דוקומנטציה**: [TMDB API Reference](https://developer.themoviedb.org/reference/intro/getting-started)
- **תמונות**: `https://image.tmdb.org/t/p/w500/{poster_path}`

---

## הגדרות (Environment Variables)
יש לשכפל את הקובץ `.env.example` לקובץ חדש בשם `.env` ולמלא את ה-API Keys שלכם:
```bash
cp .env.example .env
```
**הקובץ `.env` מוחרג ב-git ולא יעלה לשרת הציבורי.**

## הרצה ראשונית
הפרויקט מבוסס Gradle (Android/Kotlin).
1. פתחו ב-Android Studio.
2. ודאו שה-SDK מותקן.
3. סנכרנו את ה-Gradle.

## בדיקה בלייב בלי Android Studio
אפשר להריץ פריוויו בדפדפן:

```bash
/Users/hyh/Documents/translate-sub/preview/live-preview.sh
```

- כתובת: `http://localhost:7890`
- כולל Live Reload בשמירת קבצים תחת `preview/`
- פירוט מלא: [LIVE_PREVIEW_FLOW.md](./LIVE_PREVIEW_FLOW.md)
