# Troubleshooting and diagnostics

The app is designed to produce more useful reports than “app bad”.

## What the status means

- **Field error:** malformed address, port, or UDP target; fixed immediately before connecting.
- **Network access failed:** Android did not expose a usable local IPv4 network or the app could not prepare TCP/UDP sockets. No server check was attempted.
- **Connecting:** valid input; opening the iperf3 control connection.
- **Connected:** a valid iperf3 exchange started. Progress, live rate, and intervals follow.
- **Complete:** every requested stage returned a valid final result.
- **Failed:** the sequence stopped at the named stage; completed earlier results are retained.

Every throughput/quality button performs two preflights: Android network/socket access, then a real iperf3 server exchange. A reachable non-iperf service does not count as a successful check.

## Server discovery

**Find iperf3 servers** scans the current Wi-Fi/Ethernet IPv4 subnet on the selected port. It can also recheck saved recent servers, including routed or VPN endpoints. It may not discover a new server across VLANs, guest-Wi-Fi isolation, a VPN-only route, or a subnet larger than the local `/24`; enter that address manually once and it will join the recent list after a successful verification.

On devices with mobile data and Wi-Fi active together, v0.3.5 and newer explicitly send Java scan probes through Wi-Fi/Ethernet. Starting with v0.3.6, native iperf3 always uses Android's normal route; this avoids a source-bind incompatibility found on an Android 12 TV while preserving multi-network-safe discovery.

If the scan finds nothing, confirm that Android is on the expected LAN, the container publishes TCP port `5201`, and local client isolation is disabled. UDP-only availability is insufficient because iperf3 control begins over TCP.

## Sharing a failure

1. Read the short reason.
2. Press **Copy privacy-safe diagnostics**.
3. Use the full diagnostic copy only for a trusted private conversation.
4. Paste it into a [GitHub issue](https://github.com/Anders0lesen/free-iperf3-client/issues/new) or support chat.

The safe report contains UTC time, app/engine/Android versions, CPU architecture, network transport/prefix, failed stage, UDP target, completed results, redacted raw iperf3 output, error, and stack trace. The full local copy additionally retains the server, device model, local address, and gateway. Neither is uploaded automatically.

## Common failures

- **Invalid address:** correct the highlighted server field.
- **Network access failed:** confirm Ethernet/Wi-Fi is connected and enabled. If technical details say `Permission denied`, share the privacy-safe diagnostics; the server has not yet been blamed or tested.
- **Connection refused:** the host answered, but iperf3 is not listening on that port.
- **Timeout/unreachable:** check Wi-Fi/VLAN isolation, firewall, Tailscale, Docker state, and port mapping.
- **Not an iperf3 response:** another service may be using the port.
- **No bidirectional result:** update the server's iperf3 version.
- **UDP Poor/Fair while TCP is fast:** packet loss, jitter, or inability to sustain the selected UDP target can still hurt real-time streaming.

Confirm the Docker container is running and publishes both `5201/tcp` and `5201/udp`. Keep it on a trusted private network.
