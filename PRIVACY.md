# Privacy

Free iperf3 Client is designed to work without collecting personal information.

- No account or login
- No ads or advertising identifiers
- No analytics, telemetry, or tracking
- No automatic crash or diagnostic upload
- No storage, media, location, notification, microphone, camera, or background-service permission
- No saved result or performance history

The app requests Android's `INTERNET` and `ACCESS_NETWORK_STATE` permissions. They allow on-demand network tests and reading the active network's local addressing; neither permission reveals location or produces a runtime prompt.

When the user presses **Find iperf3 servers**, the app scans at most the current local IPv4 `/24` and any previously successful server addresses on the selected port. It performs a real iperf3 verification for at most eight open candidates and lists only servers that pass it. The scan is never automatic and does not contact a project-operated service.

The app keeps up to eight successfully verified server hostnames/IP addresses, ports, and last-used timestamps in app-private preferences. This list never leaves the device, is excluded from backup/device transfer, and can be removed one entry at a time or with **Clear all**. Results, throughput, raw output, and diagnostics are not saved.

Copy/share actions run only after the user presses them. The default **privacy-safe** result and diagnostic actions replace the entered server with `<redacted-server>` and omit the device manufacturer/model. A separately labelled full-copy action retains those details for private troubleshooting. Reports stay on the Android clipboard/share sheet and are never uploaded by the app.

**Show result QR for phone** creates a QR image locally in memory. It contains the selected result and exact command, including the entered server address, but excludes raw iperf output. Nothing is uploaded and the app does not open a local web server. Treat the QR like the full-copy action and scan it only with a device you trust.

The GitHub repository contains no private test IP addresses, personal screenshots, account data, or telemetry output. Documentation uses the reserved example address `192.0.2.10` when an address is needed.
