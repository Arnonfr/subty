# גירסה 2.0 - תרגום כתוביות חינמי עם שרת

## מטרה
להטמיע באפליקציה תרגום כתוביות `SRT` בחינם באמת, בלי LLM ובלי API בתשלום (OpenAI/Gemini/DeepL/Microsoft).

הפתרון: שרת `LibreTranslate` פרטי (self-hosted), המבוסס על `Argos Translate`.

## למה עושים את זה
- אין עלות פר תרגום.
- אין תלות בספקי AI בתשלום.
- שליטה מלאה בפרטיות ובזמינות.

## מצב נוכחי בפרויקט (לפני 2.0)
- קיימים מנועים חינמיים: `MyMemory` + `LibreTranslate` ציבורי.
- קיימת מגבלת מוצר של `10` תרגומים לחודש.
- ברירת מחדל נוטה למודל בתשלום (`microsoft`).

קבצים מרכזיים:
- `app/src/main/java/com/subtranslate/data/remote/translation/LibreTranslateService.kt`
- `app/src/main/java/com/subtranslate/data/repository/TranslationRepositoryImpl.kt`
- `app/src/main/java/com/subtranslate/data/local/datastore/SettingsDataStore.kt`
- `app/src/main/java/com/subtranslate/presentation/translate/TranslateViewModel.kt`
- `app/src/main/java/com/subtranslate/presentation/settings/SettingsScreen.kt`

## דרישות מערכת
1. שרת Linux (VPS קטן מספיק להתחלה).
2. Docker + Docker Compose.
3. LibreTranslate container.
4. HTTPS דרך reverse proxy (Nginx/Caddy).
5. מודלי שפה של Argos לפי קהל יעד.
6. ניטור בסיסי + restart אוטומטי + לוגים.

## עלויות משוערות
1. בסיס ניסוי: `$6-$12` לחודש.
2. פרודקשן קטן יציב: `$12-$24` לחודש.
3. ללא עלות שימוש פר תרגום.

## ארכיטקטורה
`Android App` -> `Local Cache` -> `Our LibreTranslate Endpoint` -> `Argos Models` -> `Translated SRT`

## תכנית יישום

### שלב 1 - הקמת שרת
1. ליצור VPS עם Ubuntu 22/24.
2. להתקין Docker ו-Compose.
3. להרים LibreTranslate על פורט פנימי `5000`.
4. להגדיר HTTPS + דומיין (למשל `translate.<domain>`).
5. להגדיר `restart: always`.
6. לבצע בדיקת API ידנית עם `curl`.

### שלב 2 - מודלי שפה
להתחיל עם:
1. `en -> he`
2. `he -> en`

להרחיב לפי שימוש:
1. `ar -> he`
2. `ru -> he`
3. `es -> he`
4. `fr -> he`

### שלב 3 - שינויים באנדרואיד
1. להוסיף בהגדרות:
- `libretranslate_base_url`
- `libretranslate_api_key` (אופציונלי)
2. להפוך ברירת מחדל של מנוע ל-`libretranslate`.
3. לבטל תלות ב-fallback ציבורי לא אמין ככל האפשר.
4. לשפר retry/backoff לשגיאות רשת.
5. להציג שגיאת שרת ברורה למשתמש.
6. להסיר/להתאים מגבלת 10 תרגומים במסלול free.

### שלב 4 - Cache מקומי
להוסיף אחסון מקומי לפי מפתח:
`hash(source_text + source_lang + target_lang + engine)`

התנהגות:
1. לפני תרגום: לבדוק cache.
2. לתרגם רק misses.
3. אחרי תרגום: לשמור תוצאות.

### שלב 5 - Batch ויציבות פורמט
1. לתרגם רק את הטקסט של הכתוביות, לא timestamps.
2. לעבוד בבאצ'ים של `25-50` שורות.
3. לשמור mapping לפי `entry.index`.
4. להחזיר לתוך אותו מבנה `SRT` בלי לשבור פורמט.

### שלב 6 - אבטחה ותפעול
1. Rate limiting.
2. API key פנימי (header) אם נדרש.
3. Firewall בסיסי.
4. Timeout מוגדר.
5. `health` endpoint.
6. לוגים בלי טקסט מלא של כתוביות.

### שלב 7 - בדיקות קבלה
1. תרגום `en -> he` לקובץ קצר.
2. תרגום לקובץ ארוך (800+ שורות).
3. בדיקת tags (למשל `{\\an8}`).
4. בדיקת התנהגות בזמן נפילת שרת.
5. אימות cache (אין תרגום כפול).
6. אימות שמירת קובץ תקינה.

## סדר עבודה מומלץ ליום היישום
1. להרים קודם שרת בדיקה מקומי/זמני.
2. לחבר `base_url` באפליקציה.
3. לשנות ברירת מחדל ל-`libretranslate`.
4. להטמיע cache בסיסי.
5. להריץ בדיקות E2E על SRT אמיתי.
6. רק אחרי הצלחה, להעלות לסביבת שרת קבועה.

## הנחיה לסוכן עתידי
כאשר המשתמש כותב "בוא נטמיע תרגום עם שרת", יש לפעול לפי מסמך זה:
1. להתחיל בהקמת/בדיקת שרת LibreTranslate.
2. להטמיע endpoint מותאם באפליקציה.
3. לבצע שינויים בברירת מחדל, cache ומגבלת שימוש.
4. לא לשלב LLM בתהליך 2.0.
