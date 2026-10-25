package io.github.siddhantpardeshi.aetherplay.lab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.siddhantpardeshi.aetherplay.lab.bridge.BridgeClient
import io.github.siddhantpardeshi.aetherplay.lab.bridge.ConnState
import io.github.siddhantpardeshi.aetherplay.lab.data.Experiment
import io.github.siddhantpardeshi.aetherplay.lab.data.ExperimentStore
import io.github.siddhantpardeshi.aetherplay.model.ValueCodec
import io.github.siddhantpardeshi.aetherplay.model.ValueType
import io.github.siddhantpardeshi.aetherplay.protocol.FreezeEntry
import io.github.siddhantpardeshi.aetherplay.protocol.JournalEntry
import io.github.siddhantpardeshi.aetherplay.protocol.RegionInfo
import io.github.siddhantpardeshi.aetherplay.protocol.ResultItem
import io.github.siddhantpardeshi.aetherplay.protocol.SearchPhase
import io.github.siddhantpardeshi.aetherplay.protocol.SearchQuery
import io.github.siddhantpardeshi.aetherplay.protocol.SearchStatus
import io.github.siddhantpardeshi.aetherplay.protocol.SessionState
import io.github.siddhantpardeshi.aetherplay.protocol.SessionStatus
import io.github.siddhantpardeshi.aetherplay.protocol.ValueSnapshot
import io.github.siddhantpardeshi.aetherplay.protocol.WritePreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val typeWire: Int = 5,
    val kind: Int = 0, // 0 exact, 1 range, 2 unknown
    val valueText: String = "",
    val minText: String = "",
    val maxText: String = "",
    val alignment: Int = 1,
    val regions: List<RegionInfo> = emptyList(),
    val selectedRegions: Set<String> = setOf("state", "playfield"),
    val advancedOpen: Boolean = false,
    val activeTicket: Long? = null,
    val status: SearchStatus? = null,
    val items: List<ResultItem> = emptyList(),
    val nextOffset: Int = -1,
    val totalCount: Int = 0,
    val running: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class DetailUiState(
    val item: ResultItem,
    val typeWire: Int,
    val liveValue: String? = null,
    val writeText: String = "",
    val preview: WritePreview? = null,
    val frozen: Boolean = false,
    val message: String? = null,
)

class LabViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ExperimentStore(app)
    private val client = BridgeClient(app, viewModelScope, "0.1.0")

    val conn: StateFlow<ConnState> = client.state

    private val _session = MutableStateFlow<SessionStatus?>(null)
    val session: StateFlow<SessionStatus?> = _session

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search

    private val _detail = MutableStateFlow<DetailUiState?>(null)
    val detail: StateFlow<DetailUiState?> = _detail

    private val _journal = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journal: StateFlow<List<JournalEntry>> = _journal

    private val _freezes = MutableStateFlow<List<FreezeEntry>>(emptyList())
    val freezes: StateFlow<List<FreezeEntry>> = _freezes

    private val _experiments = MutableStateFlow(store.list())
    val experiments: StateFlow<List<Experiment>> = _experiments

    private var pollJob: Job? = null
    private var pageLoadedForTicket: Long? = null

    init {
        client.connect()
        viewModelScope.launch {
            conn.collect { state ->
                if (state is ConnState.Connected) {
                    startPolling()
                } else {
                    stopPolling()
                }
            }
        }
    }

    fun reconnect() = client.connect()

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                tick()
                delay(500)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun tick() {
        val status = client.call { it.getSessionStatus() } ?: return
        _session.value = status

        val s = _search.value
        if (s.activeTicket != null) {
            val st = client.call { it.getSearchStatus(s.activeTicket!!, status.generation) }
            if (st != null) {
                _search.value = s.copy(status = st, running = st.phase == SearchPhase.QUEUED || st.phase == SearchPhase.SCANNING)
                if (st.phase == SearchPhase.DONE && pageLoadedForTicket != s.activeTicket) {
                    pageLoadedForTicket = s.activeTicket
                    loadPage(0)
                }
            }
        }

        val d = _detail.value
        if (d != null) {
            val snaps: List<ValueSnapshot>? = client.call {
                it.readValues(longArrayOf(d.item.address), d.typeWire, status.generation)
            }
            if (snaps != null && snaps.isNotEmpty()) {
                _detail.value = d.copy(liveValue = if (snaps[0].readable) snaps[0].valueText else "unreadable")
            }
        }
    }

    // ---- search ---------------------------------------------------------

    fun updateSearch(transform: (SearchUiState) -> SearchUiState) {
        _search.value = transform(_search.value)
    }

    fun currentType(): ValueType = ValueType.fromWire(_search.value.typeWire) ?: ValueType.I32

    fun startSearch() {
        val s = _search.value
        val session = _session.value ?: return
        if (session.state != SessionState.RUNNING) {
            _search.value = s.copy(error = "No active session. Start one in AetherPlay Space.")
            return
        }
        val type = currentType()
        val exactBits: Pair<Long, Double>?
        val minBits: Pair<Long, Double>?
        val maxBits: Pair<Long, Double>?
        try {
            exactBits = if (s.kind == 0) parseBits(s.valueText, type) else null
            minBits = if (s.kind == 1) parseBits(s.minText, type) else null
            maxBits = if (s.kind == 1) parseBits(s.maxText, type) else null
            if (s.kind == 0 && exactBits == null) throw IllegalArgumentException("Enter a value")
            if (s.kind == 1 && (minBits == null || maxBits == null)) throw IllegalArgumentException("Enter both min and max")
        } catch (e: Exception) {
            _search.value = s.copy(error = e.message ?: "Invalid value")
            return
        }
        if (s.selectedRegions.isEmpty()) {
            _search.value = s.copy(error = "Select at least one region")
            return
        }
        val query = SearchQuery(
            valueType = s.typeWire,
            kind = s.kind,
            longValue = exactBits?.first ?: minBits?.first ?: 0L,
            longValue2 = maxBits?.first ?: 0L,
            hasValue = exactBits != null || (s.kind == 1 && minBits != null),
            hasValue2 = s.kind == 1 && maxBits != null,
            doubleExact = exactBits?.second ?: minBits?.second ?: 0.0,
            doubleMin = minBits?.second ?: 0.0,
            doubleMax = maxBits?.second ?: 0.0,
            regionIds = s.selectedRegions.toList(),
            alignment = s.alignment,
        )
        viewModelScope.launch {
            val handle = client.call { it.startSearch(query, session.generation) } ?: return@launch
            if (handle.code != 0) {
                _search.value = _search.value.copy(error = handle.message)
            } else {
                pageLoadedForTicket = null
                _search.value = _search.value.copy(
                    activeTicket = handle.ticketId,
                    running = true,
                    items = emptyList(),
                    nextOffset = -1,
                    totalCount = 0,
                    error = null,
                    message = null,
                    status = SearchStatus(SearchPhase.QUEUED, 0, 0, 0, 0, ""),
                )
            }
        }
    }

    fun cancelSearch() {
        val s = _search.value
        val ticket = s.activeTicket ?: return
        val session = _session.value ?: return
        viewModelScope.launch {
            client.call { it.cancelOperation(ticket, session.generation) }
        }
    }

    fun refine(op: Int, valueText: String) {
        val s = _search.value
        val session = _session.value ?: return
        val ticket = s.activeTicket ?: run {
            _search.value = s.copy(error = "Run a search first")
            return
        }
        val type = currentType()
        val bits = if (op == 0) {
            try {
                parseBits(valueText, type)
            } catch (e: Exception) {
                _search.value = s.copy(error = e.message ?: "Invalid value")
                return
            }
        } else null
        viewModelScope.launch {
            val handle = client.call {
                it.refineSearch(
                    ticket,
                    io.github.siddhantpardeshi.aetherplay.protocol.RefineQuery(
                        op = op,
                        longValue = bits?.first ?: 0L,
                        hasValue = bits != null,
                        doubleExact = bits?.second ?: 0.0,
                        floatTolerance = 0.0,
                    ),
                    session.generation,
                )
            } ?: return@launch
            if (handle.code != 0) {
                _search.value = _search.value.copy(error = handle.message)
            } else {
                pageLoadedForTicket = null
                _search.value = _search.value.copy(
                    items = emptyList(),
                    nextOffset = -1,
                    totalCount = 0,
                    running = true,
                    error = null,
                )
            }
        }
    }

    fun loadMore() {
        val s = _search.value
        if (s.nextOffset >= 0 && !s.running) {
            viewModelScope.launch { loadPage(s.nextOffset) }
        }
    }

    private suspend fun loadPage(offset: Int) {
        val s = _search.value
        val session = _session.value ?: return
        val ticket = s.activeTicket ?: return
        val page = client.call { it.getResultPage(ticket, offset, 100, session.generation) } ?: return
        _search.value = _search.value.copy(
            items = _search.value.items + page.items,
            nextOffset = page.nextOffset,
            totalCount = page.totalCount,
        )
    }

    private fun parseBits(text: String, type: ValueType): Pair<Long, Double> {
        val bits = ValueCodec.parse(text, type)
        val d = if (type.isFloat) ValueCodec.decodeFloat(bits, type) else ValueCodec.decodeInt(bits, type).toDouble()
        val l = if (type.isFloat) {
            when (type) {
                ValueType.F32 -> java.lang.Float.floatToIntBits(d.toFloat()).toLong()
                else -> java.lang.Double.doubleToRawLongBits(d)
            }
        } else ValueCodec.decodeInt(bits, type)
        return l to d
    }

    // ---- detail / write -------------------------------------------------

    fun openDetail(item: ResultItem) {
        _detail.value = DetailUiState(item = item, typeWire = _search.value.typeWire, liveValue = item.valueText)
    }

    fun closeDetail() {
        _detail.value = null
    }

    fun setWriteText(text: String) {
        _detail.value = _detail.value?.copy(writeText = text, preview = null, message = null)
    }

    fun previewWrite() {
        val d = _detail.value ?: return
        val session = _session.value ?: return
        viewModelScope.launch {
            val preview = client.call {
                it.previewWrite(longArrayOf(d.item.address), d.typeWire, d.writeText, session.generation)
            } ?: return@launch
            if (preview.code != 0) {
                _detail.value = _detail.value?.copy(message = preview.message)
            } else {
                _detail.value = _detail.value?.copy(preview = preview, message = null)
            }
        }
    }

    fun commitWrite() {
        val d = _detail.value ?: return
        val preview = d.preview ?: return
        val session = _session.value ?: return
        viewModelScope.launch {
            val result = client.call { it.commitWrite(preview.previewId, session.generation) } ?: return@launch
            _detail.value = _detail.value?.copy(
                preview = null,
                message = if (result.code == 0) {
                    "Applied to ${result.affectedCount} value${if (result.affectedCount == 1) "" else "s"}"
                } else result.message,
            )
            refreshHistory()
            refreshFreezes()
        }
    }

    fun toggleFreeze() {
        val d = _detail.value ?: return
        val session = _session.value ?: return
        val value = d.writeText.ifBlank { d.liveValue ?: return }
        viewModelScope.launch {
            val result = client.call {
                it.setFreeze(longArrayOf(d.item.address), d.typeWire, value, !d.frozen, session.generation)
            } ?: return@launch
            _detail.value = _detail.value?.copy(
                frozen = if (result.code == 0) !d.frozen else d.frozen,
                message = if (result.code != 0) result.message else null,
            )
            refreshFreezes()
        }
    }

    // ---- history --------------------------------------------------------

    fun refreshHistory() {
        val session = _session.value ?: return
        viewModelScope.launch {
            val journal = client.call { it.listJournal(session.generation) } ?: return@launch
            _journal.value = journal
        }
    }

    fun refreshFreezes() {
        val session = _session.value ?: return
        viewModelScope.launch {
            val freezes = client.call { it.listFreezes(session.generation) } ?: return@launch
            _freezes.value = freezes
        }
    }

    fun undoLast() {
        val session = _session.value ?: return
        viewModelScope.launch {
            val result = client.call { it.undoLastWrite(session.generation) } ?: return@launch
            _search.value = _search.value.copy(message = if (result.code == 0) "Undone" else result.message)
            refreshHistory()
            refreshFreezes()
        }
    }

    fun stopAllFreezes() {
        val session = _session.value ?: return
        viewModelScope.launch {
            client.call { it.clearFreezes(session.generation) }
            refreshFreezes()
        }
    }

    // ---- experiments ----------------------------------------------------

    fun saveExperiment(name: String) {
        val s = _search.value
        if (name.isBlank()) return
        store.save(
            Experiment(
                name = name.trim(),
                typeWire = s.typeWire,
                kind = s.kind,
                valueText = s.valueText,
                minText = s.minText,
                maxText = s.maxText,
                alignment = s.alignment,
                regionIds = s.selectedRegions.toList(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        _experiments.value = store.list()
    }

    fun deleteExperiment(name: String) {
        store.delete(name)
        _experiments.value = store.list()
    }

    fun runExperiment(exp: Experiment) {
        _search.value = _search.value.copy(
            typeWire = exp.typeWire,
            kind = exp.kind,
            valueText = exp.valueText,
            minText = exp.minText,
            maxText = exp.maxText,
            alignment = exp.alignment,
            selectedRegions = exp.regionIds.toSet(),
            error = null,
        )
        startSearch()
    }

    fun exportExperiments(): String = store.exportJson()

    fun loadRegions() {
        val session = _session.value ?: return
        if (session.state != SessionState.RUNNING) return
        viewModelScope.launch {
            val regions = client.call { it.listRegions(session.generation) } ?: return@launch
            if (regions.isNotEmpty()) {
                _search.value = _search.value.copy(
                    regions = regions,
                    selectedRegions = _search.value.selectedRegions.intersect(regions.map { it.id }.toSet()).ifEmpty { regions.map { it.id }.toSet() },
                )
            }
        }
    }

    override fun onCleared() {
        client.disconnect()
        super.onCleared()
    }
}
