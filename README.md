# Free iperf3 Client

<img src="docs/assets/app-icon.png" width="160" alt="Free iperf3 Client icon">

A deliberately simple, free, ad-free iperf3 client for Android phones, tablets, and Android/Google TV.

[Download the latest APK](https://github.com/Anders0lesen/free-iperf3-client/releases/latest) · [Wiki](wiki/Home.md) · [Report a problem](https://github.com/Anders0lesen/free-iperf3-client/issues)

## What it does

Enter an iperf3 server IP/hostname and port, then run a 10-second TCP upload or reverse/download test. The result is shown in Mbit/s or Gbit/s.

No accounts. No ads. No analytics. No tracking.

If a test fails, the app shows a readable reason plus **Copy diagnostics** and **Show details** buttons. The copied report includes the app/Android versions, device, CPU architecture, test settings, error, and stack trace so a useful bug report is one tap away.

## Install

Download the APK from the [latest GitHub release](https://github.com/Anders0lesen/free-iperf3-client/releases/latest). The same APK supports Android phones, tablets, Android TV, and Google TV running Android 9 or newer.

Android will ask you to allow installation from the browser or file manager you use to open the APK.

## Use

1. Start an iperf3 server, for example `iperf3 -s`.
2. Enter its IP address or hostname in the app.
3. Leave port `5201` unless the server uses a different port.
4. Choose **Test download** or **Test upload**.

Android/Google TV is fully remote-control navigable. Select the server field, enter the address with the on-screen keyboard, dismiss the keyboard, then move to the test buttons with the D-pad.

## Troubleshooting

When a test fails, press **Copy diagnostics** and paste the result into a [GitHub issue](https://github.com/Anders0lesen/free-iperf3-client/issues/new) or the chat where you are getting help. The report does not collect or send anything automatically. It only goes to the Android clipboard when you press the button.

## Run an iperf3 server with Docker

The app needs a reachable [iperf3 server](https://github.com/esnet/iperf). This Compose file runs one on the standard TCP and UDP port using the [networkstatic/iperf3 image](https://hub.docker.com/r/networkstatic/iperf3):

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

Start it with `docker compose up -d`, then enter the Docker host's LAN or Tailscale IP in the app. TCP port `5201` is all this app needs; UDP is exposed for other iperf3 clients. See the [Docker server wiki page](wiki/Docker-iperf3-server.md) for verification and security notes.

## Build

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Pushes to `main` also produce a downloadable GitHub Actions artifact, while `v*` tags publish a GitHub release asset.

Implementation notes, test evidence, and the development history are kept in the [project wiki](wiki/Home.md). The pages are versioned in this repository and the build workflow also publishes them to GitHub's native Wiki whenever that repository feature is enabled.

## Licensing

The application source in this repository is released under The Unlicense. It bundles iperf3 3.21 Android binaries built by the [android-iperf3 project](https://github.com/davidBar-On/android-iperf3) from the BSD-3-Clause iperf3 source. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
