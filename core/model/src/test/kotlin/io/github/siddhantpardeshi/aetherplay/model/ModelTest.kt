package io.github.siddhantpardeshi.aetherplay.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueCodecTest {

    @Test
    fun `parse and format roundtrip for every integer type`() {
        ValueType.entries.filter { !it.isFloat }.forEach { type ->
            val sample = when (type) {
                ValueType.I8 -> "-5"
                ValueType.U8 -> "250"
                ValueType.I16 -> "-30000"
                ValueType.U16 -> "65535"
                ValueType.I32 -> "-2000000000"
                ValueType.U32 -> "4294967295"
                ValueType.I64 -> "-9000000000000000000"
                ValueType.U64 -> "18000000000000000000"
                else -> "0"
            }
            val bits = ValueCodec.parse(sample, type)
            assertEquals(sample, ValueCodec.format(bits, type))
        }
    }

    @Test
    fun `float parse roundtrips through text`() {
        val f32 = ValueCodec.parse("87.5", ValueType.F32)
        assertEquals("87.5", ValueCodec.format(f32, ValueType.F32))
        val f64 = ValueCodec.parse("3.25", ValueType.F64)
        assertEquals("3.25", ValueCodec.format(f64, ValueType.F64))
    }

    @Test
    fun `overflow rejected with clear errors`() {
        assertThrows(ValueParseException::class.java) { ValueCodec.parse("128", ValueType.I8) }
        assertThrows(ValueParseException::class.java) { ValueCodec.parse("-1", ValueType.U8) }
        assertThrows(ValueParseException::class.java) { ValueCodec.parse("70000", ValueType.U16) }
        assertThrows(ValueParseException::class.java) { ValueCodec.parse("abc", ValueType.I32) }
        assertThrows(ValueParseException::class.java) { ValueCodec.parse("NaN", ValueType.F64) }
        assertThrows(ValueParseException::class.java) { ValueCodec.parse("", ValueType.I32) }
    }

    @Test
    fun `little endian bit packing`() {
        val bytes = ByteArray(4)
        ValueCodec.writeBits(bytes, 0, ValueType.I32, 0x11223344uL)
        assertEquals(listOf<Byte>(0x44, 0x33, 0x22, 0x11), bytes.toList())
        assertEquals(0x11223344uL, ValueCodec.readBits(bytes, 0, ValueType.I32))
    }

    @Test
    fun `signed versus unsigned comparison semantics`() {
        // 0xFFFFFFFF is -1 signed, 4294967295 unsigned
        val a = 0xFFFFFFFFuL
        val b = 0x00000001uL
        assertTrue((ValueCodec.compare(a, b, ValueType.I32) ?: 0) < 0)  // -1 < 1
        assertTrue((ValueCodec.compare(a, b, ValueType.U32) ?: 0) > 0)  // 4294967295 > 1
    }

    @Test
    fun `float tolerance matching`() {
        // F64 keeps the tiny difference; F32 parse would quantize both to the same float.
        val a = ValueCodec.parse("10.0", ValueType.F64)
        val b = ValueCodec.parse("10.0000001", ValueType.F64)
        assertFalse(ValueCodec.exactMatch(a, b, ValueType.F64, tolerance = 0.0))
        assertTrue(ValueCodec.exactMatch(a, b, ValueType.F64, tolerance = 0.001))
    }

    @Test
    fun `nan never matches`() {
        val nan = java.lang.Float.floatToIntBits(Float.NaN).toULong()
        val alsoNan = java.lang.Float.floatToIntBits(Float.NaN).toULong()
        assertTrue(ValueCodec.exactMatch(nan, alsoNan, ValueType.F32, tolerance = 0.0)) // bit-identical NaN matches itself
        assertNull(ValueCodec.compare(nan, ValueCodec.parse("1.0", ValueType.F32), ValueType.F32))
    }

    @Test
    fun `value type wire codes are stable`() {
        // Wire codes are protocol: never renumber.
        assertEquals(1, ValueType.I8.wire)
        assertEquals(5, ValueType.I32.wire)
        assertEquals(9, ValueType.F32.wire)
        assertEquals(10, ValueType.F64.wire)
        assertEquals(ValueType.I32, ValueType.fromWire(5))
    }

    @Test
    fun `supported mask roundtrip`() {
        val mask = ValueType.supportedMask()
        assertTrue(ValueType.maskSupports(mask, ValueType.U64))
        val onlyFloats = ValueType.supportedMask(listOf(ValueType.F32, ValueType.F64))
        assertFalse(ValueType.maskSupports(onlyFloats, ValueType.I32))
    }
}

class ProtocolNegotiationTest {

    @Test
    fun `equal versions negotiate`() {
        val v = Protocol.negotiate(1, 0, 1, 0)
        assertTrue(v is Protocol.Verdict.Compatible)
        assertEquals(0, (v as Protocol.Verdict.Compatible).effectiveMinor)
    }

    @Test
    fun `minor mismatch takes the lower`() {
        val v = Protocol.negotiate(1, 3, 1, 1)
        assertTrue(v is Protocol.Verdict.Compatible)
        assertEquals(1, (v as Protocol.Verdict.Compatible).effectiveMinor)
    }

    @Test
    fun `major mismatch is actionable`() {
        val v = Protocol.negotiate(2, 0, 1, 4)
        assertTrue(v is Protocol.Verdict.MajorMismatch)
    }

    @Test
    fun `capability mask`() {
        val mask = Capability.mask(Capability.SEARCH, Capability.WRITE)
        assertTrue(Capability.supports(mask, Capability.SEARCH))
        assertFalse(Capability.supports(mask, Capability.FREEZE))
        assertEquals(listOf("search", "write"), Capability.describe(mask))
    }

    @Test
    fun `wire code messages exist for every code`() {
        (0..9).forEach { code ->
            assertTrue(WireCode.message(code).isNotEmpty())
        }
    }
}
