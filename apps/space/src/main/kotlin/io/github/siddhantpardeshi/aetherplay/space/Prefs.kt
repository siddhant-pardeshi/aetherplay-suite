package io.github.siddhantpardeshi.aetherplay.space

import android.content.Context
import io.github.siddhantpardeshi.aetherplay.design.ThemeMode

/** Small shared-preferences store (theme + motion + last space). */
object Prefs {
    private const val FILE = "aether_prefs"

    private fun read(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun themeMode(context: Context): ThemeMode {
        val name = read(context).getString("theme_mode", null) ?: return ThemeMode.DARK
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.DARK)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        read(context).edit().putString("theme_mode", mode.name).apply()
    }

    fun reduceMotion(context: Context): Boolean = read(context).getBoolean("reduce_motion", false)

    fun setReduceMotion(context: Context, value: Boolean) {
        read(context).edit().putBoolean("reduce_motion", value).apply()
    }

    fun lastSpace(context: Context): String? = read(context).getString("last_space", null)

    fun setLastSpace(context: Context, key: String) {
        read(context).edit().putString("last_space", key).apply()
    }
}
