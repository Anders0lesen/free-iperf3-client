package com.freeiperf3client.app

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_LOG_LINES = 400

@Composable
internal fun ServerScreen(
    engine: IperfEngine,
    serverDiscovery: ServerDiscovery,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    var port by remember { mutableStateOf("5201") }
    var oneOff by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Not running") }
    var job by remember { mutableStateOf<Job?>(null) }
    val log = remember { mutableStateListOf<String>() }
    val localAddress = remember { serverDiscovery.activeNetwork()?.localAddress }

    fun append(line: String) {
        log.add(line)
        while (log.size > MAX_LOG_LINES) log.removeAt(0)
    }

    fun stop() {
        engine.stopServer()
        job?.cancel()
        job = null
        running = false
        status = "Stopped"
    }

    fun start() {
        val p = port.toIntOrNull()
        if (p == null || p !in 1..65535) {
            status = "Enter a port from 1 to 65535"
            return
        }
        if (running) return
        log.clear()
        running = true
        status = "Listening on ${localAddress ?: "this device"}:$p"
        job = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    engine.runServer(ServerConfig(port = p, oneOff = oneOff)) { line ->
                        activity.runOnUiThread { append(line) }
                    }
                }
                activity.runOnUiThread {
                    running = false
                    status = "Stopped"
                    append("— server stopped —")
                }
            } catch (_: CancellationException) {
                // Stopped by the user.
            } catch (error: Throwable) {
                activity.runOnUiThread {
                    running = false
                    status = friendlyError(error)
                    append("Error: ${error.message ?: error.javaClass.simpleName}")
                }
            } finally {
                job = null
            }
        }
    }

    // Foreground only: leaving this screen (role switch, back, or teardown) stops the server.
    DisposableEffect(Unit) {
        onDispose { engine.stopServer() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Top bar
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            VectorIconButton(TablerGlyph.ARROW_LEFT, "Back to selection", onExit)
            Text("Server", color = AppText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
        }

        // Address / status card
        GlassCard {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconOrb(TablerGlyph.SERVER, if (running) Green else AppMuted, 44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (running) "Serving" else "iperf3 server", color = if (running) Green else AppText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (localAddress != null) "Point a client at $localAddress:${port.ifBlank { "5201" }}" else status,
                        color = AppMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (!running) {
            // Config
            GlassCard {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconOrb(TablerGlyph.PORT, Teal, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Port", color = AppMuted, fontSize = 12.sp)
                            BasicTextField(
                                value = port,
                                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                                singleLine = true,
                                textStyle = TextStyle(color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = Brush.verticalGradient(listOf(Teal, Teal)),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconOrb(TablerGlyph.CHECK, Teal, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Stop after one test", color = AppText, fontSize = 15.sp)
                            Text("Serve a single client, then exit (-1)", color = AppMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = oneOff,
                            onCheckedChange = { oneOff = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Teal,
                                uncheckedThumbColor = AppMuted,
                                uncheckedTrackColor = AppBorder,
                            ),
                        )
                    }
                }
            }
            Text(
                engine.serverCommand(ServerConfig(port = port.toIntOrNull() ?: 5201, oneOff = oneOff)),
                color = AppMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Box(Modifier.weight(1f))
            ServerActionButton("Start server", TablerGlyph.PLAY, Green, ::start)
        } else {
            // Live log
            GlassCard(Modifier.weight(1f)) {
                val listState = rememberLazyListState()
                LaunchedEffect(log.size) {
                    if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
                }
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Text(status, color = Green, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    if (log.isEmpty()) {
                        Text("Waiting for a client to connect…", color = AppMuted, fontSize = 12.sp)
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(log) { line ->
                                Text(line, color = AppMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
            ServerActionButton("Stop server", TablerGlyph.STOP, Red, ::stop)
        }
    }
}

@Composable
private fun ServerActionButton(label: String, glyph: TablerGlyph, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = .13f))
            .border(1.dp, color.copy(alpha = .4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TablerIcon(glyph, null, color, Modifier.size(22.dp))
            Text(label, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
