package com.freeiperf3client.app

import android.content.Context
import android.os.Build
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress
import java.time.Instant
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class ConfigValidation(
    val config: TestConfig? = null,
    val hostError: String? = null,
    val portError: String? = null,
    val durationError: String? = null,
    val udpError: String? = null,
) {
    val valid: Boolean get() = config != null
    val firstError: String?
        get() = hostError ?: portError ?: durationError ?: udpError
}

internal fun validateConfig(
    hostText: String,
    portText: String,
    durationText: String,
    udpText: String,
): ConfigValidation {
    val hostname = hostText.trim().removeSurrounding("[", "]")
    val port = portText.toIntOrNull()
    val duration = durationText.toIntOrNull()
    val udp = udpText.toIntOrNull()
    val hostError = when {
        hostname.isBlank() -> "Enter a server IP address or hostname"
        !isValidServerName(hostname) -> "That is not a valid IP address or hostname"
        else -> null
    }
    val portError = if (port == null || port !in 1..65535) {
        "Port must be between 1 and 65535"
    } else null
    val durationError = if (duration == null || duration !in 1..300) {
        "Duration must be between 1 and 300 seconds"
    } else null
    val udpError = if (udp == null || udp !in 1..10_000) {
        "UDP target must be between 1 and 10000 Mbit/s"
    } else null
    return if (listOf(hostError, portError, durationError, udpError).all { it == null }) {
        ConfigValidation(TestConfig(hostname, port!!, duration!!, udp!!))
    } else {
        ConfigValidation(
            hostError = hostError,
            portError = portError,
            durationError = durationError,
            udpError = udpError,
        )
    }
}

internal fun validateEndpoint(hostText: String, portText: String): ConfigValidation {
    val hostname = hostText.trim().removeSurrounding("[", "]")
    val port = portText.toIntOrNull()
    val hostError = when {
        hostname.isBlank() -> "Enter a server IP address or hostname"
        !isValidServerName(hostname) -> "That is not a valid IP address or hostname"
        else -> null
    }
    val portError = if (port == null || port !in 1..65535) {
        "Port must be between 1 and 65535"
    } else null
    return if (hostError == null && portError == null) {
        ConfigValidation(TestConfig(hostname, port!!, durationSeconds = 10, udpTargetMbps = 50))
    } else {
        ConfigValidation(hostError = hostError, portError = portError)
    }
}

internal fun isValidServerName(value: String): Boolean {
    if (value.length > 253 || value.any(Char::isWhitespace)) return false
    val ipv4Parts = value.split('.')
    if (ipv4Parts.size == 4 && ipv4Parts.all { part ->
            part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull() in 0..255
        }) return true
    if (value.contains(':') && runCatching {
            InetAddress.getByName(value) is Inet6Address
        }.getOrDefault(false)) return true
    if (value.all { it.isDigit() || it == '.' }) return false
    val ascii = runCatching { IDN.toASCII(value.removeSuffix(".")) }.getOrNull() ?: return false
    if (ascii.isBlank() || ascii.length > 253) return false
    return ascii.split('.').all { label ->
        label.length in 1..63 &&
            label.first().isLetterOrDigit() &&
            label.last().isLetterOrDigit() &&
            label.all { it.isLetterOrDigit() || it == '-' }
    }
}

internal fun modesFor(choice: TestChoice): List<TestMode> = when (choice) {
    TestChoice.TCP_DOWNLOAD -> listOf(TestMode.DETECT, TestMode.TCP_DOWNLOAD)
    TestChoice.TCP_UPLOAD -> listOf(TestMode.DETECT, TestMode.TCP_UPLOAD)
    TestChoice.TCP_BIDIRECTIONAL -> listOf(TestMode.DETECT, TestMode.TCP_BIDIRECTIONAL)
    TestChoice.UDP_QUALITY -> listOf(TestMode.DETECT, TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD)
    TestChoice.RUN_ALL -> listOf(
        TestMode.DETECT,
        TestMode.TCP_DOWNLOAD,
        TestMode.TCP_UPLOAD,
        TestMode.TCP_BIDIRECTIONAL,
        TestMode.UDP_DOWNLOAD,
        TestMode.UDP_UPLOAD,
    )
}

