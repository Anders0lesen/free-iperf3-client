# Audit record

## v0.3.6 — 2026-08-31

### Functional sanity check

- A clean `testDebugUnitTest lintDebug assembleDebug` passed with ten unit tests and no lint failure.
- The exact published v0.3.5 APK merged manifest was inspected: it contains `INTERNET`, `ACCESS_NETWORK_STATE`, and only Android's app-scoped non-exported dynamic-receiver permission. Four native iperf3 ABIs and one activity are packaged.
- The same locally built v0.3.6 APK completed a real TCP download on a phone emulator using Wi-Fi and a Google TV emulator using Ethernet. The displayed native command contains no source-address `-B` argument.
- The network preflight executed before server validation and captured the selected transport, local IPv4 prefix, and gateway. With Wi-Fi and mobile data deliberately disabled, the app stopped at `Network access failed`, stated that the server had not been tested, and did not start native iperf3.
- Physical Android 12 TV confirmation remains an owner acceptance step after the published APK is installed; emulator testing cannot claim that hardware result in advance.

Private endpoints, device identity, measured throughput, commands, raw reports, emulator dumps, and supplied photographs are intentionally omitted.

### Security audit

- The v0.3.5 and v0.3.6 packaged manifests both declare `INTERNET` and `ACCESS_NETWORK_STATE`. v0.3.6 retains `allowBackup=false`, native-library extraction, one activity, four supported native ABIs, and no app service/provider/receiver.
- Native iperf3 source-address binding was removed. Java discovery still uses Android's selected `Network`, while native control/data sockets use Android's normal routing on every form factor.
- Network preflight verifies TCP and UDP socket creation before server contact. Inputs remain validated and are passed as individual `ProcessBuilder` arguments without a shell; cancellation and watchdog handling remain in place.
- No ad, analytics, telemetry, account, WebView, dynamic-code, automatic-upload, file/storage, camera, location, microphone, notification, or background-service behavior was added.
- OSV Scanner 2.5.1 resolved and queried all 85 packaged debug-runtime Maven coordinates against the official OSV data on 2026-08-31: zero findings. A separate broad all-configuration pass found advisories only in non-packaged build tooling; those components are absent from the APK and have no runtime path in the app.
- All four bundled iperf3 3.21 SHA-256 hashes match `THIRD_PARTY_NOTICES.md`.
- Final local APK SHA-256 before CI release: `11458F743D2889993A521F2334D25691E22C88042EE8F05EA8EEE047E7004BFD` (GitHub Actions independently rebuilds and signs the release asset).
- The exact public `free-iperf3-client-v0.3.6.apk` release asset was downloaded after publication. It reports version `0.3.6`/code `7`, passes APK Signature Scheme v2 verification, packages both required network permissions and all four native ABIs, and has SHA-256 `216C9E108F5319FF86D990798D1CDB4721D56702CE8F645B5A28231DF542E5F3`.
- That same downloaded public APK was freshly installed on both emulators. A real phone/Wi-Fi TCP download and a sequential TV/Ethernet TCP download both completed successfully, and both live commands omitted `-B`.

### Personal-information audit

- Tracked text, filenames, diff, resources, workflow inputs, image inventory, and release inputs were checked for private endpoints, local paths, clipboard filenames, device identity, personal details, credentials, tokens, photographs, screenshots, emulator dumps, commands, rates, and diagnostics.
- The supplied TV photographs and all temporary UI dumps remain local and are excluded from source and release assets. Runtime test endpoints and device details are not recorded in the repository.
- Documentation uses only standards-reserved examples; the existing public GitHub repository identity is the only personal-style identifier.

## v0.3.5 — 2026-08-29

### Functional sanity check

- A clean `testDebugUnitTest lintDebug assembleDebug` passed with eight unit tests and no lint failure.
- A fresh phone emulator kept validated Wi-Fi and mobile networks connected simultaneously. With the server field blank, discovery found and selected an iperf3 endpoint on the Wi-Fi subnet.
- The discovered same-subnet endpoint showed the network-bound native command and completed a real TCP download test. A manually entered off-subnet endpoint showed no forced bind and also completed successfully.
- Discovery and native routing behavior were reviewed together so the fix does not break hostname, Tailscale, VPN, or other routed manual tests.

