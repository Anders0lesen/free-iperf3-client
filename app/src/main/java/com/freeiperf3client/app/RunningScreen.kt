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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RunningScreen(state: RunState?, onCancel: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
    ) {
        val wide = maxWidth >= 800.dp || maxWidth > maxHeight
        val contentWidth = if (maxWidth > 1240.dp) 1200.dp else maxWidth
        Column(
            Modifier
                .width(contentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (maxWidth > 600.dp) 22.dp else 16.dp, vertical = 10.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTopBar("Testing", onCancel, TablerGlyph.CLOSE, "Cancel test")
            if (state == null) {
                GlassCard { Text("Preparing test…", modifier = Modifier.padding(20.dp), color = AppText) }
                return@Column
            }
            GlassCard {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconOrb(glyphForMode(state.currentMode), colorForMode(state.currentMode), 40.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(state.currentMode.title, color = colorForMode(state.currentMode), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("${state.stage} · stage ${state.currentIndex + 1} of ${state.modes.size}", color = AppMuted, fontSize = 12.sp)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = colorForMode(state.currentMode),
                trackColor = AppBorder,
            )
            if (wide) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(.85f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LiveHero(state, big = true)
                        RunningCommandCard(state.command)
                        FocusButton("Cancel test", TablerGlyph.STOP, Red, onCancel)
                    }
                    ThroughputChart(
                        samples = state.samples,
                        mode = state.currentMode,
                        modifier = Modifier.weight(1.15f).height(320.dp),
                    )
                }
            } else {
                LiveHero(state, big = false)
                ThroughputChart(
                    samples = state.samples,
                    mode = state.currentMode,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TablerIcon(TablerGlyph.TERMINAL, null, AppMuted, Modifier.size(20.dp))
            Text(
                command,
                color = AppMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LiveHero(state: RunState, big: Boolean) {
    val live = state.live
    val primary = live.downloadBitsPerSecond ?: live.uploadBitsPerSecond
    GlassCard {
        Column(
            Modifier.fillMaxWidth().padding(if (big) 20.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (state.currentMode == TestMode.DETECT) {
                IconOrb(TablerGlyph.SERVER, Orange, 48.dp)
                Text("Confirming iperf3", color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(live.connection ?: "Waiting for the server response", color = AppMuted, fontSize = 13.sp)
            } else if (primary != null) {
                val parts = formatRateParts(primary)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(parts.first, color = colorForMode(state.currentMode), fontSize = if (big) 56.sp else 40.sp, fontWeight = FontWeight.Bold)
                    Text(parts.second, color = AppMuted, fontSize = if (big) 18.sp else 15.sp, modifier = Modifier.padding(bottom = if (big) 8.dp else 6.dp))
                }
                Text(
                    "Running ${live.elapsedSeconds.toInt()} of ${state.config.durationSeconds} seconds",
                    color = AppMuted,
                    fontSize = 12.sp,
                )
                if (state.currentMode == TestMode.TCP_BIDIRECTIONAL) {
                    Text(
                        "Download ${formatRate(live.downloadBitsPerSecond ?: 0.0)}   Upload ${formatRate(live.uploadBitsPerSecond ?: 0.0)}",
                        color = AppText,
                        fontSize = 13.sp,
                    )
                }
                if (live.jitterMs != null || live.lossPercent != null) {
                    Text(
                        "Jitter ${formatMilliseconds(live.jitterMs ?: 0.0)}   Loss ${formatPercent(live.lossPercent ?: 0.0)}",
                        color = AppText,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Text("Connecting", color = Orange, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Opening the iperf3 control connection", color = AppMuted, fontSize = 13.sp)
            }
        }
    }
}
