package com.freeiperf3client.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal data class DiscoveryProgress(
    val checked: Int,
    val total: Int,
    val openPorts: Int,
    val verifying: Boolean = false,
)

internal data class DiscoveryNetwork(
    val localAddress: String,
    val prefixLength: Int,
    val candidates: List<String>,
)

internal data class DiscoveredServer(
    val hostname: String,
    val port: Int,
    val result: TestResult,
)

internal class ServerDiscovery(
    context: Context,
    private val engine: IperfEngine,
) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    suspend fun discover(
        port: Int,
        extraCandidates: List<String> = emptyList(),
        onProgress: (DiscoveryProgress) -> Unit,
    ): List<DiscoveredServer> = coroutineScope {
        val network = activeNetwork()
            ?: throw IllegalStateException("No active local IPv4 network is available")
        val candidates = (extraCandidates + network.candidates)
            .map { it.trim().removeSurrounding("[", "]") }
            .filter(::isValidServerName)
            .distinct()
        if (candidates.isEmpty()) {
            throw IllegalStateException("The active network has no addresses to scan")
        }

        val checked = AtomicInteger(0)
        val openCount = AtomicInteger(0)
        val semaphore = Semaphore(MAX_PARALLEL_CONNECTIONS)
        val openHosts = candidates.map { hostname ->
            async(Dispatchers.IO) {
                val open = semaphore.withPermit { isPortOpen(hostname, port) }
                val completed = checked.incrementAndGet()
                if (open) openCount.incrementAndGet()
                if (open || completed == candidates.size || completed % 8 == 0) {
                    onProgress(DiscoveryProgress(completed, candidates.size, openCount.get()))
                }
                hostname.takeIf { open }
            }
        }.awaitAll().filterNotNull()

        if (openHosts.isEmpty()) return@coroutineScope emptyList()
        val verificationHosts = openHosts.take(MAX_VERIFICATION_CANDIDATES)
        onProgress(DiscoveryProgress(candidates.size, candidates.size, verificationHosts.size, verifying = true))
        val verified = mutableListOf<DiscoveredServer>()
        verificationHosts.forEach { hostname ->
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    engine.execute(
                        TestConfig(hostname, port, durationSeconds = 1, udpTargetMbps = 50),
                        TestMode.DETECT,
                    ) { }
                }
            }.getOrNull()
            if (result != null) verified += DiscoveredServer(hostname, port, result)
        }
        verified
    }

    @Suppress("DEPRECATION") // Needed to prefer the underlying Wi-Fi/Ethernet network when a VPN is active.
    internal fun activeNetwork(): DiscoveryNetwork? {
        val active = connectivityManager.activeNetwork
        val preferredNetworks = connectivityManager.allNetworks.sortedBy { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 0
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> 1
                network == active -> 2
                else -> 3
            }
        }
        val address = preferredNetworks.firstNotNullOfOrNull { network ->
            connectivityManager.getLinkProperties(network)?.linkAddresses?.firstOrNull {
                it.address is Inet4Address &&
                    !it.address.isLoopbackAddress &&
                    !it.address.isLinkLocalAddress &&
                    it.prefixLength in 1..30
            }
        } ?: return null
        val ipv4 = address.address as Inet4Address
        return DiscoveryNetwork(
            localAddress = ipv4.hostAddress ?: return null,
            prefixLength = address.prefixLength,
            candidates = subnetCandidates(ipv4.hostAddress ?: return null, address.prefixLength),
        )
    }

    private fun isPortOpen(hostname: String, port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(hostname, port), CONNECT_TIMEOUT_MS)
        }
        true
    }.getOrDefault(false)

    private companion object {
        const val CONNECT_TIMEOUT_MS = 350
        const val MAX_PARALLEL_CONNECTIONS = 32
        const val MAX_VERIFICATION_CANDIDATES = 8
    }
}

internal fun subnetCandidates(localAddress: String, prefixLength: Int): List<String> {
    val octets = localAddress.split('.').mapNotNull(String::toIntOrNull)
    require(octets.size == 4 && octets.all { it in 0..255 }) { "Invalid IPv4 address" }
    val address = octets.fold(0L) { value, octet -> (value shl 8) or octet.toLong() }
    val scanPrefix = prefixLength.coerceIn(24, 30)
    val mask = (0xFFFF_FFFFL shl (32 - scanPrefix)) and 0xFFFF_FFFFL
    val network = address and mask
    val broadcast = network or (mask.inv() and 0xFFFF_FFFFL)
    return ((network + 1) until broadcast)
        .asSequence()
        .filter { it != address }
        .map(::ipv4String)
        .toList()
}

private fun ipv4String(value: Long): String = listOf(24, 16, 8, 0)
    .joinToString(".") { shift -> ((value shr shift) and 0xFF).toString() }
