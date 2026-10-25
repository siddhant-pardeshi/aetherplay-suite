package io.github.siddhantpardeshi.aetherplay.search

import io.github.siddhantpardeshi.aetherplay.model.RefineOp
import io.github.siddhantpardeshi.aetherplay.model.SearchKind
import io.github.siddhantpardeshi.aetherplay.model.ValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixed fixture memory: two small regions with known, controlled contents. */
class FixtureSource(mutate: (Int) -> ByteArray = { i -> initial(i) }) : MemorySource {

    companion object {
        const val STATE_BASE = 0x1000L
        const val FIELD_BASE = 0x2000L

        fun initial(gen: Int): ByteArray {
            // 64 bytes of state
            val b = ByteArray(64)
            putI32(b, 0, 42)              // crystals
            putF32(b, 4, 87.5f)           // energy
            putI64(b, 8, 123456789L)      // best
            putI32(b, 16, 42)             // mirror of crystals
            putI32(b, 20, 84)             // double mirror
            putI32(b, 24, 42 + gen)       // lagging decoy
            putI32(b, 28, 7)
            return b
        }

        fun putI32(b: ByteArray, off: Int, v: Int) {
            b[off] = (v and 0xFF).toByte()
            b[off + 1] = ((v shr 8) and 0xFF).toByte()
            b[off + 2] = ((v shr 16) and 0xFF).toByte()
            b[off + 3] = ((v shr 24) and 0xFF).toByte()
        }

        fun putF32(b: ByteArray, off: Int, v: Float) = putI32(b, off, java.lang.Float.floatToIntBits(v))

        fun putI64(b: ByteArray, off: Int, v: Long) {
            for (i in 0 until 7) b[off + i] = ((v shr (8 * i)) and 0xFF).toByte()
        }
    }

    private var gen = 0L
    private val stateBytes = mutate(0)
    private val fieldBytes = ByteArray(32).also { putI32(it, 0, 42); putI32(it, 4, 43) }

    override fun regions() = listOf(
        MemoryRegion("state", "state", STATE_BASE, 64, true),
        MemoryRegion("field", "field", FIELD_BASE, 32, false),
    )

    override fun read(regionId: String): ByteArray = if (regionId == "state") stateBytes else fieldBytes

    override fun generation(): Long = gen

    fun bumpGeneration() { gen += 1 }
}

class SearchEngineTest {

    private fun engine() = SearchEngine(maxResults = 10_000)

    @Test
    fun `exact i32 search finds all matching locations across regions`() {
        val set = engine().initialSearch(
            FixtureSource(),
            spec(ValueType.I32, SearchKind.EXACT, exact = 42uL),
        )
        // state@0 (crystals), state@16 (mirror), state@24 (lagging, 42+0), field@0
        val addresses = set.candidates.map { it.address }
        assertTrue(FixtureSource.STATE_BASE + 0 in addresses)
        assertTrue(FixtureSource.STATE_BASE + 16 in addresses)
        assertTrue(FixtureSource.STATE_BASE + 24 in addresses)
        assertTrue(FixtureSource.FIELD_BASE + 0 in addresses)
        assertEquals(4, set.candidates.size)
    }

    @Test
    fun `alignment skips unaligned candidates`() {
        val set = engine().initialSearch(
            FixtureSource(),
            spec(ValueType.I32, SearchKind.EXACT, exact = 42uL, alignment = 4),
        )
        val offsets = set.candidates.map { (it.address - FixtureSource.STATE_BASE).toInt() }
        assertTrue(offsets.all { it % 4 == 0 })
        assertTrue(FixtureSource.STATE_BASE + 16 in set.candidates.map { it.address })
    }

    @Test
    fun `unknown search returns every aligned location of the type`() {
        val set = engine().initialSearch(
            FixtureSource(),
            spec(ValueType.I32, SearchKind.UNKNOWN),
        )
        // (64 + 32) / 4 possible, unaligned byte-level matches included at alignment 1 -> far more
        assertTrue(set.candidates.size >= 24)
    }

