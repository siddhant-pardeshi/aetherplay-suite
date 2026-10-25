package io.github.siddhantpardeshi.aetherplay.design

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MonoValue(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MonoValueStyle,
    color: Color = LocalPalette.current.textPrimary,
) {
    Text(text = text, style = style, color = color, modifier = modifier)
}

@Composable
fun HexAddress(address: Long, modifier: Modifier = Modifier, color: Color = LocalPalette.current.textSecondary) {
    MonoValue(
        text = "0x" + java.lang.Long.toHexString(address).uppercase().padStart(8, '0'),
        modifier = modifier,
        style = MonoValueStyle.copy(fontSize = MonoValueStyle.fontSize),
        color = color,
    )
}

/** Animated integer counter for result counts and live values. */
@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.textPrimary,
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "count",
    )
    MonoValue(text = animated.toString(), modifier = modifier, color = color)
}

/** Determinate ring when [progress] is set; slow indeterminate sweep when null. */
@Composable
fun ProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    stroke: Dp = 4.dp,
    color: Color = LocalAccent.current.accent,
    track: Color = LocalPalette.current.outline,
) {
    val sweep by animateFloatAsState(targetValue = progress ?: 0f, label = "ring")
    Canvas(modifier = modifier.size(size)) {
        val strokePx = stroke.toPx()
        val inset = strokePx / 2
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - strokePx, this.size.height - strokePx),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = (if (progress != null) sweep else (System.nanoTime() / 30_000_000f % 360f)) *
                (if (progress != null) 3.6f else 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - strokePx, this.size.height - strokePx),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val reduce = LocalReduceMotion.current
    val alpha: Float
    if (reduce) {
        alpha = 0.9f
    } else {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "dot")
        val a by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween<Float>(900),
                androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "alpha",
        )
        alpha = a
    }
    Canvas(modifier = modifier.size(size)) {
        drawCircle(color = color.copy(alpha = alpha), radius = this.size.minDimension / 2)
    }
}
