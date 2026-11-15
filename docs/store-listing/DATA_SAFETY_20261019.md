# Data safety update — 19 October 2026

Submitted successfully using the Android Publisher Data safety API. Receipt: `artifacts/release-20261019/data-safety-submit.json`. This records API acceptance; the public store may update after Google review.

The CSV declares collection of device identifiers, crash logs, diagnostics, app interactions and in-app search history. App interactions are optional under the in-app usage setting. Search queries are required for user-initiated provider searches, while optional telemetry of raw search terms remains separately opt-in. Device identifiers and diagnostics also account for the ML Kit SDK disclosure. The prior crash-log declaration is retained. Data is encrypted in transit; there is no developer data deletion interface and no app accounts. No subtitle dialogue, translated dialogue, ads, location, or user file contents are declared as collected. No sharing is declared under the service-provider / user-initiated-transfer definitions.

The public English privacy policy now distinguishes optional Subty operational events from ML Kit SDK diagnostics and provider searches. Its latest deployment commit is `47580d7d6efa15f5da27e8b9734654fc9db3db31`; HTTPS read-back confirmed the October 19 policy and SDK link.

Sources checked:
- https://developers.google.com/android-publisher/api-ref/rest/v3/applications/dataSafety
- https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
- https://developers.google.com/ml-kit/android-data-disclosure
- https://developers.google.com/ml-kit/terms

No credentials or user subtitle files are included in this declaration.
