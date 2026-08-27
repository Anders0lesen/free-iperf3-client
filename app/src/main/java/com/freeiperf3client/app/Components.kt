package com.freeiperf3client.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable visual building blocks shared across every screen. Layout tweaks to
 * these (card corners, focus rings, orbs, buttons) propagate everywhere at once.
 */

@Composable
internal fun IconOrb(glyph: TablerGlyph, accent: Color, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(accent.copy(alpha = .11f))
            .border(1.dp, accent.copy(alpha = .25f), RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center,
    ) {
        TablerIcon(glyph, null, accent, Modifier.size(size * .55f))
    }
}

@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    border: Color = AppBorder,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(AppSurfaceRaised.copy(alpha = .96f), AppSurface.copy(alpha = .96f))))
            .border(1.dp, border, shape),
    ) { content() }
}

@Composable
internal fun FocusableGlassCard(
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, tween(120), label = "cardFocus")
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (selected) listOf(accent.copy(alpha = .16f), AppSurface)
                    else listOf(AppSurfaceRaised, AppSurface),
                ),
            )
            .border(
                width = if (focused || selected) 2.dp else 1.dp,
                color = when {
                    focused -> accent
                    selected -> accent.copy(alpha = .7f)
                    else -> AppBorder
                },
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled),
    ) { content() }
}

@Composable
internal fun FocusButton(
    label: String,
    glyph: TablerGlyph,
    color: Color,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 0.dp, color, RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = .13f),
            contentColor = color,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        TablerIcon(glyph, null, color, Modifier.size(23.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun SectionTitle(glyph: TablerGlyph, title: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TablerIcon(glyph, null, color, Modifier.size(23.dp))
        Text(title, color = color, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ScreenTopBar(title: String, onAction: () -> Unit, glyph: TablerGlyph, description: String) {
    Row(Modifier.fillMaxWidth().height(68.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(48.dp))
        Text(title, color = AppText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        VectorIconButton(glyph, description, onAction)
    }
}

@Composable
internal fun VectorIconButton(glyph: TablerGlyph, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        TablerIcon(glyph, description, AppText, Modifier.size(28.dp))
    }
}
