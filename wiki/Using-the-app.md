# Using the app

You need a reachable [iperf3](https://github.com/esnet/iperf) server.

1. Enter the server IP address or hostname.
2. Leave port `5201` unless the server uses another port.
3. Choose the duration in seconds.
4. Enter the UDP target rate in Mbit/s. For game streaming, use the bitrate the stream needs to sustain; `50` is a useful starting point.
5. Choose a single test or **Run all tests**.

Malformed input is rejected immediately. Before any measurement, the app performs a small real iperf3 control-and-data exchange. It proceeds only if the endpoint answers as iperf3.

## Tests

- **Check / detect:** confirms that iperf3 is responding, without running a full throughput test.
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

The default copy/share actions are privacy-safe: they redact the server and device identity. Full technical copies remain available through a clearly labelled action for private troubleshooting.

## UDP score

Each direction receives a 0–100 app heuristic and an **Excellent**, **Good**, **Fair**, or **Poor** grade. The score penalizes:

- Received bitrate below 95% of the selected target
- Packet loss (weighted most heavily)
- Jitter above 5 ms

This is a practical comparison aid, not an industry certification. Always keep the received rate, loss, jitter, and per-second intervals when diagnosing Steam Link or another real-time stream.

## TV remote navigation

Use the D-pad to highlight a configuration row and press Select to open its editor. Press **Done**, then continue through the test and report cards. Focused controls have a teal outline and the page scrolls with focus.

Example server: `192.0.2.10` (documentation-only address; replace it with a reachable LAN, DNS, or Tailscale endpoint).
