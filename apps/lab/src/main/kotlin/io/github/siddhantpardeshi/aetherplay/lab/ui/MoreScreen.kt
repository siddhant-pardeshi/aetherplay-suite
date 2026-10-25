package io.github.siddhantpardeshi.aetherplay.lab.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.siddhantpardeshi.aetherplay.design.IcSpark
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.Pill
import io.github.siddhantpardeshi.aetherplay.design.PrimaryButton
import io.github.siddhantpardeshi.aetherplay.design.SecondaryButton
import io.github.siddhantpardeshi.aetherplay.design.SectionLabel
import io.github.siddhantpardeshi.aetherplay.design.ThemeMode
import io.github.siddhantpardeshi.aetherplay.design.Tone
import io.github.siddhantpardeshi.aetherplay.lab.LabViewModel
import io.github.siddhantpardeshi.aetherplay.lab.Prefs
import io.github.siddhantpardeshi.aetherplay.lab.bridge.ConnState
import java.text.DateFormat
import java.util.Date

@Composable
fun MoreScreen(vm: LabViewModel, onThemeChange: (ThemeMode) -> Unit, onMotionChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val context = LocalContext.current
    val conn = vm.conn.collectAsState().value
    val experiments = vm.experiments.collectAsState().value
    var showSave by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(Prefs.themeMode(context)) }
    var reduceMotion by remember { mutableStateOf(Prefs.reduceMotion(context)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.bg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(6.dp)) }

        // Tutorial
        item {
            AetherCard(raised = true) {
                SectionLabel("First session tutorial")
                Spacer(Modifier.height(10.dp))
                listOf(
                    "Install both apps as a pair (same signer).",
                    "Open AetherPlay Space → Session → Start session.",
                    "Tap Open game and catch a few crystals.",
                    "Back in Lab: Search → Int32, exact, enter the crystal count.",
                    "Catch one more crystal, then Refine → Increased.",
                    "Refine → Exact with the new count until one result remains.",
                    "Tap the result, type a new value, Preview, Commit.",
                    "Watch the HUD change. Undo it. Freeze it. Stop the session.",
                ).forEachIndexed { i, step ->
                    Text(
                        "${i + 1}.  $step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "The full guide with screenshots and recovery steps is docs/TUTORIAL_01_FIRST_SESSION.md in the repository.",
                    style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                )
            }
        }

        // Experiments
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Saved experiments")
                Spacer(Modifier.weight(1f))
                SecondaryButton("Save current") { showSave = true }
            }
        }
        if (experiments.isEmpty()) {
            item {
                AetherCard {
                    Text(
                        "Save a search recipe — type, mode, value, regions — and run it again in one tap next session.",
                        style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary,
                    )
                }
            }
        } else {
            items(experiments, key = { it.name }) { exp ->
                AetherCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exp.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                describeExperiment(exp.kind, exp.valueText, exp.minText, exp.maxText) +
                                    " · " + DateFormat.getDateInstance(DateFormat.SHORT).format(Date(exp.createdAt)),
                                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                            )
                        }
                        PrimaryButton("Run", onClick = { vm.runExperiment(exp) })
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { vm.deleteExperiment(exp.name) }) {
                            Text("Delete", color = io.github.siddhantpardeshi.aetherplay.design.AuroraError)
                        }
                    }
                }
            }
            item {
                SecondaryButton("Share as JSON") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, vm.exportExperiments())
                    }
                    context.startActivity(Intent.createChooser(send, "Share experiments"))
                }
            }
        }

        // Settings
        item {
            AetherCard {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Spacer(Modifier.height(12.dp))
                ThemeMode.entries.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        RadioButton(selected = mode == m, onClick = {
                            mode = m
                            Prefs.setThemeMode(context, m)
                            onThemeChange(m)
                        })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reduce motion", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                        Text("Static aurora, no ambient animation", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
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
        }

        // About
        item {
            AetherCard {
                Text("About", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Spacer(Modifier.height(10.dp))
                AboutRow("App", "AetherPlay Lab 0.1.0")
                AboutRow("Bridge protocol", "1.0 · signature-guarded")
                val info = (conn as? ConnState.Connected)?.info
                AboutRow("Space build", info?.spaceBuild ?: "not connected")
                AboutRow("Supported types", if (info != null) countMask(info.valueTypeMask) else "10 (local)")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("local-only", Tone.NEUTRAL, icon = IcSpark)
                    Spacer(Modifier.width(8.dp))
                    Pill("no account", Tone.NEUTRAL)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "AetherPlay is a local experimentation and education tool for games you own or have permission to test. It does not target online, competitive, or server-authoritative games.",
                    style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showSave) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSave = false },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.saveExperiment(name)
                    showSave = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } },
            title = { Text("Save current search") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true) },
        )
    }
}

private fun describeExperiment(kind: Int, value: String, min: String, max: String): String = when (kind) {
    0 -> "exact $value"
    1 -> "range $min..$max"
    else -> "unknown + refine"
}

private fun countMask(mask: Int): String {
    var count = 0
    for (i in 1..10) if (mask and (1 shl (i - 1)) != 0) count++
    return "$count of 10"
}

@Composable
private fun AboutRow(label: String, value: String) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
    }
}
