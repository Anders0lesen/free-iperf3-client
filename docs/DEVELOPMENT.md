# Development notes

This file records the important implementation decisions behind Free iperf3 Client. User-facing guidance lives in the project wiki.

## 2026-08-31 — v0.3.6

- Diagnosed a physical Android 12 TV failure where native iperf3 3.21 reported `unable to create a new stream: Permission denied` immediately after the control exchange.
- Inspected the exact published v0.3.5 APK rather than relying on source configuration. Its merged manifest contains `INTERNET` and `ACCESS_NETWORK_STATE`, and its native process started and emitted valid JSON on the TV, ruling out a missing permission, missing ABI, extraction failure, or inability to execute the bundled engine.
- Identified the v0.3.5 native `-B <local-address>` addition as the regression boundary. Removed native source binding entirely so Android's normal routing owns every iperf3 data socket. Java LAN-discovery probes remain bound to the selected Android `Network` because that separate mechanism is required when multiple networks are active.
- Added a Java network preflight before discovery and every test sequence. It selects a usable Ethernet/Wi-Fi IPv4 network, records transport/address/prefix/gateway, and verifies TCP and UDP socket creation through that Android network.
- Added a dedicated `Network access` failure stage. A socket failure now stops before server validation and clearly states that the server has not been tested.
- Kept network details in full local diagnostics while redacting local addresses and gateways in the privacy-safe report.

## Test evidence for v0.3.6

- The same locally built APK completed a real TCP download on a phone emulator using Wi-Fi and a Google TV emulator using Ethernet.
- The displayed phone command and live TV command contained no `-B` argument.
- With the phone emulator's Wi-Fi and mobile data disabled, the sequence stopped at the distinct network-access stage and explicitly stated that the server had not been tested.
- The TV emulator first encountered a busy control socket when both emulators competed for the single-client server; retrying it alone completed successfully. This is retained here to avoid misclassifying a server-capacity condition as a platform networking regression.
- The physical Android 12 TV still requires owner verification with the published v0.3.6 APK; emulator success does not substitute for that final hardware acceptance test.
- Private endpoints, device identity, rates, screenshots, raw reports, and emulator dumps are intentionally omitted.

## 2026-08-29 — v0.3.5

- Bound discovery sockets to the selected Wi-Fi/Ethernet `Network.socketFactory`, preventing simultaneous mobile data or VPN routing from diverting local subnet probes.
- Removed the unbound-socket fallback so a failed network bind cannot silently reintroduce the original discovery fault.
- Added subnet-aware native binding: a numeric same-subnet destination receives iperf3 `-B <local-address>`, while hostnames and off-subnet destinations remain under Android's normal route selection.
- Added unit coverage for same-subnet, off-subnet, routed private, and hostname binding decisions.

## Test evidence for v0.3.5

- A clean `testDebugUnitTest lintDebug assembleDebug` passed with eight unit tests and no lint failure.
- On a fresh phone emulator with validated Wi-Fi and mobile networks connected simultaneously, a blank-address scan found and selected an iperf3 endpoint on the Wi-Fi subnet.
- The discovered same-subnet endpoint displayed the expected bound command and completed a real TCP download test.
- A manually entered off-subnet endpoint displayed no `-B` argument and completed through Android's normal route.
- Private endpoints, measured rates, commands, screenshots, emulator dumps, and raw output are intentionally omitted.

## 2026-08-27 — v0.3.4

- Split the Compose interface into focused theme, component, chart, home, running, result, and orchestration files while leaving the native iperf3 execution model intact.
- Back-ported the compact Claude Design artboards into the real Compose UI, including separate portrait and wide-screen compositions.
- Kept immediate validation, LAN discovery, recent servers, live progress/commands, rotation-safe sessions, privacy-safe sharing, and local QR export.
- Corrected two runtime-only layout regressions found during release review: wrapped/truncated narrow-phone actions and a collapsed wide-screen chart caused by applying the caller's size modifier to the inner canvas rather than the chart card.

## Test evidence for v0.3.4

- Fresh `clean testDebugUnitTest lintDebug assembleDebug` passed with seven unit tests and no lint failure.
- Phone API 35 completed a real TCP test, displayed live progress/chart/command data, retained the completed session through rotation, and preserved the verified server in the recent-server picker.
- Google TV API 36 launched through Leanback, accepted D-pad address editing and start navigation, completed a real TCP test, rendered the corrected two-column live/result charts, and opened the local result QR with D-pad input.
- Private endpoints, measured rates, screenshots, QR payloads, emulator dumps, and raw output are intentionally omitted.

## 2026-08-25 — v0.3.1

