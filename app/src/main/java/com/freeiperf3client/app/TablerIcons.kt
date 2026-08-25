package com.freeiperf3client.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Native vector renderings based on the 24x24, 2px-stroke Tabler Icons system.
 * Tabler Icons is MIT licensed: https://github.com/tabler/tabler-icons
 */
internal enum class TablerGlyph {
    SERVER,
    MONITOR,
    PORT,
    CLOCK,
    DOWNLOAD,
    UPLOAD,
    ARROWS_EXCHANGE,
    ACTIVITY,
    LAYERS,
    PLAY,
    CHECK,
    CHEVRON_RIGHT,
    EDIT,
    ARROW_LEFT,
    SHARE,
    COPY,
    TERMINAL,
    ALERT,
    CLOSE,
    REFRESH,
    SHIELD,
    STOP,
    INFO,
    QR_CODE,
}

@Composable
internal fun TablerIcon(
    glyph: TablerGlyph,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = if (contentDescription == null) modifier else {
            modifier.semantics { this.contentDescription = contentDescription }
        },
    ) {
        val scale = size.minDimension / 24f
        val left = (size.width - 24f * scale) / 2f
        val top = (size.height - 24f * scale) / 2f
        fun point(x: Float, y: Float) = Offset(left + x * scale, top + y * scale)
        val stroke = Stroke(
            width = 2f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, point(x1, y1), point(x2, y2), stroke.width, StrokeCap.Round)
        fun polyline(vararg values: Float) {
            val path = Path()
            values.toList().chunked(2).forEachIndexed { index, pair ->
                val p = point(pair[0], pair[1])
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, tint, style = stroke)
        }
        fun circle(x: Float, y: Float, radius: Float) =
            drawCircle(tint, radius * scale, point(x, y), style = stroke)
        fun roundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float = 2f) =
            drawRoundRect(
                color = tint,
                topLeft = point(x, y),
                size = Size(width * scale, height * scale),
                cornerRadius = CornerRadius(radius * scale),
                style = stroke,
            )

        when (glyph) {
            TablerGlyph.SERVER -> {
                roundedRect(3f, 4f, 18f, 6f, 1.5f)
                roundedRect(3f, 14f, 18f, 6f, 1.5f)
                circle(7f, 7f, .35f)
                circle(7f, 17f, .35f)
                line(11f, 7f, 18f, 7f)
                line(11f, 17f, 18f, 17f)
            }
            TablerGlyph.MONITOR -> {
                roundedRect(3f, 4f, 18f, 13f)
                line(8f, 21f, 16f, 21f)
                line(10f, 17f, 10f, 21f)
                line(14f, 17f, 14f, 21f)
                line(7f, 8f, 9f, 10f)
                line(9f, 10f, 7f, 12f)
                line(13f, 12f, 17f, 12f)
            }
            TablerGlyph.PORT -> {
                roundedRect(5f, 3f, 14f, 7f)
                line(8f, 10f, 8f, 14f)
                line(16f, 10f, 16f, 14f)
                line(4f, 14f, 20f, 14f)
                line(6f, 14f, 6f, 21f)
                line(10f, 14f, 10f, 19f)
                line(14f, 14f, 14f, 19f)
                line(18f, 14f, 18f, 21f)
            }
            TablerGlyph.CLOCK -> {
                circle(12f, 13f, 8f)
                line(12f, 13f, 12f, 9f)
                line(12f, 13f, 15f, 15f)
                line(9f, 2f, 15f, 2f)
                line(12f, 2f, 12f, 5f)
                line(18f, 6f, 20f, 8f)
            }
            TablerGlyph.DOWNLOAD -> {
                line(12f, 3f, 12f, 16f)
                polyline(7f, 11f, 12f, 16f, 17f, 11f)
                line(5f, 21f, 19f, 21f)
            }
            TablerGlyph.UPLOAD -> {
                line(12f, 21f, 12f, 8f)
                polyline(7f, 13f, 12f, 8f, 17f, 13f)
                line(5f, 3f, 19f, 3f)
            }
            TablerGlyph.ARROWS_EXCHANGE -> {
                line(7f, 7f, 20f, 7f)
                polyline(17f, 4f, 20f, 7f, 17f, 10f)
                line(17f, 17f, 4f, 17f)
                polyline(7f, 14f, 4f, 17f, 7f, 20f)
            }
            TablerGlyph.ACTIVITY ->
                polyline(3f, 12f, 6f, 12f, 9f, 3f, 15f, 21f, 18f, 12f, 21f, 12f)
            TablerGlyph.LAYERS -> {
                polyline(12f, 2f, 3f, 7f, 12f, 12f, 21f, 7f, 12f, 2f)
                polyline(3f, 12f, 12f, 17f, 21f, 12f)
                polyline(3f, 17f, 12f, 22f, 21f, 17f)
            }
            TablerGlyph.PLAY -> {
                val path = Path().apply {
                    val a = point(7f, 4f)
                    val b = point(20f, 12f)
                    val c = point(7f, 20f)
                    moveTo(a.x, a.y)
                    lineTo(b.x, b.y)
                    lineTo(c.x, c.y)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            TablerGlyph.CHECK -> {
                circle(12f, 12f, 9f)
                polyline(8f, 12f, 11f, 15f, 16f, 9f)
            }
            TablerGlyph.CHEVRON_RIGHT -> polyline(9f, 6f, 15f, 12f, 9f, 18f)
            TablerGlyph.EDIT -> {
                polyline(4f, 20f, 8f, 19f, 19f, 8f, 16f, 5f, 5f, 16f, 4f, 20f)
                polyline(14f, 7f, 17f, 10f)
            }
            TablerGlyph.ARROW_LEFT -> {
                line(5f, 12f, 19f, 12f)
                polyline(10f, 6f, 4f, 12f, 10f, 18f)
            }
            TablerGlyph.SHARE -> {
                circle(6f, 12f, 2f)
                circle(18f, 6f, 2f)
                circle(18f, 18f, 2f)
                line(8f, 11f, 16f, 7f)
                line(8f, 13f, 16f, 17f)
            }
            TablerGlyph.COPY -> {
                roundedRect(8f, 8f, 12f, 12f)
                polyline(16f, 8f, 16f, 6f, 14f, 4f, 6f, 4f, 4f, 6f, 4f, 14f, 6f, 16f, 8f, 16f)
            }
            TablerGlyph.TERMINAL -> {
                roundedRect(3f, 4f, 18f, 16f)
                polyline(7f, 9f, 10f, 12f, 7f, 15f)
                line(13f, 15f, 17f, 15f)
            }
            TablerGlyph.ALERT -> {
                val path = Path().apply {
                    val a = point(12f, 3f)
                    val b = point(21f, 20f)
                    val c = point(3f, 20f)
                    moveTo(a.x, a.y)
                    lineTo(b.x, b.y)
                    lineTo(c.x, c.y)
                    close()
                }
                drawPath(path, tint, style = stroke)
                line(12f, 9f, 12f, 13f)
                circle(12f, 17f, .35f)
            }
            TablerGlyph.CLOSE -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }
            TablerGlyph.REFRESH -> {
                drawArc(
                    color = tint,
                    startAngle = -55f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = point(4f, 4f),
                    size = Size(16f * scale, 16f * scale),
                    style = stroke,
                )
                polyline(17f, 3f, 20f, 4f, 19f, 7f)
            }
            TablerGlyph.SHIELD -> {
                val path = Path().apply {
                    val points = listOf(
                        point(12f, 3f), point(20f, 6f), point(19f, 14f),
                        point(16f, 19f), point(12f, 21f), point(8f, 19f),
                        point(5f, 14f), point(4f, 6f), point(12f, 3f),
                    )
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, tint, style = stroke)
                polyline(9f, 12f, 11f, 14f, 15f, 10f)
            }
            TablerGlyph.STOP -> roundedRect(6f, 6f, 12f, 12f)
            TablerGlyph.INFO -> {
                circle(12f, 12f, 9f)
                line(12f, 11f, 12f, 16f)
                circle(12f, 8f, .35f)
            }
            TablerGlyph.QR_CODE -> {
                roundedRect(3f, 3f, 7f, 7f, 1f)
                roundedRect(14f, 3f, 7f, 7f, 1f)
                roundedRect(3f, 14f, 7f, 7f, 1f)
                roundedRect(5f, 5f, 3f, 3f, .5f)
                roundedRect(16f, 5f, 3f, 3f, .5f)
                roundedRect(5f, 16f, 3f, 3f, .5f)
                line(14f, 14f, 14f, 18f)
                line(14f, 18f, 17f, 18f)
                line(17f, 14f, 21f, 14f)
                line(19f, 14f, 19f, 17f)
                line(17f, 21f, 21f, 21f)
                line(21f, 17f, 21f, 21f)
            }
        }
    }
}
