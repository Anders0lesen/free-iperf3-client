package com.freeiperf3client.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Central design tokens for the app. This is the primary surface for visual
 * edits (for example when back-porting palette changes from the design canvas):
 * change a colour here and every screen follows.
 */
internal val AppBackground = Color(0xFF05090D)
internal val AppSurface = Color(0xFF0B1218)
internal val AppSurfaceRaised = Color(0xFF101920)
internal val AppBorder = Color(0xFF24313A)
internal val AppText = Color(0xFFF4F7FA)
internal val AppMuted = Color(0xFFAEB9C2)
internal val Teal = Color(0xFF14D8C4)
internal val Blue = Color(0xFF2F80FF)
internal val Green = Color(0xFF35D05B)
internal val Purple = Color(0xFFC05EF5)
internal val Orange = Color(0xFFFF9800)
internal val Red = Color(0xFFFF6070)

@Composable
internal fun IperfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Teal,
            secondary = Blue,
            tertiary = Purple,
            background = AppBackground,
            surface = AppSurface,
            error = Red,
            onPrimary = Color(0xFF001713),
            onBackground = AppText,
            onSurface = AppText,
        ),
        content = content,
    )
}

@Composable
internal fun AppBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Teal.copy(alpha = .09f), Color.Transparent),
                        center = Offset(size.width * .12f, size.height * .18f),
                        radius = size.width * .8f,
                    ),
                    radius = size.width * .8f,
                    center = Offset(size.width * .12f, size.height * .18f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Blue.copy(alpha = .055f), Color.Transparent),
                        center = Offset(size.width, size.height * .7f),
                        radius = size.width * .7f,
                    ),
                    radius = size.width * .7f,
                    center = Offset(size.width, size.height * .7f),
                )
            },
    ) { content() }
}

internal fun glyphForMode(mode: TestMode): TablerGlyph = when (mode) {
    TestMode.DETECT -> TablerGlyph.SERVER
    TestMode.TCP_DOWNLOAD -> TablerGlyph.DOWNLOAD
    TestMode.TCP_UPLOAD -> TablerGlyph.UPLOAD
    TestMode.TCP_BIDIRECTIONAL -> TablerGlyph.ARROWS_EXCHANGE
    TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD -> TablerGlyph.ACTIVITY
}

internal fun colorForMode(mode: TestMode): Color = when (mode) {
    TestMode.DETECT -> Teal
    TestMode.TCP_DOWNLOAD -> Blue
    TestMode.TCP_UPLOAD -> Green
    TestMode.TCP_BIDIRECTIONAL -> Purple
    TestMode.UDP_DOWNLOAD, TestMode.UDP_UPLOAD -> Orange
}
