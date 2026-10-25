package io.github.siddhantpardeshi.aetherplay.space.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

/**
 * Crystal Sprint: the original AetherPlay training game.
 * Cooperative by design: its live state is exposed through a small, bounded
 * "memory surface" (GameMemory) that the Lab can search, read and edit.
 */
data class Crystal(val id: Int, val x: Float, val y: Float, val vy: Float)

data class GameState(
    val crystals: Int = 0,
    val energy: Float = 100f,
    val best: Long = 0L,
    val speed: Float = 1f,
    val gems: Int = 0,
    val tokens: Int = 0,
    val runTime: Float = 0f,
    val playerX: Float = 0.5f,
    val falling: List<Crystal> = emptyList(),
    val displayCrystals: Int = 0,
    val activityActive: Boolean = false,
)

/** Address layout of the cooperative memory surface. Little-endian. */
object GameMemory {
    const val STATE_BASE = 0x7B000000L
    const val STATE_SIZE = 64
    const val ADDR_CRYSTALS = STATE_BASE        // I32
    const val ADDR_ENERGY = STATE_BASE + 4      // F32, clamped 0..100
    const val ADDR_BEST = STATE_BASE + 8        // I64
    const val ADDR_SPEED = STATE_BASE + 16      // F32, clamped 0.25..4
    const val ADDR_GEMS = STATE_BASE + 20       // I32
    const val ADDR_TOKENS = STATE_BASE + 24     // I32, drifting decoy
    const val ADDR_RUN_TIME = STATE_BASE + 28   // F32

    const val FIELD_BASE = 0x7B100000L
    const val FIELD_SIZE = 128 // 32 x I32, read-only playfield telemetry

    val writableAddresses = setOf(
        ADDR_CRYSTALS, ADDR_ENERGY, ADDR_BEST, ADDR_SPEED, ADDR_GEMS, ADDR_TOKENS, ADDR_RUN_TIME,
    )
}

class GameCore(private val saveFile: File?) {

    private val lock = Any()

    private var crystals = 0
    private var energy = 100f
    private var best = 0L
    private var speed = 1f
    private var gems = 0
    private var tokens = 0
    private var runTime = 0f
    private var playerX = 0.5f
    private var falling = ArrayList<Crystal>()
    private var displayCrystals = 0
    private var displayLag = 0f
    private var spawnTimer = 0f
    private var nextId = 1
    private var activityActive = false

    /** Bumped on reset/restore so Lab result sets know they are stale. */
    @Volatile
    var generation: Long = 0L
        private set

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<GameState> = _state

    private fun snapshot() = GameState(
        crystals = crystals, energy = energy, best = best, speed = speed,
        gems = gems, tokens = tokens, runTime = runTime, playerX = playerX,
        falling = falling.toList(), displayCrystals = displayCrystals,
        activityActive = activityActive,
    )

    private fun publish() {
        _state.value = snapshot()
    }

    fun setActivityActive(active: Boolean) {
        synchronized(lock) { activityActive = active }
        publish()
        if (!active) save()
    }

    fun setPlayerX(x: Float) {
        synchronized(lock) { playerX = x.coerceIn(0.04f, 0.96f) }
        publish()
    }

