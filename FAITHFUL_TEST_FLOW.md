# Faithful Test Flow (1:1 עם האפליקציה האמיתית)

אם המטרה היא נאמנות מלאה למקור, צריך להריץ את אפליקציית האנדרואיד עצמה (`app/`) על אמולטור.
`preview/index.html` הוא רק דמו ולא משקף 1:1 את ה־Compose UI/Behavior.

## הרצה בפקודה אחת

```bash
/Users/hyh/Documents/translate-sub/run-faithful-test.sh
```

מה הפקודה עושה:
1. בודקת SDK/JDK
2. מתקינה (אם חסר) רכיבי אמולטור + system image
3. יוצרת AVD אם לא קיים
4. בונה `assembleDebug`
5. מעלה אמולטור
6. מתקינה APK
7. פותחת את האפליקציה (`com.subtranslate`)

## למה זה 1:1

- זה אותו קוד אמיתי של האפליקציה (Jetpack Compose)
- אותו Navigation / ViewModel / BuildConfig
- אותה התנהגות ריצה כמו מכשיר אנדרואיד

## טיפים

- לוגים:
```bash
adb logcat | rg subtranslate
```

- התקנה מהירה אחרי שינוי קוד:
```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```
