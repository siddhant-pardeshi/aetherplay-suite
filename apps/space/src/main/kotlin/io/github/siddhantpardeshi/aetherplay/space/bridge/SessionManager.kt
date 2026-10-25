package io.github.siddhantpardeshi.aetherplay.space.bridge

import android.content.Context
import android.content.pm.PackageManager
import io.github.siddhantpardeshi.aetherplay.model.Capability
import io.github.siddhantpardeshi.aetherplay.model.RefineOp
import io.github.siddhantpardeshi.aetherplay.model.SearchKind
import io.github.siddhantpardeshi.aetherplay.model.ValueCodec
import io.github.siddhantpardeshi.aetherplay.model.ValueType
import io.github.siddhantpardeshi.aetherplay.protocol.BridgeContract
import io.github.siddhantpardeshi.aetherplay.protocol.BridgeContract as Contract
import io.github.siddhantpardeshi.aetherplay.protocol.FreezeEntry
import io.github.siddhantpardeshi.aetherplay.protocol.GuestInfo
import io.github.siddhantpardeshi.aetherplay.protocol.GuestKind
import io.github.siddhantpardeshi.aetherplay.protocol.HandshakeInfo
import io.github.siddhantpardeshi.aetherplay.protocol.IBridgeBroker
import io.github.siddhantpardeshi.aetherplay.protocol.JournalEntry
import io.github.siddhantpardeshi.aetherplay.protocol.OpResult
import io.github.siddhantpardeshi.aetherplay.protocol.RegionInfo
import io.github.siddhantpardeshi.aetherplay.protocol.ResultItem
import io.github.siddhantpardeshi.aetherplay.protocol.ResultPage
import io.github.siddhantpardeshi.aetherplay.protocol.SearchHandle
import io.github.siddhantpardeshi.aetherplay.protocol.SearchPhase
import io.github.siddhantpardeshi.aetherplay.protocol.SearchQuery
import io.github.siddhantpardeshi.aetherplay.protocol.SearchStatus
import io.github.siddhantpardeshi.aetherplay.protocol.SessionState
import io.github.siddhantpardeshi.aetherplay.protocol.SessionStatus
import io.github.siddhantpardeshi.aetherplay.protocol.ValueSnapshot
import io.github.siddhantpardeshi.aetherplay.protocol.WritePreview
import io.github.siddhantpardeshi.aetherplay.protocol.RefineQuery
import io.github.siddhantpardeshi.aetherplay.protocol.PreviewItem
import io.github.siddhantpardeshi.aetherplay.search.CandidateSet
import io.github.siddhantpardeshi.aetherplay.search.SearchCancelledException
import io.github.siddhantpardeshi.aetherplay.search.SearchEngine
import io.github.siddhantpardeshi.aetherplay.search.SearchSpec
import io.github.siddhantpardeshi.aetherplay.space.data.SpaceStore
import io.github.siddhantpardeshi.aetherplay.space.game.GameCore
import io.github.siddhantpardeshi.aetherplay.space.game.GameHost
import io.github.siddhantpardeshi.aetherplay.space.game.GameMemory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** UI-observable session snapshot for the Space app (same process). */
object SessionBus {
    val state = kotlinx.coroutines.flow.MutableStateFlow(statusNone())
    var onUiStop: (() -> Unit)? = null

    fun statusNone() = SessionStatus(SessionState.NONE, null, null, 0, 0, 0L, false)
}

class SessionManager(private val context: Context) {

