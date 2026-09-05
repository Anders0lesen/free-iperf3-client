package com.freeiperf3client.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.DatagramSocket
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
    val link: Network,
    val transport: String,
    val gateway: String?,
)

internal data class NetworkInfo(
    val transport: String,
    val localAddress: String,
    val prefixLength: Int,
    val gateway: String?,
) {
    val summary: String
        get() = "$transport · $localAddress/$prefixLength"
}

internal class NetworkAccessFailure(
    message: String,
    val technicalDetails: String,
    val networkInfo: NetworkInfo? = null,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

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
        onNetworkReady: (NetworkInfo) -> Unit = {},
        onProgress: (DiscoveryProgress) -> Unit,
    ): List<DiscoveredServer> = coroutineScope {
        val network = withContext(Dispatchers.IO) { checkedNetwork() }
        onNetworkReady(network.info())
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
                val bindToLan = isIpv4InSubnet(hostname, network.localAddress, network.prefixLength)
                val open = semaphore.withPermit {
                    isPortOpen(hostname, port, network.link.takeIf { bindToLan })
                }
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

    suspend fun preflightNetwork(): NetworkInfo =
        withContext(Dispatchers.IO) { checkedNetwork().info() }

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
        for (network in preferredNetworks) {
            val linkProperties = connectivityManager.getLinkProperties(network) ?: continue
            val linkAddress = linkProperties.linkAddresses.firstOrNull {
                it.address is Inet4Address &&
                    !it.address.isLoopbackAddress &&
                    !it.address.isLinkLocalAddress &&
                    it.prefixLength in 1..30
            } ?: continue
            val host = (linkAddress.address as Inet4Address).hostAddress ?: continue
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val transport = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
                else -> "Network"
            }
            return DiscoveryNetwork(
                localAddress = host,
                prefixLength = linkAddress.prefixLength,
                candidates = subnetCandidates(host, linkAddress.prefixLength),
                link = network,
                transport = transport,
                gateway = (
                    linkProperties.routes.firstOrNull {
                        it.isDefaultRoute && it.gateway is Inet4Address
                    } ?: linkProperties.routes.firstOrNull { it.isDefaultRoute }
                )?.gateway?.hostAddress,
            )
        }
        return null
    }

    private fun checkedNetwork(): DiscoveryNetwork {
        val network = activeNetwork() ?: throw NetworkAccessFailure(
            message = "No usable Ethernet or Wi-Fi IPv4 network is available",
            technicalDetails = "ConnectivityManager did not report an active local IPv4 network",
        )
        try {
            network.link.socketFactory.createSocket().use { socket ->
                socket.bind(InetSocketAddress(0))
            }
            DatagramSocket(null).use { socket ->
                network.link.bindSocket(socket)
                socket.bind(InetSocketAddress(0))
            }
        } catch (error: Exception) {
            throw NetworkAccessFailure(
                message = "The app could not create a network connection",
                technicalDetails = "${error.javaClass.simpleName}: ${error.message ?: "socket creation failed"}",
                networkInfo = network.info(),
                cause = error,
            )
        }
        return network
    }

    private fun isPortOpen(hostname: String, port: Int, link: Network?): Boolean = runCatching {
        // Subnet scan probes are explicitly bound to the LAN network. Saved hostnames and
        // off-subnet endpoints intentionally use Android's normal routing path.
        (link?.socketFactory?.createSocket() ?: Socket()).use {
            it.connect(InetSocketAddress(hostname, port), CONNECT_TIMEOUT_MS)
        }
        true
    }.getOrDefault(false)

    private companion object {
        const val CONNECT_TIMEOUT_MS = 600
        const val MAX_PARALLEL_CONNECTIONS = 32
        const val MAX_VERIFICATION_CANDIDATES = 8
    }
}

private fun DiscoveryNetwork.info(): NetworkInfo = NetworkInfo(
    transport = transport,
    localAddress = localAddress,
    prefixLength = prefixLength,
    gateway = gateway,
)

internal fun isIpv4InSubnet(address: String, localAddress: String, prefixLength: Int): Boolean {
    val target = ipv4ValueOrNull(address) ?: return false
    val local = ipv4ValueOrNull(localAddress) ?: return false
    val prefix = prefixLength.coerceIn(1, 32)
    val mask = (0xFFFF_FFFFL shl (32 - prefix)) and 0xFFFF_FFFFL
    return target and mask == local and mask
}

private fun ipv4ValueOrNull(address: String): Long? {
    val octets = address.split('.').map { it.toIntOrNull() }
    if (octets.size != 4 || octets.any { it == null || it !in 0..255 }) return null
    return octets.fold(0L) { value, octet -> (value shl 8) or octet!!.toLong() }
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
