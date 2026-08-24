# Architecture

Free iperf3 Client deliberately has a small surface area:

- One Kotlin/AppCompat activity builds the UI programmatically.
- One APK exposes ordinary Android and Leanback launcher entries.
- iperf3 3.21 executables are bundled for Android ARM and x86 architectures.
- The app starts the selected executable with `--json-stream --forceflush` and parses each event as it arrives.
- Every measurement is preceded by a tiny `-n 1` iperf3 exchange.
- TCP upload, reverse/download, and `--bidir` provide the three TCP modes.
- UDP reverse/download and upload run at the selected `-b` target; final receiver values provide rate, loss, jitter, and packet count.
- Per-test watchdogs and activity teardown terminate stuck or abandoned native processes.

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

There is no account, ad/analytics SDK, telemetry, WebView, dynamic code loading, automatic crash upload, stored history, or background service. The only platform permission is `INTERNET`; application backup and debugger attachment are disabled. Optional library startup/profile hooks are removed. The app is client-only and never listens for incoming connections.

See `PRIVACY.md`, `SECURITY.md`, and `docs/AUDITS.md` in the main repository.
