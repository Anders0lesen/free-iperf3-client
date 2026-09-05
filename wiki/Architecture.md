# Architecture

Free iperf3 Client deliberately has a small surface area:

- One Kotlin/Jetpack Compose activity renders a dark, responsive phone/TV interface.
- Native Canvas line graphics follow Tabler's 24×24, 2-pixel-stroke geometry; no runtime icon library or emoji controls are used.
- One APK exposes ordinary Android and Leanback launcher entries.
- iperf3 3.21 executables are bundled for Android ARM and x86 architectures.
- The app starts the selected executable with `--json-stream --forceflush` and parses each event as it arrives.
- Every discovery or test sequence first uses Android's connectivity APIs to identify a usable Ethernet/Wi-Fi IPv4 network and verify TCP and UDP socket creation. Every measurement is then preceded by a tiny `-n 1` iperf3 exchange.
- User-triggered discovery enumerates at most the selected Wi-Fi/Ethernet LAN `/24`, sends Java probes through that Android `Network` with 32 bounded workers, and verifies at most eight open candidates through a real iperf3 exchange. Native iperf3 processes do not force a source-address bind; Android's normal routing owns their control and data sockets on phone, tablet, TV, VPN, and Tailscale paths.
- Up to eight verified endpoints are stored in app-private preferences for the recent-server picker; they are excluded from backup and removable in the UI.
- TCP upload, reverse/download, and `--bidir` provide the three TCP modes.
- UDP reverse/download and upload run at the selected `-b` target; final receiver values provide rate, loss, jitter, and packet count.
- Per-test watchdogs and final activity teardown terminate stuck or abandoned native processes. Orientation and screen-size changes are handled without recreating the activity, so an active native test continues.
- TV configuration rows use focusable editor dialogs so D-pad navigation never becomes trapped in an inline text cursor.
- Width-aware Compose layouts place configuration/test controls and result/chart panels side by side on TV while retaining the single-column phone flow.
- ZXing Core creates result QR images in memory. The selected summary and command are encoded directly, so the app never needs a QR service, file export, or local HTTP listener.

No shell is used. Validated values are individual `ProcessBuilder` arguments, so a host string cannot become a command.

## UDP score

The 0–100 score is intentionally transparent:

- Up to 40 points removed when received rate is below 95% of target
- 20 points removed per percentage point of packet loss, capped at 80
- 0.8 points removed per millisecond of jitter above 5 ms, capped at 20

Grades are Excellent (90+), Good (75+), Fair (50+), and Poor (below 50).

## Native engine provenance

The binaries come from [android-iperf3](https://github.com/davidBar-On/android-iperf3), built from official [ESnet iperf3](https://github.com/esnet/iperf) source. The exact source commit, licenses, architectures, and SHA-256 checksums are in `THIRD_PARTY_NOTICES.md`.

## Privacy and security

There is no account, ad/analytics SDK, telemetry, WebView, dynamic code loading, automatic crash upload, result history, or background service. The only platform permissions are `INTERNET` and prompt-free `ACCESS_NETWORK_STATE`; application backup/device transfer and debugger attachment are disabled. Optional library startup/profile hooks are removed. The app is client-only and never listens for incoming connections. Privacy-safe reports redact the endpoint and device model before reaching the clipboard/share sheet.

See `PRIVACY.md`, `SECURITY.md`, and `docs/AUDITS.md` in the main repository.
