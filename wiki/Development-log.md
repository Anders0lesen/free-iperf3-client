# Development log

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
