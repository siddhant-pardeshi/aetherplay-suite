package io.github.siddhantpardeshi.aetherplay.lab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import io.github.siddhantpardeshi.aetherplay.design.AetherCard
import io.github.siddhantpardeshi.aetherplay.design.AnimatedCounter
import io.github.siddhantpardeshi.aetherplay.design.IcCrystal
import io.github.siddhantpardeshi.aetherplay.design.IcScan
import io.github.siddhantpardeshi.aetherplay.design.IcSpark
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.Pill
import io.github.siddhantpardeshi.aetherplay.design.PrimaryButton
import io.github.siddhantpardeshi.aetherplay.design.PulsingDot
import io.github.siddhantpardeshi.aetherplay.design.SecondaryButton
import io.github.siddhantpardeshi.aetherplay.design.SectionLabel
import io.github.siddhantpardeshi.aetherplay.design.Tone
import io.github.siddhantpardeshi.aetherplay.lab.LabViewModel
import io.github.siddhantpardeshi.aetherplay.lab.bridge.ConnState
import io.github.siddhantpardeshi.aetherplay.protocol.SessionState
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun HomeScreen(vm: LabViewModel, onOpenSearch: () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val conn = vm.conn.collectAsState().value
    val session = vm.session.collectAsState().value

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.bg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Hero
        item {
            Column {
                Text("AETHERPLAY LAB", style = MaterialTheme.typography.labelSmall, color = accent.accent)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The instrument\nfor your games",
                    style = MaterialTheme.typography.displaySmall,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Search values, refine them down, preview every change, undo, and freeze — against a live game session on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }
        }

        // Connection card
        item {
            AetherCard(raised = true) {
                val connState = conn
                val title: String
                val body: String
                when (connState) {
                    is ConnState.Connected -> {
                        val info = connState.info
                        title = "Connected to AetherPlay Space"
                        body = "Bridge protocol ${info.protocolMajor}.${info.protocolMinor} · Space build ${info.spaceBuild}"
                    }
                    is ConnState.Connecting -> {
                        title = "Connecting…"
                        body = "Reaching AetherPlay Space"
                    }
                    is ConnState.Incompatible -> {
                        title = "Version mismatch"
                        body = connState.reason
                    }
                    else -> {
                        title = "Space is not connected"
                        body = "Install and open AetherPlay Space, start a session, then come back."
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dot = when (connState) {
                        is ConnState.Connected -> palette.textPrimary
                        is ConnState.Incompatible -> io.github.siddhantpardeshi.aetherplay.design.AuroraError
                        else -> palette.textSecondary
                    }
                    PulsingDot(dot, size = 9.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                if (connState !is ConnState.Connected) {
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Retry connection") { vm.reconnect() }
                }
            }
        }

        // Session card
        item {
            AetherCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IcCrystal, contentDescription = null, tint = accent.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when {
                            session == null -> "Reading session status…"
                            session?.state == SessionState.RUNNING -> "Crystal Sprint — live"
                            else -> "No active session"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    if (session?.state == SessionState.RUNNING) {
                        Pill("Gen ${session?.generation ?: 0}", Tone.ACCENT)
                    }
                }
                if (session == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "One moment — checking the session state with AetherPlay Space.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                } else if (session?.state == SessionState.RUNNING) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column {
                            AnimatedCounter(session?.freezeCount ?: 0, color = accent.accent)
                            Text("FREEZES", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column {
                            Mono(session?.let { fmtElapsed(it.elapsedMs) } ?: "—")
                            Text("ELAPSED", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Start searching", onClick = onOpenSearch, icon = IcScan)
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Open AetherPlay Space, pick a space, and start a session there. Sessions always begin with a visible action in Space.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
            }
        }

        // How it works
        item {
            AetherCard {
                SectionLabel("How a first experiment works")
                Spacer(Modifier.height(12.dp))
                listOf(
                    "1" to "Start a session in AetherPlay Space and open Crystal Sprint",
                    "2" to "Connect Lab — the handshake checks both apps are a matched pair",
                    "3" to "Search the crystals count you see on screen",
                    "4" to "Play a moment, then refine: increased, unchanged, exact…",
                    "5" to "Preview a write, commit it, watch the game change",
                    "6" to "Undo it or freeze it — then stop the session",
                ).forEach { (n, text) ->
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Brush.linearGradient(listOf(accent.gradientStart, accent.gradientEnd))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(n, style = MaterialTheme.typography.labelSmall, color = accent.onAccent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    }
                }
            }
        }

        item {
            AetherCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IcSpark, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Honest capabilities", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Everything in this build works against the included cooperative training game. Editing other installed games is not supported: that requires the experimental guest runtime, which is not part of this release.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Mono(text: String) {
    io.github.siddhantpardeshi.aetherplay.design.MonoValue(text = text, color = LocalPalette.current.textPrimary)
}

private fun fmtElapsed(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
