# Architecture

Free iperf3 Client deliberately has a small surface area:

- One Kotlin/AppCompat activity builds the UI programmatically.
- One APK declares both ordinary Android and Leanback launcher entries.
- The app bundles iperf3 3.21 executables for Android ARM and x86 architectures.
- A test runs the bundled executable with JSON output for 10 seconds.
- Upload reads `end.sum_sent`; reverse/download reads `end.sum_received`.
- A 20-second outer timeout prevents a stuck process from leaving the UI busy forever.

## Native engine provenance

The binaries come from [davidBar-On/android-iperf3](https://github.com/davidBar-On/android-iperf3), built from the official [ESnet iperf3](https://github.com/esnet/iperf) source. The exact commit, licenses, architectures, and SHA-256 checksums are recorded in `THIRD_PARTY_NOTICES.md` in the main repository.

## Privacy

There is no account, analytics SDK, advertising SDK, telemetry, or automatic crash upload. The only network process is the user-requested iperf3 test to the user-entered endpoint.

## Build and release

The repository includes its Gradle wrapper. GitHub Actions builds the debug APK for each main-branch push. A `v*` tag additionally publishes that APK on a GitHub release.
