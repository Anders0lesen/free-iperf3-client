package com.freeiperf3client.app

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Build
import android.text.InputType
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var result: TextView
    private lateinit var upload: Button
    private lateinit var download: Button
    private lateinit var copyDiagnostics: Button
    private lateinit var toggleDiagnostics: Button
    private lateinit var diagnostics: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
        }

        box.addView(TextView(this).apply {
            text = "Free iperf3 Client"
            textSize = 28f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = pad })

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
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        box.addView(port, LinearLayout.LayoutParams(-1, -2))

        download = Button(this).apply {
            id = View.generateViewId()
            text = "TEST DOWNLOAD  (SERVER → THIS DEVICE)"
            isFocusable = true
            setOnClickListener { runTest(true) }
        }
        box.addView(download, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })

        upload = Button(this).apply {
            id = View.generateViewId()
            text = "TEST UPLOAD  (THIS DEVICE → SERVER)"
            isFocusable = true
            setOnClickListener { runTest(false) }
        }
        box.addView(upload, LinearLayout.LayoutParams(-1, -2))

        result = TextView(this).apply {
            text = "Enter an iperf3 server and run a test."
            textSize = 18f
            setPadding(0, pad, 0, 0)
            isFocusable = false
            setTextIsSelectable(true)
        }
        box.addView(result, LinearLayout.LayoutParams(-1, -2))

        copyDiagnostics = Button(this).apply {
            id = View.generateViewId()
            text = "COPY DIAGNOSTICS"
            isFocusable = true
            visibility = View.GONE
            setOnClickListener { copyDiagnosticsToClipboard() }
        }
        box.addView(copyDiagnostics, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })

        toggleDiagnostics = Button(this).apply {
            id = View.generateViewId()
            text = "SHOW DETAILS"
            isFocusable = true
            visibility = View.GONE
            setOnClickListener { toggleDiagnosticDetails() }
        }
        box.addView(toggleDiagnostics, LinearLayout.LayoutParams(-1, -2))

        diagnostics = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            visibility = View.GONE
            setPadding(0, pad, 0, pad)
        }
        box.addView(diagnostics, LinearLayout.LayoutParams(-1, -2))

        host.nextFocusDownId = port.id
        port.nextFocusUpId = host.id
        port.nextFocusDownId = download.id
        download.nextFocusUpId = port.id
        download.nextFocusDownId = upload.id
        upload.nextFocusUpId = download.id
        upload.nextFocusDownId = copyDiagnostics.id
        copyDiagnostics.nextFocusUpId = upload.id
        copyDiagnostics.nextFocusDownId = toggleDiagnostics.id
        toggleDiagnostics.nextFocusUpId = copyDiagnostics.id

        host.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                port.requestFocus()
                true
            } else false
        }
        port.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                download.requestFocus()
                true
            } else false
        }
        host.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                port.requestFocus()
                true
            } else false
        }
        port.setOnKeyListener { _, keyCode, event ->
            when {
                event.action != KeyEvent.ACTION_DOWN -> false
                keyCode == KeyEvent.KEYCODE_DPAD_UP -> host.requestFocus().let { true }
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> download.requestFocus().let { true }
                else -> false
            }
        }

        setContentView(ScrollView(this).apply { addView(box) })
    }

    private fun hideKeyboard() {
        val keyboard = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.hideSoftInputFromWindow(port.windowToken, 0)
    }

    private fun runTest(reverse: Boolean) {
        val hostname = host.text.toString().trim()
        val portNumber = port.text.toString().toIntOrNull()
        if (hostname.isBlank()) {
            host.error = "Enter a server IP or hostname"
            return
        }
        if (portNumber == null || portNumber !in 1..65535) {
            port.error = "Enter a port between 1 and 65535"
            return
        }

        setBusy(true)
        clearDiagnostics()
        result.text = if (reverse) "Testing download…" else "Testing upload…"

        Thread {
            try {
                val output = executeIperf(hostname, portNumber, reverse)
                runOnUiThread {
                    result.text = prettyResult(output, reverse)
                    setBusy(false)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showFailure(hostname, portNumber, reverse, e)
                    setBusy(false)
                }
            }
        }.start()
    }

    private fun executeIperf(hostname: String, portNumber: Int, reverse: Boolean): String {
        val executable = File(applicationInfo.nativeLibraryDir, "libiperf3.so")
        if (!executable.canExecute()) {
            throw IllegalStateException("The bundled iperf3 engine is unavailable on this device")
        }

        val command = mutableListOf(
            executable.absolutePath,
            "-c", hostname,
            "-p", portNumber.toString(),
            "-t", "10",
            "-J"
        )
        if (reverse) command += "-R"

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw IllegalStateException("The test timed out after 20 seconds")
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (process.exitValue() != 0) {
            val message = output.ifBlank { "iperf3 exited with code ${process.exitValue()}" }
            throw IllegalStateException(message)
        }
        return output
    }

    private fun prettyResult(raw: String, reverse: Boolean): String {
        val json = JSONObject(raw)
        val end = json.getJSONObject("end")
        val summaryName = if (reverse) "sum_received" else "sum_sent"
        val bitsPerSecond = end.getJSONObject(summaryName).getDouble("bits_per_second")
        val summary = formatBitsPerSecond(bitsPerSecond)
        return buildString {
            append(if (reverse) "DOWNLOAD RESULT\n\n" else "UPLOAD RESULT\n\n")
            append(summary).append("\n\n")
            append("TCP · 10 seconds · ").append(host.text).append(':').append(port.text)
        }
    }

    private fun formatBitsPerSecond(bitsPerSecond: Double): String = when {
        bitsPerSecond >= 1_000_000_000 -> "%.2f Gbit/s".format(bitsPerSecond / 1_000_000_000)
        bitsPerSecond >= 1_000_000 -> "%.1f Mbit/s".format(bitsPerSecond / 1_000_000)
        bitsPerSecond >= 1_000 -> "%.1f Kbit/s".format(bitsPerSecond / 1_000)
        else -> "%.0f bit/s".format(bitsPerSecond)
    }

    private fun showFailure(
        hostname: String,
        portNumber: Int,
        reverse: Boolean,
        error: Exception
    ) {
        val reason = friendlyError(error)
        result.text = buildString {
            append("TEST FAILED\n\n")
            append(reason)
            append("\n\nUse COPY DIAGNOSTICS to share the technical details.")
        }
        diagnostics.text = buildDiagnostics(hostname, portNumber, reverse, error)
        diagnostics.visibility = View.GONE
        copyDiagnostics.visibility = View.VISIBLE
        toggleDiagnostics.visibility = View.VISIBLE
        toggleDiagnostics.text = "SHOW DETAILS"
    }

    private fun friendlyError(error: Exception): String {
        val message = error.message.orEmpty().trim()
        if (message.startsWith('{')) {
            runCatching {
                JSONObject(message).optString("error")
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return message.lineSequence().firstOrNull { it.isNotBlank() }
            ?: error.javaClass.simpleName
    }

    private fun buildDiagnostics(
        hostname: String,
        portNumber: Int,
        reverse: Boolean,
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
            appendLine("Test: ${if (reverse) "download/reverse" else "upload"} TCP, 10 seconds")
            appendLine("Server: $hostname:$portNumber")
            appendLine("Error type: ${error.javaClass.name}")
            appendLine("Error: ${error.message ?: "(no message)"}")
            appendLine()
            appendLine("Stack trace:")
            append(error.stackTraceToString())
        }
    }

    private fun copyDiagnosticsToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("iperf3 diagnostics", diagnostics.text))
        Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show()
    }

    private fun toggleDiagnosticDetails() {
        val show = diagnostics.visibility != View.VISIBLE
        diagnostics.visibility = if (show) View.VISIBLE else View.GONE
        toggleDiagnostics.text = if (show) "HIDE DETAILS" else "SHOW DETAILS"
    }

    private fun clearDiagnostics() {
        diagnostics.text = ""
        diagnostics.visibility = View.GONE
        copyDiagnostics.visibility = View.GONE
        toggleDiagnostics.visibility = View.GONE
    }

    private fun setBusy(busy: Boolean) {
        upload.isEnabled = !busy
        download.isEnabled = !busy
        host.isEnabled = !busy
        port.isEnabled = !busy
    }
}
