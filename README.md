# Free iperf3 Client

<img src="docs/assets/app-icon.png" width="160" alt="Free iperf3 Client icon">

A deliberately simple iperf3 client for Android phones, tablets, Android TV, and Google TV.

[Download the latest APK](https://github.com/Anders0lesen/free-iperf3-client/releases/latest) · [Documentation](wiki/Home.md) · [Report a problem](https://github.com/Anders0lesen/free-iperf3-client/issues)

## Version 0.2.0

- Checks that the endpoint is a responding iperf3 server before every test.
- Measures TCP download, upload, and simultaneous bidirectional throughput.
- Tests UDP streaming quality in both directions at a configurable target rate.
- Gives each UDP direction a clearly labelled 0–100 heuristic score based on delivered rate, packet loss, and jitter.
- **Run all tests** performs the complete sequence.
- Shows connection state, the exact command, overall progress, live rates, and per-second intervals while testing.
- Copies complete results on success or detailed diagnostics on failure.
- Opens this repository from the GitHub logo in the top-right corner.

The default UDP target is `50 Mbit/s`, which is a useful starting point for local game-streaming checks. Set it to the bitrate you actually want the network to sustain.

## No ads, tracking, or accounts

Official builds of this project will not contain ads. There is no advertising SDK, analytics, telemetry, account, login, or automatic crash upload.

The app requests only Android's `INTERNET` permission. It does not request storage, file, media, location, notification, microphone, camera, or background-service access. It stores no test history and sends data only to the server entered by the user. See [PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md).

## Install

Download the APK from the [latest GitHub release](https://github.com/Anders0lesen/free-iperf3-client/releases/latest). The same APK supports Android 9 and newer on phones, tablets, Android TV, and Google TV.

Android will ask you to allow installation from the browser or file manager used to open the APK. An early v0.1 development build may need to be uninstalled once before installing v0.2 because the original builds did not yet have durable release signing.

## Use

1. Start a reachable iperf3 server, for example `iperf3 -s`.
2. Enter its IP address or hostname; leave port `5201` unless you changed it.
3. For UDP, enter the target rate you want to test in Mbit/s.
4. Choose one test or **Run all tests**.

Input errors are shown immediately. For valid input, the app first performs a small real iperf3 exchange. A throughput test starts only after that preflight succeeds.

Android/Google TV is remote-control navigable. Use the on-screen keyboard for the fields, dismiss it, then move through the buttons with the D-pad.

## Failure reports

Failures show **Copy diagnostics** and **Show details**. The report includes versions, device architecture, test settings, the command, raw iperf3 output, error, and stack trace. Nothing is uploaded automatically.

The report contains the server address and device model, so review it before posting publicly. Repository documentation and release assets never include private test addresses or personal screenshots.

## Run an iperf3 server with Docker

The app needs a reachable [iperf3 server](https://github.com/esnet/iperf). This Compose file uses the [networkstatic/iperf3 image](https://hub.docker.com/r/networkstatic/iperf3):

```yaml
name: iperf3

services:
  iperf3:
    container_name: iperf3_server
    image: networkstatic/iperf3:latest
    command: ["-s"]
    ports:
      - "5201:5201/tcp"
      - "5201:5201/udp"
    restart: unless-stopped
```

Start it with `docker compose up -d`, then enter the Docker host's LAN, DNS, or Tailscale address. Both mappings are required for the app's TCP and UDP tests. Keep the server on a trusted LAN or private network rather than exposing it to the public internet. More detail is in [Docker iperf3 server](wiki/Docker-iperf3-server.md).

## Build

```powershell
.\gradlew.bat clean assembleDebug lintDebug
```

The local APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Pushes to `main` run the same build and publish the versioned GitHub release asset.

Implementation decisions, acceptance evidence, and the development history are kept in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) and the versioned [project wiki](wiki/Home.md).

## Licensing

The application source is released under The Unlicense. It bundles iperf3 3.21 Android binaries built by [android-iperf3](https://github.com/davidBar-On/android-iperf3) from the BSD-3-Clause [ESnet iperf3 source](https://github.com/esnet/iperf). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
