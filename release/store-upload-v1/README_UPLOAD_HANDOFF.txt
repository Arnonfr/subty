STORE UPLOAD HANDOFF (single source of truth)

Folder:
/Users/hyh/Documents/translate-sub/release/store-upload-v1

Primary artifact to upload:
/Users/hyh/Documents/translate-sub/release/store-upload-v1/subty-stable-v1.12-no-translate.aab

App version in this artifact:
- versionName: 1.12
- versionCode: 15
- flavor/build: stableRelease

What is disabled in this build:
- Translation UI and navigation hidden for store v1
- Dark mode toggle removed; app forced to light mode
- BuildConfig keys not packaged in this build:
  - GEMINI_API_KEY
  - GOOGLE_TRANSLATE_API_KEY
  - MICROSOFT_API_KEY
  - DEEPL (disabled from available models)

Reference doc:
/Users/hyh/Documents/translate-sub/release/store-upload-v1/TEMP_DISABLED_FOR_STORE_V1.md

Notes for upload agent:
1) Use ONLY the AAB in this folder.
2) Do not rebuild from source unless explicitly requested.
3) If Play Console asks for release notes, use your prepared text for v1 no-translate release.
