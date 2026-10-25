package io.github.siddhantpardeshi.aetherplay.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween

object AetherMotion {
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val ExitEasing: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    const val TAP = 120
    const val FAST = 180
    const val NORMAL = 220
    const val SLOW = 360

    fun <T> enter(duration: Int = NORMAL) = tween<T>(duration, easing = Emphasized)
    fun <T> exit(duration: Int = FAST) = tween<T>(duration, easing = ExitEasing)
    fun <T> drift(duration: Int) = tween<T>(duration, easing = LinearEasing)
}
