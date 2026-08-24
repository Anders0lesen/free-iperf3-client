# Troubleshooting and diagnostics

The app is designed to produce more useful reports than “app bad”.

## When a test fails

1. Read the short reason shown under **TEST FAILED**.
2. Press **Copy diagnostics**.
3. Paste the report into a [GitHub issue](https://github.com/Anders0lesen/free-iperf3-client/issues/new) or the chat where you are getting help.
4. Use **Show details** if you want to inspect and select the report inside the app.

The report includes:

- UTC time
- app and bundled iperf3 versions
- Android version and API level
- device manufacturer/model and CPU architectures
- test direction, duration, server, and port
- error type, message, and stack trace

Nothing is uploaded automatically. The report is created locally and only copied to Android's clipboard when you press the button. It contains the server address and device model, so review it before posting publicly. The report itself includes this privacy reminder.

## Common failures

- **Connection refused:** the address is reachable, but no iperf3 server is listening on that port.
- **Timed out:** check the address, firewall, Wi-Fi/VLAN isolation, Tailscale connection, and Docker port mapping.
- **Different result directions:** download and upload use opposite network directions and may legitimately differ.

## Quick server checks

Confirm the container is running, its logs say it is listening, and TCP port `5201` is published on the Docker host. Then try an ordinary desktop client before comparing Android results.
