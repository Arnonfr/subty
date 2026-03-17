# Subty — Launch Guide

## Prerequisites
- Android Studio Ladybug (2024.2.1) or later
- JDK 17+
- Android device or emulator (API 26+)

## 1. Clone & Configure

```bash
git clone <repo-url>
cd translate-sub
```

Create `local.properties` in the root (already in `.gitignore`):
```
sdk.dir=/path/to/Android/sdk
OPENSUBTITLES_API_KEY=<your key>
GEMINI_API_KEY=<your key>
GOOGLE_TRANSLATE_API_KEY=<your key>
```

## 2. Build

```bash
./gradlew assembleDebug
```

Or open in Android Studio and click **Run**.

## 3. First Launch

The app is ready to use immediately — no user setup required.
All API keys are baked into the build via `BuildConfig`.

## 4. User-facing Settings (in-app)

| Setting | Default | Description |
|---------|---------|-------------|
| Show posters | On | Movie cover art in results |
| Compact results | Off | Denser results list |
| Default target language | עברית | Language to translate into |
| Translation model | Gemini Flash | Flash = fast/cheap, Pro = quality |
| Auto-translate on download | Off | Starts translation right after download |
| Preferred save format | SRT | SRT / VTT / ASS |
| Auto-save translated | Off | Saves to Downloads automatically |

## 5. Key API Limits

| Service | Free Tier | Notes |
|---------|-----------|-------|
| OpenSubtitles | 5 downloads/day | Upgrade to VIP ($3/mo) for production |
| Google Translate | 500K chars/month free | ~300 movies/month for free |
| Gemini Flash | $0 until quota, then ~$0.008/movie | Cheapest quality option |

## 6. Release Build

1. Create a signing keystore:
   ```bash
   keytool -genkey -v -keystore subty-release.jks -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add to `local.properties`:
   ```
   KEYSTORE_PATH=../subty-release.jks
   KEYSTORE_PASS=<password>
   KEY_ALIAS=subty
   KEY_PASS=<password>
   ```

3. Build:
   ```bash
   ./gradlew assembleRelease
   ```

## 7. Architecture Overview

```
Search → OpenSubtitles API
          ↓
Results → Download (SRT/VTT/ASS file)
          ↓ (optional)
Translate → Google Translate API (default, free)
            Gemini Flash API (premium option)
          ↓
Save → Local file (user picks folder via SAF)
     → History (Room DB)
```

## 8. Source Language Detection

Subtitles from OpenSubtitles carry a `language_code` field.
This is passed through the nav graph into `TranslateViewModel.downloadAndLoad(fileId, languageCode)`
and shown as read-only "Detected language" — the user never inputs it manually.
