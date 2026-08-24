# Docker iperf3 server

The Android app is a client. It needs a reachable [iperf3](https://github.com/esnet/iperf) server. The working NAS setup used during v0.1.0 testing runs the [networkstatic/iperf3 Docker image](https://hub.docker.com/r/networkstatic/iperf3).

## Docker Compose

```yaml
# iperf3 network throughput test server
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

Start and inspect it:

```shell
docker compose up -d
docker compose ps
docker compose logs iperf3
```

Free iperf3 Client v0.2 uses TCP `5201` for control/TCP tests and UDP `5201` for UDP quality tests, so publish both mappings.

## Check from another computer

```shell
iperf3 -c 192.0.2.10
iperf3 -c 192.0.2.10 -R
```

Replace the example address with the Docker host's LAN, DNS, or Tailscale address.

## Network and security notes

- The port mapping binds port `5201` on all host interfaces by default. Host firewall rules still apply.
- Do not expose port `5201` to the public internet. Keep it on a trusted LAN or private network such as Tailscale.
- Keep the image current: iperf3's server parses data from network peers, and server-side security fixes matter even when clients are simple.
- `restart: unless-stopped` keeps the server available after a NAS or Docker restart while allowing it to remain stopped when deliberately stopped.
- `latest` matches the tested NAS configuration. For reproducible long-term deployment, pin a reviewed image digest.

## Tested reference setup

- Container: `iperf3_server`
- Image: `networkstatic/iperf3:latest`
- Command: `iperf3 -s`
- Published: `5201/tcp` and `5201/udp` on IPv4 and IPv6
- Endpoint used by the app: the Docker host's private LAN address on port `5201`
- Restart policy: `unless-stopped`