    @Test
    fun `range search respects bounds`() {
        val set = engine().initialSearch(
            FixtureSource(),
            spec(ValueType.I32, SearchKind.RANGE, min = 40uL, max = 45uL),
        )
        val values = set.candidates.map { io.github.siddhantpardeshi.aetherplay.model.ValueCodec.decodeInt(it.bits, ValueType.I32) }
        assertTrue(values.all { it in 40..45 })
        assertTrue(42L in values)
        assertTrue(43L in values)
    }

    @Test
    fun `float exact search matches f32`() {
        val set = engine().initialSearch(
            FixtureSource(),
            spec(ValueType.F32, SearchKind.EXACT, exact = java.lang.Float.floatToIntBits(87.5f).toULong()),
        )
        assertEquals(listOf(FixtureSource.STATE_BASE + 4), set.candidates.map { it.address })
    }

    @Test
    fun `refine increased keeps only rising values`() {
        val engine = engine()
        val first = engine.initialSearch(FixtureSource(), spec(ValueType.I32, SearchKind.EXACT, exact = 42uL))
        val mutated = FixtureSource { _ ->
            ByteArray(64).also { b ->
                FixtureSource.putI32(b, 0, 43)
                FixtureSource.putI32(b, 16, 43)
                FixtureSource.putI32(b, 24, 42)
            }
        }
        val second = engine.refine(mutated, first, RefineOp.INCREASED)
        val addresses = second.candidates.map { it.address }
        assertTrue(FixtureSource.STATE_BASE + 0 in addresses)
        assertTrue(FixtureSource.STATE_BASE + 16 in addresses)
        assertTrue(FixtureSource.STATE_BASE + 24 !in addresses)
        assertTrue(FixtureSource.FIELD_BASE + 0 !in addresses)
    }

    @Test
    fun `refine unchanged keeps stable values`() {
        val engine = engine()
        val first = engine.initialSearch(FixtureSource(), spec(ValueType.I32, SearchKind.EXACT, exact = 42uL))
        val stable = FixtureSource()
        val second = engine.refine(stable, first, RefineOp.UNCHANGED)
        assertEquals(4, second.candidates.size)
    }

    @Test
    fun `refine exact narrows to matching value`() {
        val engine = engine()
        val first = engine.initialSearch(FixtureSource(), spec(ValueType.I32, SearchKind.UNKNOWN, alignment = 4))
        val second = engine.refine(source = FixtureSource(), current = first, op = RefineOp.EXACT, exactBits = 84uL)
        val addresses = second.candidates.map { it.address }
        assertTrue(FixtureSource.STATE_BASE + 20 in addresses)
        assertEquals(1, addresses.size)
    }

    @Test
    fun `cancellation throws`() {
        val engine = engine()
        assertThrows(SearchCancelledException::class.java) {
            engine.initialSearch(
                FixtureSource(),
                spec(ValueType.I32, SearchKind.UNKNOWN),
                isCancelled = { true },
            )
        }
    }

    @Test
    fun `result cap respected`() {
        val set = SearchEngine(maxResults = 5).initialSearch(
            FixtureSource(),
            spec(ValueType.I32, SearchKind.UNKNOWN, alignment = 4),
        )
        assertEquals(5, set.candidates.size)
    }

    @Test
    fun `bad queries rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine().initialSearch(FixtureSource(), spec(ValueType.I32, SearchKind.EXACT, exact = null))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine().initialSearch(FixtureSource(), SearchSpec(ValueType.I32, SearchKind.RANGE, emptySet(), 1, 0uL, 0uL, 0uL))
        }
    }

    private fun spec(
        type: ValueType,
        kind: SearchKind,
        exact: ULong? = null,
        min: ULong? = null,
        max: ULong? = null,
        alignment: Int = 1,
    ) = SearchSpec(
        type = type,
        kind = kind,
        regionIds = setOf("state", "field"),
        alignment = alignment,
        exactBits = exact,
        minBits = min,
        maxBits = max,
    )
}
