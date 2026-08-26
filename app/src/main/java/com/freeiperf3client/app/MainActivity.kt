package com.freeiperf3client.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BasicTooltipState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val REPOSITORY_URL = "https://github.com/Anders0lesen/free-iperf3-client"

private enum class AppScreen { HOME, RUNNING, RESULTS }

private enum class DetectionStatus { NOT_CHECKED, CHECKING, DETECTED, FAILED }

private data class SessionFailure(
    val mode: TestMode,
    val error: Throwable,
)

private data class RunState(
    val title: String,
    val config: TestConfig,
    val modes: List<TestMode>,
    val currentMode: TestMode,
    val currentIndex: Int,
    val stage: String,
    val progress: Float,
    val live: LiveUpdate = LiveUpdate(),
    val samples: List<IntervalSample> = emptyList(),
    val command: String = "",
    val completed: List<TestResult> = emptyList(),
)

private data class CompletedSession(
    val title: String,
    val config: TestConfig,
    val results: List<TestResult>,
    val failure: SessionFailure? = null,
)

class MainActivity : ComponentActivity() {
    private lateinit var engine: IperfEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
        )
        engine = IperfEngine(this)
        setContent {
            IperfTheme {
                IperfApp(
                    engine = engine,
                    openRepository = ::openRepository,
                    copyText = ::copyText,
                    shareText = ::shareText,
                )
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

    private fun copyText(label: String, value: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(subject: String, value: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, value)
        }
        startActivity(Intent.createChooser(intent, "Share iperf3 results"))
    }

    override fun onDestroy() {
        engine.cancel()
        super.onDestroy()
    }
}

