# Translation Model Update

Date: 2026-06-07

## What I changed

- Added `inclusionai/ling-2.6-flash` as a selectable OpenRouter translation model.
- Made `inclusionai/ling-2.6-flash` the default translation model for new users.
- Exposed it in both places where the app shows translation-model choices:
  - Settings screen
  - Translate screen
- Added a friendly display label for it: `Ling 2.6 Flash`.
- Updated the settings model mapper so saved values for Ling load correctly instead of falling back to the default model.
- Updated the settings helper text so it no longer says only Grok and Gemini are enabled.

## Files changed

- [/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/data/local/datastore/SettingsDataStore.kt](/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/data/local/datastore/SettingsDataStore.kt)
- [/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/presentation/settings/SettingsViewModel.kt](/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/presentation/settings/SettingsViewModel.kt)
- [/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/presentation/translate/TranslateViewModel.kt](/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/presentation/translate/TranslateViewModel.kt)
- [/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/presentation/settings/SettingsScreen.kt](/Users/hyh/Documents/translate-sub/app/src/main/java/com/subtranslate/presentation/settings/SettingsScreen.kt)

## Notes

- This change does not alter the OpenRouter request payload or batching logic.
- It only expands the model list and preserves existing saved models.
- Ling is now the default cheap option, while existing saved models continue to work.