internal fun scoreUdp(test: TestResult, config: TestConfig): Pair<Int, String> {
    val receivedRate = test.downloadBitsPerSecond ?: test.uploadBitsPerSecond ?: 0.0
    val targetRate = config.udpTargetMbps * 1_000_000.0
    val deliveryRatio = if (targetRate > 0) receivedRate / targetRate else 1.0
    val ratePenalty = if (deliveryRatio >= 0.95) 0.0 else min(40.0, (0.95 - deliveryRatio) * 80.0)
    val lossPenalty = min(80.0, (test.lossPercent ?: 0.0) * 20.0)
    val jitterPenalty = min(20.0, max(0.0, (test.jitterMs ?: 0.0) - 5.0) * 0.8)
    val score = (100.0 - ratePenalty - lossPenalty - jitterPenalty)
        .roundToInt().coerceIn(0, 100)
    val grade = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Good"
        score >= 50 -> "Fair"
        else -> "Poor"
    }
    return score to grade
}

internal fun formatRate(bitsPerSecond: Double): String = when {
    bitsPerSecond >= 1_000_000_000 -> formatNumber(bitsPerSecond / 1_000_000_000, 2) + " Gbit/s"
    bitsPerSecond >= 1_000_000 -> formatNumber(bitsPerSecond / 1_000_000, 1) + " Mbit/s"
    bitsPerSecond >= 1_000 -> formatNumber(bitsPerSecond / 1_000, 1) + " Kbit/s"
    else -> formatNumber(bitsPerSecond, 0) + " bit/s"
}

internal fun formatRateParts(bitsPerSecond: Double): Pair<String, String> = when {
    bitsPerSecond >= 1_000_000_000 -> formatNumber(bitsPerSecond / 1_000_000_000, 2) to "Gbit/s"
    bitsPerSecond >= 1_000_000 -> formatNumber(bitsPerSecond / 1_000_000, 0) to "Mbit/s"
    bitsPerSecond >= 1_000 -> formatNumber(bitsPerSecond / 1_000, 0) to "Kbit/s"
    else -> formatNumber(bitsPerSecond, 0) to "bit/s"
}

internal fun formatBytes(bytes: Double): String = when {
    bytes >= 1_000_000_000 -> formatNumber(bytes / 1_000_000_000, 2) + " GB"
    bytes >= 1_000_000 -> formatNumber(bytes / 1_000_000, 1) + " MB"
    bytes >= 1_000 -> formatNumber(bytes / 1_000, 1) + " KB"
    else -> formatNumber(bytes, 0) + " B"
}

internal fun formatMilliseconds(value: Double): String = formatNumber(value, 2) + " ms"
internal fun formatPercent(value: Double): String = formatNumber(value, 2) + "%"
internal fun formatNumber(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)

internal fun resultRate(test: TestResult): Double =
    test.downloadBitsPerSecond ?: test.uploadBitsPerSecond ?: 0.0

internal fun resultSummary(test: TestResult, config: TestConfig): String = when (test.mode) {
    TestMode.DETECT -> buildString {
        appendLine("Server detected")
        test.connection?.let { appendLine("Connection: $it") }
        append("The iperf3 control and data exchange succeeded.")
    }
    TestMode.TCP_DOWNLOAD -> "TCP download: ${formatRate(test.downloadBitsPerSecond ?: 0.0)}"
    TestMode.TCP_UPLOAD -> "TCP upload: ${formatRate(test.uploadBitsPerSecond ?: 0.0)}"
    TestMode.TCP_BIDIRECTIONAL -> buildString {
        appendLine("TCP bidirectional")
        appendLine("Download: ${formatRate(test.downloadBitsPerSecond ?: 0.0)}")
        append("Upload: ${formatRate(test.uploadBitsPerSecond ?: 0.0)}")
    }
    TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD -> {
        val (score, grade) = scoreUdp(test, config)
        buildString {
            appendLine("${test.mode.title}: ${formatRate(resultRate(test))}")
            appendLine("Score: $score/100 ($grade)")
            appendLine("Target: ${config.udpTargetMbps} Mbit/s")
            append("Loss: ${formatPercent(test.lossPercent ?: 0.0)}, jitter: ${formatMilliseconds(test.jitterMs ?: 0.0)}")
        }
    }
}

