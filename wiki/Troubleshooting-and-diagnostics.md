# Troubleshooting and diagnostics

The app is designed to produce more useful reports than “app bad”.

## What the status means

- **Field error:** malformed address, port, or UDP target; fixed immediately before connecting.
- **Connecting:** valid input; opening the iperf3 control connection.
- **Connected:** a valid iperf3 exchange started. Progress, live rate, and intervals follow.
- **Complete:** every requested stage returned a valid final result.
- **Failed:** the sequence stopped at the named stage; completed earlier results are retained.

Every throughput/quality button automatically performs the server-detection preflight first. A reachable non-iperf service does not count as a successful check.

## Sharing a failure

1. Read the short reason.
2. Press **Copy diagnostics**.
3. Review the server and device fields.
4. Paste it into a [GitHub issue](https://github.com/Anders0lesen/free-iperf3-client/issues/new) or support chat.

The report contains UTC time, app/engine/Android versions, device and CPU architecture, failed stage, server, UDP target, completed results, raw iperf3 output, error, and stack trace. It is generated locally and never uploaded automatically.

## Common failures

- **Invalid address:** correct the highlighted server field.
- **Connection refused:** the host answered, but iperf3 is not listening on that port.
- **Timeout/unreachable:** check Wi-Fi/VLAN isolation, firewall, Tailscale, Docker state, and port mapping.
- **Not an iperf3 response:** another service may be using the port.
- **No bidirectional result:** update the server's iperf3 version.
- **UDP Poor/Fair while TCP is fast:** packet loss, jitter, or inability to sustain the selected UDP target can still hurt real-time streaming.

Confirm the Docker container is running and publishes both `5201/tcp` and `5201/udp`. Keep it on a trusted private network.
