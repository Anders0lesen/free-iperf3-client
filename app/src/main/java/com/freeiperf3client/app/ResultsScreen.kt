package com.freeiperf3client.app

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
internal fun ResultsScreen(
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


