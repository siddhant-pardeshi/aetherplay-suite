package io.github.siddhantpardeshi.aetherplay.space.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.siddhantpardeshi.aetherplay.design.AetherTheme
import io.github.siddhantpardeshi.aetherplay.design.AppIdentity
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.MonoValueStyle
import io.github.siddhantpardeshi.aetherplay.design.PrimaryButton
import io.github.siddhantpardeshi.aetherplay.design.SpaceAccent
import io.github.siddhantpardeshi.aetherplay.design.Tone
import io.github.siddhantpardeshi.aetherplay.design.auroraBackdrop
import kotlinx.coroutines.android.awaitFrame

class GameActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        GameHost.core?.setActivityActive(true)
    }

    override fun onPause() {
        GameHost.core?.setActivityActive(false)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AetherTheme(identity = AppIdentity.SPACE) {
                val core = GameHost.core
                if (core == null) {
                    Box(Modifier.fillMaxSize().auroraBackdrop(), contentAlignment = Alignment.Center) {
                        Text(
                            "No active session.\nStart a session in AetherPlay Space first.",
                            textAlign = TextAlign.Center,
                            color = LocalPalette.current.textSecondary,
                        )
                    }
                } else {
                    GameCanvas(core)
                }
            }
        }
    }
}

@Composable
private fun GameCanvas(core: GameCore) {
    val palette = LocalPalette.current
    val accent = SpaceAccent
    val state by core.state.collectAsState()

    LaunchedEffect(Unit) {
        var last = awaitFrame()
        while (true) {
            val now = awaitFrame()
            val dt = (now - last) / 1_000_000_000f
            last = now
            // Pause the simulation whenever the game screen is not the active
            // activity — otherwise a backgrounded task silently keeps playing.
            if (core.state.value.activityActive) {
                core.tick(dt)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF07080C)).statusBarsPadding().navigationBarsPadding()) {
        Hud(state)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        core.setPlayerX(change.position.x / size.width)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        core.setPlayerX(offset.x / size.width)
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // subtle grid
                val grid = Color(0xFF141A24)
                var gx = 0f
                while (gx < w) {
                    drawLine(grid, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                    gx += 56.dp.toPx()
                }
                var gy = 0f
                while (gy < h) {
                    drawLine(grid, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                    gy += 56.dp.toPx()
                }

                // falling crystals
                state.falling.forEach { c ->
                    val cx = c.x * w
                    val cy = c.y * h
                    val r = 14.dp.toPx()
                    val path = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.72f, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r * 0.72f, cy)
                        close()
                    }
                    drawPath(path, Brush.verticalGradient(listOf(accent.gradientStart, accent.gradientEnd), startY = cy - r, endY = cy + r))
                    drawPath(path, Color.White.copy(alpha = 0.18f), style = Stroke(width = 1.5f))
                }

                // player orb
                val px = state.playerX * w
                val py = h * 0.88f
                val pr = 20.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.gradientStart, accent.gradientEnd),
                        center = Offset(px, py),
                        radius = pr,
                    ),
                    radius = pr,
                    center = Offset(px, py),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.gradientStart.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(px, py),
                        radius = pr * 2.4f,
                    ),
                    radius = pr * 2.4f,
                    center = Offset(px, py),
                )
                drawCircle(Color.White.copy(alpha = 0.35f), radius = pr * 0.4f, center = Offset(px - pr * 0.25f, py - pr * 0.3f))
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)) {
                Text("drag to move", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary.copy(alpha = 0.6f))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            PrimaryButton("Reset run", onClick = { core.resetRun() })
        }
    }
}

@Composable
private fun Hud(state: GameState) {
    val palette = LocalPalette.current
    val accent = SpaceAccent
    val energyFrac by animateFloatAsState(
        targetValue = (state.energy / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 200f),
        label = "energy",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("CRYSTALS", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Text(state.crystals.toString(), style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = palette.textPrimary)
        }
        Spacer(Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text("ENERGY", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.raised),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(energyFrac)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(accent.gradientStart, accent.gradientEnd))),
                )
            }
        }
        Spacer(Modifier.width(28.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("BEST", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Text(state.best.toString(), style = MonoValueStyle, color = palette.textSecondary)
        }
    }
}
