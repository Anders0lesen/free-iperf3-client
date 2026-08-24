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
        download = addActionButton(box, "TCP DOWNLOAD  (SERVER â†’ THIS DEVICE)") {
            startSequence("TCP DOWNLOAD", listOf(TestMode.TCP_DOWNLOAD))
        }
        upload = addActionButton(box, "TCP UPLOAD  (THIS DEVICE â†’ SERVER)") {
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
 ßNº¶‰žËkºwµçEÑ”™Õ¸Á…ÉÍ•1¥Ù•UÁ‘…Ñ”¡µ½‘”èQ•ÍÑ5½‘”°‘…Ñ„è)M=9=‰©•Ð¤è1¥Ù•UÁ‘…Ñ”ì(€€€€€€€Ù…°ÍÕ´€ô‘…Ñ„¹½ÁÑ)M=9=‰©•Ð ‰ÍÕ´ˆ¤(€€€€€€€Ù…°É•Ù•ÉÍ”€ô‘…Ñ„¹½ÁÑ)M=9=‰©•Ð ‰ÍÕµ}‰¥‘¥É}É•Ù•ÉÍ”ˆ¤(€€€€€€€Ù…°•±…ÁÍ•€ôµ…à (€€€€€€€€€€€ÍÕ´ü¹½ÁÑ½Õ‰±” ‰•¹ˆ°€À¸À¤€üè€À¸À°(€€€€€€€€€€€É•Ù•ÉÍ”ü¹½ÁÑ½Õ‰±” ‰•¹ˆ°€À¸À¤€üè€À¸À(€€€€€€€€¤(€€€€€€€Ù…°ÁÉ¥µ…ÉåI…Ñ”€ôÍÕ´ü¹½ÁÑ¥½¹…±½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ¤(€€€€€€€Ù…°É•Ù•ÉÍ•I…Ñ”€ôÉ•Ù•ÉÍ”ü¹½ÁÑ¥½¹…±½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ¤(€€€€€€€Ù…°¥¹Ñ•ÉÙ…±Q•áÐ€ô±¥ÍÑ=™9½Ñ9Õ±° (€€€€€€€€€€€ÍÕ´ü¹±•Ðì™½Éµ…Ñ%¹Ñ•ÉÙ…°¡¥Ð°¥˜€¡µ½‘”€ôôQ•ÍÑ5½‘”¹QA}=]91=ñðµ½‘”€ôôQ•ÍÑ5½‘”¹UA}=]91=¤€‹ŠLˆ•±Í”€‹ŠDˆ¤ô°(€€€€€€€€€€€É•Ù•ÉÍ”ü¹±•Ðì™½Éµ…Ñ%¹Ñ•ÉÙ…°¡¥Ð°€‹ŠLˆ¤ô(€€€€€€€€¤¹©½¥¹Q½MÑÉ¥¹œ ‰q¸ˆ¤¹¥™	±…¹¬ì¹Õ±°ô(€€€€€€€É•ÑÕÉ¸Ý¡•¸€¡µ½‘”¤ì(€€€€€€€€€€€Q•ÍÑ5½‘”¹QA}=]91=°Q•ÍÑ5½‘”¹UA}=]91=€´ø1¥Ù•UÁ‘…Ñ” (€€€€€€€€€€€€€€€•±…ÁÍ•‘M•½¹‘Ì€ô•±…ÁÍ•°(€€€€€€€€€€€€€€€‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹€ôÁÉ¥µ…ÉåI…Ñ”°(€€€€€€€€€€€€€€€©¥ÑÑ•É5Ì€ôÍÕ´ü¹½ÁÑ¥½¹…±½Õ‰±” ‰©¥ÑÑ•É}µÌˆ¤°(€€€€€€€€€€€€€€€±½ÍÍA•É•¹Ð€ôÍÕ´ü¹½ÁÑ¥½¹…±½Õ‰±” ‰±½ÍÑ}Á•É•¹Ðˆ¤°(€€€€€€€€€€€€€€€¥¹Ñ•ÉÙ…±Q•áÐ€ô¥¹Ñ•ÉÙ…±Q•áÐ(€€€€€€€€€€€€¤(€€€€€€€€€€€Q•ÍÑ5½‘”¹QA}	%%IQ%=90€´ø1¥Ù•UÁ‘…Ñ” (€€€€€€€€€€€€€€€•±…ÁÍ•‘M•½¹‘Ì€ô•±…ÁÍ•°(€€€€€€€€€€€€€€€ÕÁ±½…‘	¥ÑÍA•ÉM•½¹€ôÁÉ¥µ…ÉåI…Ñ”°(€€€€€€€€€€€€€€€‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹€ôÉ•Ù•ÉÍ•I…Ñ”°(€€€€€€€€€€€€€€€¥¹Ñ•ÉÙ…±Q•áÐ€ô¥¹Ñ•ÉÙ…±Q•áÐ(€€€€€€€€€€€€¤(€€€€€€€€€€€•±Í”€´ø1¥Ù•UÁ‘…Ñ” (€€€€€€€€€€€€€€€•±…ÁÍ•‘M•½¹‘Ì€ô•±…ÁÍ•°(€€€€€€€€€€€€€€€ÕÁ±½…‘	¥ÑÍA•ÉM•½¹€ôÁÉ¥µ…ÉåI…Ñ”°(€€€€€€€€€€€€€€€©¥ÑÑ•É5Ì€ôÍÕ´ü¹½ÁÑ¥½¹…±½Õ‰±” ‰©¥ÑÑ•É}µÌˆ¤°(€€€€€€€€€€€€€€€±½ÍÍA•É•¹Ð€ôÍÕ´ü¹½ÁÑ¥½¹…±½Õ‰±” ‰±½ÍÑ}Á•É•¹Ðˆ¤°(€€€€€€€€€€€€€€€¥¹Ñ•ÉÙ…±Q•áÐ€ô¥¹Ñ•ÉÙ…±Q•áÐ(€€€€€€€€€€€€¤(€€€€€€€ô(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…Ñ%¹Ñ•ÉÙ…°¡ÍÕ´è)M=9=‰©•Ð°‘¥É•Ñ¥½¸èMÑÉ¥¹œ¤èMÑÉ¥¹œì(€€€€€€€Ù…°ÍÑ…ÉÐ€ôÍÕ´¹½ÁÑ½Õ‰±” ‰ÍÑ…ÉÐˆ°€À¸À¤(€€€€€€€Ù…°•¹€ôÍÕ´¹½ÁÑ½Õ‰±” ‰•¹ˆ°€À¸À¤(€€€€€€€Ù…°É…Ñ”€ôÍÕ´¹½ÁÑ½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ°€À¸À¤(€€€€€€€Ù…°‰åÑ•Ì€ôÍÕ´¹½ÁÑ½Õ‰±” ‰‰åÑ•Ìˆ°€À¸À¤(€€€€€€€É•ÑÕÉ¸‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€€€€€…ÁÁ•¹¡™½Éµ…Ñ9Õµ‰•È¡ÍÑ…ÉÐ°€È¤¤¹…ÁÁ•¹ ‹ŠLˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ9Õµ‰•È¡•¹°€È¤¤¹…ÁÁ•¹ ˆÌ€€ˆ¤(€€€€€€€€€€€…ÁÁ•¹¡‘¥É•Ñ¥½¸¤¹…ÁÁ•¹ ˆ€ˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ	¥ÑÍA•ÉM•½¹¡É…Ñ”¤¤(€€€€€€€€€€€¥˜€¡‰åÑ•Ì€ø€À¤…ÁÁ•¹ ˆ€€ˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ	åÑ•Ì¡‰åÑ•Ì¤¤(€€€€€€€€€€€ÍÕ´¹½ÁÑ¥½¹…±½Õ‰±” ‰©¥ÑÑ•É}µÌˆ¤ü¹±•Ðì…ÁÁ•¹ ˆ€©¥ÑÑ•È€ˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ5¥±±¥Í•½¹‘Ì¡¥Ð¤¤ô(€€€€€€€€€€€ÍÕ´¹½ÁÑ¥½¹…±½Õ‰±” ‰±½ÍÑ}Á•É•¹Ðˆ¤ü¹±•Ðì…ÁÁ•¹ ˆ€±½ÍÌ€ˆ¤¹…ÁÁ•¹¡™½Éµ…ÑA•É•¹Ð¡¥Ð¤¤ô(€€€€€€€ô(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸Á…ÉÍ•¥¹…±I•ÍÕ±Ð (€€€€€€€µ½‘”èQ•ÍÑ5½‘”°(€€€€€€€•¹è)M=9=‰©•Ð°(€€€€€€€½¹¹•Ñ¥½¸èMÑÉ¥¹œü°(€€€€€€€É…Ý=ÕÑÁÕÐèMÑÉ¥¹œ(€€€€¤èQ•ÍÑI•ÍÕ±Ðì(€€€€€€€Ù…°Í•¹Ð€ô•¹¹½ÁÑ)M=9=‰©•Ð ‰ÍÕµ}Í•¹Ðˆ¤(€€€€€€€Ù…°É••¥Ù•€ô•¹¹½ÁÑ)M=9=‰©•Ð ‰ÍÕµ}É••¥Ù•ˆ¤(€€€€€€€É•ÑÕÉ¸Ý¡•¸€¡µ½‘”¤ì(€€€€€€€€€€€Q•ÍÑ5½‘”¹QP€´øQ•ÍÑI•ÍÕ±Ð¡µ½‘”°½¹¹•Ñ¥½¸°É…Ý=ÕÑÁÕÐ€ôÉ…Ý=ÕÑÁÕÐ¤(€€€€€€€€€€€Q•ÍÑ5½‘”¹QA}=]91=€´øQ•ÍÑI•ÍÕ±Ð (€€€€€€€€€€€€€€€µ½‘”°½¹¹•Ñ¥½¸°(€€€€€€€€€€€€€€€‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹€ôÉ••¥Ù•¹É•ÅÕ¥É•½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ°µ½‘”¤°(€€€€€€€€€€€€€€€É…Ý=ÕÑÁÕÐ€ôÉ…Ý=ÕÑÁÕÐ(€€€€€€€€€€€€¤(€€€€€€€€€€€Q•ÍÑ5½‘”¹QA}UA1=€´øQ•ÍÑI•ÍÕ±Ð (€€€€€€€€€€€€€€€µ½‘”°½¹¹•Ñ¥½¸°(€€€€€€€€€€€€€€€ÕÁ±½…‘	¥ÑÍA•ÉM•½¹€ôÍ•¹Ð¹É•ÅÕ¥É•½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ°µ½‘”¤°(€€€€€€€€€€€€€€€É…Ý=ÕÑÁÕÐ€ôÉ…Ý=ÕÑÁÕÐ(€€€€€€€€€€€€¤(€€€€€€€€€€€Q•ÍÑ5½‘”¹QA}	%%IQ%=90€´øì(€€€€€€€€€€€€€€€Ù…°É•Ù•ÉÍ•I••¥Ù•€ô•¹¹½ÁÑ)M=9=‰©•Ð ‰ÍÕµ}É••¥Ù•‘}‰¥‘¥É}É•Ù•ÉÍ”ˆ¤(€€€€€€€€€€€€€€€€€€€€üèÑ¡É½Ü%Á•É™…¥±ÕÉ” (€€€€€€€€€€€€€€€€€€€€€€€€‰Q¡”Í•ÉÙ•È‘¥¹½ÐÉ•ÑÕÉ¸‰¥‘¥É•Ñ¥½¹…°É•ÍÕ±ÑÌìÕÁ‘…Ñ”¥ÑÌ¥Á•É˜ÌÙ•ÉÍ¥½¸ˆ°(€€€€€€€€€€€€€€€€€€€€€€€É…Ý=ÕÑÁÕÐ(€€€€€€€€€€€€€€€€€€€€¤(€€€€€€€€€€€€€€€Q•ÍÑI•ÍÕ±Ð (€€€€€€€€€€€€€€€€€€€µ½‘”°½¹¹•Ñ¥½¸°(€€€€€€€€€€€€€€€€€€€ÕÁ±½…‘	¥ÑÍA•ÉM•½¹€ôÍ•¹Ð¹É•ÅÕ¥É•½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ°µ½‘”¤°(€€€€€€€€€€€€€€€€€€€‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹€ôÉ•Ù•ÉÍ•I••¥Ù•¹É•ÅÕ¥É•½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ°µ½‘”¤°(€€€€€€€€€€€€€€€€€€€É…Ý=ÕÑÁÕÐ€ôÉ…Ý=ÕÑÁÕÐ(€€€€€€€€€€€€€€€€¤(€€€€€€€€€€€ô(€€€€€€€€€€€Q•ÍÑ5½‘”¹UA}=]91=°Q•ÍÑ5½‘”¹UA}UA1=€´øì(€€€€€€€€€€€€€€€Ù…°ÍÕµµ…Éä€ôÉ••¥Ù•(€€€€€€€€€€€€€€€€€€€€üèÑ¡É½Ü%Á•É™…¥±ÕÉ” ‰¥Á•É˜Ì‘¥¹½ÐÉ•ÑÕÉ¸U@É••¥Ù•ÈÍÑ…Ñ¥ÍÑ¥Ìˆ°É…Ý=ÕÑÁÕÐ¤(€€€€€€€€€€€€€€€Ù…°É…Ñ”€ôÍÕµµ…Éä¹É•ÅÕ¥É•½Õ‰±” ‰‰¥ÑÍ}Á•É}Í•½¹ˆ°µ½‘”¤(€€€€€€€€€€€€€€€Q•ÍÑI•ÍÕ±Ð (€€€€€€€€€€€€€€€€€€€µ½‘”°½¹¹•Ñ¥½¸°(€€€€€€€€€€€€€€€€€€€ÕÁ±½…‘	¥ÑÍA•ÉM•½¹€ôÉ…Ñ”¹Ñ…­•%˜ìµ½‘”€ôôQ•ÍÑ5½‘”¹UA}UA1=ô°(€€€€€€€€€€€€€€€€€€€‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹€ôÉ…Ñ”¹Ñ…­•%˜ìµ½‘”€ôôQ•ÍÑ5½‘”¹UA}=]91=ô°(€€€€€€€€€€€€€€€€€€€©¥ÑÑ•É5Ì€ôÍÕµµ…Éä¹½ÁÑ¥½¹…±½Õ‰±” ‰©¥ÑÑ•É}µÌˆ¤€üè€À¸À°(€€€€€€€€€€€€€€€€€€€±½ÍÍA•É•¹Ð€ôÍÕµµ…Éä¹½ÁÑ¥½¹…±½Õ‰±” ‰±½ÍÑ}Á•É•¹Ðˆ¤€üè€À¸À°(€€€€€€€€€€€€€€€€€€€Á…­•ÑÌ€ôÍÕµµ…Éä¹½ÁÑ1½¹œ ‰Á…­•ÑÌˆ°€À¤¹Ñ…­•%˜ì¥Ð€ø€Àô°(€€€€€€€€€€€€€€€€€€€É…Ý=ÕÑÁÕÐ€ôÉ…Ý=ÕÑÁÕÐ(€€€€€€€€€€€€€€€€¤(€€€€€€€€€€€ô(€€€€€€€ô(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸)M=9=‰©•Ðü¹É•ÅÕ¥É•½Õ‰±”¡­•äèMÑÉ¥¹œ°µ½‘”èQ•ÍÑ5½‘”¤è½Õ‰±”ì(€€€€€€€Ù…°Ù…±Õ”€ôÑ¡¥Ìü¹½ÁÑ¥½¹…±½Õ‰±”¡­•ä¤(€€€€€€€É•ÑÕÉ¸Ù…±Õ”€üèÑ¡É½Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰5¥ÍÍ¥¹œ€‘­•ä¥¸€‘íµ½‘”¹±…‰•±ôÉ•ÍÕ±Ðˆ¤(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸)M=9=‰©•Ð¹½ÁÑ¥½¹…±½Õ‰±”¡­•äèMÑÉ¥¹œ¤è½Õ‰±”ü€ô(€€€€€€€¥˜€¡¡…Ì¡­•ä¤€˜˜€…¥Í9Õ±°¡­•ä¤¤½ÁÑ½Õ‰±”¡­•ä¤•±Í”¹Õ±°((€€€ÁÉ¥Ù…Ñ”™Õ¸Í¡½ÝMÕ•ÍÌ¡Ñ¥Ñ±”èMÑÉ¥¹œ°½¹™¥œèQ•ÍÑ½¹™¥œ°É•ÍÕ±ÑÌè1¥ÍÐñQ•ÍÑI•ÍÕ±Ðø¤ì(€€€€€€€ÁÉ½É•ÍÌ¹ÁÉ½É•ÍÌ€ôÁÉ½É•ÍÌ¹µ…à(€€€€€€€Í•ÑMÑ…ÑÕÌ ‰½µÁ±•Ñ”ƒŠrLˆ°€ˆŒØÙ	Ùˆ¤(€€€€€€€±¥Ù”¹Ñ•áÐ€ô€‰±°É•ÅÕ•ÍÑ•Ñ•ÍÑÌ½µÁ±•Ñ•ÍÕ•ÍÍ™Õ±±ä¸ˆ(€€€€€€€É•ÍÕ±Ð¹Ñ•áÐ€ô™½Éµ…ÑI•ÍÕ±ÑÌ¡Ñ¥Ñ±”°½¹™¥œ°É•ÍÕ±ÑÌ¤(€€€€€€€Í¡…É•Q•áÐ€ô‰Õ¥±‘M¡…É•I•Á½ÉÐ¡Ñ¥Ñ±”°½¹™¥œ°É•ÍÕ±ÑÌ°¥¹Ñ•ÉÙ…±1¥¹•Ì¹©½¥¹Q½MÑÉ¥¹œ ‰q¸ˆ¤¤(€€€€€€€‘•Ñ…¥±Ì¹Ñ•áÐ€ôÍ¡…É•Q•áÐ(€€€€€€€½ÁåM¡…É”¹Ñ•áÐ€ô€‰=AdIMU1QLˆ(€€€€€€€½ÁåM¡…É”¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹Y%M%	1(€€€€€€€Ñ½±••Ñ…¥±Ì¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹Y%M%	1(€€€€€€€Í•Ñ	ÕÍä¡™…±Í”¤(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸Í¡½Ý…¥±ÕÉ” (€€€€€€€Ñ¥Ñ±”èMÑÉ¥¹œ°(€€€€€€€½¹™¥œèQ•ÍÑ½¹™¥œ°(€€€€€€€µ½‘”èQ•ÍÑ5½‘”°(€€€€€€€½µÁ±•Ñ•è1¥ÍÐñQ•ÍÑI•ÍÕ±Ðø°(€€€€€€€•ÉÉ½Èèá•ÁÑ¥½¸(€€€€¤ì(€€€€€€€Í•ÑMÑ…ÑÕÌ ‰…¥±•ƒ
Ü€‘íµ½‘”¹±…‰•±ôˆ°€ˆÔÌÔÀˆ¤(€€€€€€€±¥Ù”¹Ñ•áÐ€ô™É¥•¹‘±åÉÉ½È¡•ÉÉ½È¤(€€€€€€€É•ÍÕ±Ð¹Ñ•áÐ€ô‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€€€€€…ÁÁ•¹ ‰QMP%1q¹q¸ˆ¤(€€€€€€€€€€€…ÁÁ•¹¡™É¥•¹‘±åÉÉ½È¡•ÉÉ½È¤¤(€€€€€€€€€€€¥˜€¡½µÁ±•Ñ•¹¥Í9½ÑµÁÑä ¤¤ì(€€€€€€€€€€€€€€€…ÁÁ•¹ ‰q¹q¹½µÁ±•Ñ•‰•™½É”Ñ¡”™…¥±ÕÉ”éq¸ˆ¤(€€€€€€€€€€€€€€€…ÁÁ•¹¡™½Éµ…ÑI•ÍÕ±ÑÌ¡Ñ¥Ñ±”°½¹™¥œ°½µÁ±•Ñ•¤¤(€€€€€€€€€€€ô(€€€€€€€€€€€…ÁÁ•¹ ‰q¹q¹UÍ”=Ad%9=MQ%LÑ¼Í¡…É”Ñ¡”Ñ•¡¹¥…°‘•Ñ…¥±Ì¸ˆ¤(€€€€€€€ô(€€€€€€€Í¡…É•Q•áÐ€ô‰Õ¥±‘¥…¹½ÍÑ¥I•Á½ÉÐ¡Ñ¥Ñ±”°½¹™¥œ°µ½‘”°½µÁ±•Ñ•°•ÉÉ½È¤(€€€€€€€‘•Ñ…¥±Ì¹Ñ•áÐ€ôÍ¡…É•Q•áÐ(€€€€€€€‘•Ñ…¥±Ì¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹=9(€€€€€€€½ÁåM¡…É”¹Ñ•áÐ€ô€‰=Ad%9=MQ%Lˆ(€€€€€€€½ÁåM¡…É”¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹Y%M%	1(€€€€€€€Ñ½±••Ñ…¥±Ì¹Ñ•áÐ€ô€‰M!=\Q%1Lˆ(€€€€€€€Ñ½±••Ñ…¥±Ì¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹Y%M%	1(€€€€€€€Í•Ñ	ÕÍä¡™…±Í”¤(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…ÑI•ÍÕ±ÑÌ (€€€€€€€Ñ¥Ñ±”èMÑÉ¥¹œ°(€€€€€€€½¹™¥œèQ•ÍÑ½¹™¥œ°(€€€€€€€É•ÍÕ±ÑÌè1¥ÍÐñQ•ÍÑI•ÍÕ±Ðø(€€€€¤èMÑÉ¥¹œ€ô‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€…ÁÁ•¹¡Ñ¥Ñ±”¤¹…ÁÁ•¹ ˆIMU1QMq¸ˆ¤(€€€€€€€É•ÍÕ±ÑÌ¹™½É… ìÑ•ÍÐ€´ø(€€€€€€€€€€€…ÁÁ•¹ ‰q¸ˆ¤¹…ÁÁ•¹¡™½Éµ…ÑI•ÍÕ±Ð¡Ñ•ÍÐ°½¹™¥œ¤¤¹…ÁÁ•¹ ‰q¸ˆ¤(€€€€€€€ô(€€€ô¹ÑÉ¥µ¹ ¤((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…ÑI•ÍÕ±Ð¡Ñ•ÍÐèQ•ÍÑI•ÍÕ±Ð°½¹™¥œèQ•ÍÑ½¹™¥œ¤èMÑÉ¥¹œ€ôÝ¡•¸€¡Ñ•ÍÐ¹µ½‘”¤ì(€€€€€€€Q•ÍÑ5½‘”¹QP€´ø‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€€€€€…ÁÁ•¹ ‹ŠrL%AIÌMIYHQQˆ¤(€€€€€€€€€€€Ñ•ÍÐ¹½¹¹•Ñ¥½¸ü¹±•Ðì…ÁÁ•¹ ‰q¸€½¹¹•Ñ•Ñ¼€ˆ¤¹…ÁÁ•¹¡¥Ð¤ô(€€€€€€€€€€€…ÁÁ•¹ ‰q¸€½¹ÑÉ½°…¹‘…Ñ„Ñ•ÍÐÍÕ••‘•ˆ¤(€€€€€€€ô(€€€€€€€Q•ÍÑ5½‘”¹QA}=]91=€´ø€‰Q@‘½Ý¹±½…‘q¸€€‘í™½Éµ…Ñ	¥ÑÍA•ÉM•½¹¡Ñ•ÍÐ¹‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹„„¥ôˆ(€€€€€€€Q•ÍÑ5½‘”¹QA}UA1=€´ø€‰Q@ÕÁ±½…‘q¸€€‘í™½Éµ…Ñ	¥ÑÍA•ÉM•½¹¡Ñ•ÍÐ¹ÕÁ±½…‘	¥ÑÍA•ÉM•½¹„„¥ôˆ(€€€€€€€Q•ÍÑ5½‘”¹QA}	%%IQ%=90€´ø‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€€€€€…ÁÁ•¹ ‰Q@‰¥‘¥É•Ñ¥½¹…°€¡Í¥µÕ±Ñ…¹•½ÕÌ¤ˆ¤(€€€€€€€€€€€…ÁÁ•¹ ‰q¸€½Ý¹±½…è€ˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ	¥ÑÍA•ÉM•½¹¡Ñ•ÍÐ¹‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹„„¤¤(€€€€€€€€€€€…ÁÁ•¹ ‰q¸€UÁ±½…è€€€ˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ	¥ÑÍA•ÉM•½¹¡Ñ•ÍÐ¹ÕÁ±½…‘	¥ÑÍA•ÉM•½¹„„¤¤(€€€€€€€ô(€€€€€€€Q•ÍÑ5½‘”¹UA}=]91=°Q•ÍÑ5½‘”¹UA}UA1=€´ø™½Éµ…ÑU‘ÁI•ÍÕ±Ð¡Ñ•ÍÐ°½¹™¥œ¤(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…ÑU‘ÁI•ÍÕ±Ð¡Ñ•ÍÐèQ•ÍÑI•ÍÕ±Ð°½¹™¥œèQ•ÍÑ½¹™¥œ¤èMÑÉ¥¹œì(€€€€€€€Ù…°É…Ñ”€ôÑ•ÍÐ¹‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹€üèÑ•ÍÐ¹ÕÁ±½…‘	¥ÑÍA•ÉM•½¹€üè€À¸À(€€€€€€€Ù…°€¡Í½É”°É…‘”¤€ôÍ½É•U‘À¡Ñ•ÍÐ°½¹™¥œ¤(€€€€€€€Ù…°‘¥É•Ñ¥½¸€ô¥˜€¡Ñ•ÍÐ¹µ½‘”€ôôQ•ÍÑ5½‘”¹UA}=]91=¤€‰‘½Ý¹±½…ˆ•±Í”€‰ÕÁ±½…ˆ(€€€€€€€É•ÑÕÉ¸‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€€€€€…ÁÁ•¹ ‰U@€ˆ¤¹…ÁÁ•¹¡‘¥É•Ñ¥½¸¤¹…ÁÁ•¹ ˆÅÕ…±¥Ñäˆ¤(€€€€€€€€€€€…ÁÁ•¹ ‰q¸€M½É”è€ˆ¤¹…ÁÁ•¹¡Í½É”¤¹…ÁÁ•¹ ˆ¼ÄÀÀƒ
Ü€ˆ¤¹…ÁÁ•¹¡É…‘”¤(€€€€€€€€€€€…ÁÁ•¹ ‰q¸€I••¥Ù•è€ˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ	¥ÑÍA•ÉM•½¹¡É…Ñ”¤¤(€€€€€€€€€€€€€€€€¹…ÁÁ•¹ ˆƒ
ÜQ…É•Ðè€ˆ¤¹…ÁÁ•¹¡½¹™¥œ¹Õ‘ÁQ…É•Ñ5‰ÁÌ¤¹…ÁÁ•¹ ˆ5‰¥Ð½Ìˆ¤(€€€€€€€€€€€…ÁÁ•¹ ‰q¸€1½ÍÌè€ˆ¤¹…ÁÁ•¹¡™½Éµ…ÑA•É•¹Ð¡Ñ•ÍÐ¹±½ÍÍA•É•¹Ð€üè€À¸À¤¤(€€€€€€€€€€€€€€€€¹…ÁÁ•¹ ˆƒ
Ü)¥ÑÑ•Èè€ˆ¤¹…ÁÁ•¹¡™½Éµ…Ñ5¥±±¥Í•½¹‘Ì¡Ñ•ÍÐ¹©¥ÑÑ•É5Ì€üè€À¸À¤¤(€€€€€€€€€€€Ñ•ÍÐ¹Á…­•ÑÌü¹±•Ðì…ÁÁ•¹ ˆƒ
ÜA…­•ÑÌè€ˆ¤¹…ÁÁ•¹¡¥Ð¤ô(€€€€€€€ô(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸Í½É•U‘À¡Ñ•ÍÐèQ•ÍÑI•ÍÕ±Ð°½¹™¥œèQ•ÍÑ½¹™¥œ¤èA…¥Èñ%¹Ð°MÑÉ¥¹œøì(€€€€€€€Ù…°É••¥Ù•‘I…Ñ”€ôÑ•ÍÐ¹‘½Ý¹±½…‘	¥ÑÍA•ÉM•½¹€üèÑ•ÍÐ¹ÕÁ±½…‘	¥ÑÍA•ÉM•½¹€üè€À¸À(€€€€€€€Ù…°Ñ…É•ÑI…Ñ”€ô½¹™¥œ¹Õ‘ÁQ…É•Ñ5‰ÁÌ€¨€Å|ÀÀÁ|ÀÀÀ¸À(€€€€€€€Ù…°‘•±¥Ù•ÉåI…Ñ¥¼€ô¥˜€¡Ñ…É•ÑI…Ñ”€ø€À¤É••¥Ù•‘I…Ñ”€¼Ñ…É•ÑI…Ñ”•±Í”€Ä¸À(€€€€€€€Ù…°É…Ñ•A•¹…±Ñä€ô¥˜€¡‘•±¥Ù•ÉåI…Ñ¥¼€øô€À¸äÔ¤€À¸À•±Í”µ¥¸ ÐÀ¸À°€ À¸äÔ€´‘•±¥Ù•ÉåI…Ñ¥¼¤€¨€àÀ¸À¤(€€€€€€€Ù…°±½ÍÍA•¹…±Ñä€ôµ¥¸ àÀ¸À°€¡Ñ•ÍÐ¹±½ÍÍA•É•¹Ð€üè€À¸À¤€¨€ÈÀ¸À¤(€€€€€€€Ù…°©¥ÑÑ•ÉA•¹…±Ñä€ôµ¥¸ ÈÀ¸À°µ…à À¸À°€¡Ñ•ÍÐ¹©¥ÑÑ•É5Ì€üè€À¸À¤€´€Ô¸À¤€¨€À¸à¤(€€€€€€€Ù…°Í½É”€ô€ ÄÀÀ¸À€´É…Ñ•A•¹…±Ñä€´±½ÍÍA•¹…±Ñä€´©¥ÑÑ•ÉA•¹…±Ñä¤(€€€€€€€€€€€€¹É½Õ¹‘Q½%¹Ð ¤¹½•É•%¸ À°€ÄÀÀ¤(€€€€€€€Ù…°É…‘”€ôÝ¡•¸ì(€€€€€€€€€€€Í½É”€øô€äÀ€´ø€‰a119Pˆ(€€€€€€€€€€€Í½É”€øô€ÜÔ€´ø€‰==ˆ(€€€€€€€€€€€Í½É”€øô€ÔÀ€´ø€‰%Hˆ(€€€€€€€€€€€•±Í”€´ø€‰A==Hˆ(€€€€€€€ô(€€€€€€€É•ÑÕÉ¸Í½É”Ñ¼É…‘”(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸‰Õ¥±‘M¡…É•I•Á½ÉÐ (€€€€€€€Ñ¥Ñ±”èMÑÉ¥¹œ°(€€€€€€€½¹™¥œèQ•ÍÑ½¹™¥œ°(€€€€€€€É•ÍÕ±ÑÌè1¥ÍÐñQ•ÍÑI•ÍÕ±Ðø°(€€€€€€€¥¹Ñ•ÉÙ…±=ÕÑÁÕÐèMÑÉ¥¹œ(€€€€¤èMÑÉ¥¹œ€ô‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€…ÁÁ•¹‘1¥¹” ‰É•”¥Á•É˜Ì±¥•¹Ð€‘Ñ¥Ñ±”É•Á½ÉÐˆ¤(€€€€€€€…ÁÁ•¹‘1¥¹” ‰Q¥µ”€¡UQ¤è€‘í%¹ÍÑ…¹Ð¹¹½Ü ¥ôˆ¤(€€€€€€€…ÁÁ•¹‘1¥¹” ‰M•ÉÙ•Èè€‘í½¹™¥œ¹¡½ÍÑ¹…µ•ôè‘í½¹™¥œ¹Á½ÉÑôˆ¤(€€€€€€€…ÁÁ•¹‘1¥¹” ‰U@Ñ…É•Ðè€‘í½¹™¥œ¹Õ‘ÁQ…É•Ñ5‰ÁÍô5‰¥Ð½Ìˆ¤(€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€…ÁÁ•¹¡™½Éµ…ÑI•ÍÕ±ÑÌ¡Ñ¥Ñ±”°½¹™¥œ°É•ÍÕ±ÑÌ¤¤(€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€…ÁÁ•¹‘1¥¹” ‰½µµ…¹‘Ìèˆ¤(€€€€€€€É•ÍÕ±ÑÌ¹™½É… ì…ÁÁ•¹‘1¥¹”¡™½Éµ…Ñ¥ÍÁ±…å½µµ…¹¡½¹™¥œ°¥Ð¹µ½‘”¤¤ô(€€€€€€€¥˜€¡¥¹Ñ•ÉÙ…±=ÕÑÁÕÐ¹¥Í9½Ñ	±…¹¬ ¤¤ì(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰A•ÈµÍ•½¹¥¹Ñ•ÉÙ…±Ìèˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹”¡¥¹Ñ•ÉÙ…±=ÕÑÁÕÐ¤(€€€€€€€ô(€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€…ÁÁ•¹ ‰U@Í½É”¥Ì…¸…ÁÀ¡•ÕÉ¥ÍÑ¥Œ‰…Í•½¸É••¥Ù•É…Ñ”°Á…­•Ð±½ÍÌ°…¹©¥ÑÑ•È¸ˆ¤(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸‰Õ¥±‘¥…¹½ÍÑ¥I•Á½ÉÐ (€€€€€€€Ñ¥Ñ±”èMÑÉ¥¹œ°(€€€€€€€½¹™¥œèQ•ÍÑ½¹™¥œ°(€€€€€€€µ½‘”èQ•ÍÑ5½‘”°(€€€€€€€½µÁ±•Ñ•è1¥ÍÐñQ•ÍÑI•ÍÕ±Ðø°(€€€€€€€•ÉÉ½Èèá•ÁÑ¥½¸(€€€€¤èMÑÉ¥¹œì(€€€€€€€Ù…°Á…­…•%¹™¼€ôÁ…­…•5…¹…•È¹•ÑA…­…•%¹™¼¡Á…­…•9…µ”°€À¤(€€€€€€€É•ÑÕÉ¸‰Õ¥±‘MÑÉ¥¹œì(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰É•”¥Á•É˜Ì±¥•¹Ð‘¥…¹½ÍÑ¥ŒÉ•Á½ÉÐˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰AÉ¥Ù…äèÉ•Ù¥•ÜÑ¡”Í•ÉÙ•È…¹‘•Ù¥”™¥•±‘Ì‰•™½É”Í¡…É¥¹œÁÕ‰±¥±äˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰Q¥µ”€¡UQ¤è€‘í%¹ÍÑ…¹Ð¹¹½Ü ¥ôˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰ÁÀè€‘íÁ…­…•%¹™¼¹Ù•ÉÍ¥½¹9…µ•ô€ ‘íÁ…­…•%¹™¼¹±½¹Y•ÉÍ¥½¹½‘•ô¤ˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰¹¥¹”è¥Á•É˜Ì€Ì¸ÈÄˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰¹‘É½¥è€‘í	Õ¥±¹YIM%=8¹I1Mô€¡A$€‘í	Õ¥±¹YIM%=8¹M-}%9Qô¤ˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰•Ù¥”è€‘í	Õ¥±¹59UQUIIô€‘í	Õ¥±¹5=1ôˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰	%Ìè€‘í	Õ¥±¹MUAA=IQ}	%L¹©½¥¹Q½MÑÉ¥¹œ ¥ôˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰M•ÅÕ•¹”è€‘Ñ¥Ñ±”ˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰…¥±•ÍÑ…”è€‘íµ½‘”¹±…‰•±ôˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰M•ÉÙ•Èè€‘í½¹™¥œ¹¡½ÍÑ¹…µ•ôè‘í½¹™¥œ¹Á½ÉÑôˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰U@Ñ…É•Ðè€‘í½¹™¥œ¹Õ‘ÁQ…É•Ñ5‰ÁÍô5‰¥Ð½Ìˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰ÉÉ½ÈÑåÁ”è€‘í•ÉÉ½È¹©…Ù…±…ÍÌ¹¹…µ•ôˆ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰ÉÉ½Èè€‘í•ÉÉ½È¹µ•ÍÍ…”€üè€ˆ¡¹¼µ•ÍÍ…”¤‰ôˆ¤(€€€€€€€€€€€¥˜€¡½µÁ±•Ñ•¹¥Í9½ÑµÁÑä ¤¤ì(€€€€€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰½µÁ±•Ñ•É•ÍÕ±ÑÌèˆ¤(€€€€€€€€€€€€€€€…ÁÁ•¹‘1¥¹”¡™½Éµ…ÑI•ÍÕ±ÑÌ¡Ñ¥Ñ±”°½¹™¥œ°½µÁ±•Ñ•¤¤(€€€€€€€€€€€ô(€€€€€€€€€€€¥˜€¡•ÉÉ½È¥Ì%Á•É™…¥±ÕÉ”€˜˜•ÉÉ½È¹É…Ý=ÕÑÁÕÐ¹¥Í9½Ñ	±…¹¬ ¤¤ì(€€€€€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰I…Ü¥Á•É˜Ì½ÕÑÁÕÐèˆ¤(€€€€€€€€€€€€€€€…ÁÁ•¹‘1¥¹”¡•ÉÉ½È¹É…Ý=ÕÑÁÕÐ¹ÑÉ¥´ ¤¤(€€€€€€€€€€€ô(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ¤(€€€€€€€€€€€…ÁÁ•¹‘1¥¹” ‰MÑ…¬ÑÉ…”èˆ¤(€€€€€€€€€€€…ÁÁ•¹¡•ÉÉ½È¹ÍÑ…­QÉ…•Q½MÑÉ¥¹œ ¤¤(€€€€€€€ô(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸™É¥•¹‘±åÉÉ½È¡•ÉÉ½Èèá•ÁÑ¥½¸¤èMÑÉ¥¹œ€ô(€€€€€€€•ÉÉ½È¹µ•ÍÍ…”ü¹±¥¹•M•ÅÕ•¹” ¤ü¹™¥ÉÍÑ=É9Õ±°ì¥Ð¹¥Í9½Ñ	±…¹¬ ¤ô(€€€€€€€€€€€€üè•ÉÉ½È¹©…Ù…±…ÍÌ¹Í¥µÁ±•9…µ”((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…Ñ	¥ÑÍA•ÉM•½¹¡‰¥ÑÍA•ÉM•½¹è½Õ‰±”¤èMÑÉ¥¹œ€ôÝ¡•¸ì(€€€€€€€‰¥ÑÍA•ÉM•½¹€øô€Å|ÀÀÁ|ÀÀÁ|ÀÀÀ€´ø™½Éµ…Ñ9Õµ‰•È¡‰¥ÑÍA•ÉM•½¹€¼€Å|ÀÀÁ|ÀÀÁ|ÀÀÀ°€È¤€¬€ˆ‰¥Ð½Ìˆ(€€€€€€€‰¥ÑÍA•ÉM•½¹€øô€Å|ÀÀÁ|ÀÀÀ€´ø™½Éµ…Ñ9Õµ‰•È¡‰¥ÑÍA•ÉM•½¹€¼€Å|ÀÀÁ|ÀÀÀ°€Ä¤€¬€ˆ5‰¥Ð½Ìˆ(€€€€€€€‰¥ÑÍA•ÉM•½¹€øô€Å|ÀÀÀ€´ø™½Éµ…Ñ9Õµ‰•È¡‰¥ÑÍA•ÉM•½¹€¼€Å|ÀÀÀ°€Ä¤€¬€ˆ-‰¥Ð½Ìˆ(€€€€€€€•±Í”€´ø™½Éµ…Ñ9Õµ‰•È¡‰¥ÑÍA•ÉM•½¹°€À¤€¬€ˆ‰¥Ð½Ìˆ(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…Ñ	åÑ•Ì¡‰åÑ•Ìè½Õ‰±”¤èMÑÉ¥¹œ€ôÝ¡•¸ì(€€€€€€€‰åÑ•Ì€øô€Å|ÀÀÁ|ÀÀÁ|ÀÀÀ€´ø™½Éµ…Ñ9Õµ‰•È¡‰åÑ•Ì€¼€Å|ÀÀÁ|ÀÀÁ|ÀÀÀ°€È¤€¬€ˆˆ(€€€€€€€‰åÑ•Ì€øô€Å|ÀÀÁ|ÀÀÀ€´ø™½Éµ…Ñ9Õµ‰•È¡‰åÑ•Ì€¼€Å|ÀÀÁ|ÀÀÀ°€Ä¤€¬€ˆ5ˆ(€€€€€€€‰åÑ•Ì€øô€Å|ÀÀÀ€´ø™½Éµ…Ñ9Õµ‰•È¡‰åÑ•Ì€¼€Å|ÀÀÀ°€Ä¤€¬€ˆ-ˆ(€€€€€€€•±Í”€´ø™½Éµ…Ñ9Õµ‰•È¡‰åÑ•Ì°€À¤€¬€ˆˆ(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…Ñ5¥±±¥Í•½¹‘Ì¡Ù…±Õ”è½Õ‰±”¤èMÑÉ¥¹œ€ô™½Éµ…Ñ9Õµ‰•È¡Ù…±Õ”°€È¤€¬€ˆµÌˆ((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…ÑA•É•¹Ð¡Ù…±Õ”è½Õ‰±”¤èMÑÉ¥¹œ€ô™½Éµ…Ñ9Õµ‰•È¡Ù…±Õ”°€È¤€¬€ˆ”ˆ((€€€ÁÉ¥Ù…Ñ”™Õ¸™½Éµ…Ñ9Õµ‰•È¡Ù…±Õ”è½Õ‰±”°‘•¥µ…±Ìè%¹Ð¤èMÑÉ¥¹œ€ô(€€€€€€€MÑÉ¥¹œ¹™½Éµ…Ð¡1½…±”¹UL°€ˆ”¸‘í‘•¥µ…±Íõ˜ˆ°Ù…±Õ”¤((€€€ÁÉ¥Ù…Ñ”™Õ¸½ÁåM¡…É•Q•áÐ ¤ì(€€€€€€€Ù…°±¥Á‰½…É€ô•ÑMåÍÑ•µM•ÉÙ¥”¡1%A	=I}MIY%¤…Ì±¥Á‰½…É‘5…¹…•È(€€€€€€€±¥Á‰½…É¹Í•ÑAÉ¥µ…Éå±¥À¡±¥Á…Ñ„¹¹•ÝA±…¥¹Q•áÐ ‰¥Á•É˜ÌÉ•Á½ÉÐˆ°Í¡…É•Q•áÐ¤¤(€€€€€€€Q½…ÍÐ¹µ…­•Q•áÐ¡Ñ¡¥Ì°€‰I•Á½ÉÐ½Á¥•ˆ°Q½…ÍÐ¹19Q!}M!=IP¤¹Í¡½Ü ¤(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸Ñ½±•Q•¡¹¥…±•Ñ…¥±Ì ¤ì(€€€€€€€Ù…°Í¡½Ü€ô‘•Ñ…¥±Ì¹Ù¥Í¥‰¥±¥Ñä€„ôY¥•Ü¹Y%M%	1(€€€€€€€‘•Ñ…¥±Ì¹Ù¥Í¥‰¥±¥Ñä€ô¥˜€¡Í¡½Ü¤Y¥•Ü¹Y%M%	1•±Í”Y¥•Ü¹=9(€€€€€€€Ñ½±••Ñ…¥±Ì¹Ñ•áÐ€ô¥˜€¡Í¡½Ü¤€‰!%Q%1Lˆ•±Í”€‰M!=\Q%1Lˆ(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸±•…ÉM¡…É•É•„ ¤ì(€€€€€€€Í¡…É•Q•áÐ€ô€ˆˆ(€€€€€€€‘•Ñ…¥±Ì¹Ñ•áÐ€ô€ˆˆ(€€€€€€€‘•Ñ…¥±Ì¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹=9(€€€€€€€½ÁåM¡…É”¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹=9(€€€€€€€Ñ½±••Ñ…¥±Ì¹Ù¥Í¥‰¥±¥Ñä€ôY¥•Ü¹=9(€€€€€€€Ñ½±••Ñ…¥±Ì¹Ñ•áÐ€ô€‰M!=\Q%1Lˆ(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸Í•ÑMÑ…ÑÕÌ¡µ•ÍÍ…”èMÑÉ¥¹œ°½±½ÈèMÑÉ¥¹œ¤ì(€€€€€€€ÍÑ…ÑÕÌ¹Ñ•áÐ€ôµ•ÍÍ…”(€€€€€€€ÍÑ…ÑÕÌ¹Í•ÑQ•áÑ½±½È¡½±½È¹Á…ÉÍ•½±½È¡½±½È¤¤(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸Í•Ñ	ÕÍä¡‰ÕÍäè	½½±•…¸¤ì(€€€€€€€…Ñ¥½¹	ÕÑÑ½¹Ì¹™½É… ì¥Ð¹¥Í¹…‰±•€ô€…‰ÕÍäô(€€€€€€€¡½ÍÐ¹¥Í¹…‰±•€ô€…‰ÕÍä(€€€€€€€Á½ÉÐ¹¥Í¹…‰±•€ô€…‰ÕÍä(€€€€€€€Õ‘ÁQ…É•Ð¹¥Í¹…‰±•€ô€…‰ÕÍä(€€€ô((€€€½Ù•ÉÉ¥‘”™Õ¸½¹•ÍÑÉ½ä ¤ì(€€€€€€€…Ñ¥Ù•AÉ½•ÍÌü¹‘•ÍÑÉ½å½É¥‰±ä ¤(€€€€€€€…Ñ¥Ù•AÉ½•ÍÌ€ô¹Õ±°(€€€€€€€ÍÕÁ•È¹½¹•ÍÑÉ½ä ¤(€€€ô)ô(