package io.github.siddhantpardeshi.aetherplay.lab.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.siddhantpardeshi.aetherplay.design.AetherCard
import io.github.siddhantpardeshi.aetherplay.design.EmptyState
import io.github.siddhantpardeshi.aetherplay.design.HexAddress
import io.github.siddhantpardeshi.aetherplay.design.IcLayers
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.MonoValue
import io.github.siddhantpardeshi.aetherplay.design.Pill
import io.github.siddhantpardeshi.aetherplay.design.PrimaryButton
import io.github.siddhantpardeshi.aetherplay.design.SecondaryButton
import io.github.siddhantpardeshi.aetherplay.design.SectionLabel
import io.github.siddhantpardeshi.aetherplay.design.Tone
import io.github.siddhantpardeshi.aetherplay.lab.LabViewModel
import io.github.siddhantpardeshi.aetherplay.protocol.SessionState
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: LabViewModel) {
    val palette = LocalPalette.current
    val session = vm.session.collectAsState().value
    val journal = vm.journal.collectAsState().value
    val freezes = vm.freezes.collectAsState().value

    LaunchedEffect(session?.state, session?.generation) {
        if (session?.state == SessionState.RUNNING) {
            vm.refreshHistory()
            vm.refreshFreezes()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.bg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(6.dp)) }

        if (session?.state != SessionState.RUNNING) {
            item {
                EmptyState(
                    icon = IcLayers,
                    title = "No session",
                    body = "The change journal and freeze list live inside an active session. Start one in AetherPlay Space.",
                )
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Freezes")
                    Spacer(Modifier.width(10.dp))
                    Text("${freezes.size} active", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Spacer(Modifier.weight(1f))
                    if (freezes.isNotEmpty()) {
                        SecondaryButton("Stop all", tone = Tone.ERROR) { vm.stopAllFreezes() }
                    }
                }
            }
            if (freezes.isEmpty()) {
                item {
                    AetherCard {
                        Text(
                            "No values frozen. Freeze keeps re-applying a value while the session runs, and always stops with the session.",
                            style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary,
                        )
                    }
                }
            } else {
                items(freezes, key = { it.address }) { f ->
                    AetherCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HexAddress(f.address)
                            Spacer(Modifier.width(12.dp))
                            MonoValue(text = f.valueText)
                            Spacer(Modifier.weight(1f))
                            Pill("held", Tone.ACCENT)
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Change journal")
                    Spacer(Modifier.weight(1f))
                    SecondaryButton("Undo last write") { vm.undoLast() }
                }
            }
            if (journal.isEmpty()) {
                item {
                    AetherCard {
                        Text(
                            "Every committed write is journaled here with its old and new value. Undo restores the most recent one while the values still match.",
                            style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary,
                        )
                    }
                }
            } else {
                items(journal, key = { it.id }) { entry ->
                    AetherCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.timeMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                            )
                            Spacer(Modifier.width(10.dp))
                            if (entry.undone) Pill("undone", Tone.NEUTRAL)
                            Spacer(Modifier.weight(1f))
                            Pill("${entry.addresses.size} value${if (entry.addresses.size == 1) "" else "s"}", Tone.NEUTRAL)
                        }
                        Spacer(Modifier.height(8.dp))
                        entry.addresses.forEachIndexed { i, addr ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                HexAddress(addr)
                                Spacer(Modifier.width(10.dp))
                                MonoValue(text = entry.oldValues[i], color = palette.textSecondary)
                                Text("  →  ", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                MonoValue(text = entry.newValues[i])
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
