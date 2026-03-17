# AdMob Integration Guidelines (Version 2)

This document outlines the requirements and infrastructure needed to integrate Google Ads (AdMob) into the project for its second version.

## 1. Beta & App Readiness Restrictions
*   **Approval Process**: All apps must pass an **App Readiness Review** before serving real ads.
*   **Beta Tracks**:
    *   **Internal/Closed Testing**: Generally **not eligible** for review. Google's crawlers cannot access the app to verify content.
    *   **Open Beta**: Eligible for review once you link your Google Play Store listing in the AdMob console.
*   **Ad Serving**: Limited or no ads will be served until the review is complete.

## 2. Safety & Policy Compliance
*   **Test Ads (Critical)**: During development and beta testing, you **MUST** use [Google's Sample Ad Units](https://developers.google.com/admob/android/test-ads#sample_ad_units).
    *   **Danger**: Clicking your own real ads or serving them during testing can lead to immediate account suspension for "Invalid Traffic".
*   **Invalid Impressions**: Ensure you don't refresh ads too quickly or place them where they can be clicked accidentally.

## 3. Infrastructure Requirements
*   **App ID**: You can create your AdMob App ID (e.g., `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`) before the app is on the store.
    *   Add this to your `AndroidManifest.xml`.
*   **app-ads.txt**:
    *   You **must** have a developer website (domain).
    *   Host a file at `yourdomain.com/app-ads.txt` containing your publisher ID. This is mandatory for approval.
*   **Verification (Sept 2026)**: Be aware that mandatory Android developer verification is rolling out. Ensure your developer account information is up to date.

## 4. SDK Configuration (Kotlin)
*   **Min SDK**: 23+
*   **Library**: `com.google.android.gms:play-services-ads:23.x.x`
*   **Initialization**: Initialize the SDK once at app launch:
    ```kotlin
    MobileAds.initialize(this) {}
    ```

## 5. Placeholders in .env
Added placeholders for future IDs in `.env`.
