package com.freeiperf3client.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress
import java.time.Instant
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.json.JSONObject

private const val REPOSITORY_URL = "https://github.com/Anders0lesen/free-iperf3-client"

private enum class TestMode(val label: String, val durationSeconds: Int) {
    DETECT("Server detection", 1),
    TCP_DOWNLOAD("TCP download", 10),
    TCP_UPLOAD("TCP upload", 10),
    TCP_BIDIRECTIONAL("TCP bidirectional", 10),
    UDP_DOWNLOAD("UDP download quality", 5),
    UDP_UPLOAD("UDP upload quality", 5)
}

private data class TestConfig(
    val hostname: String,
    val port: Int,
    val udpTargetMbps: Int
)

private data class LiveUpdate(
    val connected: Boolean = false,
    val connection: String? = null,
    val elapsedSeconds: Double = 0.0,
    val uploadBitsPerSecond: Double? = null,
    val downloadBitsPerSecond: Double? = null,
    val jitterMs: Double? = null,
    val lossPercent: Double? = null,
    val intervalText: String? = null
)

private data class TestResult(
    val mode: TestMode,
    val connection: String?,
    val uploadBitsPerSecond: Double? = null,
    val downloadBitsPerSecond: Double? = null,
    val jitterMs: Double? = null,
    val lossPercent: Double? = null,
    val packets: Long? = null,
    val rawOutput: String
)

private class IperfFailure(
    message: String,
    val rawOutput: String
) : IllegalStateException(message)

