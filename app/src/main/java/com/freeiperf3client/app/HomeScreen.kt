package com.freeiperf3client.app

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        // Landscape (tilted phone/tablet) or a genuinely wide screen (TV, big tablet)
        // gets the two-column reflow; portrait phones get the tall single column.
        val wide = maxWidth >= 800.dp || maxWidth > maxHeight
        val contentWidth = if (maxWidth > 1240.dp) 1200.dp else maxWidth
        val hPad = if (maxWidth > 600.dp) 22.dp else 16.dp
        val startEnabled = validation.valid && detectionStatus != DetectionStatus.CHECKING
        val startHelper = validation.firstError ?: "Ready to test"

        Column(
            modifier = Modifier
                .width(contentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = hPad)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeHeader()
            if (wide) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(.9f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ConfigCard(
                            host, port, duration, udpTarget, attemptedStart, validation,
                            onHostChange, onPortChange, onDurationChange, onUdpChange,
                        ) { focusManager.clearFocus() }
                        if (recentServers.isNotEmpty()) {
                            RecentServersCard(recentServers, host, port.toIntOrNull(), onRecentSelect, onRecentRemove, onRecentClear)
                        }
                    }
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TestChoiceGrid(selectedChoice, onChoice, showSubtitle = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                ChoiceRow(TestChoice.RUN_ALL, TablerGlyph.LAYERS, Teal, selectedChoice == TestChoice.RUN_ALL, false, Modifier.fillMaxWidth()) { onChoice(TestChoice.RUN_ALL) }
                            }
                            Box(Modifier.weight(1f)) { DetectionCard(detectionStatus, detectionMessage, onDetect, Modifier.fillMaxWidth()) }
                        }
                        StartCard(selectedChoice, startEnabled, startHelper, wide = true, onClick = onStart)
                    }
                }
            } else {
                ConfigCard(
                    host, port, duration, udpTarget, attemptedStart, validation,
                    onHostChange, onPortChange, onDurationChange, onUdpChange,
                ) { focusManager.clearFocus() }
                if (recentServers.isNotEmpty()) {
                    RecentServersCard(recentServers, host, port.toIntOrNull(), onRecentSelect, onRecentRemove, onRecentClear)
                }
                TestChoiceGrid(selectedChoice, onChoice, showSubtitle = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        ChoiceRow(TestChoice.RUN_ALL, TablerGlyph.LAYERS, Teal, selectedChoice == TestChoice.RUN_ALL, false, Modifier.fillMaxWidth()) { onChoice(TestChoice.RUN_ALL) }
                    }
                    Box(Modifier.weight(1f)) { DetectionCard(detectionStatus, detectionMessage, onDetect, Modifier.fillMaxWidth()) }
                }
                StartCard(selectedChoice, startEnabled, startHelper, wide = false, onClick = onStart)
            }
        }

        // GitHub mark, pinned 3dp from the top-right corner.
        GitHubMark(Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 3.dp), openRepository)
    }
}