Private endpoints, measured throughput, commands, raw reports, emulator dumps, and screenshots are intentionally omitted.

### Security audit

- Discovery sockets are created by Android's selected Wi-Fi/Ethernet network socket factory. A failed bind now fails that candidate instead of falling back to an unbound socket.
- Native `-B` is limited to validated numeric addresses inside the selected LAN subnet; hostnames and off-subnet addresses remain on Android's normal routing path.
- The packaged manifest declares only `INTERNET`, prompt-free `ACCESS_NETWORK_STATE`, and Android's app-scoped non-exported dynamic-receiver permission. Backup and debugger attachment remain disabled.
- No ad, analytics, telemetry, account, WebView, dynamic-code, automatic-upload, file/storage, camera, location, microphone, notification, or background-service behavior was added.
- Inputs remain validated and passed as individual `ProcessBuilder` arguments without a shell; real-iperf preflight, cancellation, and watchdog handling remain in place.
- OSV Scanner 2.5.1 resolved and queried all 85 Maven runtime coordinates against the official OSV data on 2026-08-29: zero findings.
- All four bundled iperf3 3.21 SHA-256 hashes match `THIRD_PARTY_NOTICES.md`.
- Final local APK SHA-256 before CI release: `47011CB9A6794F8C16BD4B34A758FDE48156A065D002A1602C10402215566CEE` (GitHub Actions independently rebuilds and signs the release asset).

### Personal-information audit

- Tracked text, filenames, diff, resources, workflow inputs, image inventory, and generated APK inputs were checked for private endpoints, local paths, clipboard filenames, personal details, credentials, tokens, screenshots, and diagnostics.
- Test-only endpoint data, UI dumps, relay code, commands, rates, and screenshots are excluded from the repository and release.
- Documentation uses only standards-reserved example addresses; the existing public GitHub repository identity is the only personal-style identifier.

## v0.3.4 — 2026-08-27

### Functional sanity check

- Fresh `clean testDebugUnitTest lintDebug assembleDebug` passed with seven unit tests and no lint failure.
- Phone API 35 completed a real TCP test with live state, chart, and command feedback; the result survived portrait-to-landscape rotation and the verified endpoint remained available in recent servers after reinstall.
- Google TV API 36 launched through Leanback, accepted D-pad editor/start navigation, completed a real TCP test, and displayed the wide live/result charts and local result QR without clipping.
- Runtime review found and fixed narrow-phone action wrapping and a collapsed wide-screen chart before release.

Private endpoints, measured throughput, QR payloads, raw reports, emulator dumps, and screenshots are intentionally omitted.

### Security audit

- The packaged manifest declares only `INTERNET`, prompt-free `ACCESS_NETWORK_STATE`, and Android's app-scoped non-exported dynamic-receiver permission. Backup and debugger attachment remain disabled; there is no service or app receiver.
- The refactor adds no ad, analytics, telemetry, account, WebView, dynamic-code, automatic-upload, file/storage, camera, location, microphone, notification, or background-service behavior.
- Input remains validated and passed to `ProcessBuilder` without a shell; all measurements still require a real iperf3 preflight and retain process cancellation/watchdog handling.
- OSV Scanner 2.5.1 resolved and queried all 85 Maven runtime coordinates against the official OSV data on 2026-08-27: zero findings.
- All four bundled iperf3 3.21 SHA-256 hashes still match `THIRD_PARTY_NOTICES.md`.
- Final local APK SHA-256 before CI release: `8A690450414F5091B1458C973A366ED401DA239F7FB4F085F067FA64F393EA8D` (GitHub Actions independently rebuilds and signs the release asset).

### Personal-information audit

