package io.github.siddhantpardeshi.aetherplay.lab

import android.content.Context
import io.github.siddhantpardeshi.aetherplay.design.ThemeMode

object Prefs {
    private const val FILE = "aether_prefs"

    private fun read(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun themeMode(context: Context): ThemeMode =
        read(context).getString("theme_mode", null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK

    fun setThemeMode(context: Context, mode: ThemeMode) {
        read(context).edit().putString("theme_mode", mode.name).apply()
    }

    fun reduceMotion(context: Context): Boolean = read(context).getBoolean("reduce_motion", false)

    fun setReduceMotion(context: Context, value: Boolean) {
        read(context).edit().putBoolean("reduce_motion", value).apply()
    }

    fun tutorialDone(context: Context): Boolean = read(context).getBoolean("tutorial_done", false)

    fun setTutorialDone(context: Context) {
        read(context).edit().putBoolean("tutorial_done", true).apply()
    }
}
