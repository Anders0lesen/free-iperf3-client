# Free iperf3 Client

A deliberately simple, free, ad-free iperf3 client for Android phones, tablets, and Android/Google TV.

## v0.1 goal

Enter an iperf3 server IP/hostname, press a button, and see the throughput. Both upload and reverse/download TCP tests are supported.

No accounts. No ads. No analytics. No tracking.

## Build

Pushes to `main` automatically build a debug APK in GitHub Actions. Open the latest **Build APK** workflow run and download the `free-iperf3-client-debug` artifact.

## Licensing

The application source in this repository is released under The Unlicense. Third-party dependencies retain their own licenses. The Android iperf integration currently uses `com.synaptic-tools:iperf:1.0.0` (Apache-2.0), which wraps iperf3. iperf3 itself is BSD-3-Clause licensed.
