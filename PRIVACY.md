# Privacy

Free iperf3 Client is designed to work without collecting personal information.

- No account or login
- No ads or advertising identifiers
- No analytics, telemetry, or tracking
- No automatic crash or diagnostic upload
- No storage, media, location, notification, microphone, camera, or background-service permission
- No saved test history

The app requests only Android's `INTERNET` permission. When the user starts a test, the bundled iperf3 client connects directly to the user-entered server and port. The app does not contact a project-operated service.

**Copy results** and **Copy diagnostics** place a local report on the Android clipboard only after the user presses the button. Reports include the entered server address. Diagnostics also include the device manufacturer/model, Android version, CPU architecture, errors, and raw iperf3 output. The app warns the user to review these fields before sharing publicly.

The GitHub repository contains no private test IP addresses, personal screenshots, account data, or telemetry output. Documentation uses the reserved example address `192.0.2.10` when an address is needed.
