package io.github.siddhantpardeshi.aetherplay.space.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import io.github.siddhantpardeshi.aetherplay.design.AetherCard
import io.github.siddhantpardeshi.aetherplay.design.AuroraError
import io.github.siddhantpardeshi.aetherplay.design.FadeReveal
import io.github.siddhantpardeshi.aetherplay.design.IcCrystal
import io.github.siddhantpardeshi.aetherplay.design.IcLayers
import io.github.siddhantpardeshi.aetherplay.design.IcSpark
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.Pill
import io.github.siddhantpardeshi.aetherplay.design.PrimaryButton
import io.github.siddhantpardeshi.aetherplay.design.PulsingDot
import io.github.siddhantpardeshi.aetherplay.design.SecondaryButton
import io.github.siddhantpardeshi.aetherplay.design.SectionLabel
import io.github.siddhantpardeshi.aetherplay.design.Tone
import io.github.siddhantpardeshi.aetherplay.space.SpaceViewModel
import io.github.siddhantpardeshi.aetherplay.space.data.SpaceInfo
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(vm: SpaceViewModel, onOpenSession: () -> Unit, onOpenSpaces: () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val session = vm.session.collectAsState().value
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.bg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Game library")
        }
        item {
            AetherCard(raised = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(accent.gradientStart, accent.gradientEnd))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IcCrystal, contentDescription = null, tint = accent.onAccent, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Crystal Sprint", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                        Text(
                            "Training game · cooperative memory · offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "A tiny original runner built for learning: catch crystals, watch your energy, and let AetherPlay Lab peek at the numbers behind the game — with your permission, inside this app only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton("Start session", onClick = onOpenSession)
                    SecondaryButton("Spaces", onClick = onOpenSpaces)
                }
            }
        }
        item {
            AetherCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IcLayers, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Import a game", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Spacer(Modifier.weight(1f))
                    Pill("Experimental", Tone.WARN)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Running third-party games inside a separate container is the experimental milestone this suite is working toward. It is not part of this build, and no game data is touched without it. The honest status and roadmap live in the project documentation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }
        }
        item {
            AetherCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(if (session.state == 2) palette.textPrimary else AuroraError)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (session.state == 2) "Session running — Lab can connect"
                        else "No session running",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (session.state == 2)
                        "Crystal Sprint is live and AetherPlay Lab can search, edit, undo and freeze values."
                    else
                        "Start a session to make Crystal Sprint available to AetherPlay Lab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun SpacesScreen(vm: SpaceViewModel, activeSpaceKey: String?) {
    val palette = LocalPalette.current
    val spaces = vm.spaces.collectAsState().value
    val storage = vm.storageBytes.collectAsState().value
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SpaceInfo?>(null) }
    var renameTarget by remember { mutableStateOf<SpaceInfo?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.bg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Named spaces")
                    Text("Separate save data per space", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
                SecondaryButton("New space") { showCreate = true }
            }
        }
        items(spaces, key = { it.key }) { space ->
            AetherCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(space.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(
                            "Last used " + DateFormat.getDateInstance(DateFormat.SHORT).format(Date(space.lastUsedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                    if (space.key == activeSpaceKey) Pill("Active", Tone.ACCENT, icon = IcSpark)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { renameTarget = space }) { Text("Rename") }
                    TextButton(onClick = { deleteTarget = space }) { Text("Delete", color = AuroraError) }
                }
            }
        }
        item {
            Text(
                "Storage used by spaces: ${storage / 1024} KB",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.createSpace(name)
                    showCreate = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
            title = { Text("New space") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Space name") }, singleLine = true)
            },
        )
    }

    renameTarget?.let { target ->
        var name by remember(target) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameSpace(target.key, name)
                    renameTarget = null
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
            title = { Text("Rename space") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSpace(target.key)
                    deleteTarget = null
                }) { Text("Delete", color = AuroraError) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            title = { Text("Delete “${target.name}”?") },
            text = {
                Text("This permanently removes this space's Crystal Sprint save data. This cannot be undone.")
            },
        )
    }

    FadeReveal(visible = spaces.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No spaces yet — create one to keep separate save data.", color = palette.textSecondary)
        }
    }
}
