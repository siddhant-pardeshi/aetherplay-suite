package io.github.siddhantpardeshi.aetherplay.lab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.siddhantpardeshi.aetherplay.design.AetherCard
import io.github.siddhantpardeshi.aetherplay.design.AnimatedCounter
import io.github.siddhantpardeshi.aetherplay.design.AuroraError
import io.github.siddhantpardeshi.aetherplay.design.EmptyState
import io.github.siddhantpardeshi.aetherplay.design.HexAddress
import io.github.siddhantpardeshi.aetherplay.design.IcScan
import io.github.siddhantpardeshi.aetherplay.design.IcSpark
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.MonoValue
import io.github.siddhantpardeshi.aetherplay.design.Pill
import io.github.siddhantpardeshi.aetherplay.design.PrimaryButton
import io.github.siddhantpardeshi.aetherplay.design.ProgressRing
import io.github.siddhantpardeshi.aetherplay.design.PulsingDot
import io.github.siddhantpardeshi.aetherplay.design.SecondaryButton
import io.github.siddhantpardeshi.aetherplay.design.SectionLabel
import io.github.siddhantpardeshi.aetherplay.design.Tone
import io.github.siddhantpardeshi.aetherplay.lab.LabViewModel
import io.github.siddhantpardeshi.aetherplay.model.ValueType
import io.github.siddhantpardeshi.aetherplay.protocol.ResultItem
import io.github.siddhantpardeshi.aetherplay.protocol.SearchPhase
import io.github.siddhantpardeshi.aetherplay.protocol.SessionState

