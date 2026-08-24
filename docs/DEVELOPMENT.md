# Development notes

This file records the important implementation decisions behind Free iperf3 Client. User-facing guidance lives in the project wiki.

## 2026-08-24 — v0.1.0

- Started with a minimal Kotlin/AppCompat application for Android phones and Android/Google TV.
- Kept the same activity and APK for touch and D-pad devices. The manifest exposes both regular launcher and Leanback launcher entries and does not require a touchscreen.
- Replaced `com.synaptic-tools:iperf:1.0.0`. Its old Android integration pulled obsolete transitive dependencies and did not interoperate reliably with the test server.
- Bundled iperf3 3.21 Android executables from `davidBar-On/android-iperf3` for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`. Exact provenance and checksums are in `THIRD_PARTY_NOTICES.md`.
- Run iperf3 as an app-local process with JSON output. Upload reads `end.sum_sent`; reverse/download reads `end.sum_received`.
- Added a 20-second process timeout around each 10-second test.
- Added a failure view with a friendly message, selectable details, and one-tap clipboard diagnostics.
- Added the user-supplied blue speedometer artwork as the Android launcher icon and project branding.
- Added a Gradle wrapper and GitHub Actions workflow. Main-branch pushes create an Actions artifact; `v*` tags also create a GitHub release asset.

## Test evidence for v0.1.0

- Clean `assembleDebug` and `lintDebug` completed successfully with JDK 17 and Android SDK 35.
- Phone test: Android API 35 x86_64 emulator connected to a private LAN server; upload and reverse/download both completed and displayed results.
- TV test: Google TV API 36 x86_64 emulator resolved the Leanback launcher, accepted D-pad navigation, connected to the same private server, and displayed a result.
- Failure-path test: an unreachable local port displayed the error controls and generated a copyable diagnostic report.

Emulator throughput values are deliberately not recorded as performance benchmarks. They only prove that the complete app-to-server flow works.
