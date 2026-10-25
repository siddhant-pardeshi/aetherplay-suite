package io.github.siddhantpardeshi.aetherplay.protocol

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Wire types for the Lab <-> Space bridge. Field layout is part of the
 * protocol contract: change major version in BridgeContract when shape changes.
 */

@Parcelize
data class HandshakeInfo(
    val protocolMajor: Int,
    val protocolMinor: Int,
    val spaceBuild: String,
    val runtimeBuild: String,
    val capabilities: Int,
    val valueTypeMask: Int,
    val maxResults: Int,
    val sessionActive: Boolean,
    val gameKey: String?,
    val spaceKey: String?,
    val generation: Int,
) : Parcelable

@Parcelize
data class GuestInfo(
    val key: String,
    val name: String,
    val kind: Int, // GuestKind
    val version: String,
) : Parcelable

@Parcelize
data class OpResult(
    val code: Int,
    val message: String,
    val generation: Int,
    val affectedCount: Int = 0,
    val failedCount: Int = 0,
) : Parcelable {
    companion object {
        fun ok(generation: Int, affected: Int = 0, failed: Int = 0) =
            OpResult(0, "OK", generation, affected, failed)

        fun err(code: Int, message: String, generation: Int) =
            OpResult(code, message, generation)
    }
}

@Parcelize
data class SessionStatus(
    val state: Int, // SessionState
    val gameKey: String?,
    val spaceKey: String?,
    val generation: Int,
    val freezeCount: Int,
    val elapsedMs: Long,
    val gameRunning: Boolean,
) : Parcelable

@Parcelize
data class RegionInfo(
    val id: String,
    val label: String,
    val startAddress: Long,
    val sizeBytes: Int,
    val writable: Boolean,
) : Parcelable

@Parcelize
data class SearchQuery(
    val valueType: Int, // ValueType.wire
    val kind: Int, // 0 exact, 1 range, 2 unknown
    val longValue: Long, // exact/min bits low word for ints; float bits as long
    val longValue2: Long, // max bits (range)
    val hasValue: Boolean,
    val hasValue2: Boolean,
    val doubleExact: Double,
    val doubleMin: Double,
    val doubleMax: Double,
    val regionIds: List<String>,
    val alignment: Int,
) : Parcelable

@Parcelize
data class SearchHandle(
    val ticketId: Long,
    val generation: Int,
    val code: Int,
    val message: String,
) : Parcelable

@Parcelize
data class SearchStatus(
    val phase: Int, // SearchPhase
    val scannedBytes: Long,
    val totalBytes: Long,
    val resultCount: Int,
    val elapsedMs: Long,
    val message: String,
) : Parcelable

@Parcelize
data class ResultItem(
    val address: Long,
    val regionId: String,
    val valueText: String,
    val stale: Boolean,
) : Parcelable

@Parcelize
data class ResultPage(
    val items: List<ResultItem>,
    val nextOffset: Int, // -1 when exhausted
    val totalCount: Int,
) : Parcelable

@Parcelize
data class ValueSnapshot(
    val address: Long,
    val valueText: String,
    val stale: Boolean,
    val readable: Boolean,
) : Parcelable

@Parcelize
data class PreviewItem(
    val address: Long,
    val oldValue: String,
    val newValue: String,
    val writable: Boolean,
) : Parcelable

@Parcelize
data class WritePreview(
    val previewId: Long,
    val items: List<PreviewItem>,
    val generation: Int,
    val code: Int,
    val message: String,
) : Parcelable

@Parcelize
data class RefineQuery(
    val op: Int, // 0 exact, 1 changed, 2 unchanged, 3 increased, 4 decreased
    val longValue: Long, // exact bits (ints) or float bits (floats)
    val hasValue: Boolean,
    val doubleExact: Double,
    val floatTolerance: Double,
) : Parcelable

@Parcelize
data class JournalEntry(
    val id: Long,
    val timeMs: Long,
    val addresses: List<Long>,
    val oldValues: List<String>,
    val newValues: List<String>,
    val valueType: Int,
    val undone: Boolean,
) : Parcelable

@Parcelize
data class FreezeEntry(
    val address: Long,
    val valueType: Int,
    val valueText: String,
) : Parcelable