    companion object {
        const val GUEST_CRYSTAL_SPRINT = "crystal-sprint"
        const val MAX_RESULTS = 100_000
        private const val FREEZE_INTERVAL_MS = 300L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine = SearchEngine(maxResults = MAX_RESULTS)
    private val adapter = GameMemoryAdapter()
    private val store = SpaceStore(context)

    @Volatile
    private var generation = 0

    @Volatile
    private var sessionState = SessionState.NONE

    @Volatile
    private var currentGameKey: String? = null

    @Volatile
    private var currentSpaceKey: String? = null

    @Volatile
    private var sessionStartMs = 0L

    private val ticketIds = AtomicLong(1)
    private val previewIds = AtomicLong(1)
    private val journalIds = AtomicLong(1)

    private inner class Ticket(val id: Long) {
        @Volatile var phase = SearchPhase.IDLE
        @Volatile var scannedBytes = 0L
        @Volatile var totalBytes = 0L
        @Volatile var resultCount = 0
        @Volatile var elapsedMs = 0L
        @Volatile var message = ""
        @Volatile var startedAt = 0L
        @Volatile var cancelled = false
        @Volatile var job: Job? = null
        @Volatile var candidateSet: CandidateSet? = null
    }

    private data class PendingWrite(
        val addresses: List<Long>,
        val type: ValueType,
        val newBits: ULong,
        val oldBits: List<ULong>,
        val generation: Int,
    )

    private data class FreezeData(val address: Long, val type: ValueType, val bits: ULong)

    private val tickets = ConcurrentHashMap<Long, Ticket>()
    private val previews = ConcurrentHashMap<Long, PendingWrite>()
    private val journal = ArrayList<JournalEntry>()
    private val freezes = ArrayList<FreezeData>()
    private var freezeJob: Job? = null

    // ---- security -------------------------------------------------------

    private fun enforceCaller() {
        if (context.checkCallingOrSelfPermission(Contract.PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("AetherPlay bridge access requires the paired-app signature permission.")
        }
    }

    private fun requireSession(g: Int): OpResult? {
        if (sessionState != SessionState.RUNNING) {
            return OpResult.err(2, "No active session. Start one from AetherPlay Space.", generation)
        }
        if (g != generation) {
            return OpResult.err(1, "The session changed. Refresh and try again.", generation)
        }
        return null
    }

    // ---- lifecycle ------------------------------------------------------

    fun startSessionFromUi(gameKey: String, spaceKey: String): OpResult {
        synchronized(this) {
            if (sessionState == SessionState.RUNNING) {
                return OpResult.ok(generation)
            }
            if (gameKey != GUEST_CRYSTAL_SPRINT) {
                return OpResult.err(4, "Unknown game. Only the training game is available in this build.", generation)
            }
            val space = store.list().firstOrNull { it.key == spaceKey }
                ?: return OpResult.err(5, "That space no longer exists.", generation)
            store.touch(space.key)
            val save = store.saveFileFor(space.key)
            val core = GameCore(save)
            if (save.exists()) {
                try {
                    core.restore(save.readText())
                } catch (_: Exception) {
                }
            }
            GameHost.attach(core)
            currentGameKey = gameKey
            currentSpaceKey = space.key
            generation += 1
            sessionState = SessionState.RUNNING
            sessionStartMs = System.currentTimeMillis()
            clearSessionData()
            startFreezeLoop()
            publish()
            return OpResult.ok(generation)
        }
    }

    fun stopSession(save: Boolean) {
        synchronized(this) {
            if (sessionState == SessionState.NONE) return
            sessionState = SessionState.STOPPING
            publish()
            freezeJob?.cancel()
            freezeJob = null
            scope.launch {
                if (save) GameHost.core?.save()
                GameHost.detach(save)
                currentGameKey = null
                currentSpaceKey = null
                clearSessionData()
                generation += 1
                sessionState = SessionState.NONE
                publish()
            }
        }
    }

    fun shutdown() {
        stopSession(save = true)
    }

    private fun clearSessionData() {
        tickets.values.forEach { it.job?.cancel() }
        tickets.clear()
        previews.clear()
        synchronized(journal) { journal.clear() }
        synchronized(freezes) { freezes.clear() }
    }

    private fun publish() {
        SessionBus.state.value = buildStatus()
    }

    private fun buildStatus() = SessionStatus(
        state = sessionState,
        gameKey = currentGameKey,
        spaceKey = currentSpaceKey,
        generation = generation,
        freezeCount = synchronized(freezes) { freezes.size },
        elapsedMs = if (sessionState == SessionState.RUNNING) System.currentTimeMillis() - sessionStartMs else 0L,
        gameRunning = GameHost.state.value.activityActive,
    )

    private fun startFreezeLoop() {
        freezeJob?.cancel()
        freezeJob = scope.launch {
            while (sessionState == SessionState.RUNNING) {
                delay(FREEZE_INTERVAL_MS)
                val snapshot = synchronized(freezes) { freezes.toList() }
                val core = GameHost.core ?: break
                for (f in snapshot) core.writeAt(f.address, f.bits, f.type.sizeBytes)
            }
        }
    }

    // ---- query mapping --------------------------------------------------

    private fun bitsFor(type: ValueType, longBits: Long, doubleValue: Double, hasValue: Boolean): ULong? {
        if (!hasValue) return null
        return if (type.isFloat) {
            when (type) {
                ValueType.F32 -> java.lang.Float.floatToIntBits(doubleValue.toFloat()).toULong()
                else -> java.lang.Double.doubleToRawLongBits(doubleValue).toULong()
            }
        } else longBits.toULong()
    }

    private fun specFromQuery(q: SearchQuery): SearchSpec? {
        val type = ValueType.fromWire(q.valueType) ?: return null
        val kind = when (q.kind) {
            0 -> SearchKind.EXACT
            1 -> SearchKind.RANGE
            2 -> SearchKind.UNKNOWN
            else -> return null
        }
        val exact = bitsFor(type, q.longValue, q.doubleExact, q.hasValue)
        val min = bitsFor(type, q.longValue, q.doubleMin, q.hasValue)
        val max = bitsFor(type, q.longValue2, q.doubleMax, q.hasValue2)
        if (kind == SearchKind.EXACT && exact == null) return null
        if (kind == SearchKind.RANGE && (min == null || max == null)) return null
        if (q.regionIds.isEmpty()) return null
        return SearchSpec(
            type = type,
            kind = kind,
            regionIds = q.regionIds.toSet(),
            alignment = q.alignment.coerceIn(1, 8),
            exactBits = exact,
            minBits = min,
            maxBits = max,
        )
    }

    private fun refineOpFrom(wire: Int): RefineOp? = when (wire) {
        0 -> RefineOp.EXACT
        1 -> RefineOp.CHANGED
        2 -> RefineOp.UNCHANGED
        3 -> RefineOp.INCREASED
        4 -> RefineOp.DECREASED
        else -> null
    }

    // ---- broker ---------------------------------------------------------

    val broker: IBridgeBroker = object : IBridgeBroker.Stub() {

        override fun handshake(labBuild: String, labProtocolMajor: Int, labProtocolMinor: Int): HandshakeInfo {
            enforceCaller()
            return HandshakeInfo(
                protocolMajor = Contract.PROTOCOL_MAJOR,
                protocolMinor = Contract.PROTOCOL_MINOR,
                spaceBuild = "0.1.0",
                runtimeBuild = "training-adapter-1",
                capabilities = Capability.mask(Capability.SEARCH, Capability.REFINE, Capability.READ, Capability.WRITE, Capability.FREEZE, Capability.UNDO),
                valueTypeMask = ValueType.supportedMask(),
                maxResults = MAX_RESULTS,
                sessionActive = sessionState == SessionState.RUNNING,
                gameKey = currentGameKey,
                spaceKey = currentSpaceKey,
                generation = generation,
            )
        }

        override fun listGuests(): List<GuestInfo> {
            enforceCaller()
            return listOf(
                GuestInfo(GUEST_CRYSTAL_SPRINT, "Crystal Sprint", GuestKind.TRAINING_COOPERATIVE, "1.0"),
            )
        }

        override fun startSession(gameKey: String, spaceKey: String): OpResult {
            enforceCaller()
            return if (sessionState == SessionState.RUNNING) {
                OpResult.ok(generation)
            } else {
                OpResult.err(2, "Sessions are started from AetherPlay Space. Open Space, then start a session there.", generation)
            }
        }

        override fun stopSession(): OpResult {
            enforceCaller()
            if (sessionState == SessionState.NONE) {
                return OpResult.err(2, "No active session.", generation)
            }
            this@SessionManager.stopSession(save = true)
            return OpResult.ok(generation)
        }

        override fun getSessionStatus(): SessionStatus {
            enforceCaller()
            val s = buildStatus()
            publish()
            return s
        }

        override fun listRegions(g: Int): List<RegionInfo> {
            enforceCaller()
            requireSession(g)?.let { return emptyList() }
            return adapter.regions().map {
                RegionInfo(it.id, it.label, it.startAddress, it.sizeBytes, it.writable)
            }
        }

        override fun startSearch(query: SearchQuery, g: Int): SearchHandle {
            enforceCaller()
            requireSession(g)?.let { return SearchHandle(0, generation, it.code, it.message) }
            val spec = specFromQuery(query)
                ?: return SearchHandle(0, generation, 3, "The search query could not be understood.")
            val ticket = Ticket(ticketIds.getAndIncrement())
            ticket.phase = SearchPhase.QUEUED
            ticket.startedAt = System.currentTimeMillis()
            tickets[ticket.id] = ticket
            ticket.job = scope.launch {
                runSearch(ticket, spec)
            }
            return SearchHandle(ticket.id, generation, 0, "OK")
        }

        override fun refineSearch(ticketId: Long, query: RefineQuery, g: Int): SearchHandle {
            enforceCaller()
            requireSession(g)?.let { return SearchHandle(0, generation, it.code, it.message) }
            val ticket = tickets[ticketId]
                ?: return SearchHandle(0, generation, 5, "That search is no longer available.")
            val set = ticket.candidateSet
                ?: return SearchHandle(0, generation, 6, "Wait for the current search to finish first.")
            if (ticket.phase == SearchPhase.SCANNING) {
                return SearchHandle(0, generation, 6, "Wait for the current search to finish first.")
            }
            val op = refineOpFrom(query.op)
                ?: return SearchHandle(0, generation, 3, "Unknown refine operation.")
            val type = set.type
            val exact = if (query.hasValue) {
                if (type.isFloat) {
                    when (type) {
                        ValueType.F32 -> java.lang.Float.floatToIntBits(query.doubleExact.toFloat()).toULong()
                        else -> java.lang.Double.doubleToRawLongBits(query.doubleExact).toULong()
                    }
                } else query.longValue.toULong()
            } else null
            if (op == RefineOp.EXACT && exact == null) {
                return SearchHandle(0, generation, 3, "Enter a value for an exact refine.")
            }
            ticket.phase = SearchPhase.SCANNING
            ticket.startedAt = System.currentTimeMillis()
            ticket.cancelled = false
            ticket.job = scope.launch {
                try {
                    val newSet = engine.refine(
                        source = adapter,
                        current = set,
                        op = op,
                        exactBits = exact,
                        floatTolerance = query.floatTolerance,
                        isCancelled = { ticket.cancelled },
                    )
                    ticket.candidateSet = newSet
                    ticket.resultCount = newSet.candidates.size
                    ticket.phase = if (ticket.cancelled) SearchPhase.CANCELLED else SearchPhase.DONE
                } catch (e: SearchCancelledException) {
                    ticket.phase = SearchPhase.CANCELLED
                } catch (e: Exception) {
                    ticket.phase = SearchPhase.FAILED
                    ticket.message = e.message ?: "Refine failed"
                }
                ticket.elapsedMs = System.currentTimeMillis() - ticket.startedAt
            }
            return SearchHandle(ticket.id, generation, 0, "OK")
        }

        override fun cancelOperation(ticketId: Long, g: Int): OpResult {
            enforceCaller()
            val ticket = tickets[ticketId] ?: return OpResult.err(5, "Unknown operation.", generation)
            ticket.cancelled = true
            return OpResult.ok(generation)
        }

        override fun getSearchStatus(ticketId: Long, g: Int): SearchStatus {
            enforceCaller()
            val ticket = tickets[ticketId]
                ?: return SearchStatus(SearchPhase.IDLE, 0, 0, 0, 0, "Unknown operation.")
            if (ticket.job?.isActive == true) {
                ticket.elapsedMs = System.currentTimeMillis() - ticket.startedAt
            }
            return SearchStatus(ticket.phase, ticket.scannedBytes, ticket.totalBytes, ticket.resultCount, ticket.elapsedMs, ticket.message)
        }

        override fun getResultPage(ticketId: Long, offset: Int, count: Int, g: Int): ResultPage {
            enforceCaller()
            val ticket = tickets[ticketId] ?: return ResultPage(emptyList(), -1, 0)
            val set = ticket.candidateSet ?: return ResultPage(emptyList(), -1, 0)
            val total = set.candidates.size
            if (offset < 0 || offset >= total) return ResultPage(emptyList(), -1, total)
            val n = count.coerceIn(1, 200)
            val end = minOf(offset + n, total)
            val items = (offset until end).map { i ->
                val c = set.candidates[i]
                val current = GameHost.core?.readAt(c.address, set.type.sizeBytes)
                val stale = current != null && current != c.bits
                ResultItem(c.address, c.regionId, current?.let { ValueCodec.format(it, set.type) } ?: "?", stale)
            }
            return ResultPage(items, if (end < total) end else -1, total)
        }

        override fun readValues(addresses: LongArray, valueType: Int, g: Int): List<ValueSnapshot> {
            enforceCaller()
            requireSession(g)?.let { return addresses.map { ValueSnapshot(it, "", false, false) } }
            val type = ValueType.fromWire(valueType) ?: return addresses.map { ValueSnapshot(it, "", false, false) }
            return addresses.map { a ->
                val bits = GameHost.core?.readAt(a, type.sizeBytes)
                if (bits == null) ValueSnapshot(a, "", false, false)
                else ValueSnapshot(a, ValueCodec.format(bits, type), false, true)
            }
        }

        override fun previewWrite(addresses: LongArray, valueType: Int, newValueText: String, g: Int): WritePreview {
            enforceCaller()
            requireSession(g)?.let { return WritePreview(0, emptyList(), generation, it.code, it.message) }
            val type = ValueType.fromWire(valueType)
                ?: return WritePreview(0, emptyList(), generation, 3, "Unknown value type.")
            val newBits = try {
                ValueCodec.parse(newValueText, type)
            } catch (e: Exception) {
                return WritePreview(0, emptyList(), generation, 3, e.message ?: "Invalid value")
            }
            val core = GameHost.core
                ?: return WritePreview(0, emptyList(), generation, 2, "No active session.")
            val items = addresses.map { a ->
                val writable = GameMemory.writableAddresses.contains(a)
                val old = if (writable) core.readAt(a, type.sizeBytes) else null
                PreviewItem(
                    address = a,
                    oldValue = old?.let { ValueCodec.format(it, type) } ?: "—",
                    newValue = if (writable) ValueCodec.format(newBits, type) else "—",
                    writable = writable,
                )
            }
            val id = previewIds.getAndIncrement()
            previews[id] = PendingWrite(
                addresses = addresses.toList(),
                type = type,
                newBits = newBits,
                oldBits = addresses.map { a -> core.readAt(a, type.sizeBytes) ?: 0uL },
                generation = g,
            )
            return WritePreview(id, items, generation, 0, "OK")
        }

        override fun commitWrite(previewId: Long, g: Int): OpResult {
            enforceCaller()
            requireSession(g)?.let { return it }
            val pending = previews.remove(previewId)
                ?: return OpResult.err(5, "That preview expired. Preview again, then commit.", generation)
            val core = GameHost.core ?: return OpResult.err(2, "No active session.", generation)
            var affected = 0
            var failed = 0
            pending.addresses.forEach { a ->
                if (GameMemory.writableAddresses.contains(a)) {
                    if (core.writeAt(a, pending.newBits, pending.type.sizeBytes)) affected++ else failed++
                } else failed++
            }
            if (affected > 0) {
                val writable = pending.addresses.filter { GameMemory.writableAddresses.contains(it) }
                val entry = JournalEntry(
                    id = journalIds.getAndIncrement(),
                    timeMs = System.currentTimeMillis(),
                    addresses = writable,
                    oldValues = writable.map { a ->
                        val i = pending.addresses.indexOf(a)
                        ValueCodec.format(pending.oldBits[i], pending.type)
                    },
                    newValues = writable.map { ValueCodec.format(pending.newBits, pending.type) },
                    valueType = pending.type.wire,
                    undone = false,
                )
                synchronized(journal) { journal.add(entry) }
            }
            return OpResult.ok(generation, affected, failed)
        }

        override fun undoLastWrite(g: Int): OpResult {
            enforceCaller()
            requireSession(g)?.let { return it }
            val core = GameHost.core ?: return OpResult.err(2, "No active session.", generation)
            val entry = synchronized(journal) { journal.lastOrNull { !it.undone } }
                ?: return OpResult.err(5, "Nothing to undo.", generation)
            val type = ValueType.fromWire(entry.valueType)
                ?: return OpResult.err(4, "Journal entry has an unknown type.", generation)
            var affected = 0
            var failed = 0
            entry.addresses.forEachIndexed { i, a ->
                val nowBits = core.readAt(a, type.sizeBytes)
                if (nowBits == null) {
                    failed++
                    return@forEachIndexed
                }
                val recordedOld = try {
                    ValueCodec.parse(entry.oldValues[i], type)
                } catch (_: Exception) {
                    failed++
                    return@forEachIndexed
                }
                // Identity re-check: only restore when the current value is still our write.
                val currentIsOurWrite = try {
                    ValueCodec.parse(entry.newValues[i], type) == nowBits
                } catch (_: Exception) {
                    false
                }
                if (currentIsOurWrite && core.writeAt(a, recordedOld, type.sizeBytes)) {
                    affected++
                } else {
                    failed++
                }
            }
            synchronized(journal) {
                val idx = journal.indexOfFirst { it.id == entry.id }
                if (idx >= 0) journal[idx] = entry.copy(undone = true)
            }
            return OpResult.ok(generation, affected, failed)
        }

        override fun listJournal(g: Int): List<JournalEntry> {
            enforceCaller()
            requireSession(g)?.let { return emptyList() }
            return synchronized(journal) { journal.asReversed().take(50) }
        }

        override fun listFreezes(g: Int): List<FreezeEntry> {
            enforceCaller()
            requireSession(g)?.let { return emptyList() }
            return synchronized(freezes) {
                freezes.map { FreezeEntry(it.address, it.type.wire, ValueCodec.format(it.bits, it.type)) }
            }
        }

        override fun setFreeze(addresses: LongArray, valueType: Int, valueText: String, enable: Boolean, g: Int): OpResult {
            enforceCaller()
            requireSession(g)?.let { return it }
            val type = ValueType.fromWire(valueType) ?: return OpResult.err(3, "Unknown value type.", generation)
            val bits = try {
                ValueCodec.parse(valueText, type)
            } catch (e: Exception) {
                return OpResult.err(3, e.message ?: "Invalid value", generation)
            }
            val valid = addresses.filter { GameMemory.writableAddresses.contains(it) }
            if (valid.isEmpty()) return OpResult.err(9, "Those addresses are read-only.", generation)
            synchronized(freezes) {
                freezes.removeAll { f -> valid.contains(f.address) && f.type == type }
                if (enable) {
                    freezes.addAll(valid.map { FreezeData(it, type, bits) })
                }
            }
            publish()
            return OpResult.ok(generation, valid.size, 0)
        }

        override fun clearFreezes(g: Int): OpResult {
            enforceCaller()
            requireSession(g)?.let { return it }
            synchronized(freezes) { freezes.clear() }
            publish()
            return OpResult.ok(generation)
        }
    }

    private suspend fun runSearch(ticket: Ticket, spec: SearchSpec) {
        ticket.phase = SearchPhase.SCANNING
        try {
            val set = engine.initialSearch(
                source = adapter,
                spec = spec,
                isCancelled = { ticket.cancelled },
                onProgress = { scanned, total, found ->
                    ticket.scannedBytes = scanned
                    ticket.totalBytes = total
                    ticket.resultCount = found
                },
            )
            ticket.candidateSet = set
            ticket.resultCount = set.candidates.size
            ticket.phase = SearchPhase.DONE
        } catch (e: SearchCancelledException) {
            ticket.phase = SearchPhase.CANCELLED
        } catch (e: Exception) {
            ticket.phase = SearchPhase.FAILED
            ticket.message = e.message ?: "Search failed"
        }
        ticket.elapsedMs = System.currentTimeMillis() - ticket.startedAt
    }
}
