# Google Play Store Materials - Subty

Updated: 2026-07-04

## App Details

- App name: `Subty: Subtitle Finder`
- Default language: `English (United States)`
- Package name: `com.subtranslate`
- Category suggestion: `Video Players & Editors` or `Tools`
- Contains ads: `No`
- In-app purchases: `No`
- Privacy policy file: `docs/privacy-policy.html`
- Privacy policy action: host this file on a public HTTPS URL before submission.

## English Listing

Short description:

```text
Find subtitles for movies and shows, then translate them when you need to.
```

Full description:

```text
Subty helps you find subtitles for movies and TV shows, download the right file, and translate subtitles when you need another language.

FIND THE RIGHT SUBTITLE
Search by title, pick a movie or show, and narrow TV results by season and episode. Subty searches multiple subtitle sources and shows the source of each result, so you can choose the file that fits your release.

BUILT FOR MOVIES AND SERIES
Recent titles stay on the Search screen, and TV shows can continue from the next episode with one tap. You can also change season and episode from the results screen without starting over.

DOWNLOAD AND SAVE
Download subtitle files directly to your device. Subty supports common subtitle formats including .srt, .vtt, .ass, .ssa, and .sub.

TRANSLATE WHEN NEEDED
Translate downloaded subtitles on your device. Download a language model once, then translate again without an internet connection.

KEY FEATURES
- Search movies and TV shows by title
- Select season and episode for series
- Continue to the next episode from recent searches
- Search multiple subtitle sources, including OpenSubtitles and SubDL
- Optional Hebrew-focused Screwzira source when enabled
- Filter results by language
- Download subtitle files to your device
- Translate subtitles in-app
- Keep search and download history
- Supports .srt, .vtt, .ass, .ssa, and .sub formats

Subty is built for people who watch across languages and want a fast, focused subtitle workflow without ads or clutter.
```

Release notes for version code `16`:

```text
Improved subtitle search reliability for recent TV shows and next-episode searches.
Added stronger multi-source results with OpenSubtitles, SubDL, and optional Screwzira support.
Improved duplicate handling and source labels in subtitle results.
Added on-device subtitle translation with downloadable language models.
Fixed several search, download, and translation flow issues found in phone QA.
```

## Hebrew Listing

App name:

```text
Subty - כתוביות ותרגום
```

Short description:

```text
מצאו כתוביות לסרטים ולסדרות ותרגמו אותן כשצריך.
```

Full description:

```text
Subty עוזרת למצוא כתוביות לסרטים ולסדרות, להוריד את הקובץ המתאים, ולתרגם כתוביות כשצריך שפה אחרת.

מציאת כתובית מתאימה
חפשו לפי שם הסרט או הסדרה, בחרו את הכותר הנכון, וסננו סדרות לפי עונה ופרק. Subty מחפשת בכמה מקורות כתוביות ומציגה את מקור התוצאה, כדי שתוכלו לבחור את הקובץ שמתאים לגרסה שלכם.

נבנתה לסרטים ולסדרות
כותרים אחרונים נשמרים במסך החיפוש, ובסדרות אפשר להמשיך לפרק הבא בלחיצה אחת. ניתן גם לשנות עונה ופרק ממסך התוצאות בלי להתחיל מחדש.

הורדה ושמירה
הורידו קבצי כתוביות ישירות למכשיר. Subty תומכת בפורמטים נפוצים, כולל .srt, .vtt, .ass, .ssa ו-.sub.

תרגום כשצריך
תרגמו כתוביות שהורדתם מתוך האפליקציה ישירות על המכשיר. הורידו פעם אחת את מודל השפה המתאים, ולאחר מכן אפשר לתרגם גם ללא חיבור לאינטרנט.

תכונות עיקריות
- חיפוש סרטים וסדרות לפי שם
- בחירת עונה ופרק לסדרות
- המשך לפרק הבא מתוך החיפושים האחרונים
- חיפוש בכמה מקורות כתוביות, כולל OpenSubtitles ו-SubDL
- מקור Screwzira ממוקד עברית כאשר הוא מופעל
- סינון תוצאות לפי שפה
- הורדת כתוביות למכשיר
- תרגום כתוביות בתוך האפליקציה
- היסטוריית חיפושים והורדות
- תמיכה בפורמטים .srt, .vtt, .ass, .ssa ו-.sub

Subty מיועדת למי שצופה בתוכן בכמה שפות ורוצה תהליך מהיר, ממוקד ונקי למציאת כתוביות.
```

## Screenshot Set

Store-ready screenshots prepared for Play Console (`1080x1920`):

- `fastlane/metadata/android/en-US/images/phoneScreenshots-store/01_search_home.png`
- `fastlane/metadata/android/en-US/images/phoneScreenshots-store/02_series_episode_picker.png`
- `fastlane/metadata/android/en-US/images/phoneScreenshots-store/03_subtitle_results_sources.png`

Feature graphic (`1024x500`):

- `fastlane/metadata/android/en-US/images/featureGraphic.png`

Suggested gallery order:

1. Search home - "Find subtitles fast"
2. Series episode picker - "Pick the exact episode"
3. Results with source labels and a secondary translation action - "Compare subtitle sources"

Raw uncropped screenshots are also preserved in `phoneScreenshots-current/`.
The cropped set is `1080x1920` (9:16), and the feature graphic is ready for
Play Console. Translation is deliberately not featured in the gallery because
it is an optional, on-device capability rather than the product's primary value.

## Data Safety Draft

Use this as a starting point in Play Console, then verify against the final build and every SDK/provider policy.

- Data processed/transmitted by app function: search query, title, language, season, episode, IMDB/TMDB identifiers sent to title/subtitle providers.
- Data processed/transmitted by app function: downloaded ML Kit language models are fetched from Google only when the user elects to translate.
- Data stored locally: settings, search history, download/translation history, and downloaded on-device language models.
- Encryption in transit: yes, app network calls use HTTPS endpoints.
- Account deletion: not applicable; no Subty user account.
- Ads: no ads.
- Data sharing: disclose third-party SDK/provider processing where Play Console asks about sharing/collection.

Recommended purposes:

- App functionality

## App Content Answers Draft

- App access: no login required for subtitle search or download. Translation is optional, runs on-device, and may first download a language model.
- Ads: no.
- Target audience: general audience, not specifically directed to children.
- Content rating: app is a subtitle utility. User searches may reference movies/shows, but the app itself does not provide video content.
- Sensitive permissions: no location, contacts, camera, microphone, SMS, or call log. Storage permissions are limited by SDK version for subtitle file compatibility. Foreground service is used for translation/data sync.
- Financial features: none.

## Current release boundaries

- Translation is offline-only through Google ML Kit after an optional language-model
  download. The release has no cloud translation provider, in-app purchase, credit,
  Firebase, Firestore, Remote Config, or hosted analytics path.
- Do not create Play Billing products or make purchase/credit claims for this release.
- Before production, host `docs/privacy-policy.html` at a public HTTPS URL and complete
  the Play Console App content and Data safety declarations against the final AAB.
