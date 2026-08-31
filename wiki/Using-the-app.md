# Using the app

You need a reachable [iperf3](https://github.com/esnet/iperf) server.

1. Leave port `5201` unless the server uses another port.
2. Press **Find iperf3 servers** to scan the current LAN, choose a recent server, or enter an address manually.
3. Choose the duration in seconds.
4. Enter the UDP target rate in Mbit/s. For game streaming, use the bitrate the stream needs to sustain; `50` is a useful starting point.
5. Choose a single test or **Run all tests**.

Malformed input is rejected immediately. Before discovery or measurement, the app identifies the active Android network, records its transport/address/prefix/gateway, and verifies that TCP and UDP sockets can be created. Before a measurement it then performs a small real iperf3 control-and-data exchange. It proceeds only if both preflights pass.

## Tests

- **Find iperf3 servers:** scans the current local IPv4 network and verifies open candidates with iperf3. It does not require a server address first.
- **TCP download:** server to Android device for the selected duration.
- **TCP upload:** Android device to server for the selected duration.
- **TCP bidirectional:** simultaneous upload and download for the selected duration.
- **UDP quality:** the selected duration in each direction at the target rate.
- **Run all:** detection plus all three TCP and both UDP stages. At the 10-second default, allow about 51 seconds plus connection setup.

## Live feedback

During a run the app shows:

- Connecting or connected state and sequence stage
- The exact iperf3 command
- Overall progress
- Current upload/download rate
- UDP jitter and loss when reported
- A growing per-second interval table with transfer and bitrate

Successful results and intervals can be copied. Failures produce a separate copyable diagnostic report.

Successfully verified endpoints appear under **Recent servers**. Tap one to reuse it, use the remove button for one entry, or press **Clear all**. Up to eight endpoints are kept locally; result history is not saved.

Rotating a phone or tablet reflows the interface without cancelling or resetting an active test.

The default copy/share actions are privacy-safe: they redact the server and device identity. Full technical copies remain available through a clearly labelled action for private troubleshooting.

## UDP score

Each direction receives a 0–100 app heuristic and an **Excellent**, **Good**, **Fair**, or **Poor** grade. The score penalizes:

- Received bitrate below 95% of the selected target
- Packet loss (weighted most heavily)
- Jitter above 5 ms

This is a practical comparison aid, not an industry certification. Always keep the received rate, loss, jitter, and per-second intervals when diagnosing Steam Link or another real-time stream.

## TV remote navigation

Wide TV screens use side-by-side configuration/test panels while running, then place the result summary and statistics beside the large chart and interval table. The screen remains vertically scrollable for the command and export actions.

On a successful result, choose **Show result QR for phone**. The QR is generated entirely inside the app and carries the selected test's summary and command, including the entered server address but excluding raw iperf output. The app does not upload the report, contact a QR website, or start a temporary web server.

Use the D-pad to highlight a configuration row and press Select to open its editor. Press **Done**, then continue through the test and report cards. Focused controls have a teal outline and the page scrolls with focus.

Example server: `192.0.2.10` (documentation-only address; replace it with a reachable LAN, DNS, or Tailscale endpoint).
