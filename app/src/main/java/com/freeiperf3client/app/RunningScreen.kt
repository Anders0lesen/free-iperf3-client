package com.freeiperf3client.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RunningScreen(state: RunState?, onCancel: () -> Unit) {
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
