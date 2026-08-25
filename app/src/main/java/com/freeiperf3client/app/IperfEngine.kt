package com.freeiperf3client.app

import android.content.Context
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import org.json.JSONObject

internal enum class TestMode(
    val title: String,
    val subtitle: String,
) {
    DETECT("Server check", "Confirm iperf3 is responding"),
    TCP_DOWNLOAD("TCP Download", "Server to this device"),
    TCP_UPLOAD("TCP Upload", "This device to server"),
    TCP_BIDIRECTIONAL("TCP Bidirectional", "Simultaneous upload and download"),
    UDP_DOWNLOAD("UDP Download", "Streaming quality to this device"),
    UDP_UPLOAD("UDP Upload", "Streaming quality to server"),
}

internal enum class TestChoice(val title: String, val subtitle: String) {
    TCP_DOWNLOAD("TCP Download", "Server to this device"),
    TCP_UPLOAD("TCP Upload", "This device to server"),
    TCP_BIDIRECTIONAL("TCP Bidirectional", "Simultaneous"),
    UDP_QUALITY("UDP Quality", "Both directions"),
    RUN_ALL("Run All Tests", "Everything"),
}

internal data class TestConfig(
    val hostname: String,
    val port: Int,
    val durationSeconds: Int,
    val udpTargetMbps: Int,
)

internal data class IntervalSample(
    val startSeconds: Double,
    val endSeconds: Double,
    val uploadBitsPerSecond: Double? = null,
    val downloadBitsPerSecond: Double? = null,
    val uploadBytes: Double? = null,
    val downloadBytes: Double? = null,
    val jitterMs: Double? = null,
    val lossPercent: Double? = null,
)

internal data class LiveUpdate(
    val connected: Boolean = false,
    val connection: String? = null,
    val elapsedSeconds: Double = 0.0,
    val uploadBitsPerSecond: Double? = null,
    val downloadBitsPerSecond: Double? = null,
    val jitterMs: Double? = null,
    val lossPercent: Double? = null,
    val sample: IntervalSample? = null,
)

internal data class TestResult(
    val mode: TestMode,
    val connection: String?,
    val uploadBitsPerSecond: Double? = null,
    val downloadBitsPerSecond: Double? = null,
    val jitterMs: Double? = null,
    val lossPercent: Double? = null,
    val packets: Long? = null,
    val samples: List<IntervalSample> = emptyList(),
    val rawOutput: String,
)

internal class IperfFailure(
    message: String,
    val rawOutput: String,
) : IllegalStateException(message)

internal class IperfEngine(private val context: Context) {
    @Volatile
    private var activeProcess: Process? = null

    fun cancel() {
        activeProcess?.destroyForcibly()
        activeProcess = null
    }

    fun durationFor(config: TestConfig, mode: TestMode): Int =
        if (mode == TestMode.DETECT) 1 else config.durationSeconds

