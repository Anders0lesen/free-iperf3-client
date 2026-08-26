package com.freeiperf3client.app

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HomeScreen(
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