@Composable
private fun HomeHeader() {
    Column(
        Modifier.fillMaxWidth().height(52.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            "Free iperf3 Client",
            color = AppText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.5).sp,
        )
        Text("Simply free.", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GitHubMark(modifier: Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier.size(32.dp).semantics { contentDescription = "Open project on GitHub" }) {
        Icon(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = null,
            tint = AppMuted,
            modifier = Modifier.size(24.dp),
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
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent servers", color = AppText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("Clear all", color = AppMuted, fontSize = 13.sp) }
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
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TablerIcon(TablerGlyph.SERVER, null, if (selected) Teal else AppMuted, Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(server.endpoint, color = if (selected) Teal else AppText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (selected) "Selected" else "Tap to use", color = AppMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { onRemove(server) }, modifier = Modifier.semantics { contentDescription = "Forget ${server.endpoint}" }) {
                        TablerIcon(TablerGlyph.CLOSE, null, AppMuted, Modifier.size(18.dp))
                    }
                }
                if (index != servers.lastIndex) HorizontalDivider(color = AppBorder, modifier = Modifier.padding(horizontal = 16.dp))
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
            } else {
                // Tapping anywhere on the row (the pen included) opens the keyboard on the field.
                Modifier.clickable { focusRequester.requestFocus() }
            }
        )
        .padding(horizontal = 16.dp, vertical = 11.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconOrb(glyph, Teal, 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, color = AppMuted, fontSize = 12.sp)
            if (isTelevision) {
                Text(
                    text = value.ifBlank { placeholder } + if (value.isNotBlank()) suffix else "",
                    color = if (value.isBlank()) AppMuted.copy(alpha = .55f) else AppText,
                    fontSize = 16.sp,
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
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        onDone = { onDone() },
                    ),
                    cursorBrush = Brush.verticalGradient(listOf(Teal, Teal)),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                if (value.isBlank()) Text(placeholder, color = AppMuted.copy(alpha = .55f), fontSize = 15.sp)
                                inner()
                            }
                            if (value.isNotBlank() && suffix.isNotBlank()) {
                                Text(suffix, color = AppMuted, fontSize = 13.sp)
                            }
                        }
                    },
                )
            }
            AnimatedVisibility(error != null) {
                Text(error.orEmpty(), color = Red, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        TablerIcon(TablerGlyph.EDIT, null, AppMuted, Modifier.size(20.dp))
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
    HorizontalDivider(color = AppBorder.copy(alpha = .65f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun TestChoiceGrid(selected: TestChoice, onChoice: (TestChoice) -> Unit, showSubtitle: Boolean) {
    val choices = listOf(
        Triple(TestChoice.TCP_DOWNLOAD, TablerGlyph.DOWNLOAD, Blue),
        Triple(TestChoice.TCP_UPLOAD, TablerGlyph.UPLOAD, Green),
        Triple(TestChoice.TCP_BIDIRECTIONAL, TablerGlyph.ARROWS_EXCHANGE, Purple),
        Triple(TestChoice.UDP_QUALITY, TablerGlyph.ACTIVITY, Orange),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        choices.chunked(2).forEach { rowChoices ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowChoices.forEach { (choice, glyph, accent) ->
                    ChoiceRow(choice, glyph, accent, selected == choice, showSubtitle, Modifier.weight(1f)) { onChoice(choice) }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    choice: TestChoice,
    glyph: TablerGlyph,
    accent: Color,
    selected: Boolean,
    showSubtitle: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FocusableGlassCard(
        selected = selected,
        accent = accent,
        onClick = onClick,
        modifier = modifier.height(if (showSubtitle) 72.dp else 64.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconOrb(glyph, accent, 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    choice.title,
                    color = AppText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (showSubtitle) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showSubtitle) {
                    Text(choice.subtitle, color = AppMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DetectionCard(status: DetectionStatus, message: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        DetectionStatus.NOT_CHECKED -> "Find servers"
        DetectionStatus.CHECKING -> "Scanning…"
        DetectionStatus.DETECTED -> "Server found"
        DetectionStatus.FAILED -> "Scan finished"
    }
    FocusableGlassCard(
        selected = status == DetectionStatus.DETECTED,
        accent = accent,
        onClick = onClick,
        enabled = status != DetectionStatus.CHECKING,
        modifier = modifier.height(56.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconOrb(glyph, accent, 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, color = AppText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (status != DetectionStatus.NOT_CHECKED) {
                    Text(message, color = if (status == DetectionStatus.FAILED) Red else AppMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun StartCard(choice: TestChoice, enabled: Boolean, helper: String, wide: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val circle = if (wide) 40.dp else 38.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color(0xFF0A2625), Color(0xFF0B4441)))
                else Brush.horizontalGradient(listOf(AppSurface, AppSurfaceRaised)),
            )
            .border(1.dp, if (focused) Teal else AppBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(circle).clip(CircleShape).background(if (enabled) Teal else AppBorder),
                contentAlignment = Alignment.Center,
            ) {
                TablerIcon(TablerGlyph.PLAY, null, if (enabled) Color.White else AppMuted, Modifier.size(circle * .58f))
            }
            if (wide) {
                Text(
                    "Start Test",
                    color = if (enabled) Teal else AppMuted,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (enabled) choice.title else helper,
                    color = AppMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            } else {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "Start Test",
                        color = if (enabled) Teal else AppMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        if (enabled) choice.title else helper,
                        color = AppMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