internal fun buildResultReport(
    title: String,
    config: TestConfig,
    results: List<TestResult>,
    engine: IperfEngine,
    safe: Boolean,
): String = buildString {
    appendLine("Free iperf3 Client $title report")
    appendLine("Time (UTC): ${Instant.now()}")
    appendLine("Server: ${if (safe) "<redacted-server>" else "${config.hostname}:${config.port}"}")
    appendLine("Duration: ${config.durationSeconds} seconds")
    appendLine("UDP target: ${config.udpTargetMbps} Mbit/s")
    appendLine()
    results.filter { it.mode != TestMode.DETECT }.forEach { test ->
        appendLine(resultSummary(test, config))
        appendLine()
    }
    appendLine("Commands:")
    results.forEach { test ->
                appendLine(if (safe) redactSensitiveText(engine.displayCommand(config, test.mode), config) else engine.displayCommand(config, test.mode))
    }
    appendLine()
    append("UDP scores are app heuristics based on delivered rate, packet loss, and jitter.")
}

internal fun buildDiagnosticReport(
    context: Context,
    title: String,
    config: TestConfig,
    failedStage: String,
    networkInfo: NetworkInfo?,
    completed: List<TestResult>,
    error: Throwable,
    safe: Boolean,
): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return buildString {
        appendLine("Free iperf3 Client diagnostic report")
        appendLine("Time (UTC): ${Instant.now()}")
        appendLine("App: ${packageInfo.versionName} (${packageInfo.longVersionCode})")
        appendLine("Engine: iperf3 3.21")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${if (safe) "<redacted-device>" else "${Build.MANUFACTURER} ${Build.MODEL}"}")
        appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("Network transport: ${networkInfo?.transport ?: "Unavailable"}")
        appendLine("Local address: ${if (safe && networkInfo != null) "<redacted-local-address>" else networkInfo?.localAddress ?: "Unavailable"}")
        appendLine("Prefix length: ${networkInfo?.prefixLength ?: "Unavailable"}")
        appendLine("Gateway: ${if (safe && networkInfo?.gateway != null) "<redacted-gateway>" else networkInfo?.gateway ?: "Unavailable"}")
        appendLine("Sequence: $title")
        appendLine("Failed stage: $failedStage")
        appendLine("Server: ${if (safe) "<redacted-server>" else "${config.hostname}:${config.port}"}")
        appendLine("Duration: ${config.durationSeconds} seconds")
        appendLine("UDP target: ${config.udpTargetMbps} Mbit/s")
        appendLine("Error type: ${error.javaClass.name}")
        appendLine("Error: ${error.message ?: "(no message)"}")
        if (error is NetworkAccessFailure) {
            appendLine("Network technical details: ${error.technicalDetails}")
        }
        if (completed.isNotEmpty()) {
            appendLine()
            appendLine("Completed results:")
            completed.filter { it.mode != TestMode.DETECT }.forEach {
                appendLine(resultSummary(it, config))
            }
        }
        if (error is IperfFailure && error.rawOutput.isNotBlank()) {
            appendLine()
            appendLine("Raw iperf3 output:")
            appendLine(if (safe) redactSensitiveText(error.rawOutput, config) else error.rawOutput)
        }
        appendLine()
        appendLine("Stack trace:")
        append(if (safe) redactSensitiveText(error.stackTraceToString(), config) else error.stackTraceToString())
    }
}

internal fun commandAndOutput(
    config: TestConfig,
    result: TestResult,
    engine: IperfEngine,
): String = buildString {
    appendLine("Command")
    appendLine(engine.displayCommand(config, result.mode))
    appendLine()
    appendLine("Raw output")
    append(result.rawOutput.trim())
}

internal fun friendlyError(error: Throwable): String = when (error) {
    is NetworkAccessFailure -> "Network access failed: ${error.message}. Check the app's network permissions or device configuration."
    else -> error.message?.lineSequence()?.firstOrNull { it.isNotBlank() }
        ?: error.javaClass.simpleName
}

internal fun redactSensitiveText(command: String, config: TestConfig): String =
    command.replace(config.hostname, "<redacted-server>")