- Scanned tracked text, filenames, diff, resources, workflow inputs, image inventory, and generated APK inputs for private endpoints, Tailscale addresses, local Windows paths, clipboard filenames, personal names/details, emails, credentials, tokens, screenshots, and diagnostic output.
- Replaced private-range placeholder addresses in the design artboards with the RFC 5737 documentation address `192.0.2.10`; no real test endpoint, throughput figure, QR payload, or emulator artifact is tracked.
- The existing public GitHub repository identity is the only personal-style identifier. The tracked images are non-personal app artwork only.

## v0.3.1 — 2026-08-25

### Functional sanity check

- Fresh `clean testDebugUnitTest lintDebug assembleDebug` passed with seven unit tests and no lint failure.
- Phone API 35 completed a real TCP test. During the run, the emulator rotated from portrait to landscape: the app process ID remained unchanged, the same test/stage stayed live, and the run completed successfully.
- Google TV API 35 completed a real simultaneous TCP bidirectional test. Leanback launch, two-column home/running/result layouts, D-pad focus and scrolling, editor dialog, chart/statistics/details, and the local QR dialog were visually checked at 1920×1080.
- The QR dialog was corrected after the first TV pass exposed vertical overflow; the final title, QR, disclosure, and focused close action all fit on screen.
- LAN discovery progress/no-result handling was exercised without an entered address; candidate generation, `/24` boundaries, network/broadcast exclusion, and broader-network capping have unit coverage.

Private endpoints, measured throughput, QR payloads, raw reports, emulator dumps, and screenshots are intentionally omitted.

### Security audit

- Packaged manifest declares only `INTERNET`, prompt-free `ACCESS_NETWORK_STATE`, and Android's app-scoped non-exported dynamic-receiver permission. The APK contains one activity and no service, receiver, or provider; debugger attachment and app backup/device transfer are disabled.
- There is no ad, analytics, telemetry, account, WebView, dynamic-code, automatic upload, storage, camera, location, microphone, notification, or background-service feature.
- Manual input is validated and passed to `ProcessBuilder` without a shell. Every measurement is gated by a real iperf3 preflight and all native processes have cancellation/watchdog handling.
- Discovery is explicit, limited to the local `/24`, uses 32 short bounded probes, and performs sequential real-iperf verification for at most eight open candidates. Known recent endpoints are prioritized.
- QR export uses ZXing Core in memory, starts no listener, writes no file, calls no QR service, and adds no permission. Its disclosure states that the selected server address is included while raw output is excluded.
- All 85 resolved Maven runtime coordinates were queried against the official OSV batch API on 2026-08-25: zero findings.
- All four bundled iperf3 3.21 SHA-256 hashes match `THIRD_PARTY_NOTICES.md`.
- Final local APK SHA-256 before CI release: `DD1B8C86F8C3A419FD47FF019D1CF6F42930D83195A75578A803D428530FB5D5` (GitHub Actions independently rebuilds and signs the release asset).

### Personal-information audit

- Removed all local emulator XML dumps and screenshots before staging; none is tracked or included in the release.
- Scanned text, filenames, diff, resources, workflow inputs, and image inventory for private endpoints, Tailscale addresses, local Windows paths, clipboard filenames, personal names/details, emails, credentials, tokens, and diagnostic output.
- Repository addresses are limited to RFC 5737 documentation ranges, one deliberately malformed RFC 5737 test value, and loopback in a historical failure test. The existing public GitHub repository identity is the only personal-style identifier.
- The only tracked images are the supplied non-personal launcher artwork and its Android density variants. Runtime safe-copy tests continue to verify complete endpoint redaction.

## v0.3.0 — 2026-08-25

### Functional sanity check

- Fresh Android clean build, five unit tests, and lint pass.
- Phone API 35: malformed IPv4 rejected during entry; successful iperf3 server detection; TCP download; simultaneous bidirectional TCP; UDP download/upload with live rate, loss, jitter, packet count and score; complete Run All sequence.
- Unreachable valid endpoint stopped at the server-check stage and produced a short reason, retry action, technical details, privacy-safe copy, and full-copy option.
- Google TV API 35: Leanback launch, landscape scaling, D-pad traversal, focus-driven scrolling, and configuration editor dialog verified.
- Hardware Back returns from results and cancels active native work cleanly.

