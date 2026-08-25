# Privacy

Free iperf3 Client is designed to work without collecting personal information.

- No account or login
- No ads or advertising identifiers
- No analytics, telemetry, or tracking
- No automatic crash or diagnostic upload
- No storage, media, location, notification, microphone, camera, or background-service permission
- No saved test history

The app requests only Android's `INTERNET` permission. When the user starts a test, the bundled iperf3 client connects directly to the user-entered server and port. The app does not contact a project-operated service.

Copy/share actions run only after the user presses them. The default **privacy-safe** result and diagnostic actions replace the entered server with `<redacted-server>` and omit the device manufacturer/model. A separately labelled full-copy action retains those details for private troubleshooting. Reports stay on the Android clipboard/share sheet and are never uploaded by the app.

The GitHub repository contains no private test IP addresses, personal screenshots, account data, or telemetry output. Documentation uses the reserved example address `192.0.2.10` when an address is needed.