class MainActivity : AppCompatActivity() {
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var udpTarget: EditText
    private lateinit var github: ImageButton
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var command: TextView
    private lateinit var live: TextView
    private lateinit var intervals: TextView
    private lateinit var result: TextView
    private lateinit var detect: Button
    private lateinit var download: Button
    private lateinit var upload: Button
    private lateinit var bidirectional: Button
    private lateinit var udpQuality: Button
    private lateinit var runAll: Button
    private lateinit var copyShare: Button
    private lateinit var toggleDetails: Button
    private lateinit var details: TextView
    private val actionButtons = mutableListOf<Button>()
    private val intervalLines = mutableListOf<String>()
    private var shareText = ""
    @Volatile private var activeProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val smallPad = (12 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Free iperf3 Client"
            textSize = 28f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, -2, 1f))

        github = ImageButton(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_github)
            contentDescription = "Open project on GitHub"
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(smallPad, smallPad, smallPad, smallPad)
            isFocusable = true
            tooltipText = "Open GitHub repository"
            setOnClickListener { openRepository() }
        }
        header.addView(github, LinearLayout.LayoutParams(
            (52 * resources.displayMetrics.density).toInt(),
            (52 * resources.displayMetrics.density).toInt()
        ))
        box.addView(header, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = smallPad })

        host = EditText(this).apply {
            id = View.generateViewId()
            hint = "Server IP or hostname"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }
        box.addView(host, LinearLayout.LayoutParams(-1, -2))

        port = EditText(this).apply {
            id = View.generateViewId()
            setText("5201")
            hint = "Port"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }
        box.addView(port, LinearLayout.LayoutParams(-1, -2))

        udpTarget = EditText(this).apply {
            id = View.generateViewId()
            setText("50")
            hint = "UDP target Mbit/s"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        box.addView(udpTarget, LinearLayout.LayoutParams(-1, -2))

        detect = addActionButton(box, "CHECK / DETECT IPERF3 SERVER", pad) {
            startSequence("SERVER CHECK", listOf(TestMode.DETECT))
        }
        download = addActionButton(box, "TCP DOWNLOAD  (SERVER → THIS DEVICE)") {
            startSequence("TCP DOWNLOAD", listOf(TestMode.TCP_DOWNLOAD))
        }
        upload = addActionButton(box, "TCP UPLOAD  (THIS DEVICE → SERVER)") {
            startSequence("TCP UPLOAD", listOf(TestMode.TCP_UPLOAD))
        }
        bidirectional = addActionButton(box, "TCP BIDIRECTIONAL  (SIMULTANEOUS)") {
            startSequence("TCP BIDIRECTIONAL", listOf(TestMode.TCP_BIDIRECTIONAL))
        }
        udpQuality = addActionButton(box, "UDP QUALITY  (BOTH DIRECTIONS)") {
            startSequence("UDP QUALITY", listOf(TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD))
        }
        runAll = addActionButton(box, "RUN ALL TESTS") {
            startSequence("FULL TEST", TestMode.entries.toList())
        }

        status = TextView(this).apply {
            text = "Ready to test"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, pad, 0, smallPad)
        }
        box.addView(status, LinearLayout.LayoutParams(-1, -2))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 0
            visibility = View.GONE
        }
        box.addView(progress, LinearLayout.LayoutParams(-1, -2))

        command = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(smallPad, smallPad, smallPad, smallPad)
            setBackgroundColor(Color.argb(24, 128, 128, 128))
            visibility = View.GONE
        }
        box.addView(command, LinearLayout.LayoutParams(-1, -2).apply { topMargin = smallPad })

        live = TextView(this).apply {
            textSize = 16f
            visibility = View.GONE
            setPadding(0, smallPad, 0, 0)
        }
        box.addView(live, LinearLayout.LayoutParams(-1, -2))

        intervals = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(0, smallPad, 0, 0)
            visibility = View.GONE
        }
        box.addView(intervals, LinearLayout.LayoutParams(-1, -2))

        result = TextView(this).apply {
            text = "Enter an iperf3 server, choose a test, or run them all."
            textSize = 18f
            setPadding(0, pad, 0, 0)
            isFocusable = false
            setTextIsSelectable(true)
        }
        box.addView(result, LinearLayout.LayoutParams(-1, -2))

        copyShare = Button(this).apply {
            id = View.generateViewId()
            text = "COPY RESULTS"
            isFocusable = true
            visibility = View.GONE
            setOnClickListener { copyShareText() }
        }
        box.addView(copyShare, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })

        toggleDetails = Button(this).apply {
            id = View.generateViewId()
            text = "SHOW DETAILS"
            isFocusable = true
            visibility = View.GONE
            setOnClickListener { toggleTechnicalDetails() }
        }
        box.addView(toggleDetails, LinearLayout.LayoutParams(-1, -2))

        details = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            visibility = View.GONE
            setPadding(0, pad, 0, pad)
        }
        box.addView(details, LinearLayout.LayoutParams(-1, -2))

        configureFocusOrder()
        configureInputNavigation()

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(box)
        })
    }

    private fun addActionButton(
        parent: LinearLayout,
        label: String,
        topMargin: Int = 0,
        action: () -> Unit
    ): Button = Button(this).apply {
        id = View.generateViewId()
        text = label
        isFocusable = true
        setOnClickListener { action() }
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply {
            this.topMargin = topMargin
        })
        actionButtons += this
    }

    private fun configureFocusOrder() {
        val focusViews = listOf<View>(
            github, host, port, udpTarget, detect, download, upload,
            bidirectional, udpQuality, runAll, copyShare, toggleDetails
        )
        focusViews.forEachIndexed { index, view ->
            if (index > 0) view.nextFocusUpId = focusViews[index - 1].id
            if (index < focusViews.lastIndex) view.nextFocusDownId = focusViews[index + 1].id
        }
    }

    private fun configureInputNavigation() {
        host.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                port.requestFocus()
                true
            } else false
        }
        port.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                udpTarget.requestFocus()
                true
            } else false
        }
        udpTarget.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                detect.requestFocus()
                true
            } else false
        }
        host.setDpadFocus(github, port)
        port.setDpadFocus(host, udpTarget)
        udpTarget.setDpadFocus(port, detect)
    }

    private fun EditText.setDpadFocus(up: View, down: View) {
        setOnKeyListener { _, keyCode, event ->
            when {
                event.action != KeyEvent.ACTION_DOWN -> false
                keyCode == KeyEvent.KEYCODE_DPAD_UP -> up.requestFocus().let { true }
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> down.requestFocus().let { true }
                else -> false
            }
        }
    }

    private fun openRepository() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No browser is available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val keyboard = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.hideSoftInputFromWindow(udpTarget.windowToken, 0)
    }

    private fun readConfig(needsUdp: Boolean): TestConfig? {
        val hostname = host.text.toString().trim().removeSurrounding("[", "]")
        val portNumber = port.text.toString().toIntOrNull()
        val udpMbps = udpTarget.text.toString().toIntOrNull()
        if (hostname.isBlank()) {
            showInputError(host, "Enter a server IP or hostname")
            return null
        }
        if (!isValidServerName(hostname)) {
            showInputError(host, "That is not a valid IP address or hostname")
            return null
        }
        if (portNumber == null || portNumber !in 1..65535) {
            showInputError(port, "Enter a port between 1 and 65535")
            return null
        }
        if (needsUdp && (udpMbps == null || udpMbps !in 1..10_000)) {
            showInputError(udpTarget, "Enter a UDP target between 1 and 10000 Mbit/s")
            return null
        }
        return TestConfig(hostname, portNumber, udpMbps ?: 50)
    }

    private fun showInputError(field: EditText, message: String) {
        field.error = message
        field.requestFocus()
        setStatus("Input needs attention", "#EF5350")
        result.text = message
    }

    private fun isValidServerName(value: String): Boolean {
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

    private fun startSequence(title: String, modes: List<TestMode>) {
        val plannedModes = if (modes.firstOrNull() == TestMode.DETECT) {
            modes
        } else {
            listOf(TestMode.DETECT) + modes
        }
        val config = readConfig(plannedModes.any { it == TestMode.UDP_DOWNLOAD || it == TestMode.UDP_UPLOAD })
            ?: return
        hideKeyboard()
        setBusy(true)
        clearShareArea()
        progress.visibility = View.VISIBLE
        progress.progress = 0
        command.visibility = View.VISIBLE
        live.visibility = View.VISIBLE
        live.text = "Preparing…"
        intervalLines.clear()
        intervals.text = ""
        intervals.visibility = View.VISIBLE
        result.text = "No completed results yet."

        Thread {
            val completed = mutableListOf<TestResult>()
            val totalSeconds = plannedModes.sumOf { it.durationSeconds }.coerceAtLeast(1)
            var completedSeconds = 0
            var activeMode = plannedModes.first()
            try {
                plannedModes.forEachIndexed { index, mode ->
                    activeMode = mode
                    showStageWaiting(config, mode, index, plannedModes.size, completedSeconds, totalSeconds)
                    val testResult = executeIperf(config, mode) { update ->
                        showLiveUpdate(
                            mode, update, index, plannedModes.size,
                            completedSeconds, totalSeconds
                        )
                    }
                    completed += testResult
                    completedSeconds += mode.durationSeconds
                    runOnUiThread {
                        result.text = formatResults(title, config, completed)
                        progress.progress = ((completedSeconds.toDouble() / totalSeconds) * progress.max)
                            .roundToInt()
                    }
                }
                runOnUiThread { showSuccess(title, config, completed) }
            } catch (error: Exception) {
                runOnUiThread { showFailure(title, config, activeMode, completed, error) }
            }
        }.start()
    }

    private fun showStageWaiting(
        config: TestConfig,
        mode: TestMode,
        index: Int,
        count: Int,
        completedSeconds: Int,
        totalSeconds: Int
    ) {
        runOnUiThread {
            setStatus("Connecting… · ${mode.label} · ${index + 1}/$count", "#FFB74D")
            live.text = "Opening the iperf3 control connection…"
            command.text = "Command\n${formatDisplayCommand(config, mode)}"
            intervalLines += if (intervalLines.isEmpty()) mode.label else "\n${mode.label}"
            intervals.text = intervalLines.joinToString("\n")
            progress.progress = ((completedSeconds.toDouble() / totalSeconds) * progress.max)
                .roundToInt()
        }
    }

    private fun showLiveUpdate(
        mode: TestMode,
        update: LiveUpdate,
        index: Int,
        count: Int,
        completedSeconds: Int,
        totalSeconds: Int
    ) {
        runOnUiThread {
            if (update.connected) {
                setStatus("Connected ✓ · ${mode.label} · ${index + 1}/$count", "#66BB6A")
            }
            val elapsed = update.elapsedSeconds.coerceIn(0.0, mode.durationSeconds.toDouble())
            val overall = (completedSeconds + elapsed) / totalSeconds
            progress.progress = (overall * progress.max).roundToInt()
            live.text = formatLiveUpdate(mode, update)
            update.intervalText?.let {
                intervalLines += it
                intervals.text = intervalLines.joinToString("\n")
            }
        }
    }

    private fun formatLiveUpdate(mode: TestMode, update: LiveUpdate): String = buildString {
        if (update.connection != null && update.elapsedSeconds == 0.0) {
            append("iperf3 connection established: ").append(update.connection)
            return@buildString
        }
        append("Running: ")
            .append(min(mode.durationSeconds.toDouble(), update.elapsedSeconds).roundToInt())
            .append(" / ").append(mode.durationSeconds).append(" seconds")
        update.downloadBitsPerSecond?.let {
            append("\nDownload now: ").append(formatBitsPerSecond(it))
        }
        update.uploadBitsPerSecond?.let {
            append("\nUpload now: ").append(formatBitsPerSecond(it))
        }
        if (update.jitterMs != null || update.lossPercent != null) {
            append("\n")
            update.jitterMs?.let { append("Jitter: ").append(formatMilliseconds(it)).append("  ") }
            update.lossPercent?.let { append("Loss: ").append(formatPercent(it)) }
        }
    }

    private fun executeIperf(
        config: TestConfig,
        mode: TestMode,
        onUpdate: (LiveUpdate) -> Unit
    ): TestResult {
        val executable = File(applicationInfo.nativeLibraryDir, "libiperf3.so")
        if (!executable.canExecute()) {
            throw IllegalStateException("The bundled iperf3 engine is unavailable on this device")
        }

        val processCommand = buildCommand(config, mode, executable.absolutePath)

        val process = ProcessBuilder(processCommand)
            .redirectErrorStream(true)
            .start()
        activeProcess = process
        val timedOut = AtomicBoolean(false)
        val watchdog = Executors.newSingleThreadScheduledExecutor()
        val timeoutTask = watchdog.schedule({
            timedOut.set(true)
            process.destroyForcibly()
        }, mode.durationSeconds + 12L, TimeUnit.SECONDS)

        val raw = StringBuilder()
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
                            val data = event.optJSONObject("data") ?: JSONObject()
                            val connected = data.optJSONArray("connected")?.optJSONObject(0)
                            connection = connected?.let {
                                "${it.optString("remote_host")}:${it.optInt("remote_port")}"
                            }
                            onUpdate(LiveUpdate(connected = true, connection = connection))
                        }
                        "interval" -> {
                            val data = event.optJSONObject("data") ?: JSONObject()
                            onUpdate(parseLiveUpdate(mode, data))
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

        if (timedOut.get()) {
            throw IperfFailure("The ${mode.label.lowercase()} timed out", raw.toString())
        }
        if (!errorMessage.isNullOrBlank()) {
            throw IperfFailure(errorMessage!!, raw.toString())
        }
        if (process.exitValue() != 0) {
            throw IperfFailure(
                "iperf3 exited with code ${process.exitValue()}",
                raw.toString()
            )
        }
        val finalData = endData
            ?: throw IperfFailure("iperf3 finished without a final result", raw.toString())
        return parseFinalResult(mode, finalData, connection, raw.toString())
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
            listOf("-t", mode.durationSeconds.toString())
        }
        command += listOf(
            "-i", "1", "--connect-timeout", "3000", "--json-stream", "--forceflush"
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

    private fun formatDisplayCommand(config: TestConfig, mode: TestMode): String =
        buildCommand(config, mode, "iperf3").joinToString(" ") { argument ->
            if (argument.any(Char::isWhitespace)) "\"${argument.replace("\"", "\\\"")}\"" else argument
        }

    private fun parseLiveUpdate(mode: TestMode, data: JSONObject): LiveUpdate {
        val sum = data.optJSONObject("sum")
        val reverse = data.optJSONObject("sum_bidir_reverse")
        val elapsed = max(
            sum?.optDouble("end", 0.0) ?: 0.0,
            reverse?.optDouble("end", 0.0) ?: 0.0
        )
        val primaryRate = sum?.optionalDouble("bits_per_second")
        val reverseRate = reverse?.optionalDouble("bits_per_second")
        val intervalText = listOfNotNull(
            sum?.let { formatInterval(it, if (mode == TestMode.TCP_DOWNLOAD || mode == TestMode.UDP_DOWNLOAD) "↓" else "↑") },
            reverse?.let { formatInterval(it, "↓") }
        ).joinToString("\n").ifBlank { null }
        return when (mode) {
            TestMode.TCP_DOWNLOAD, TestMode.UDP_DOWNLOAD -> LiveUpdate(
                elapsedSeconds = elapsed,
                downloadBitsPerSecond = primaryRate,
                jitterMs = sum?.optionalDouble("jitter_ms"),
                lossPercent = sum?.optionalDouble("lost_percent"),
                intervalText = intervalText
            )
            TestMode.TCP_BIDIRECTIONAL -> LiveUpdate(
                elapsedSeconds = elapsed,
                uploadBitsPerSecond = primaryRate,
                downloadBitsPerSecond = reverseRate,
                intervalText = intervalText
            )
            else -> LiveUpdate(
                elapsedSeconds = elapsed,
                uploadBitsPerSecond = primaryRate,
                jitterMs = sum?.optionalDouble("jitter_ms"),
                lossPercent = sum?.optionalDouble("lost_percent"),
                intervalText = intervalText
            )
        }
    }

    private fun formatInterval(sum: JSONObject, direction: String): String {
        val start = sum.optDouble("start", 0.0)
        val end = sum.optDouble("end", 0.0)
        val rate = sum.optDouble("bits_per_second", 0.0)
        val bytes = sum.optDouble("bytes", 0.0)
        return buildString {
            append(formatNumber(start, 2)).append("–").append(formatNumber(end, 2)).append(" s  ")
            append(direction).append(" ").append(formatBitsPerSecond(rate))
            if (bytes > 0) append("  ").append(formatBytes(bytes))
            sum.optionalDouble("jitter_ms")?.let { append("  jitter ").append(formatMilliseconds(it)) }
            sum.optionalDouble("lost_percent")?.let { append("  loss ").append(formatPercent(it)) }
        }
    }

    private fun parseFinalResult(
        mode: TestMode,
        end: JSONObject,
        connection: String?,
        rawOutput: String
    ): TestResult {
        val sent = end.optJSONObject("sum_sent")
        val received = end.optJSONObject("sum_received")
        return when (mode) {
            TestMode.DETECT -> TestResult(mode, connection, rawOutput = rawOutput)
            TestMode.TCP_DOWNLOAD -> TestResult(
                mode, connection,
                downloadBitsPerSecond = received.requireDouble("bits_per_second", mode),
                rawOutput = rawOutput
            )
            TestMode.TCP_UPLOAD -> TestResult(
                mode, connection,
                uploadBitsPerSecond = sent.requireDouble("bits_per_second", mode),
                rawOutput = rawOutput
            )
            TestMode.TCP_BIDIRECTIONAL -> {
                val reverseReceived = end.optJSONObject("sum_received_bidir_reverse")
                    ?: throw IperfFailure(
                        "The server did not return bidirectional results; update its iperf3 version",
                        rawOutput
                    )
                TestResult(
                    mode, connection,
                    uploadBitsPerSecond = sent.requireDouble("bits_per_second", mode),
                    downloadBitsPerSecond = reverseReceived.requireDouble("bits_per_second", mode),
                    rawOutput = rawOutput
                )
            }
            TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD -> {
                val summary = received
                    ?: throw IperfFailure("iperf3 did not return UDP receiver statistics", rawOutput)
                val rate = summary.requireDouble("bits_per_second", mode)
                TestResult(
                    mode, connection,
                    uploadBitsPerSecond = rate.takeIf { mode == TestMode.UDP_UPLOAD },
                    downloadBitsPerSecond = rate.takeIf { mode == TestMode.UDP_DOWNLOAD },
                    jitterMs = summary.optionalDouble("jitter_ms") ?: 0.0,
                    lossPercent = summary.optionalDouble("lost_percent") ?: 0.0,
                    packets = summary.optLong("packets", 0).takeIf { it > 0 },
                    rawOutput = rawOutput
                )
            }
        }
    }

    private fun JSONObject?.requireDouble(key: String, mode: TestMode): Double {
        val value = this?.optionalDouble(key)
        return value ?: throw IllegalStateException("Missing $key in ${mode.label} result")
    }

    private fun JSONObject.optionalDouble(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key) else null

    private fun showSuccess(title: String, config: TestConfig, results: List<TestResult>) {
        progress.progress = progress.max
        setStatus("Complete ✓", "#66BB6A")
        live.text = "All requested tests completed successfully."
        result.text = formatResults(title, config, results)
        shareText = buildShareReport(title, config, results, intervalLines.joinToString("\n"))
        details.text = shareText
        copyShare.text = "COPY RESULTS"
        copyShare.visibility = View.VISIBLE
        toggleDetails.visibility = View.VISIBLE
        setBusy(false)
    }

    private fun showFailure(
        title: String,
        config: TestConfig,
        mode: TestMode,
        completed: List<TestResult>,
        error: Exception
    ) {
        setStatus("Failed · ${mode.label}", "#EF5350")
        live.text = friendlyError(error)
        result.text = buildString {
            append("TEST FAILED\n\n")
            append(friendlyError(error))
            if (completed.isNotEmpty()) {
                append("\n\nCompleted before the failure:\n")
                append(formatResults(title, config, completed))
            }
            append("\n\nUse COPY DIAGNOSTICS to share the technical details.")
        }
        shareText = buildDiagnosticReport(title, config, mode, completed, error)
        details.text = shareText
        details.visibility = View.GONE
        copyShare.text = "COPY DIAGNOSTICS"
        copyShare.visibility = View.VISIBLE
        toggleDetails.text = "SHOW DETAILS"
        toggleDetails.visibility = View.VISIBLE
        setBusy(false)
    }

    private fun formatResults(
        title: String,
        config: TestConfig,
        results: List<TestResult>
    ): String = buildString {
        append(title).append(" RESULTS\n")
        results.forEach { test ->
            append("\n").append(formatResult(test, config)).append("\n")
        }
    }.trimEnd()

    private fun formatResult(test: TestResult, config: TestConfig): String = when (test.mode) {
        TestMode.DETECT -> buildString {
            append("✓ IPERF3 SERVER DETECTED")
            test.connection?.let { append("\n  Connected to ").append(it) }
            append("\n  Control and data test succeeded")
        }
        TestMode.TCP_DOWNLOAD -> "TCP download\n  ${formatBitsPerSecond(test.downloadBitsPerSecond!!)}"
        TestMode.TCP_UPLOAD -> "TCP upload\n  ${formatBitsPerSecond(test.uploadBitsPerSecond!!)}"
        TestMode.TCP_BIDIRECTIONAL -> buildString {
            append("TCP bidirectional (simultaneous)")
            append("\n  Download: ").append(formatBitsPerSecond(test.downloadBitsPerSecond!!))
            append("\n  Upload:   ").append(formatBitsPerSecond(test.uploadBitsPerSecond!!))
        }
        TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD -> formatUdpResult(test, config)
    }

    private fun formatUdpResult(test: TestResult, config: TestConfig): String {
        val rate = test.downloadBitsPerSecond ?: test.uploadBitsPerSecond ?: 0.0
        val (score, grade) = scoreUdp(test, config)
        val direction = if (test.mode == TestMode.UDP_DOWNLOAD) "download" else "upload"
        return buildString {
            append("UDP ").append(direction).append(" quality")
            append("\n  Score: ").append(score).append("/100 · ").append(grade)
            append("\n  Received: ").append(formatBitsPerSecond(rate))
                .append(" · Target: ").append(config.udpTargetMbps).append(" Mbit/s")
            append("\n  Loss: ").append(formatPercent(test.lossPercent ?: 0.0))
                .append(" · Jitter: ").append(formatMilliseconds(test.jitterMs ?: 0.0))
            test.packets?.let { append(" · Packets: ").append(it) }
        }
    }

    private fun scoreUdp(test: TestResult, config: TestConfig): Pair<Int, String> {
        val receivedRate = test.downloadBitsPerSecond ?: test.uploadBitsPerSecond ?: 0.0
        val targetRate = config.udpTargetMbps * 1_000_000.0
        val deliveryRatio = if (targetRate > 0) receivedRate / targetRate else 1.0
        val ratePenalty = if (deliveryRatio >= 0.95) 0.0 else min(40.0, (0.95 - deliveryRatio) * 80.0)
        val lossPenalty = min(80.0, (test.lossPercent ?: 0.0) * 20.0)
        val jitterPenalty = min(20.0, max(0.0, (test.jitterMs ?: 0.0) - 5.0) * 0.8)
        val score = (100.0 - ratePenalty - lossPenalty - jitterPenalty)
            .roundToInt().coerceIn(0, 100)
        val grade = when {
            score >= 90 -> "EXCELLENT"
            score >= 75 -> "GOOD"
            score >= 50 -> "FAIR"
            else -> "POOR"
        }
        return score to grade
    }

    private fun buildShareReport(
        title: String,
        config: TestConfig,
        results: List<TestResult>,
        intervalOutput: String
    ): String = buildString {
        appendLine("Free iperf3 Client $title report")
        appendLine("Time (UTC): ${Instant.now()}")
        appendLine("Server: ${config.hostname}:${config.port}")
        appendLine("UDP target: ${config.udpTargetMbps} Mbit/s")
        appendLine()
        append(formatResults(title, config, results))
        appendLine()
        appendLine()
        appendLine("Commands:")
        results.forEach { appendLine(formatDisplayCommand(config, it.mode)) }
        if (intervalOutput.isNotBlank()) {
            appendLine()
            appendLine("Per-second intervals:")
            appendLine(intervalOutput)
        }
        appendLine()
        append("UDP score is an app heuristic based on received rate, packet loss, and jitter.")
    }

    private fun buildDiagnosticReport(
        title: String,
        config: TestConfig,
        mode: TestMode,
        completed: List<TestResult>,
        error: Exception
    ): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return buildString {
            appendLine("Free iperf3 Client diagnostic report")
            appendLine("Privacy: review the server and device fields before sharing publicly")
            appendLine("Time (UTC): ${Instant.now()}")
            appendLine("App: ${packageInfo.versionName} (${packageInfo.longVersionCode})")
            appendLine("Engine: iperf3 3.21")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Sequence: $title")
            appendLine("Failed stage: ${mode.label}")
            appendLine("Server: ${config.hostname}:${config.port}")
            appendLine("UDP target: ${config.udpTargetMbps} Mbit/s")
            appendLine("Error type: ${error.javaClass.name}")
            appendLine("Error: ${error.message ?: "(no message)"}")
            if (completed.isNotEmpty()) {
                appendLine()
                appendLine("Completed results:")
                appendLine(formatResults(title, config, completed))
            }
            if (error is IperfFailure && error.rawOutput.isNotBlank()) {
                appendLine()
                appendLine("Raw iperf3 output:")
                appendLine(error.rawOutput.trim())
            }
            appendLine()
            appendLine("Stack trace:")
            append(error.stackTraceToString())
        }
    }

    private fun friendlyError(error: Exception): String =
        error.message?.lineSequence()?.firstOrNull { it.isNotBlank() }
            ?: error.javaClass.simpleName

    private fun formatBitsPerSecond(bitsPerSecond: Double): String = when {
        bitsPerSecond >= 1_000_000_000 -> formatNumber(bitsPerSecond / 1_000_000_000, 2) + " Gbit/s"
        bitsPerSecond >= 1_000_000 -> formatNumber(bitsPerSecond / 1_000_000, 1) + " Mbit/s"
        bitsPerSecond >= 1_000 -> formatNumber(bitsPerSecond / 1_000, 1) + " Kbit/s"
        else -> formatNumber(bitsPerSecond, 0) + " bit/s"
    }

    private fun formatBytes(bytes: Double): String = when {
        bytes >= 1_000_000_000 -> formatNumber(bytes / 1_000_000_000, 2) + " GB"
        bytes >= 1_000_000 -> formatNumber(bytes / 1_000_000, 1) + " MB"
        bytes >= 1_000 -> formatNumber(bytes / 1_000, 1) + " KB"
        else -> formatNumber(bytes, 0) + " B"
    }

    private fun formatMilliseconds(value: Double): String = formatNumber(value, 2) + " ms"

    private fun formatPercent(value: Double): String = formatNumber(value, 2) + "%"

    private fun formatNumber(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)

    private fun copyShareText() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("iperf3 report", shareText))
        Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show()
    }

    private fun toggleTechnicalDetails() {
        val show = details.visibility != View.VISIBLE
        details.visibility = if (show) View.VISIBLE else View.GONE
        toggleDetails.text = if (show) "HIDE DETAILS" else "SHOW DETAILS"
    }

    private fun clearShareArea() {
        shareText = ""
        details.text = ""
        details.visibility = View.GONE
        copyShare.visibility = View.GONE
        toggleDetails.visibility = View.GONE
        toggleDetails.text = "SHOW DETAILS"
    }

    private fun setStatus(message: String, color: String) {
        status.text = message
        status.setTextColor(Color.parseColor(color))
    }

    private fun setBusy(busy: Boolean) {
        actionButtons.forEach { it.isEnabled = !busy }
        host.isEnabled = !busy
        port.isEnabled = !busy
        udpTarget.isEnabled = !busy
    }

    override fun onDestroy() {
        activeProcess?.destroyForcibly()
        activeProcess = null
        super.onDestroy()
    }
}
