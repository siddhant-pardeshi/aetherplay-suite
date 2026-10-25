package io.github.siddhantpardeshi.aetherplay.space.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.siddhantpardeshi.aetherplay.design.AnimatedCounter
import io.github.siddhantpardeshi.aetherplay.design.AetherCard
import io.github.siddhantpardeshi.aetherplay.design.AuroraOk
import io.github.siddhantpardeshi.aetherplay.design.IcCrystal
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.Pill
import io.github.siddhantpardeshi.aetherplay.design.PrimaryButton
import io.github.siddhantpardeshi.aetherplay.design.PulsingDot
import io.github.siddhantpardeshi.aetherplay.design.SecondaryButton
import io.github.siddhantpardeshi.aetherplay.design.SectionLabel
import io.github.siddhantpardeshi.aetherplay.design.Tone
import io.github.siddhantpardeshi.aetherplay.protocol.SessionState
import io.github.siddhantpardeshi.aetherplay.space.Prefs
import io.github.siddhantpardeshi.aetherplay.space.SpaceViewModel
import io.github.siddhantpardeshi.aetherplay.space.bridge.BridgeService
import io.github.siddhantpardeshi.aetherplay.space.game.GameActivity

@Composable
fun SessionScreen(vm: SpaceViewModel) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val session = vm.session.collectAsState().value
    val spaces = vm.spaces.collectAsState().value
    var selectedSpace by remember {
        mutableStateOf(Prefs.lastSpace(context) ?: spaces.firstOrNull()?.key ?: "main")
    }
    val running = session.state == SessionState.RUNNING

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        SectionLabel("Experiment session")
        Spacer(Modifier.height(12.dp))

        AetherCard(raised = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor by animateColorAsState(
                    targetValue = if (running) AuroraOk else palette.textSecondary,
                    label = "dot",
                )
                PulsingDot(dotColor, size = 10.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    when (session.state) {
                        SessionState.RUNNING -> "Session running"
                        SessionState.STARTING -> "Starting…"
                        SessionState.STOPPING -> "Stopping…"
                        SessionState.FAILED -> "Session failed"
                        else -> "No session"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                if (running) Pill("Gen ${session.generation}", Tone.ACCENT)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Stat("Crystals game", if (running) "live" else "—")
                Stat("Freezes", if (running) session.freezeCount.toString() else "—")
                Stat("Elapsed", if (running) formatElapsed(session.elapsedMs) else "—")
            }
            Spacer(Modifier.height(18.dp))
            if (!running) {
                Text("Game", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                Spacer(Modifier.height(6.dp))
                Text("Crystal Sprint (training)", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Spacer(Modifier.height(14.dp))
                Text("Space", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.raised)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    spaces.take(4).forEach { space ->
                        val selected = space.key == selectedSpace
                        val bg by animateColorAsState(
                            targetValue = if (selected) accent.accent.copy(alpha = 0.18f) else Color.Transparent,
                            label = "sel",
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(bg)
                                .noRippleClickable {
                                    selectedSpace = space.key
                                    Prefs.setLastSpace(context, space.key)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                space.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) accent.accent else palette.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    "Start session",
                    onClick = {
                        Prefs.setLastSpace(context, selectedSpace)
                        if (Build.VERSION.SDK_INT >= 33) {
                            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        BridgeService.startSession(context, "crystal-sprint", selectedSpace)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = IcCrystal,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "A session keeps Crystal Sprint alive in the background and makes it visible to AetherPlay Lab on this phone only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton("Open game", onClick = {
                        context.startActivity(Intent(context, GameActivity::class.java))
                    })
                    SecondaryButton("Stop session", tone = Tone.ERROR, onClick = {
                        BridgeService.stopSession(context)
                    })
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Stopping saves the game, releases every freeze, and closes the bridge to Lab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        AetherCard {
            SectionLabel("What Lab can do in this session")
            Spacer(Modifier.height(10.dp))
            listOf(
                "Search exact, range and unknown values",
                "Refine by changed, unchanged, increased, decreased",
                "Preview a write before committing it",
                "Undo the last write, freeze a value",
            ).forEach { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent.accent))
                    Spacer(Modifier.width(10.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, color = LocalPalette.current.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.textSecondary)
    }
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        ),
    )
}
