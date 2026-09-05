package com.freeiperf3client.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ChooserScreen(
    onClient: () -> Unit,
    onServer: () -> Unit,
    openRepository: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
        val wide = maxWidth >= 800.dp || maxWidth > maxHeight
        Column(
            Modifier.fillMaxSize().padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Free iperf3", color = AppText, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
                    Text("Choose what to run", color = Teal, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                IconButton(onClick = openRepository, modifier = Modifier.size(32.dp).semantics { contentDescription = "Open project on GitHub" }) {
                    Icon(painter = painterResource(R.drawable.ic_github), contentDescription = null, tint = AppMuted, modifier = Modifier.size(24.dp))
                }
            }
            Box(Modifier.height(20.dp))
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoleCard(TablerGlyph.DOWNLOAD, Blue, "Client", "Run tests against a server", Modifier.weight(1f), tall = true, onClient)
                    RoleCard(TablerGlyph.SERVER, Teal, "Server", "Accept tests from other devices", Modifier.weight(1f), tall = true, onServer)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoleCard(TablerGlyph.DOWNLOAD, Blue, "Client", "Run tests against a server", Modifier.fillMaxWidth(), tall = false, onClient)
                    RoleCard(TablerGlyph.SERVER, Teal, "Server", "Accept tests from other devices", Modifier.fillMaxWidth(), tall = false, onServer)
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    glyph: TablerGlyph,
    accent: Color,
    title: String,
    subtitle: String,
    modifier: Modifier,
    tall: Boolean,
    onClick: () -> Unit,
) {
    FocusableGlassCard(
        selected = false,
        accent = accent,
        onClick = onClick,
        modifier = modifier.height(if (tall) 200.dp else 116.dp),
    ) {
        if (tall) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IconOrb(glyph, accent, 64.dp)
                Box(Modifier.height(14.dp))
                Text(title, color = AppText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.height(4.dp))
                Text(subtitle, color = AppMuted, fontSize = 13.sp)
            }
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconOrb(glyph, accent, 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(title, color = AppText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = AppMuted, fontSize = 13.sp)
                }
                TablerIcon(TablerGlyph.CHEVRON_RIGHT, null, AppMuted, Modifier.size(26.dp))
            }
        }
    }
}
