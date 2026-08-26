package com.freeiperf3client.app

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max

@Composable
internal fun ThroughputChart(samples: List<IntervalSample>, mode: TestMode, modifier: Modifier = Modifier) {
    val download = samples.mapNotNull { sample -> sample.downloadBitsPerSecond?.let { sample.endSeconds to it } }
    val upload = samples.mapNotNull { sample -> sample.uploadBitsPerSecond?.let { sample.endSeconds to it } }
    val allRates = (download + upload).map { it.second }
    val maxRate = max(1_000_000.0, allRates.maxOrNull() ?: 1_000_000.0)
    val roundedMax = ceil(maxRate / 100_000_000.0).coerceAtLeast(1.0) * 100_000_000.0
    val maxTime = max(1.0, samples.maxOfOrNull { it.endSeconds } ?: 1.0)
    GlassCard {
        Column(Modifier.padding(18.dp)) {
            Text("Mbit/s", color = AppMuted, fontSize = 12.sp)
            Canvas(
                modifier = modifier
                    .semantics { contentDescription = "Throughput over time chart" }
                    .padding(top = 8.dp),
            ) {
                val leftPad = 42.dp.toPx()
                val rightPad = 6.dp.toPx()
                val topPad = 8.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val plotWidth = size.width - leftPad - rightPad
                val plotHeight = size.height - topPad - bottomPad
                val labelPaint = Paint().apply {
                    color = android.graphics.Color.rgb(160, 173, 184)
                    textSize = 11.sp.toPx()
                    isAntiAlias = true
                }
                repeat(5) { index ->
                    val fraction = index / 4f
                    val y = topPad + plotHeight * fraction
                    drawLine(AppBorder.copy(alpha = .7f), Offset(leftPad, y), Offset(leftPad + plotWidth, y), 1.dp.toPx())
                    val value = roundedMax * (1f - fraction)
                    drawContext.canvas.nativeCanvas.drawText(
                        formatNumber(value / 1_000_000.0, 0),
                        0f,
                        y + 4.dp.toPx(),
                        labelPaint,
                    )
                }
                repeat(6) { index ->
                    val fraction = index / 5f
                    val x = leftPad + plotWidth * fraction
                    drawContext.canvas.nativeCanvas.drawText(
                        formatNumber(maxTime * fraction, 0) + "s",
                        x - 7.dp.toPx(),
                        size.height - 4.dp.toPx(),
                        labelPaint,
                    )
                }
                fun drawSeries(points: List<Pair<Double, Double>>, color: Color, fill: Boolean) {
                    if (points.isEmpty()) return
                    val path = Path()
                    points.forEachIndexed { index, (time, rate) ->
                        val x = leftPad + (time / maxTime).toFloat() * plotWidth
                        val y = topPad + (1f - (rate / roundedMax).toFloat().coerceIn(0f, 1f)) * plotHeight
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    if (fill && points.size > 1) {
                        val area = Path().apply {
                            val firstX = leftPad + (points.first().first / maxTime).toFloat() * plotWidth
                            val lastX = leftPad + (points.last().first / maxTime).toFloat() * plotWidth
                            moveTo(firstX, topPad + plotHeight)
                            points.forEach { (time, rate) ->
                                lineTo(
                                    leftPad + (time / maxTime).toFloat() * plotWidth,
                                    topPad + (1f - (rate / roundedMax).toFloat().coerceIn(0f, 1f)) * plotHeight,
                                )
                            }
                            lineTo(lastX, topPad + plotHeight)
                            close()
                        }
                        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = .28f), Color.Transparent)))
                    }
                    drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    points.forEach { (time, rate) ->
                        drawCircle(
                            color,
                            3.2.dp.toPx(),
                            Offset(
                                leftPad + (time / maxTime).toFloat() * plotWidth,
                                topPad + (1f - (rate / roundedMax).toFloat().coerceIn(0f, 1f)) * plotHeight,
                            ),
                        )
                    }
                }
                drawSeries(download, Blue, fill = mode != TestMode.TCP_BIDIRECTIONAL)
                drawSeries(upload, if (mode == TestMode.TCP_BIDIRECTIONAL) Green else colorForMode(mode), fill = download.isEmpty())
            }
            if (mode == TestMode.TCP_BIDIRECTIONAL) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    LegendDot(Blue, "Download")
                    Spacer(Modifier.width(22.dp))
                    LegendDot(Green, "Upload")
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, color = AppMuted, fontSize = 12.sp)
    }
}
