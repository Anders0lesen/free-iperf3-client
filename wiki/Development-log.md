# Development log

## 2026-08-25 — v0.3.1

- Kept active tests and entered/selected server state intact through device rotation.
- Added real LAN iperf3 discovery without requiring an address first, plus an eight-entry recent-server picker.
- Added wide two-column TV layouts for setup, live testing, charts, statistics, and interval details.
- Added a local, D-pad-accessible result QR for moving the selected command and summary to a phone without copying TV text.
- Fixed bidirectional result-unit and interval-text clipping and the Android 12+ system title-bar regression.
- Repeated clean build, unit, lint, real phone/TV, dependency, packaged-manifest, native-hash, security, and personal-information checks.

No private endpoint, throughput figure, QR payload, diagnostic output, emulator dump, or test screenshot is published.

## 2026-08-25 — v0.3.0

- Rebuilt the app as a polished dark-only Jetpack Compose interface for phone and TV.
- Added native Tabler-style line graphics, live charts, large result summaries, min/average/max cards, expandable intervals, and clearer stage feedback.
- Added configurable duration and D-pad-friendly TV editor dialogs with visible focus.
- Added privacy-safe copy/share actions that redact endpoint and device identity by default.
- Added hardware Back behavior for clean run cancellation and result navigation.
- Added validation/scoring/redaction unit tests and repeated the phone, TV, failure, security, dependency, and personal-information checks.

Private endpoints, throughput figures, raw reports, and test screenshots are deliberately not published.

## 2026-08-25 — v0.2.0

- Added immediate server/port/UDP-target validation and an automatic real-iperf3 preflight before every measurement.
- Added TCP simultaneous bidirectional testing.
- Added two-way UDP streaming-quality testing with configurable target rate, loss, jitter, packet count, and a documented 0–100 heuristic score.
- Added **Run all tests**.
- Added live connection state, exact command, overall progress, current rates, and a growing per-second interval table.
- Added copyable successful results while retaining detailed failure diagnostics.
- Added the top-right GitHub link.
- Added explicit no-ads, privacy, security, and audit documentation.
- Kept the permission surface to `INTERNET` only; disabled backup and debugger attachment.

### Acceptance and audit

- Real server check, TCP upload/download/bidirectional, UDP both directions, and full sequence completed on the phone test target.
- Google TV launch and D-pad navigation rechecked.
- Malformed address and unreachable/wrong-service failure paths rechecked.
- Clean build/lint passed and the exact public release APK was downloaded and re-tested.
- Runtime dependency, native-engine CVE applicability, manifest/APK, secret, and personal-information audits completed. Details are versioned in `docs/AUDITS.md`.

Private endpoints, throughput figures, and user screenshots are deliberately not published.

## 2026-08-24 — v0.1.0

- Created the minimal phone and Android/Google TV client UI.
- Added host and port input, TCP upload, TCP reverse/download, and formatted results.
- Replaced an obsolete third-party Android wrapper after it crashed on current Android and failed to communicate reliably with the NAS server.
- Integrated iperf3 3.21 executables for four Android CPU architectures.
- Added D-pad focus order, TV launcher support, a TV banner, and the supplied launcher icon.
- Added friendly failures, selectable details, and one-tap copyable diagnostics.
- Added the Gradle wrapper, clean build/lint checks, GitHub Actions artifacts, and tagged release publishing.
- Documented the working Docker/NAS server setup.

## Acceptance tests

- Phone, Android API 35 x86_64 emulator: upload and reverse/download completed against a private LAN server.
- Google TV, Android API 36 x86_64 emulator: Leanback launch, D-pad navigation, and a TCP result completed against the same server.
- Failure path: connection to `127.0.0.1:1` showed the error actions, expanded a full diagnostic report, copied it, and did not crash.
- Final clean `assembleDebug` and `lintDebug` passed.

The emulator throughput values are not performance benchmarks. The tests verify the complete app-to-server flow.