    fun tick(dt: Float) {
        if (dt <= 0f || dt > 0.25f) return
        synchronized(lock) {
            runTime += dt
            tokens = (tokens + (dt * 7f).toInt()).let { if (it > 1_000_000) 0 else it }
            energy = (energy - (2.2f + 0.14f * crystals) * dt).coerceIn(0f, 100f)
            speed = (1f + crystals * 0.035f).coerceAtMost(4f)

            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                spawnTimer = (1.35f - crystals * 0.02f).coerceIn(0.45f, 1.35f)
                if (falling.size < 8) {
                    falling.add(Crystal(nextId++, Random.nextFloat() * 0.92f + 0.04f, -0.05f, 0.14f + speed * 0.05f))
                }
            }
            val remove = ArrayList<Int>()
            for (i in falling.indices) {
                val c = falling[i]
                val ny = c.y + c.vy * speed * dt
                when {
                    ny > 1.1f -> remove.add(i)
                    ny > 0.82f && ny < 0.95f && abs(c.x - playerX) < 0.085f -> {
                        remove.add(i)
                        crystals += 1
                        gems += if (crystals % 3 == 0) 1 else 0
                        energy = (energy + 6f).coerceAtMost(100f)
                        if (crystals.toLong() > best) best = crystals.toLong()
                    }
                    else -> falling[i] = c.copy(y = ny)
                }
            }
            for (i in remove.asReversed()) falling.removeAt(i)
            displayLag += dt
            if (displayLag > 2f) {
                displayLag = 0f
                displayCrystals = crystals
            }
        }
        publish()
    }

    fun resetRun() {
        synchronized(lock) {
            crystals = 0
            energy = 100f
            speed = 1f
            gems = 0
            runTime = 0f
            falling.clear()
            displayCrystals = 0
            displayLag = 0f
            generation += 1
        }
        publish()
    }

    // ---- memory surface -------------------------------------------------

    /** Reads any in-region address; writes stay restricted to the named fields. */
    fun readAt(address: Long, size: Int): ULong? {
        synchronized(lock) {
            val bytes: ByteArray
            val offset: Int
            when {
                address >= GameMemory.STATE_BASE &&
                    address + size <= GameMemory.STATE_BASE + GameMemory.STATE_SIZE &&
                    address >= GameMemory.STATE_BASE -> {
                    bytes = snapshotStateRegion()
                    offset = (address - GameMemory.STATE_BASE).toInt()
                }
                address >= GameMemory.FIELD_BASE &&
                    address + size <= GameMemory.FIELD_BASE + GameMemory.FIELD_SIZE -> {
                    bytes = snapshotFieldRegion()
                    offset = (address - GameMemory.FIELD_BASE).toInt()
                }
                else -> return null
            }
            var acc = 0uL
            for (i in 0 until size) acc = acc or ((bytes[offset + i].toUByte().toULong()) shl (8 * i))
            return acc
        }
    }

    /** Returns false when the address is outside the cooperative surface or read-only. */
    fun writeAt(address: Long, bits: ULong, size: Int): Boolean {
        synchronized(lock) {
            val bytes = ByteArray(size)
            for (i in 0 until size) bytes[i] = ((bits shr (8 * i)) and 0xFFuL).toByte()
            return writeBytesInto(address, bytes, isWrite = true)
        }
    }

    private fun writeBytesInto(address: Long, bytes: ByteArray, isWrite: Boolean): Boolean {
        fun put(dst: ByteArray, offset: Int) { System.arraycopy(bytes, 0, dst, offset, bytes.size) }
        when (address) {
            GameMemory.ADDR_CRYSTALS -> if (bytes.size == 4) { if (isWrite) crystals = leInt(bytes).coerceAtLeast(0); return true }
            GameMemory.ADDR_ENERGY -> if (bytes.size == 4) { if (isWrite) energy = leFloat(bytes).coerceIn(0f, 100f); return true }
            GameMemory.ADDR_BEST -> if (bytes.size == 8) { if (isWrite) best = leLong(bytes).coerceAtLeast(0); return true }
            GameMemory.ADDR_SPEED -> if (bytes.size == 4) { if (isWrite) speed = leFloat(bytes).coerceIn(0.25f, 4f); return true }
            GameMemory.ADDR_GEMS -> if (bytes.size == 4) { if (isWrite) gems = leInt(bytes).coerceAtLeast(0); return true }
            GameMemory.ADDR_TOKENS -> if (bytes.size == 4) { if (isWrite) tokens = leInt(bytes); return true }
            GameMemory.ADDR_RUN_TIME -> if (bytes.size == 4) { if (isWrite) runTime = leFloat(bytes).coerceAtLeast(0f); return true }
        }
        return false
    }

    fun snapshotStateRegion(): ByteArray {
        synchronized(lock) {
            val b = ByteArray(GameMemory.STATE_SIZE)
            putInt(b, 0, crystals)
            putFloat(b, 4, energy)
            putLong(b, 8, best)
            putFloat(b, 16, speed)
            putInt(b, 20, gems)
            putInt(b, 24, tokens)
            putFloat(b, 28, runTime)
            return b
        }
    }

    fun snapshotFieldRegion(): ByteArray {
        synchronized(lock) {
            val b = ByteArray(GameMemory.FIELD_SIZE)
            for (i in 0 until 8) {
                val c = falling.getOrNull(i)
                putInt(b, i * 4, if (c == null) 0 else (c.y * 1000f).toInt())
                putInt(b, 32 + i * 4, if (c == null) 0 else (c.x * 1000f).toInt())
            }
            putInt(b, 96, displayCrystals)      // lagging display copy of crystals
            putInt(b, 100, crystals * 2)         // doubled mirror
            putInt(b, 104, 0x00C0FFEE)          // static pattern
            putInt(b, 108, energy.toInt())
            putInt(b, 112, tokens)               // drifting echo
            putInt(b, 116, 7331)                // static pattern
            putInt(b, 120, crystals)             // direct mirror (refine demo)
            return b
        }
    }

    private fun leInt(b: ByteArray): Int {
        return (b[0].toInt() and 0xFF) or ((b[1].toInt() and 0xFF) shl 8) or
            ((b[2].toInt() and 0xFF) shl 16) or ((b[3].toInt() and 0xFF) shl 24)
    }

    private fun leLong(b: ByteArray): Long {
        var v = 0L
        for (i in 7 downTo 0) v = (v shl 8) or (b[i].toLong() and 0xFF)
        return v
    }

    private fun leFloat(b: ByteArray): Float = java.lang.Float.intBitsToFloat(leInt(b))

    private fun putInt(b: ByteArray, off: Int, v: Int) {
        b[off] = (v and 0xFF).toByte()
        b[off + 1] = ((v shr 8) and 0xFF).toByte()
        b[off + 2] = ((v shr 16) and 0xFF).toByte()
        b[off + 3] = ((v shr 24) and 0xFF).toByte()
    }

    private fun putLong(b: ByteArray, off: Int, v: Long) {
        for (i in 0 until 8) b[off + i] = ((v shr (8 * i)) and 0xFF).toByte()
    }

    private fun putFloat(b: ByteArray, off: Int, v: Float) = putInt(b, off, java.lang.Float.floatToIntBits(v))

    // ---- persistence ----------------------------------------------------

    fun save() {
        val f = saveFile ?: return
        synchronized(lock) {
            try {
                val o = JSONObject()
                    .put("crystals", crystals).put("energy", energy.toDouble())
                    .put("best", best).put("gems", gems).put("tokens", tokens)
                    .put("runTime", runTime.toDouble()).put("speed", speed.toDouble())
                f.parentFile?.mkdirs()
                f.writeText(o.toString())
            } catch (_: Exception) {
                // Saves are best-effort; never crash the game loop on IO.
            }
        }
    }

    fun restore(json: String) {
        synchronized(lock) {
            try {
                val o = JSONObject(json)
                crystals = o.optInt("crystals", 0)
                energy = o.optDouble("energy", 100.0).toFloat().coerceIn(0f, 100f)
                best = o.optLong("best", 0L)
                gems = o.optInt("gems", 0)
                tokens = o.optInt("tokens", 0)
                runTime = o.optDouble("runTime", 0.0).toFloat()
                speed = o.optDouble("speed", 1.0).toFloat().coerceIn(0.25f, 4f)
                falling.clear()
                displayCrystals = crystals
                generation += 1
            } catch (_: Exception) {
            }
        }
        publish()
    }
}