- Preserved the activity and its native iperf process across orientation/screen-size changes so an active run, selected endpoint, live samples, and final result survive rotation.
- Added user-triggered local IPv4 discovery: recent servers are checked first, the current Wi-Fi/Ethernet `/24` is probed with short bounded connections, at most eight open candidates receive a real iperf3 verification, and only verified servers are shown.
- Added an app-private recent-server picker capped at eight successful endpoints, with individual removal and **Clear all**. Backup and device transfer explicitly exclude all app data.
- Added purpose-built wide layouts for TV home, running, and result screens, while retaining the compact phone flow. Corrected bidirectional-unit and interval-table clipping.
- Added a D-pad-accessible QR result handoff. ZXing encodes the selected command and summary directly in memory; there is no QR service, file, local HTTP server, or added Android permission.
- Retained immediate input validation, mandatory real-iperf preflight, live progress, clear errors, privacy-safe copy/share defaults, and the permanent no-ads rule.

## Test evidence for v0.3.1

- Phone API 35: successful TCP test; process ID and live stage remained unchanged across a real portrait-to-landscape rotation; test completed on the result screen.
- Google TV API 35: Leanback launch, wide two-column home/running/result layouts, visible D-pad focus, editor dialog, focus-driven scrolling, real TCP bidirectional result, and fully visible QR dialog.
- Discovery candidate generation has unit coverage for a `/24` and for bounding a broader network to the device's local `/24`.
- Fresh `clean testDebugUnitTest lintDebug assembleDebug` passed. Private endpoints, measured rates, screenshots, QR payloads, and raw output are intentionally omitted.

## 2026-08-25 — v0.3.0

- Replaced the programmatic AppCompat view tree with a single dark Jetpack Compose UI that adapts from portrait phones to landscape TV.
- Built native Canvas line graphics from Tabler's 24×24/2-pixel-stroke geometry rather than emoji or a runtime icon package.
- Added selected test cards, editable duration, server-status card, prominent run state, live chart, result hero, min/average/max cards, expandable intervals, and command/raw-output views.
- Added privacy-safe copy/share paths that redact the endpoint and device model, while keeping an explicitly labelled full diagnostic for private troubleshooting.
- Separated endpoint validation from throughput settings so **Check server** is useful even when a duration or UDP target still needs editing.
- Preserved immediate malformed-input feedback, real-iperf preflight, bounded processes, and raw failure output.
- Added explicit Back-button cancellation/navigation and TV editor dialogs with visible D-pad focus.
- Added unit coverage for malformed IPv4, IPv6/hostname safety, endpoint-only validation, UDP scoring, and report redaction.

## Test evidence for v0.3.0

- Phone API 35: immediate malformed-address feedback; successful server check; TCP download; simultaneous TCP bidirectional; UDP download/upload with score/loss/jitter/packets; Run All; unreachable-server stop and privacy-safe diagnostics.
- Google TV API 35: Leanback launch, responsive landscape layout, D-pad traversal, automatic focus scrolling, and configuration editor dialog.
- Fresh `clean testDebugUnitTest lintDebug assembleDebug` passed. Private endpoints, measured rates, screenshots, and raw diagnostic output are intentionally omitted.

## 2026-08-25 — v0.2.0

- Kept one activity and one APK while adding server detection, TCP bidirectional, two-way UDP quality, and full-sequence orchestration.
- Every measurement is prefixed with a small `iperf3 -n 1` exchange. This verifies the iperf protocol rather than only checking whether a TCP port is open.
- Switched execution output to `--json-stream --forceflush`. Start events drive the connected state; interval events drive the live rate/progress/table; end events drive final summaries.
- UDP uses final receiver statistics. The app score is intentionally documented as a heuristic so loss/jitter/rate remain the authoritative measurements.
- Added immediate syntactic validation for IPv4, IPv6, DNS hostnames, port, and UDP target. ProcessBuilder receives an argument list; no shell is involved.
- The native process has a watchdog and is forcibly terminated when the activity is destroyed.
- Added successful-result copying and included exact commands/per-second intervals in the report.
- Added explicit privacy/security/no-ads policies and a versioned audit record.

## Test evidence for v0.2.0

- Phone API 35: server preflight; TCP bidirectional; UDP download/upload with score; full sequence; invalid-input and connection-failure paths.
- Google TV API 36: Leanback launcher resolution and D-pad navigation through the enlarged control set.
- Clean `assembleDebug` and `lintDebug` passed. The public release artifact was downloaded, inspected, installed, and smoke-tested.
- Private endpoint names, addresses, screenshots, and raw diagnostic output are intentionally omitted.

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
