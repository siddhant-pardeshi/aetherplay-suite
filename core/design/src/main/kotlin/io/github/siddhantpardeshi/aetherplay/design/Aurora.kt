package io.github.siddhantpardeshi.aetherplay.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

/**
 * The signature Aurora backdrop: layered radial accents drifting slowly.
 * Static (zero animation cost) when reduced motion is requested.
 */
@Composable
fun Modifier.auroraBackdrop(
    palette: Palette = LocalPalette.current,
    accent: AccentColors = LocalAccent.current,
    reduceMotion: Boolean = LocalReduceMotion.current,
    intensity: Float = 1f,
): Modifier {
    if (reduceMotion) {
        return this.then(
            Modifier.drawBehind { drawAurora(size.width, size.height, palette, accent, 0f, 0.35f * intensity) }
        )
    }
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    return this.then(
        Modifier.drawBehind { drawAurora(size.width, size.height, palette, accent, phase, 0.5f * intensity) }
    )
}

private fun DrawScope.drawAurora(
    w: Float,
    h: Float,
    palette: Palette,
    accent: AccentColors,
    phase: Float,
    alpha: Float,
) {
    drawRect(palette.bg)
    val r = maxOf(w, h)
    val sweep = phase * 2f * Math.PI.toFloat()
    val c1 = Offset(w * (0.15f + 0.08f * sin(sweep)), h * (0.05f + 0.05f * cos(sweep)))
    val c2 = Offset(w * (0.85f + 0.06f * cos(sweep * 0.7f + 1f)), h * (0.25f + 0.07f * sin(sweep * 0.9f)))
    val c3 = Offset(w * (0.5f + 0.1f * sin(sweep * 0.5f + 2f)), h * 0.9f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.gradientStart.copy(alpha = 0.16f * alpha * 2), Color.Transparent),
            center = c1,
            radius = r * 0.55f,
        ),
        radius = r * 0.55f,
        center = c1,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.gradientEnd.copy(alpha = 0.12f * alpha * 2), Color.Transparent),
            center = c2,
            radius = r * 0.6f,
        ),
        radius = r * 0.6f,
        center = c2,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.accentDeep.copy(alpha = 0.08f * alpha * 2), Color.Transparent),
            center = c3,
            radius = r * 0.7f,
        ),
        radius = r * 0.7f,
        center = c3,
    )
}
