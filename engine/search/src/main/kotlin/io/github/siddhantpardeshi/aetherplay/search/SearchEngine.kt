package io.github.siddhantpardeshi.aetherplay.search

import io.github.siddhantpardeshi.aetherplay.model.RefineOp
import io.github.siddhantpardeshi.aetherplay.model.SearchKind
import io.github.siddhantpardeshi.aetherplay.model.ValueCodec
import io.github.siddhantpardeshi.aetherplay.model.ValueType

data class MemoryRegion(
    val id: String,
    val label: String,
    val startAddress: Long,
    val sizeBytes: Int,
    val writable: Boolean,
)

/**
 * Bounded, readable view of a target's memory. Implementations must be safe to
 * call from a single background thread; reads must not return null for
 * in-range offsets (unreadable pages should be modeled as separate regions).
 */
interface MemorySource {
    fun regions(): List<MemoryRegion>
    fun read(regionId: String): ByteArray
    /** Monotonic counter bumped whenever the target may have changed its data. */
    fun generation(): Long
}

data class SearchSpec(
    val type: ValueType,
    val kind: SearchKind,
    val regionIds: Set<String>,
    val alignment: Int = 1,
    val exactBits: ULong? = null,
    val minBits: ULong? = null,
    val maxBits: ULong? = null,
    val floatTolerance: Double = 0.0,
)

data class Candidate(val address: Long, val regionId: String, val bits: ULong)

class CandidateSet(
    val type: ValueType,
    val generation: Long,
    val candidates: List<Candidate>,
)

class SearchCancelledException : Exception("cancelled")

/**
 * Streaming scalar search over a [MemorySource]. Correctness first: a simple
 * bounded scan with typed comparisons; refinement compares a fresh read
 * against each candidate's captured bits.
 */
class SearchEngine(private val maxResults: Int = 200_000) {

    fun initialSearch(
        source: MemorySource,
        spec: SearchSpec,
        isCancelled: () -> Boolean = { false },
        onProgress: (scannedBytes: Long, totalBytes: Long, found: Int) -> Unit = { _, _, _ -> },
    ): CandidateSet {
        val type = spec.type
        val regions = source.regions().filter { it.id in spec.regionIds }
        if (regions.isEmpty()) throw IllegalArgumentException("no regions selected")
        require(spec.alignment in 1..8) { "alignment must be 1..8" }
        if (spec.kind == SearchKind.EXACT && spec.exactBits == null) throw IllegalArgumentException("EXACT needs a value")
        if (spec.kind == SearchKind.RANGE && (spec.minBits == null || spec.maxBits == null)) {
            throw IllegalArgumentException("RANGE needs min and max")
        }
        val total = regions.sumOf { it.sizeBytes.toLong() }
        var scanned = 0L
        val out = ArrayList<Candidate>(minOf(1024, maxResults))
        val gen = source.generation()
        for (region in regions) {
            val bytes = source.read(region.id)
            val step = spec.alignment
            var offset = 0
            while (offset + type.sizeBytes <= region.sizeBytes) {
                if (isCancelled()) throw SearchCancelledException()
                val bits = ValueCodec.readBits(bytes, offset, type)
                val hit = when (spec.kind) {
                    SearchKind.EXACT ->
                        ValueCodec.exactMatch(bits, spec.exactBits!!, type, spec.floatTolerance)
                    SearchKind.RANGE -> inRange(bits, spec, type)
                    SearchKind.UNKNOWN -> true
                }
                if (hit) {
                    out.add(Candidate(region.startAddress + offset, region.id, bits))
                    if (out.size >= maxResults) return CandidateSet(type, gen, out)
                }
                offset += step
            }
            scanned += region.sizeBytes
            onProgress(scanned, total, out.size)
        }
        return CandidateSet(type, gen, out)
    }

    fun refine(
        source: MemorySource,
        current: CandidateSet,
        op: RefineOp,
        exactBits: ULong? = null,
        floatTolerance: Double = 0.0,
        isCancelled: () -> Boolean = { false },
        onProgress: (processed: Int, total: Int, found: Int) -> Unit = { _, _, _ -> },
    ): CandidateSet {
        if (op == RefineOp.EXACT && exactBits == null) throw IllegalArgumentException("EXACT refine needs a value")
        val type = current.type
        val regionIndex = source.regions().associateBy { it.id }
        val out = ArrayList<Candidate>(current.candidates.size)
        val total = current.candidates.size
        val gen = source.generation()
        var processed = 0
        for (candidate in current.candidates) {
            if (isCancelled()) throw SearchCancelledException()
            val region = regionIndex[candidate.regionId] ?: continue
            val offset = (candidate.address - region.startAddress).toInt()
            if (offset < 0 || offset + type.sizeBytes > region.sizeBytes) continue
            val now = ValueCodec.readBits(source.read(region.id), offset, type)
            val keep = when (op) {
                RefineOp.EXACT -> ValueCodec.exactMatch(now, exactBits!!, type, floatTolerance)
                RefineOp.CHANGED -> !ValueCodec.exactMatch(now, candidate.bits, type, floatTolerance)
                RefineOp.UNCHANGED -> ValueCodec.exactMatch(now, candidate.bits, type, floatTolerance)
                RefineOp.INCREASED -> increased(candidate.bits, now, type)
                RefineOp.DECREASED -> increased(now, candidate.bits, type)
            }
            if (keep) out.add(candidate.copy(bits = now))
            processed++
            if (processed % 512 == 0) {
                onProgress(processed, total, out.size)
                if (isCancelled()) throw SearchCancelledException()
            }
        }
        onProgress(processed, total, out.size)
        return CandidateSet(type, gen, out)
    }

    private fun inRange(bits: ULong, spec: SearchSpec, type: ValueType): Boolean {
        if (type.isFloat) {
            val v = ValueCodec.decodeFloat(bits, type)
            if (v.isNaN()) return false
            val min = ValueCodec.decodeFloat(spec.minBits!!, type)
            val max = ValueCodec.decodeFloat(spec.maxBits!!, type)
            return v >= min && v <= max
        }
        val a = ValueCodec.compare(bits, spec.minBits!!, type) ?: return false
        val b = ValueCodec.compare(bits, spec.maxBits!!, type) ?: return false
        return a >= 0 && b <= 0
    }

    private fun increased(oldBits: ULong, newBits: ULong, type: ValueType): Boolean =
        ValueCodec.compare(newBits, oldBits, type)?.let { it > 0 } ?: false
}
