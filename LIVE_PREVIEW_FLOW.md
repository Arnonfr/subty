# Live Preview Flow (No Android Studio / No Phone)

המטרה: לבדוק את האפליקציה בלייב בדפדפן, כולל רענון אוטומטי בשמירת קבצים.

## 1) הפעלה מהירה (לחיצה אחת)

הרץ:

```bash
/Users/hyh/Documents/translate-sub/preview/live-preview.sh
```

מה זה עושה:
- מעלה שרת לוקאלי על `http://localhost:7890`
- פותח אוטומטית את הדפדפן
- מפעיל **Live Reload**: כל שמירה של `preview/index.html`, `preview/server.js`, `preview/*.css|*.js` מרעננת את המסך

## 2) עבודה בלייב

1. פתח את האפליקציה בדפדפן.
2. ערוך את:
- `/Users/hyh/Documents/translate-sub/preview/index.html`
- `/Users/hyh/Documents/translate-sub/preview/server.js`
3. שמור קובץ ותראה רענון אוטומטי.

## 3) עצירה

בטרמינל שבו רץ השרת:

```bash
Ctrl + C
```

## אופציונלי: פורט אחר

```bash
PORT=7895 /Users/hyh/Documents/translate-sub/preview/live-preview.sh
```

## בדיקת בריאות (Health)

```bash
curl http://localhost:7890/__health
```

אמור להחזיר JSON עם `ok: true`.