Private endpoints, measured throughput, raw reports, and emulator screenshots are intentionally omitted.

### Security audit

- Packaged-manifest review: `android.permission.INTERNET` is the only platform capability. Android's app-scoped non-exported dynamic-receiver permission is also generated; it grants no system capability.
- Only `MainActivity` is packaged. There are no services, providers, app receivers, ads, analytics, telemetry, WebView, dynamic code loading, remote configuration, or automatic crash upload.
- Backup and debugger attachment are disabled; input is validated and passed as a `ProcessBuilder` argument list without a shell.
- Every measurement is gated by an actual iperf3 exchange. Watchdogs, Cancel, and activity teardown terminate native processes.
- All 84 resolved Maven runtime packages queried against OSV on 2026-08-25: zero findings.
- All four bundled iperf3 3.21 SHA-256 hashes match `THIRD_PARTY_NOTICES.md`.
- Clean APK SHA-256 before CI release: `7169497DE9A8DACCE4FBDBA2A2A42B4592EB9CA45F96BB70920AB358B3B3BDCE` (the GitHub Actions build is independently produced and will have its own hash).

### Personal-information audit

- Scanned tracked text, filenames, diff, resources, documentation, workflow inputs, and image inventory.
- No private LAN/Tailscale endpoint, local Windows path, personal name/detail beyond the public GitHub repository identity, email, screenshot, clipboard dump, device report, token, credential, or diagnostic output is included.
- Documentation contains only RFC 5737 example addresses and loopback used in historical test notes.
- The user-supplied launcher artwork contains no personal information. No test screenshot is tracked or released.
- Runtime safe-copy actions redact every occurrence of the entered server and omit the device model; unit coverage verifies endpoint redaction.

## v0.2.0 — 2026-08-25

### Functional sanity check

- Clean Android build and lint pass.
- Real iperf3 preflight, TCP upload/download, simultaneous TCP bidirectional, UDP download/upload quality, and full-sequence tests against a private LAN server.
- Live command, connection state, progress, rates, jitter/loss, and per-second interval output verified during execution.
- Invalid address, refused connection, timeout, and copyable failure report paths checked.
- Phone API 35 and Google TV API 36 launch/navigation checked; exact published APK re-tested after download.

Private endpoints and emulator throughput figures are intentionally omitted.

### Security audit

- Manifest/APK review: `android.permission.INTERNET` is the only platform capability; no storage, location, advertising, notification, camera, microphone, account, or background-service access. AndroidX startup/profile hooks were removed, leaving only the launcher activity.
- Application backup and debugger attachment disabled.
- Published APK signature verified. The current GitHub Actions development certificate is suitable for sideload testing but is not a durable production update-signing identity; this limitation is documented for installers.
- No advertising, analytics, telemetry, WebView, dynamic code loading, or remote configuration dependency.
- User input is validated and passed as a `ProcessBuilder` argument list, never through a shell.
- Every measurement is gated by an actual iperf3 preflight; processes have bounded timeouts and are destroyed with the activity.
- All 47 resolved Maven runtime packages checked with OSV Scanner data on 2026-08-25: no known vulnerabilities reported.
- Bundled binary hashes rechecked against `THIRD_PARTY_NOTICES.md`.
- iperf3 3.21 source ancestry checked against current NVD records. It includes the bounded peer-JSON-length fix referenced by CVE-2026-71218. CVE-2026-71217 concerns server-mode parameter handling; this app does not start or expose server mode. Standing Docker servers should still be kept current and private.

### Personal-information audit

- Scanned tracked text, filenames, image metadata, Android resources, Git history diff, and release staging inputs.
- Confirmed no private test IP, Tailscale address, personal detail beyond the existing public GitHub account/repository identity, email, local Windows path, screenshot, clipboard capture, token, credential, or diagnostic dump is included.
- Documentation uses only the RFC 5737 documentation address `192.0.2.10`.
- The launcher artwork is the only user-supplied image retained; it contains no personal information.
- Runtime reports may contain the user-entered server and device model, are never uploaded automatically, and include an explicit review-before-sharing warning.
