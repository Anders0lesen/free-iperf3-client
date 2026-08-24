# Using the app

You need an iperf3 server that the Android device can reach. The official iperf3 project is at <https://github.com/esnet/iperf>.

1. Enter the server IP address or hostname.
2. Leave port `5201` unless the server uses a different port.
3. Choose **Test download** or **Test upload**.
4. Wait about 10 seconds for the throughput result.

## Direction matters

- **Download** uses iperf3 reverse mode: server to Android device.
- **Upload** sends from the Android device to the server.

Both tests use TCP. Results are displayed in bit/s, Kbit/s, Mbit/s, or Gbit/s.

## TV remote navigation

Select the server field and type with the on-screen keyboard. Dismiss the keyboard, then use the D-pad to move through port, download, upload, and any diagnostic buttons.

## Example

- Server: `192.0.2.10` (documentation-only example; replace it with your server address)
- Port: `5201`

The Tailscale address is intentionally not required by the app; enter whichever LAN, DNS, or Tailscale address is reachable from the current device.