    fun execute(
        config: TestConfig,
        mode: TestMode,
        onUpdate: (LiveUpdate) -> Unit,
    ): TestResult {
        val executable = File(context.applicationInfo.nativeLibraryDir, "libiperf3.so")
        if (!executable.canExecute()) {
            throw IllegalStateException("The bundled iperf3 engine is unavailable on this device")
        }

        val duration = durationFor(config, mode)
        val process = ProcessBuilder(buildCommand(config, mode, executable.absolutePath))
            .redirectErrorStream(true)
            .start()
        activeProcess = process
        val timedOut = AtomicBoolean(false)
        val watchdog = Executors.newSingleThreadScheduledExecutor()
        val timeoutTask = watchdog.schedule({
            timedOut.set(true)
            process.destroyForcibly()
        }, duration + 12L, TimeUnit.SECONDS)

        val raw = StringBuilder()
        val samples = mutableListOf<IntervalSample>()
        var endData: JSONObject? = null
        var connection: String? = null
        var errorMessage: String? = null
        try {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.isBlank()) return@forEach
                    raw.appendLine(line)
                    val event = runCatching { JSONObject(line) }.getOrNull() ?: return@forEach
                    when (event.optString("event")) {
                        "start" -> {
                            val connected = event.optJSONObject("data")
                                ?.optJSONArray("connected")
                                ?.optJSONObject(0)
                            connection = connected?.let {
                                "${it.optString("remote_host")}:${it.optInt("remote_port")}" 
                            }
                            onUpdate(LiveUpdate(connected = true, connection = connection))
                        }
                        "interval" -> {
                            val update = parseLiveUpdate(
                                mode,
                                event.optJSONObject("data") ?: JSONObject(),
                            )
                            update.sample?.let(samples::add)
                            onUpdate(update)
                        }
                        "end" -> endData = event.optJSONObject("data") ?: JSONObject()
                        "error" -> errorMessage = event.optString("data", "iperf3 reported an error")
                    }
                }
            }
            process.waitFor()
        } finally {
            timeoutTask.cancel(true)
            watchdog.shutdownNow()
            if (activeProcess === process) activeProcess = null
        }

        if (timedOut.get()) throw IperfFailure("The ${mode.title.lowercase()} timed out", raw.toString())
        if (!errorMessage.isNullOrBlank()) throw IperfFailure(errorMessage!!, raw.toString())
        if (process.exitValue() != 0) {
            throw IperfFailure("iperf3 exited with code ${process.exitValue()}", raw.toString())
        }
        val finalData = endData
            ?: throw IperfFailure("iperf3 finished without a final result", raw.toString())
        return parseFinalResult(mode, finalData, connection, samples, raw.toString())
    }

    fun displayCommand(config: TestConfig, mode: TestMode): String =
        buildCommand(config, mode, "iperf3").joinToString(" ") { argument ->
            if (argument.any(Char::isWhitespace)) {
                "\"${argument.replace("\"", "\\\"")}\""
            } else {
                argument
            }
        }

    private fun buildCommand(config: TestConfig, mode: TestMode, executable: String): List<String> {
        val command = mutableListOf(
            executable,
            "-c", config.hostname,
            "-p", config.port.toString(),
        )
        command += if (mode == TestMode.DETECT) {
            listOf("-n", "1")
        } else {
            listOf("-t", config.durationSeconds.toString())
        }
        command += listOf(
            "-i", "1", "--connect-timeout", "3000", "--json-stream", "--forceflush",
        )
        when (mode) {
            TestMode.DETECT -> Unit
            TestMode.TCP_DOWNLOAD -> command += "-R"
            TestMode.TCP_UPLOAD -> Unit
            TestMode.TCP_BIDIRECTIONAL -> command += "--bidir"
            TestMode.UDP_DOWNLOAD -> command += listOf("-u", "-b", "${config.udpTargetMbps}M", "-R")
            TestMode.UDP_UPLOAD -> command += listOf("-u", "-b", "${config.udpTargetMbps}M")
        }
        return command
    }

    private fun parseLiveUpdate(mode: TestMode, data: JSONObject): LiveUpdate {
        val sum = data.optJSONObject("sum")
        val reverse = data.optJSONObject("sum_bidir_reverse")
        val elapsed = max(
            sum?.optDouble("end", 0.0) ?: 0.0,
            reverse?.optDouble("end", 0.0) ?: 0.0,
        )
        val primaryRate = sum?.optionalDouble("bits_per_second")
        val reverseRate = reverse?.optionalDouble("bits_per_second")
        val start = minOf(
            sum?.optDouble("start", elapsed) ?: elapsed,
            reverse?.optDouble("start", elapsed) ?: elapsed,
        )
        val primaryBytes = sum?.optionalDouble("bytes")
        val reverseBytes = reverse?.optionalDouble("bytes")
        val jitter = sum?.optionalDouble("jitter_ms")
        val loss = sum?.optionalDouble("lost_percent")
        val sample = when (mode) {
            TestMode.TCP_DOWNLOAD, TestMode.UDP_DOWNLOAD -> IntervalSample(
                startSeconds = start,
                endSeconds = elapsed,
                downloadBitsPerSecond = primaryRate,
                downloadBytes = primaryBytes,
                jitterMs = jitter,
                lossPercent = loss,
            )
            TestMode.TCP_BIDIRECTIONAL -> IntervalSample(
                startSeconds = start,
                endSeconds = elapsed,
                uploadBitsPerSecond = primaryRate,
                downloadBitsPerSecond = reverseRate,
                uploadBytes = primaryBytes,
                downloadBytes = reverseBytes,
            )
            else -> IntervalSample(
                startSeconds = start,
                endSeconds = elapsed,
                uploadBitsPerSecond = primaryRate,
                uploadBytes = primaryBytes,
                jitterMs = jitter,
                lossPercent = loss,
            )
        }
        return LiveUpdate(
            elapsedSeconds = elapsed,
            uploadBitsPerSecond = sample.uploadBitsPerSecond,
            downloadBitsPerSecond = sample.downloadBitsPerSecond,
            jitterMs = jitter,
            lossPercent = loss,
            sample = sample,
        )
    }

    private fun parseFinalResult(
        mode: TestMode,
        end: JSONObject,
        connection: String?,
        samples: List<IntervalSample>,
        rawOutput: String,
    ): TestResult {
        val sent = end.optJSONObject("sum_sent")
        val received = end.optJSONObject("sum_received")
        return when (mode) {
            TestMode.DETECT -> TestResult(mode, connection, samples = samples, rawOutput = rawOutput)
            TestMode.TCP_DOWNLOAD -> TestResult(
                mode = mode,
                connection = connection,
                downloadBitsPerSecond = received.requireDouble("bits_per_second", mode, rawOutput),
                samples = samples,
                rawOutput = rawOutput,
            )
            TestMode.TCP_UPLOAD -> TestResult(
                mode = mode,
                connection = connection,
                uploadBitsPerSecond = sent.requireDouble("bits_per_second", mode, rawOutput),
                samples = samples,
                rawOutput = rawOutput,
            )
            TestMode.TCP_BIDIRECTIONAL -> {
                val reverseReceived = end.optJSONObject("sum_received_bidir_reverse")
                    ?: throw IperfFailure(
                        "The server did not return bidirectional results; update its iperf3 version",
                        rawOutput,
                    )
                TestResult(
                    mode = mode,
                    connection = connection,
                    uploadBitsPerSecond = sent.requireDouble("bits_per_second", mode, rawOutput),
                    downloadBitsPerSecond = reverseReceived.requireDouble("bits_per_second", mode, rawOutput),
                    samples = samples,
                    rawOutput = rawOutput,
                )
            }
            TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD -> {
                val summary = received
                    ?: throw IperfFailure("iperf3 did not return UDP receiver statistics", rawOutput)
                val rate = summary.requireDouble("bits_per_second", mode, rawOutput)
                TestResult(
                    mode = mode,
                    connection = connection,
                    uploadBitsPerSecond = rate.takeIf { mode == TestMode.UDP_UPLOAD },
                    downloadBitsPerSecond = rate.takeIf { mode == TestMode.UDP_DOWNLOAD },
                    jitterMs = summary.optionalDouble("jitter_ms") ?: 0.0,
                    lossPercent = summary.optionalDouble("lost_percent") ?: 0.0,
                    packets = summary.optLong("packets", 0).takeIf { it > 0 },
                    samples = samples,
                    rawOutput = rawOutput,
                )
            }
        }
    }

    private fun JSONObject?.requireDouble(key: String, mode: TestMode, rawOutput: String): Double {
        return this?.optionalDouble(key)
            ?: throw IperfFailure("Missing $key in ${mode.title} result", rawOutput)
    }

    private fun JSONObject.optionalDouble(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key) else null
}
