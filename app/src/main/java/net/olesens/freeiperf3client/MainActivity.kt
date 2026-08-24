package net.olesens.freeiperf3client

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.synaptictools.iperf.IPerf
import com.synaptictools.iperf.IPerfConfig
import com.synaptictools.iperf.IPerfResult
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var result: TextView
    private lateinit var upload: Button
    private lateinit var download: Button

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
            hint = "Server IP or hostname"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        box.addView(host, LinearLayout.LayoutParams(-1, -2))

        port = EditText(this).apply {
            setText("5201")
            hint = "Port"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        box.addView(port, LinearLayout.LayoutParams(-1, -2))

        download = Button(this).apply {
            text = "TEST DOWNLOAD  (SERVER → THIS DEVICE)"
            isFocusable = true
            setOnClickListener { runTest(true) }
        }
        box.addView(download, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })

        upload = Button(this).apply {
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
        }
        box.addView(result, LinearLayout.LayoutParams(-1, -2))

        setContentView(ScrollView(this).apply { addView(box) })
    }

    private fun runTest(reverse: Boolean) {
        val hostname = host.text.toString().trim()
        val portNumber = port.text.toString().toIntOrNull() ?: 5201
        if (hostname.isBlank()) {
            host.error = "Enter a server IP or hostname"
            return
        }

        setBusy(true)
        result.text = if (reverse) "Testing download…" else "Testing upload…"

        Thread {
            try {
                val stream = File(filesDir, "iperf3.${System.nanoTime()}")
                val response = IPerf.request(
                    IPerfConfig(
                        hostname = hostname,
                        port = portNumber,
                        stream = stream.path,
                        download = reverse,
                        useUDP = false,
                        json = false,
                        debug = false
                    )
                )
                val text = when (response) {
                    is IPerfResult.Success -> response.data.toString()
                    is IPerfResult.Error -> "Test failed:\n${response.error}"
                }
                runOnUiThread {
                    result.text = prettyResult(text, reverse)
                    setBusy(false)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    result.text = "Test failed:\n${e.message ?: e.javaClass.simpleName}"
                    setBusy(false)
                }
            }
        }.start()
    }

    private fun prettyResult(raw: String, reverse: Boolean): String {
        val lines = raw.lines().filter { it.contains("bits/sec") }
        val summary = lines.lastOrNull()
        return buildString {
            append(if (reverse) "DOWNLOAD RESULT\n\n" else "UPLOAD RESULT\n\n")
            if (summary != null) append(summary.trim()).append("\n\n")
            append(raw.trim())
        }
    }

    private fun setBusy(busy: Boolean) {
        upload.isEnabled = !busy
        download.isEnabled = !busy
        host.isEnabled = !busy
        port.isEnabled = !busy
    }
}
