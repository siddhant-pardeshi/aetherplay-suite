package io.github.siddhantpardeshi.aetherplay.space.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.siddhantpardeshi.aetherplay.design.AetherCard
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.SectionLabel
import io.github.siddhantpardeshi.aetherplay.design.ThemeMode
import io.github.siddhantpardeshi.aetherplay.space.Prefs
import io.github.siddhantpardeshi.aetherplay.space.SpaceViewModel

@Composable
fun SettingsScreen(vm: SpaceViewModel, onThemeChange: (ThemeMode) -> Unit, onMotionChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val context = LocalContext.current
    var mode by remember { mutableStateOf(Prefs.themeMode(context)) }
    var reduceMotion by remember { mutableStateOf(Prefs.reduceMotion(context)) }
    val storage = vm.storageBytes.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        SectionLabel("Settings")
        Spacer(Modifier.height(12.dp))

        AetherCard {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(12.dp))
            ThemeMode.entries.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (m) {
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.OLED -> "OLED black"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mode == m) accent.accent else palette.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.RadioButton(
                        selected = mode == m,
                        onClick = {
                            mode = m
                            Prefs.setThemeMode(context, m)
                            onThemeChange(m)
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reduce motion", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                    Text("Replace ambient animation with static surfaces", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
                Switch(
                    checked = reduceMotion,
                    onCheckedChange = { v ->
                        reduceMotion = v
                        Prefs.setReduceMotion(context, v)
                        onMotionChange(v)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent.accent, checkedThumbColor = accent.onAccent),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        AetherCard {
            Text("About", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(10.dp))
            AboutRow("App", "AetherPlay Space 0.1.0")
            AboutRow("Role", "Guest stage and session host")
            AboutRow("Bridge", "Signature-guarded, protocol 1.0")
            AboutRow("Spaces storage", "${storage / 1024} KB")
            Spacer(Modifier.height(10.dp))
            Text(
                "AetherPlay is a local experimentation and education tool for games you own or have permission to test. It does not target online, competitive, or server-authoritative games.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
    }
}