@Composable
private fun IperfApp(
    engine: IperfEngine,
    openRepository: () -> Unit,
    copyText: (String, String) -> Unit,
    shareText: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val recentServersStore = remember { RecentServersStore(context.applicationContext) }
    val serverDiscovery = remember { ServerDiscovery(context.applicationContext, engine) }
    val initiallyRecent = remember { recentServersStore.load() }
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var recentServers by remember { mutableStateOf(initiallyRecent) }
    var host by remember { mutableStateOf(initiallyRecent.firstOrNull()?.hostname.orEmpty()) }
    var port by remember { mutableStateOf(initiallyRecent.firstOrNull()?.port?.toString() ?: "5201") }
    var duration by remember { mutableStateOf("10") }
    var udpTarget by remember { mutableStateOf("50") }
    var selectedChoice by remember { mutableStateOf(TestChoice.TCP_DOWNLOAD) }
    var attemptedStart by remember { mutableStateOf(false) }
    var detectionStatus by remember { mutableStateOf(DetectionStatus.NOT_CHECKED) }
    var detectionMessage by remember { mutableStateOf("Scan this local network; no server address required") }
    var runState by remember { mutableStateOf<RunState?>(null) }
    var completedSession by remember { mutableStateOf<CompletedSession?>(null) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    val validation = validateConfig(host, port, duration, udpTarget)
    fun invalidateDetection() {
        if (detectionStatus != DetectionStatus.CHECKING) {
            detectionStatus = DetectionStatus.NOT_CHECKED
            detectionMessage = "Scan this local network; no server address required"
        }
    }

    fun recordServer(config: TestConfig) {
        recentServers = recentServersStore.record(config.hostname, config.port)
    }

    fun cancelRun() {
        engine.cancel()
        activeJob?.cancel()
        activeJob = null
        runState = null
        screen = AppScreen.HOME
    }

    fun runDiscovery() {
        val discoveryPort = port.toIntOrNull()
        if (discoveryPort == null || discoveryPort !in 1..65535) {
            detectionStatus = DetectionStatus.FAILED
            detectionMessage = "Enter a port from 1 to 65535 before scanning"
            return
        }
        if (detectionStatus == DetectionStatus.CHECKING) return
        detectionStatus = DetectionStatus.CHECKING
        detectionMessage = "Finding devices with port $discoveryPort open"
        activeJob = scope.launch {
            try {
                val found = serverDiscovery.discover(
                    port = discoveryPort,
                    extraCandidates = recentServers.map(RecentServer::hostname),
                ) { progress ->
                    activity.runOnUiThread {
                        detectionMessage = if (progress.verifying) {
                            "Verifying ${progress.openPorts} possible server${if (progress.openPorts == 1) "" else "s"} with iperf3"
                        } else {
                            "Scanned ${progress.checked} of ${progress.total} addresses; ${progress.openPorts} possible"
                        }
                    }
                }
                if (found.isEmpty()) {
                    detectionStatus = DetectionStatus.FAILED
                    detectionMessage = "No iperf3 server answered on port $discoveryPort"
                } else {
                    found.forEach { recentServers = recentServersStore.record(it.hostname, it.port) }
                    val selected = found.first()
                    host = selected.hostname
                    port = selected.port.toString()
                    attemptedStart = false
                    detectionStatus = DetectionStatus.DETECTED
                    detectionMessage = "Found ${found.size} server${if (found.size == 1) "" else "s"}; selected ${RecentServer(selected.hostname, selected.port, 0).endpoint}"
                }
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                detectionStatus = DetectionStatus.FAILED
                detectionMessage = friendlyError(error)
            } finally {
                activeJob = null
            }
        }
    }

    fun startTests(choice: TestChoice = selectedChoice) {
        attemptedStart = true
        val config = validation.config ?: return
        val modes = modesFor(choice)
        val title = choice.title
        val totalDuration = modes.sumOf { engine.durationFor(config, it) }.coerceAtLeast(1)
        var completedDuration = 0
        val completed = mutableListOf<TestResult>()
        detectionStatus = DetectionStatus.CHECKING
        completedSession = null
        screen = AppScreen.RUNNING
        activeJob = scope.launch {
            var activeMode = modes.first()
            try {
                modes.forEachIndexed { index, mode ->
                    activeMode = mode
                    val modeDuration = engine.durationFor(config, mode)
                    runState = RunState(
                        title = title,
                        config = config,
                        modes = modes,
                        currentMode = mode,
                        currentIndex = index,
                        stage = if (mode == TestMode.DETECT) "Checking the iperf3 server" else "Opening the iperf3 connection",
                        progress = completedDuration.toFloat() / totalDuration,
                        command = engine.displayCommand(config, mode),
                        completed = completed.toList(),
                    )
                    val currentSamples = mutableListOf<IntervalSample>()
                    val result = withContext(Dispatchers.IO) {
                        engine.execute(config, mode) { update ->
                            activity.runOnUiThread {
                                update.sample?.let(currentSamples::add)
                                val elapsed = update.elapsedSeconds.coerceIn(0.0, modeDuration.toDouble())
                                val overall = (completedDuration + elapsed) / totalDuration
                                runState = runState?.copy(
                                    stage = when {
                                        update.connection != null -> "Connected to iperf3"
                                        elapsed > 0 -> "Test running"
                                        else -> runState?.stage ?: "Connecting"
                                    },
                                    progress = overall.toFloat().coerceIn(0f, 1f),
                                    live = update,
                                    samples = currentSamples.toList(),
                                )
                            }
                        }
                    }
                    completed += result
                    completedDuration += modeDuration
                    if (mode == TestMode.DETECT) {
                        recordServer(config)
                        detectionStatus = DetectionStatus.DETECTED
                        detectionMessage = result.connection?.let { "iperf3 responded at $it" }
                            ?: "iperf3 server detected"
                    }
                    runState = runState?.copy(
                        progress = completedDuration.toFloat() / totalDuration,
                        completed = completed.toList(),
                    )
                }
                completedSession = CompletedSession(title, config, completed.toList())
                screen = AppScreen.RESULTS
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                if (activeMode == TestMode.DETECT) {
                    detectionStatus = DetectionStatus.FAILED
                    detectionMessage = friendlyError(error)
                }
                completedSession = CompletedSession(
                    title = title,
                    config = config,
                    results = completed.toList(),
                    failure = SessionFailure(activeMode, error),
                )
                screen = AppScreen.RESULTS
            } finally {
                activeJob = null
            }
        }
    }

    BackHandler(enabled = screen != AppScreen.HOME) {
        when (screen) {
            AppScreen.RUNNING -> cancelRun()
            AppScreen.RESULTS -> {
                runState = null
                completedSession = null
                screen = AppScreen.HOME
            }
            AppScreen.HOME -> Unit
        }
    }

    AppBackdrop {
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                host = host,
                port = port,
                duration = duration,
                udpTarget = udpTarget,
                selectedChoice = selectedChoice,
                attemptedStart = attemptedStart,
                validation = validation,
                detectionStatus = detectionStatus,
                detectionMessage = detectionMessage,
                recentServers = recentServers,
                onHostChange = { host = it; attemptedStart = false; invalidateDetection() },
                onPortChange = { port = it.filter(Char::isDigit); attemptedStart = false; invalidateDetection() },
                onDurationChange = { duration = it.filter(Char::isDigit); attemptedStart = false },
                onUdpChange = { udpTarget = it.filter(Char::isDigit); attemptedStart = false },
                onChoice = { selectedChoice = it },
                onRecentSelect = { server ->
                    host = server.hostname
                    port = server.port.toString()
                    attemptedStart = false
                    detectionStatus = DetectionStatus.NOT_CHECKED
                    detectionMessage = "Selected ${server.endpoint}; scan or start a test"
                },
                onRecentRemove = { server -> recentServers = recentServersStore.remove(server) },
                onRecentClear = { recentServers = recentServersStore.clear() },
                onDetect = ::runDiscovery,
                onStart = { startTests() },
                openRepository = openRepository,
            )
            AppScreen.RUNNING -> RunningScreen(
                state = runState,
                onCancel = ::cancelRun,
            )
            AppScreen.RESULTS -> completedSession?.let { session ->
                ResultsScreen(
                    session = session,
                    engine = engine,
                    onBack = {
                        runState = null
                        completedSession = null
                        screen = AppScreen.HOME
                    },
                    onRetry = {
                        screen = AppScreen.HOME
                        completedSession = null
                        startTests(selectedChoice)
                    },
                    copyText = copyText,
                    shareText = shareText,
                )
            }
        }
    }
}
