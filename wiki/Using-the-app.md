# Using the app

You need a reachable [iperf3](https://github.com/esnet/iperf) server.

1. Enter the server IP address or hostname.
2. Leave port `5201` unless the server uses another port.
3. Enter the UDP target rate in Mbit/s. For game streaming, use the bitrate the stream needs to sustain; `50` is a useful starting point.
4. Choose a single test or **Run all tests**.

Malformed input is rejected immediately. Before any measurement, the app performs a small real iperf3 control-and-data exchange. It proceeds only if the endpoint answers as iperf3.

## Tests

- **Check / detect:** confirms that iperf3 is responding, without running a full throughput test.
- **TCP download:** server to Android device for 10 seconds.
- **TCP upload:** Android device to server for 10 seconds.
- **TCP bidirectional:** simultaneous upload and download for 10 seconds.
- **UDP quality:** five seconds in each direction at the selected target rate.
- **Run all:** detection plus all TCP and UDP stages, about 41 seconds.

## Live feedback

During a run the app shows:

- Connecting or connected state and sequence stage
- The exact iperf3 command
- Overall progress
- Current upload/download rate
- UDP jitter and loss when reported
- A growing per-second interval table with transfer and bitrate

Successful results and intervals can be copied. Failures produce a separate copyable diagnostic report.

## UDP score

Each direction receives a 0–100 app heuristic and an **Excellent**, **Good**, **Fair**, or **Poor** grade. The score penalizes:

- Received bitrate below 95% of the selected target
- Packet loss (weighted most heavily)
- Jitter above 5 ms

This is a practical comparison aid, not an industry certification. Always keep the received rate, loss, jitter, and per-second intervals when diagnosing Steam Link or another real-time stream.

## TV remote navigation

Select the server field and type with the on-screen keyboard. Dismiss the keyboard, then use the D-pad through port, UDP target, test buttons, and report actions.

Example server: `192.0.2.10` (documentation-only address; replace it with a reachable LAN, DNS, or Tailscale endpoint).