@Composable
fun SearchScreen(vm: LabViewModel) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val session = vm.session.collectAsState().value
    val search = vm.search.collectAsState().value

    LaunchedEffect(session?.state) {
        if (session?.state == SessionState.RUNNING) vm.loadRegions()
    }

    val running = session?.state == SessionState.RUNNING
    val awaitingStatus = session == null

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.bg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(6.dp)) }

        if (!running && !awaitingStatus) {
            item {
                EmptyState(
                    icon = IcScan,
                    title = "No active session",
                    body = "AetherPlay Space needs to host a session first. Open Space, choose a space, and start the session — then searching becomes available here.",
                )
            }
        } else if (awaitingStatus) {
            item {
                EmptyState(
                    icon = IcScan,
                    title = "Reading session status…",
                    body = "One moment — Lab is checking the session state with AetherPlay Space.",
                )
            }
        } else {
            item { Composer(vm) }
            if (search.status != null) item { ProgressCard(search) }
            if (search.activeTicket != null && !search.running && search.items.isNotEmpty()) item { RefineBar(vm) }

            if (search.items.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Results")
                        Spacer(Modifier.width(10.dp))
                        AnimatedCounter(search.totalCount, color = accent.accent)
                        if (search.nextOffset >= 0) {
                            Spacer(Modifier.weight(1f))
                            SecondaryButton("Load more") { vm.loadMore() }
                        }
                    }
                }
                items(search.items, key = { it.address }) { item ->
                    ResultRow(item) { vm.openDetail(item) }
                }
            } else if (search.status?.phase == SearchPhase.DONE) {
                item {
                    EmptyState(
                        icon = IcSpark,
                        title = "No results",
                        body = "Try another value, widen the range, or switch to an unknown-value search and refine instead.",
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (vm.detail.value != null) DetailSheet(vm)
}

@Composable
private fun Composer(vm: LabViewModel) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val search = vm.search.collectAsState().value

    AetherCard(raised = true) {
        SectionLabel("Value type")
        Spacer(Modifier.height(8.dp))
        listOf(
            listOf(ValueType.I32, ValueType.I64, ValueType.F32, ValueType.F64),
            listOf(ValueType.I8, ValueType.I16, ValueType.U8, ValueType.U16, ValueType.U32, ValueType.U64),
        ).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowTypes.forEach { type ->
                    val selected = type.wire == search.typeWire
                    Chip(
                        text = type.name,
                        selected = selected,
                    ) { vm.updateSearch { it.copy(typeWire = type.wire) } }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        SectionLabel("Search mode")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to "Exact", 1 to "Range", 2 to "Unknown").forEach { (kind, label) ->
                Chip(text = label, selected = search.kind == kind) { vm.updateSearch { it.copy(kind = kind) } }
            }
        }
        Spacer(Modifier.height(14.dp))
        when (search.kind) {
            0 -> ValueField("Current value", search.valueText) { v -> vm.updateSearch { it.copy(valueText = v) } }
            1 -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { ValueField("Minimum", search.minText) { v -> vm.updateSearch { it.copy(minText = v) } } }
                Box(Modifier.weight(1f)) { ValueField("Maximum", search.maxText) { v -> vm.updateSearch { it.copy(maxText = v) } } }
            }
            else -> Text(
                "Unknown-value search lists every location of this type. Play the game, then refine.",
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
            )
        }
        Spacer(Modifier.height(14.dp))
        AnimatedVisibility(visible = search.advancedOpen, enter = fadeIn(), exit = fadeOut()) {
            Column {
                SectionLabel("Regions")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    search.regions.forEach { region ->
                        val selected = region.id in search.selectedRegions
                        Chip(text = region.label, selected = selected) {
                            vm.updateSearch {
                                val next = if (selected) it.selectedRegions - region.id else it.selectedRegions + region.id
                                it.copy(selectedRegions = next)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Alignment")
                    Spacer(Modifier.width(6.dp))
                    listOf(1, 2, 4).forEach { a ->
                        Chip(text = "$a", selected = search.alignment == a, small = true) {
                            vm.updateSearch { it.copy(alignment = a) }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Advanced", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
            Spacer(Modifier.width(10.dp))
            Chip(
                text = if (search.advancedOpen) "Hide" else "Show",
                selected = search.advancedOpen,
            ) { vm.updateSearch { it.copy(advancedOpen = !it.advancedOpen) } }
        }
        if (search.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(search.error!!, style = MaterialTheme.typography.bodySmall, color = AuroraError)
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            if (search.activeTicket == null) "Search" else "New search",
            onClick = { vm.startSearch() },
            modifier = Modifier.fillMaxWidth(),
            icon = IcScan,
        )
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) accent.accent.copy(alpha = 0.16f) else palette.raised)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = if (small) 8.dp else 12.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent.accent else palette.textSecondary,
        )
    }
}

@Composable
private fun ValueField(label: String, value: String, onValue: (String) -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.accent,
            focusedLabelColor = accent.accent,
            unfocusedBorderColor = palette.outline,
            unfocusedLabelColor = palette.textSecondary,
            cursorColor = accent.accent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProgressCard(search: io.github.siddhantpardeshi.aetherplay.lab.SearchUiState) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val status = search.status ?: return
    val active = status.phase == SearchPhase.QUEUED || status.phase == SearchPhase.SCANNING
    val progress = if (status.totalBytes > 0) status.scannedBytes.toFloat() / status.totalBytes else null

    AetherCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                if (active) ProgressRing(progress = progress) else PulsingDot(accent.accent, size = 10.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (status.phase) {
                        SearchPhase.QUEUED -> "Queued…"
                        SearchPhase.SCANNING -> "Scanning ${"%,d".format(status.scannedBytes)} / ${"%,d".format(status.totalBytes)} bytes"
                        SearchPhase.DONE -> "Done in ${status.elapsedMs} ms"
                        SearchPhase.CANCELLED -> "Cancelled"
                        SearchPhase.FAILED -> status.message.ifEmpty { "Failed" }
                        else -> "Idle"
                    },
                    style = MaterialTheme.typography.titleMedium, color = palette.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedCounter(status.resultCount, color = accent.accent)
                    Text(" matches", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun RefineBar(vm: LabViewModel) {
    val palette = LocalPalette.current
    var exactValue by remember { mutableStateOf("") }

    AetherCard {
        SectionLabel("Refine")
        Spacer(Modifier.height(8.dp))
        Text(
            "Play the game between steps so values move — that's how refine narrows results.",
            style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("Changed", selected = false) { vm.refine(1, "") }
            Chip("Unchanged", selected = false) { vm.refine(2, "") }
            Chip("Increased", selected = false) { vm.refine(3, "") }
            Chip("Decreased", selected = false) { vm.refine(4, "") }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                ValueField("Exact value", exactValue) { exactValue = it }
            }
            SecondaryButton("Refine exact") { vm.refine(0, exactValue) }
        }
    }
}

@Composable
private fun ResultRow(item: ResultItem, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    AetherCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HexAddress(item.address, color = palette.textSecondary)
            Spacer(Modifier.width(12.dp))
            MonoValue(text = item.valueText, color = palette.textPrimary)
            Spacer(Modifier.weight(1f))
            if (item.stale) Pill("changed", Tone.WARN) else Pill(item.regionId, Tone.NEUTRAL)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailSheet(vm: LabViewModel) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val detail = vm.detail.collectAsState().value
    val d = detail ?: return
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { vm.closeDetail() },
        sheetState = sheet,
        containerColor = palette.base,
        contentColor = palette.textPrimary,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HexAddress(d.item.address)
                Spacer(Modifier.weight(1f))
                Pill(ValueType.fromWire(d.typeWire)?.name ?: "?", Tone.ACCENT)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Live value", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                Spacer(Modifier.width(10.dp))
                PulsingDot(accent.accent, size = 6.dp)
            }
            Spacer(Modifier.height(4.dp))
            MonoValue(text = d.liveValue ?: "…", style = io.github.siddhantpardeshi.aetherplay.design.MonoValueStyle.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize))
            if (d.frozen) {
                Spacer(Modifier.height(8.dp))
                Pill("Frozen", Tone.ACCENT, icon = IcSpark)
            }
            Spacer(Modifier.height(18.dp))
            ValueField("New value", d.writeText) { vm.setWriteText(it) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Preview write") { vm.previewWrite() }
                SecondaryButton(if (d.frozen) "Unfreeze" else "Freeze", tone = if (d.frozen) Tone.ERROR else Tone.ACCENT) { vm.toggleFreeze() }
            }
            val preview = d.preview
            if (preview != null && preview.items.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                AetherCard(raised = true) {
                    SectionLabel("Preview")
                    Spacer(Modifier.height(8.dp))
                    val p = preview.items.first()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoValue(text = p.oldValue, color = palette.textSecondary)
                        Text("  →  ", color = accent.accent, style = MaterialTheme.typography.titleMedium)
                        MonoValue(text = p.newValue, color = if (p.writable) palette.textPrimary else AuroraError)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (p.writable) "Nothing changes until you commit." else "This address is read-only.",
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("Commit write", onClick = { vm.commitWrite() }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (d.message != null) {
                Spacer(Modifier.height(12.dp))
                Text(d.message!!, style = MaterialTheme.typography.bodyMedium, color = accent.accent)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Writes are previewed first, journaled, and reversible with undo while the session lives.",
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
            )
        }
    }
}
