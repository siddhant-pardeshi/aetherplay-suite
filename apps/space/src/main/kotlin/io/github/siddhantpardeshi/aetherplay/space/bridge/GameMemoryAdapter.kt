package io.github.siddhantpardeshi.aetherplay.space.bridge

import io.github.siddhantpardeshi.aetherplay.search.MemoryRegion
import io.github.siddhantpardeshi.aetherplay.search.MemorySource
import io.github.siddhantpardeshi.aetherplay.space.game.GameHost
import io.github.siddhantpardeshi.aetherplay.space.game.GameMemory

/** Bridges the live Crystal Sprint state into the bounded search-engine view. */
class GameMemoryAdapter : MemorySource {

    override fun regions(): List<MemoryRegion> = listOf(
        MemoryRegion("state", "Game state", GameMemory.STATE_BASE, GameMemory.STATE_SIZE, true),
        MemoryRegion("playfield", "Playfield telemetry (read-only)", GameMemory.FIELD_BASE, GameMemory.FIELD_SIZE, false),
    )

    override fun read(regionId: String): ByteArray {
        val core = GameHost.core ?: throw IllegalStateException("no game")
        return when (regionId) {
            "state" -> core.snapshotStateRegion()
            "playfield" -> core.snapshotFieldRegion()
            else -> throw IllegalArgumentException("unknown region $regionId")
        }
    }

    override fun generation(): Long = GameHost.core?.generation ?: 0L
}
