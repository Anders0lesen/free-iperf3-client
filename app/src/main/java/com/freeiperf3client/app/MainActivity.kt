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

@Composable
private fun HomeScreen(
    host: String,
    port: String,
    duration: String,
    udpTarget: String,
    selectedChoice: TestChoice,
    attemptedStart: Boolean,
    validation: ConfigValidation,
    detectionStatus: DetectionStatus,
    detectionMessage: String,
    recentServers: List<RecentServer>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onUdpChange: (String) -> Unit,
    onChoice: (TestChoice) -> Unit,
    onRecentSelect: (RecentServer) -> Unit,
    onRecentRemove: (RecentServer) -> Unit,
    onRecentClear: () -> Unit,
    onDetect: () -> Unit,
    onStart: () -> Unit,
    openRepository: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val wideLayout = maxWidth >= 800.dp
        val contentWidth = if (maxWidth > 1240.dp) 1200.dp else maxWidth
        Column(
            modifier = Modifier
                .width(contentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (maxWidth > 600.dp) 28.dp else 18.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            HomeHeader(openRepository)
            if (wideLayout) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(.9f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ConfigCard(
                            host, port, duration, udpTarget, attemptedStart, validation,
                            onHostChange, onPortChange, onDurationChange, onUdpChange,
                        ) { focusManager.clearFocus() }
                        if (recentServers.isNotEmpty()) {
                            RecentServersCard(
                                recentServers, host, port.toIntOrNull(),
                                onRecentSelect, onRecentRemove, onRecentClear,
                            )
                        }
                    }
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        TestChoiceGrid(selectedChoice, onChoice, compact = true)
                        WideChoiceCard(
                            TestChoice.RUN_ALL, TablerGlyph.LAYERS, Teal,
                            selectedChoice == TestChoice.RUN_ALL,
                        ) { onChoice(TestChoice.RUN_ALL) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.weight(1f)) { DetectionCard(detectionStatus, detectionMessage, onDetect, compact = true) }
                            Box(Modifier.weight(1f)) {
                                StartCard(
                                    selectedChoice,
                                    validation.valid && detectionStatus != DetectionStatus.CHECKING,
                                    validation.firstError ?: "Ready to test",
                                    onStart,
                                    compact = true,
                                )
                            }
                        }
                    }
                }
            } else {
                ConfigCard(
                    host, port, duration, udpTarget, attemptedStart, validation,
                    onHostChange, onPortChange, onDurationChange, onUdpChange,
                ) { focusManager.clearFocus() }
                if (recentServers.isNotEmpty()) {
                    RecentServersCard(
                        recentServers, host, port.toIntOrNull(),
                        onRecentSelect, onRecentRemove, onRecentClear,
                    )
                }
                TestChoiceGrid(selectedChoice, onChoice)
                WideChoiceCard(
                    TestChoice.RUN_ALL, TablerGlyph.LAYERS, Teal,
                    selectedChoice == TestChoice.RUN_ALL,
                ) { onChoice(TestChoice.RUN_ALL) }
                DetectionCard(detectionStatus, detectionMessage, onDetect)
                StartCard(
                    selectedChoice,
                    validation.valid && detectionStatus != DetectionStatus.CHECKING,
                    validation.firstError ?: "Ready to test",
                    onStart,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(openRepository: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(142.dp)) {
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "Free iperf3 Client",
                color = AppText,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Text("Simply free.", color = Teal, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
        GitHubFold(Modifier.align(Alignment.TopEnd), openRepository)
    }
}

@Composable
private fun GitHubFold(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(RoundedCornerShape(bottomStart = 34.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1C252D), Color(0xFF0A1015)),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .clickable(onClick = onClick)
            .focusable()
            .semantics { contentDescription = "Open project on GitHub" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = null,
            tint = AppMuted,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun ConfigCard(
    host: String,
    port: String,
    duration: String,
    udpTarget: String,
    attemptedStart: Boolean,
    validation: ConfigValidation,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onUdpChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val fieldFocus = remember { List(4) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    GlassCard {
        Column(
            Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Next)
                    Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Previous)
                    else -> false
                }
            },
        ) {
            ConfigRow(
                glyph = TablerGlyph.MONITOR,
                label = "Server address",
                value = host,
                placeholder = "IP address or hostname",
                error = validation.hostError.takeIf { attemptedStart || host.isNotBlank() },
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                onValueChange = onHostChange,
                focusRequester = fieldFocus[0],
                previousFocus = FocusRequester.Default,
                nextFocus = fieldFocus[1],
            )
            ConfigDivider()
            ConfigRow(
                glyph = TablerGlyph.PORT,
                label = "Port",
                value = port,
                placeholder = "5201",
                error = validation.portError.takeIf { attemptedStart || port.isNotBlank() },
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                onValueChange = onPortChange,
                focusRequester = fieldFocus[1],
                previousFocus = fieldFocus[0],
                nextFocus = fieldFocus[2],
            )
            ConfigDivider()
            ConfigRow(
                glyph = TablerGlyph.CLOCK,
                label = "Duration",
                value = duration,
                suffix = " seconds",
                placeholder = "10",
                error = validation.durationError.takeIf { attemptedStart || duration.isNotBlank() },
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                onValueChange = onDurationChange,
                focusRequester = fieldFocus[2],
                previousFocus = fieldFocus[1],
                nextFocus = fieldFocus[3],
            )
            ConfigDivider()
            ConfigRow(
                glyph = TablerGlyph.ACTIVITY,
                label = "UDP target",
                value = udpTarget,
                suffix = " Mbit/s",
                placeholder = "50",
                error = validation.udpError.takeIf { attemptedStart || udpTarget.isNotBlank() },
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onValueChange = onUdpChange,
                onDone = onDone,
                focusRequester = fieldFocus[3],
                previousFocus = fieldFocus[2],
                nextFocus = FocusRequester.Default,
            )
        }
    }
}

@Composable
private fun RecentServersCard(
    servers: List<RecentServer>,
    selectedHost: String,
    selectedPort: Int?,
    onSelect: (RecentServer) -> Unit,
    onRemove: (RecentServer) -> Unit,
    onClear: () -> Unit,
) {
    GlassCard {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent servers", color = AppText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("Clear all", color = AppMuted) }
            }
            HorizontalDivider(color = AppBorder)
            servers.forEachIndexed { index, server ->
                var focused by remember(server) { mutableStateOf(false) }
                val selected = server.hostname.equals(selectedHost, ignoreCase = true) && server.port == selectedPort
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (selected) Teal.copy(alpha = .10f) else Color.Transparent)
                        .onFocusChanged { focused = it.isFocused }
                        .border(if (focused) 2.dp else 0.dp, Teal, RoundedCornerShape(12.dp))
                        .clickable { onSelect(server) }
                        .focusable()
                        .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TablerIcon(TablerGlyph.SERVER, null, if (selected) Teal else AppMuted, Modifier.size(25.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(server.endpoint, color = if (selected) Teal else AppText, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (selected) "Selected" else "Tap to use", color = AppMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { onRemove(server) }, modifier = Modifier.semantics { contentDescription = "Forget ${server.endpoint}" }) {
                        TablerIcon(TablerGlyph.CLOSE, null, AppMuted, Modifier.size(20.dp))
                    }
                }
                if (index != servers.lastIndex) HorizontalDivider(color = AppBorder, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
private fun ConfigRow(
    glyph: TablerGlyph,
    label: String,
    value: String,
    placeholder: String,
    error: String?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    suffix: String = "",
    onDone: () -> Unit = {},
    focusRequester: FocusRequester,
    previousFocus: FocusRequester,
    nextFocus: FocusRequester,
) {
    val focusManager = LocalFocusManager.current
    val isTelevision = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    var focused by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(
            if (isTelevision) {
                Modifier
                    .focusRequester(focusRequester)
                    .focusProperties {
                        up = previousFocus
                        down = nextFocus
                        previous = previousFocus
                        next = nextFocus
                    }
                    .onFocusChanged { focused = it.isFocused }
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (focused) Teal.copy(alpha = .10f) else Color.Transparent)
                    .border(
                        width = if (focused) 1.dp else 0.dp,
                        color = if (focused) Teal.copy(alpha = .75f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { editorOpen = true }
                    .focusable()
            } else Modifier
        )
        .padding(horizontal = 20.dp, vertical = 17.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconOrb(glyph, Teal, 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = AppMuted, fontSize = 14.sp)
            if (isTelevision) {
                Text(
                    text = value.ifBlank { placeholder } + if (value.isNotBlank()) suffix else "",
                    color = if (value.isBlank()) AppMuted.copy(alpha = .55f) else AppText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .focusProperties {
                            previous = previousFocus
                            next = nextFocus
                        }
                        .onFocusChanged { focused = it.isFocused }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (focused) Teal.copy(alpha = .10f) else Color.Transparent)
                        .border(
                            width = if (focused) 1.dp else 0.dp,
                            color = if (focused) Teal.copy(alpha = .75f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        onDone = { onDone() },
                    ),
                    cursorBrush = Brush.verticalGradient(listOf(Teal, Teal)),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                if (value.isBlank()) Text(placeholder, color = AppMuted.copy(alpha = .55f), fontSize = 19.sp)
                                inner()
                            }
                            if (value.isNotBlank() && suffix.isNotBlank()) {
                                Text(suffix, color = AppMuted, fontSize = 17.sp)
                            }
                        }
                    },
                )
            }
            AnimatedVisibility(error != null) {
                Text(error.orEmpty(), color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        TablerIcon(TablerGlyph.EDIT, null, AppMuted, Modifier.size(25.dp))
    }
    if (editorOpen) {
        AlertDialog(
            onDismissRequest = { editorOpen = false },
            containerColor = Color(0xFF0D171E),
            titleContentColor = AppText,
            textContentColor = AppMuted,
            title = { Text("Edit $label") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text(label) },
                    placeholder = { Text(placeholder) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { editorOpen = false; onDone() }),
                )
            },
            confirmButton = {
                TextButton(onClick = { editorOpen = false; onDone() }) { Text("Done", color = Teal) }
            },
            dismissButton = {
                TextButton(onClick = { editorOpen = false }) { Text("Cancel", color = AppMuted) }
            },
        )
    }
}

@Composable
private fun ConfigDivider() {
    HorizontalDivider(color = AppBorder.copy(alpha = .65f), modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun TestChoiceGrid(selected: TestChoice, onChoice: (TestChoice) -> Unit, compact: Boolean = false) {
    val choices = listOf(
        Triple(TestChoice.TCP_DOWNLOAD, TablerGlyph.DOWNLOAD, Blue),
        Triple(TestChoice.TCP_UPLOAD, TablerGlyph.UPLOAD, Green),
        Triple(TestChoice.TCP_BIDIRECTIONAL, TablerGlyph.ARROWS_EXCHANGE, Purple),
        Triple(TestChoice.UDP_QUALITY, TablerGlyph.ACTIVITY, Orange),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        choices.chunked(2).forEach { rowChoices ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowChoices.forEach { (choice, glyph, accent) ->
                    ChoiceCard(
                        choice = choice,
                        glyph = glyph,
                        accent = accent,
                        selected = selected == choice,
                        onClick = { onChoice(choice) },
                        modifier = Modifier.weight(1f),
                        compact = compact,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    choice: TestChoice,
    glyph: TablerGlyph,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    FocusableGlassCard(
        selected = selected,
        accent = accent,
        onClick = onClick,
        modifier = modifier.height(if (compact) 158.dp else 190.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (compact) 14.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconOrb(glyph, accent, if (compact) 58.dp else 82.dp)
            Spacer(Modifier.height(if (compact) 7.dp else 13.dp))
            Text(choice.title, color = AppText, fontSize = if (compact) 15.sp else 18.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(choice.subtitle, color = AppMuted, fontSize = if (compact) 12.sp else 14.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WideChoiceCard(
    choice: TestChoice,
    glyph: TablerGlyph,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusableGlassCard(
        selected = selected,
        accent = accent,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(116.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            IconOrb(glyph, accent, 68.dp)
            Column(Modifier.weight(1f)) {
                Text(choice.title, color = AppText, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Text(choice.subtitle, color = AppMuted, fontSize = 15.sp)
            }
            TablerIcon(TablerGlyph.CHEVRON_RIGHT, null, AppMuted, Modifier.size(28.dp))
        }
    }
}

@Composable
private fun DetectionCard(status: DetectionStatus, message: String, onClick: () -> Unit, compact: Boolean = false) {
    val accent = when (status) {
        DetectionStatus.NOT_CHECKED -> AppMuted
        DetectionStatus.CHECKING -> Orange
        DetectionStatus.DETECTED -> Green
        DetectionStatus.FAILED -> Red
    }
    val glyph = when (status) {
        DetectionStatus.NOT_CHECKED -> TablerGlyph.SERVER
        DetectionStatus.CHECKING -> TablerGlyph.REFRESH
        DetectionStatus.DETECTED -> TablerGlyph.CHECK
        DetectionStatus.FAILED -> TablerGlyph.ALERT
    }
    val title = when (status) {
        DetectionStatus.NOT_CHECKED -> "Find iperf3 servers"
        DetectionStatus.CHECKING -> "Scanning current network"
        DetectionStatus.DETECTED -> "iperf3 server found"
        DetectionStatus.FAILED -> "Network scan finished"
    }
    FocusableGlassCard(
        selected = status == DetectionStatus.DETECTED,
        accent = accent,
        onClick = onClick,
        enabled = status != DetectionStatus.CHECKING,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 110.dp else 116.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (compact) 14.dp else 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
        ) {
            IconOrb(glyph, accent, if (compact) 46.dp else 68.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = AppText, fontSize = if (compact) 15.sp else 19.sp, fontWeight = FontWeight.Medium, maxLines = if (compact) 2 else Int.MAX_VALUE)
                Text(message, color = if (status == DetectionStatus.FAILED) Red else AppMuted, fontSize = if (compact) 11.sp else 14.sp, maxLines = if (compact) 2 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
            }
            if (status != DetectionStatus.CHECKING) {
                TablerIcon(TablerGlyph.CHEVRON_RIGHT, null, AppMuted, Modifier.size(if (compact) 20.dp else 28.dp))
            }
        }
    }
}

@Composable
private fun StartCard(choice: TestChoice, enabled: Boolean, helper: String, onClick: () -> Unit, compact: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.015f else 1f, label = "startFocus")
    val shape = RoundedCornerShape(28.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 110.dp else 136.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color(0xFF0A2625), Color(0xFF0B4441)))
                else Brush.horizontalGradient(listOf(AppSurface, AppSurfaceRaised)),
            )
            .border(1.dp, if (focused) Teal else AppBorder, shape)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .focusable(enabled)
            .padding(horizontal = if (compact) 16.dp else 26.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp)) {
            Box(
                Modifier.size(if (compact) 54.dp else 78.dp).clip(CircleShape).background(if (enabled) Teal else AppBorder),
                contentAlignment = Alignment.Center,
            ) {
                TablerIcon(TablerGlyph.PLAY, null, if (enabled) Color.White else AppMuted, Modifier.size(if (compact) 28.dp else 38.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Start Test", color = if (enabled) Teal else AppMuted, fontSize = if (compact) 19.sp else 25.sp, fontWeight = FontWeight.Bold)
                Text(if (enabled) choice.title else helper, color = AppMuted, fontSize = if (compact) 12.sp else 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RunningScreen(state: RunState?, onCancel: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
    ) {
        val wideLayout = maxWidth >= 800.dp
        val contentWidth = if (maxWidth > 1240.dp) 1200.dp else maxWidth
        Column(
            Modifier
                .width(contentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (maxWidth > 600.dp) 28.dp else 18.dp, vertical = 14.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ScreenTopBar("Testing", onCancel, TablerGlyph.CLOSE, "Cancel test")
            if (state == null) {
                GlassCard { Text("Preparing test…", modifier = Modifier.padding(24.dp), color = AppText) }
                return@Column
            }
            GlassCard {
                Row(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(17.dp),
                ) {
                    IconOrb(glyphForMode(state.currentMode), colorForMode(state.currentMode), 64.dp)
                    Column(Modifier.weight(1f)) {
                        Text(state.currentMode.title, color = colorForMode(state.currentMode), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(state.stage, color = AppText, fontSize = 16.sp)
                        Text("Stage ${state.currentIndex + 1} of ${state.modes.size}", color = AppMuted, fontSize = 13.sp)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = colorForMode(state.currentMode),
                trackColor = AppBorder,
            )
            if (wideLayout) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(.82f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        LiveHero(state)
                        RunningCommandCard(state.command)
                        FocusButton("Cancel test", TablerGlyph.STOP, Red, onCancel)
                    }
                    ThroughputChart(
                        samples = state.samples,
                        mode = state.currentMode,
                        modifier = Modifier.weight(1.18f).height(390.dp),
                    )
                }
            } else {
                LiveHero(state)
                ThroughputChart(
                    samples = state.samples,
                    mode = state.currentMode,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                )
                RunningCommandCard(state.command)
                FocusButton("Cancel test", TablerGlyph.STOP, Red, onCancel)
            }
        }
    }
}

@Composable
private fun RunningCommandCard(command: String) {
    GlassCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(TablerGlyph.TERMINAL, "Command", AppMuted)
            Text(
                command,
                color = AppMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun LiveHero(state: RunState) {
    val live = state.live
    val primary = live.downloadBitsPerSecond ?: live.uploadBitsPerSecond
    GlassCard {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.currentMode == TestMode.DETECT) {
                IconOrb(TablerGlyph.SERVER, Orange, 82.dp)
                Text("Confirming iperf3", color = AppText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(live.connection ?: "Waiting for the server response", color = AppMuted, fontSize = 15.sp)
            } else if (primary != null) {
                val parts = formatRateParts(primary)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(parts.first, color = colorForMode(state.currentMode), fontSize = 62.sp, fontWeight = FontWeight.Bold)
                    Text(parts.second, color = AppMuted, fontSize = 22.sp, modifier = Modifier.padding(bottom = 10.dp))
                }
                Text(
                    "Running ${live.elapsedSeconds.toInt()} of ${state.config.durationSeconds} seconds",
                    color = AppMuted,
                    fontSize = 15.sp,
                )
                if (state.currentMode == TestMode.TCP_BIDIRECTIONAL) {
                    Text(
                        "Download ${formatRate(live.downloadBitsPerSecond ?: 0.0)}   Upload ${formatRate(live.uploadBitsPerSecond ?: 0.0)}",
                        color = AppText,
                        fontSize = 15.sp,
                    )
                }
                if (live.jitterMs != null || live.lossPercent != null) {
                    Text(
                        "Jitter ${formatMilliseconds(live.jitterMs ?: 0.0)}   Loss ${formatPercent(live.lossPercent ?: 0.0)}",
                        color = AppText,
                        fontSize = 15.sp,
                    )
                }
            } else {
                Text("Connecting", color = Orange, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Opening the iperf3 control connection", color = AppMuted, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ResultsScreen(
    session: CompletedSession,
    engine: IperfEngine,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    copyText: (String, String) -> Unit,
    shareText: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val measuredResults = session.results.filter { it.mode != TestMode.DETECT }
    var selectedIndex by remember { mutableStateOf(0) }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    val selected = measuredResults.getOrNull(selectedIndex)
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val wideLayout = maxWidth >= 800.dp
        val contentWidth = if (maxWidth > 1240.dp) 1200.dp else maxWidth
        Column(
            Modifier
                .width(contentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (maxWidth > 600.dp) 28.dp else 18.dp, vertical = 14.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ResultTopBar(
                failure = session.failure != null,
                onBack = onBack,
                onShare = {
                    val report = if (session.failure == null) {
                        buildResultReport(session.title, session.config, session.results, engine, safe = true)
                    } else {
                        buildDiagnosticReport(
                            context,
                            session.title,
                            session.config,
                            session.failure.mode,
                            session.results,
                            session.failure.error,
                            safe = true,
                        )
                    }
                    shareText("Free iperf3 Client ${session.title}", report)
                },
            )
            if (session.failure != null) {
                FailureContent(session, onRetry, copyText)
            } else if (selected != null) {
                if (measuredResults.size > 1) {
                    ResultSelector(measuredResults, selectedIndex) { selectedIndex = it }
                }
                if (wideLayout) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(.86f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            ResultHero(selected, session.config)
                            ResultStats(selected)
                            if (selected.mode == TestMode.UDP_DOWNLOAD || selected.mode == TestMode.UDP_UPLOAD) {
                                UdpQualityCard(selected, session.config)
                            }
                        }
                        Column(Modifier.weight(1.14f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            ThroughputChart(
                                samples = selected.samples,
                                mode = selected.mode,
                                modifier = Modifier.fillMaxWidth().height(350.dp),
                            )
                            DetailsCard(selected)
                        }
                    }
                } else {
                    ResultHero(selected, session.config)
                    ThroughputChart(
                        samples = selected.samples,
                        mode = selected.mode,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )
                    ResultStats(selected)
                    if (selected.mode == TestMode.UDP_DOWNLOAD || selected.mode == TestMode.UDP_UPLOAD) {
                        UdpQualityCard(selected, session.config)
                    }
                    DetailsCard(selected)
                }
                CommandCard(selected, session.config, engine, copyText)
                if (wideLayout) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Box(Modifier.weight(1f)) {
                            FocusButton("Show result QR for phone", TablerGlyph.QR_CODE, Blue) {
                                qrPayload = buildResultReport(session.title, session.config, listOf(selected), engine, safe = false)
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            FocusButton("Copy privacy-safe summary", TablerGlyph.SHIELD, Teal) {
                                copyText(
                                    "Safe summary",
                                    buildResultReport(session.title, session.config, session.results, engine, safe = true),
                                )
                            }
                        }
                    }
                } else {
                    FocusButton("Show result QR for phone", TablerGlyph.QR_CODE, Blue) {
                        qrPayload = buildResultReport(session.title, session.config, listOf(selected), engine, safe = false)
                    }
                    FocusButton("Copy privacy-safe summary", TablerGlyph.SHIELD, Teal) {
                        copyText(
                            "Safe summary",
                            buildResultReport(session.title, session.config, session.results, engine, safe = true),
                        )
                    }
                }
            } else {
                GlassCard {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle(TablerGlyph.CHECK, "Server detected", Green)
                        Text("The iperf3 server responded correctly.", color = AppMuted)
                    }
                }
            }
        }
    }
    qrPayload?.let { payload ->
        ResultQrDialog(payload = payload, onDismiss = { qrPayload = null })
    }
}

@Composable
private fun ResultQrDialog(payload: String, onDismiss: () -> Unit) {
    val qrBitmap = remember(payload) { createQrBitmap(payload, 720) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val qrSize = listOf(
                360.dp,
                maxWidth * .82f - 44.dp,
                maxHeight - 250.dp,
            ).minOrNull()?.coerceAtLeast(180.dp) ?: 270.dp
            Surface(
                modifier = Modifier.fillMaxWidth(.82f).widthIn(max = 620.dp),
                shape = RoundedCornerShape(28.dp),
                color = AppSurfaceRaised,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(TablerGlyph.QR_CODE, "Scan result on your phone", Blue)
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code containing the selected test command and result",
                        modifier = Modifier.size(qrSize).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(10.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        "Generated locally. Includes the selected server address and command, but not raw output.",
                        color = AppMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    FocusButton("Close QR", TablerGlyph.CLOSE, Teal, onDismiss)
                }
            }
        }
    }
}

private fun createQrBitmap(payload: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
        ),
    )
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

@Composable
private fun ResultTopBar(failure: Boolean, onBack: () -> Unit, onShare: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VectorIconButton(TablerGlyph.ARROW_LEFT, "Back", onBack)
        Text(
            if (failure) "Test failed" else "Results",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = AppText,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        VectorIconButton(TablerGlyph.SHARE, "Share privacy-safe results", onShare)
    }
}

@Composable
private fun ResultSelector(results: List<TestResult>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        results.forEachIndexed { index, result ->
            val active = index == selected
            val color = colorForMode(result.mode)
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, if (active) color else AppBorder, CircleShape)
                    .clickable { onSelect(index) }
                    .focusable(),
                color = if (active) color.copy(alpha = .14f) else AppSurface,
                contentColor = if (active) color else AppMuted,
            ) {
                Text(result.mode.title, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ResultHero(result: TestResult, config: TestConfig) {
    val accent = colorForMode(result.mode)
    GlassCard {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconOrb(glyphForMode(result.mode), accent, 58.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(result.mode.title, color = accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(result.mode.subtitle, color = AppMuted, fontSize = 13.sp)
                }
                Text("Success", color = Green, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                TablerIcon(TablerGlyph.CHECK, null, Green, Modifier.size(26.dp))
            }
            Spacer(Modifier.height(3.dp))
            if (result.mode == TestMode.TCP_BIDIRECTIONAL) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroRate("Download", result.downloadBitsPerSecond ?: 0.0, Blue, Modifier.weight(1f), compact = true)
                    HeroRate("Upload", result.uploadBitsPerSecond ?: 0.0, Green, Modifier.weight(1f), compact = true)
                }
            } else {
                HeroRate("Average throughput", resultRate(result), accent, Modifier.fillMaxWidth())
            }
            if (result.mode == TestMode.UDP_DOWNLOAD || result.mode == TestMode.UDP_UPLOAD) {
                Text("Target ${config.udpTargetMbps} Mbit/s", color = AppMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HeroRate(
    label: String,
    rate: Double,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val parts = formatRateParts(rate)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(
                parts.first,
                color = accent,
                fontSize = if (compact) 42.sp else 57.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(if (compact) 4.dp else 7.dp))
            Text(
                parts.second,
                color = AppMuted,
                fontSize = if (compact) 14.sp else 20.sp,
                modifier = Modifier.padding(bottom = if (compact) 7.dp else 9.dp),
                maxLines = 1,
                softWrap = false,
            )
        }
        Text(label, color = AppMuted, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun ResultStats(result: TestResult) {
    val rates = result.samples.mapNotNull {
        if (result.downloadBitsPerSecond != null) it.downloadBitsPerSecond else it.uploadBitsPerSecond
    }
    val average = resultRate(result)
    val minimum = rates.minOrNull() ?: average
    val maximum = rates.maxOrNull() ?: average
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("MIN", minimum, Purple, Modifier.weight(1f))
        StatCard("AVG", average, Blue, Modifier.weight(1f))
        StatCard("MAX", maximum, Green, Modifier.weight(1f))
    }
    if (result.mode == TestMode.TCP_BIDIRECTIONAL) {
        val uploadRates = result.samples.mapNotNull { it.uploadBitsPerSecond }
        val uploadAverage = result.uploadBitsPerSecond ?: 0.0
        Spacer(Modifier.height(10.dp))
        Text("Upload statistics", color = AppMuted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("MIN", uploadRates.minOrNull() ?: uploadAverage, Purple, Modifier.weight(1f))
            StatCard("AVG", uploadAverage, Green, Modifier.weight(1f))
            StatCard("MAX", uploadRates.maxOrNull() ?: uploadAverage, Orange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, rate: Double, accent: Color, modifier: Modifier) {
    GlassCard(modifier) {
        val parts = formatRateParts(rate)
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(parts.first, color = AppText, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(parts.second, color = AppMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun UdpQualityCard(result: TestResult, config: TestConfig) {
    val (score, grade) = scoreUdp(result, config)
    val scoreColor = when {
        score >= 90 -> Green
        score >= 75 -> Teal
        score >= 50 -> Orange
        else -> Red
    }
    GlassCard {
        Row(
            Modifier.fillMaxWidth().padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(scoreColor.copy(alpha = .12f)).border(2.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(score.toString(), color = scoreColor, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("UDP quality: $grade", color = scoreColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Loss ${formatPercent(result.lossPercent ?: 0.0)}", color = AppText, fontSize = 15.sp)
                Text("Jitter ${formatMilliseconds(result.jitterMs ?: 0.0)}", color = AppText, fontSize = 15.sp)
                result.packets?.let { Text("Packets $it", color = AppMuted, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun DetailsCard(result: TestResult) {
    var expanded by remember(result) { mutableStateOf(true) }
    GlassCard {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.focusable().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Details (per 1 second)", color = AppText, fontSize = 18.sp, modifier = Modifier.weight(1f))
                TablerIcon(
                    if (expanded) TablerGlyph.CLOSE else TablerGlyph.CHEVRON_RIGHT,
                    null,
                    AppMuted,
                    Modifier.size(22.dp).graphicsLayer(rotationZ = if (expanded) 45f else 90f),
                )
            }
            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider(color = AppBorder)
                    Text(
                        intervalTable(result),
                        modifier = Modifier.fillMaxWidth().padding(14.dp).horizontalScroll(rememberScrollState()),
                        color = AppMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandCard(
    result: TestResult,
    config: TestConfig,
    engine: IperfEngine,
    copyText: (String, String) -> Unit,
) {
    var show by remember(result) { mutableStateOf(false) }
    GlassCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(TablerGlyph.TERMINAL, "Command view", AppText, Modifier.weight(1f))
                Switch(
                    checked = show,
                    onCheckedChange = { show = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Teal,
                        uncheckedThumbColor = AppMuted,
                        uncheckedTrackColor = AppBorder,
                    ),
                )
            }
            AnimatedVisibility(show) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        commandAndOutput(config, result, engine).take(12_000),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = .22f))
                            .padding(16.dp),
                        color = AppMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    Text(
                        "Full command output contains the server address. Review it before sharing.",
                        color = Orange,
                        fontSize = 12.sp,
                    )
                    FocusButton("Copy full command and output", TablerGlyph.COPY, AppMuted) {
                        copyText("Command and output", commandAndOutput(config, result, engine))
                    }
                }
            }
        }
    }
}

@Composable
private fun FailureContent(
    session: CompletedSession,
    onRetry: () -> Unit,
    copyText: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val failure = session.failure ?: return
    var details by remember { mutableStateOf(false) }
    GlassCard(border = Red.copy(alpha = .7f)) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconOrb(TablerGlyph.ALERT, Red, 86.dp)
            Text("${failure.mode.title} failed", color = Red, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(friendlyError(failure.error), color = AppText, fontSize = 17.sp, textAlign = TextAlign.Center)
            Text("No test was allowed to continue blindly after this failure.", color = AppMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
    FocusButton("Retry", TablerGlyph.REFRESH, Teal, onRetry)
    FocusButton("Copy privacy-safe diagnostics", TablerGlyph.SHIELD, AppMuted) {
        copyText(
            "Safe diagnostics",
            buildDiagnosticReport(
                context,
                session.title,
                session.config,
                failure.mode,
                session.results,
                failure.error,
                safe = true,
            ),
        )
    }
    GlassCard {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { details = !details }.focusable().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(TablerGlyph.TERMINAL, "Technical details", AppText, Modifier.weight(1f))
                TablerIcon(TablerGlyph.CHEVRON_RIGHT, null, AppMuted, Modifier.size(24.dp).graphicsLayer(rotationZ = if (details) 90f else 0f))
            }
            AnimatedVisibility(details) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        buildDiagnosticReport(
                            context,
                            session.title,
                            session.config,
                            failure.mode,
                            session.results,
                            failure.error,
                            safe = false,
                        ).take(12_000),
                        color = AppMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    Text("Full diagnostics contain the server and device model.", color = Orange, fontSize = 12.sp)
                    FocusButton("Copy full diagnostics", TablerGlyph.COPY, Red) {
                        copyText(
                            "Full diagnostics",
                            buildDiagnosticReport(
                                context,
                                session.title,
                                session.config,
                                failure.mode,
                                session.results,
                                failure.error,
                                safe = false,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun intervalTable(result: TestResult): String = buildString {
    appendLine("Interval       Transfer      Bitrate")
    result.samples.forEach { sample ->
        val rate = if (result.downloadBitsPerSecond != null) sample.downloadBitsPerSecond else sample.uploadBitsPerSecond
        val bytes = if (result.downloadBitsPerSecond != null) sample.downloadBytes else sample.uploadBytes
        append(formatNumber(sample.startSeconds, 2).padStart(5))
        append("-")
        append(formatNumber(sample.endSeconds, 2).padEnd(6))
        append("  ")
        append((bytes?.let(::formatBytes) ?: "-").padEnd(12))
        append("  ")
        appendLine(rate?.let(::formatRate) ?: "-")
        if (result.mode == TestMode.TCP_BIDIRECTIONAL && sample.uploadBitsPerSecond != null) {
            append("  Upload       ")
            append((sample.uploadBytes?.let(::formatBytes) ?: "-").padEnd(12))
            append("  ")
            appendLine(formatRate(sample.uploadBitsPerSecond))
        }
    }
}


