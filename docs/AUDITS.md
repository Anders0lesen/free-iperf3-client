# Audit record

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
