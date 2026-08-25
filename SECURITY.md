# Security

## Supported version

Security fixes are applied to the latest release only.

## Scope

The app is an on-demand iperf3 **client**. It never starts a server or listens for incoming connections. It has no account, privileged API, background service, result history, ads, analytics, or telemetry.

The only declared platform permissions are `INTERNET` and the normal, prompt-free `ACCESS_NETWORK_STATE`. Application backup/device transfer and debugger attachment are disabled. Optional AndroidX startup/profile provider and receiver hooks are removed, leaving the launcher activity as the only application component.

Version 0.3 is a sideload-oriented development release signed by the clean GitHub Actions build, not yet by a durable private production key. Verify that downloads come from this repository's release page. A future build with a different signing key may require uninstalling the older app first.

Server input is syntax-checked before use. Arguments are passed directly to `ProcessBuilder`; no shell is involved, so user input is not interpreted as a command. Every requested measurement is preceded by a bounded real iperf3 exchange, and the native process is terminated on timeout or when the activity is destroyed.

Local discovery runs only after a user press. It is bounded to at most 254 addresses in the device's current IPv4 subnet, uses short TCP connect timeouts with limited concurrency, verifies at most eight open candidates, and accepts a device as a result only after a real iperf3 exchange. Previously successful off-subnet entries are checked first. Manual entry remains available for other VLANs, VPNs, and routed networks.

Result QR export uses ZXing entirely in process and in memory. It starts no listener, writes no file, contacts no QR service, and adds no Android permission. The QR deliberately omits raw native output but includes the selected server address and should be treated as private test data.

iperf3 traffic is intentionally unencrypted. Use it only against a server you trust on a LAN, VPN, or private network such as Tailscale. Do not expose a standing iperf3 server directly to the public internet.

## Reporting a vulnerability

Open a GitHub security advisory or issue without including private addresses, credentials, or device identifiers. For ordinary failures, use **Copy privacy-safe diagnostics**; use the full diagnostic action only in a trusted private conversation.

The v0.3.1 audit record is in [docs/AUDITS.md](docs/AUDITS.md).
