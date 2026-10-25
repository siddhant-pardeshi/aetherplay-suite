package io.github.siddhantpardeshi.aetherplay.model

/**
 * Typed value model shared by Lab, Space and the search engine.
 * Wire values are little-endian raw bits packed into a ULong (values are at most 8 bytes).
 */
enum class ValueType(val wire: Int, val sizeBytes: Int, val isFloat: Boolean, val isSigned: Boolean) {
    I8(1, 1, false, true),
    U8(2, 1, false, false),
    I16(3, 2, false, true),
    U16(4, 2, false, false),
    I32(5, 4, false, true),
    U32(6, 4, false, false),
    I64(7, 8, false, true),
    U64(8, 8, false, false),
    F32(9, 4, true, false),
    F64(10, 8, true, false);

    companion object {
        fun fromWire(wire: Int): ValueType? = entries.firstOrNull { it.wire == wire }

        fun supportedMask(types: Collection<ValueType> = entries): Int =
            types.fold(0) { mask, t -> mask or (1 shl (t.wire - 1)) }

        fun maskSupports(mask: Int, type: ValueType): Boolean = mask and (1 shl (type.wire - 1)) != 0
    }
}

enum class SearchKind { EXACT, RANGE, UNKNOWN }

enum class RefineOp { EXACT, CHANGED, UNCHANGED, INCREASED, DECREASED }

class ValueParseException(message: String) : Exception(message)

object ValueCodec {

    /** Parses user text ("42", "3.5", "-7") into raw little-endian bits for [type]. */
    fun parse(text: String, type: ValueType): ULong {
        val s = text.trim()
        if (s.isEmpty()) throw ValueParseException("Enter a value")
        return when (type) {
            ValueType.F32 -> {
                val v = s.toDoubleOrNull() ?: throw ValueParseException("Not a number")
                if (v.isNaN() || v.isInfinite()) throw ValueParseException("Value must be finite")
                val bits = java.lang.Float.floatToIntBits(v.toFloat()).toULong()
                if (v != v.toFloat().toDouble() && !floatCloseEnough(v, v.toFloat().toDouble())) {
                    // Accept but note: value is rounded to float precision by design.
                }
                bits
            }
            ValueType.F64 -> {
                val v = s.toDoubleOrNull() ?: throw ValueParseException("Not a number")
                if (v.isNaN() || v.isInfinite()) throw ValueParseException("Value must be finite")
                java.lang.Double.doubleToRawLongBits(v).toULong()
            }
            ValueType.I8, ValueType.I16, ValueType.I32, ValueType.I64 -> {
                val v = s.toLongOrNull() ?: throw ValueParseException("Not a whole number")
                checkIntRange(v, type)
                v.toULong()
            }
            ValueType.U8, ValueType.U16, ValueType.U32, ValueType.U64 -> {
                if (s.startsWith("-")) throw ValueParseException("Unsigned values cannot be negative")
                val v = s.toULongOrNull() ?: throw ValueParseException("Not a whole number")
                checkUIntRange(v, type)
                v
            }
        }
    }

    fun format(bits: ULong, type: ValueType): String = when (type) {
        ValueType.F32 -> floatText(java.lang.Float.intBitsToFloat(bits.toInt()).toDouble())
        ValueType.F64 -> floatText(bitsBitsToDouble(bits))
        ValueType.I8 -> bits.toByte().toString()
        ValueType.U8 -> bits.toUByte().toString()
        ValueType.I16 -> bits.toShort().toString()
        ValueType.U16 -> bits.toUShort().toString()
        ValueType.I32 -> bits.toInt().toString()
        ValueType.U32 -> bits.toUInt().toString()
        ValueType.I64 -> bits.toLong().toString()
        ValueType.U64 -> bits.toString()
    }

    fun readBits(bytes: ByteArray, offset: Int, type: ValueType): ULong {
        var acc = 0uL
        for (i in 0 until type.sizeBytes) {
            acc = acc or ((bytes[offset + i].toUByte().toULong()) shl (8 * i))
        }
        return acc
    }

    fun writeBits(bytes: ByteArray, offset: Int, type: ValueType, bits: ULong) {
        for (i in 0 until type.sizeBytes) {
            bytes[offset + i] = ((bits shr (8 * i)) and 0xFFuL).toByte()
        }
    }

    /** True when [a] matches [b] under exact semantics for [type] (floats may use tolerance). */
    fun exactMatch(a: ULong, b: ULong, type: ValueType, tolerance: Double): Boolean {
        if (type.isFloat) {
            if (a == b) return true
            val da = decodeFloat(a, type)
            val db = decodeFloat(b, type)
            if (da.isNaN() || db.isNaN()) return false
            return kotlin.math.abs(da - db) <= tolerance + tolerance * kotlin.math.abs(db)
        }
        return a == b
    }

    /** Total ordering under [type] semantics. NaN compares as never equal/less/greater (returns null). */
    fun compare(a: ULong, b: ULong, type: ValueType): Int? {
        if (type.isFloat) {
            val da = decodeFloat(a, type)
            val db = decodeFloat(b, type)
            if (da.isNaN() || db.isNaN()) return null
            return da.compareTo(db)
        }
        val ia = decodeInt(a, type)
        val ib = decodeInt(b, type)
        return ia.compareTo(ib)
    }

    fun decodeFloat(bits: ULong, type: ValueType): Double = when (type) {
        ValueType.F32 -> java.lang.Float.intBitsToFloat(bits.toInt()).toDouble()
        else -> bitsBitsToDouble(bits)
    }

    fun decodeInt(bits: ULong, type: ValueType): Long = when {
        type == ValueType.I8 -> bits.toByte().toLong()
        type == ValueType.I16 -> bits.toShort().toLong()
        type == ValueType.I32 -> bits.toInt().toLong()
        type == ValueType.I64 -> bits.toLong()
        type == ValueType.U8 -> bits.toUByte().toLong()
        type == ValueType.U16 -> bits.toUShort().toLong()
        type == ValueType.U32 -> bits.toUInt().toLong()
        else -> throw IllegalArgumentException("U64 has no signed Long representation")
    }

    private fun bitsBitsToDouble(bits: ULong): Double =
        java.lang.Double.longBitsToDouble(bits.toLong())

    private fun floatText(v: Double): String {
        if (v == v.toLong().toDouble() && kotlin.math.abs(v) < 1e15) return v.toLong().toString()
        var s = String.format(java.util.Locale.ROOT, "%.6g", v)
        if (!s.contains('e') && !s.contains('E') && s.contains('.')) {
            s = s.trimEnd('0').trimEnd('.')
        }
        return s
    }

    private fun floatCloseEnough(a: Double, b: Double): Boolean =
        kotlin.math.abs(a - b) <= 1e-6 * kotlin.math.max(1.0, kotlin.math.abs(b))

    private fun checkIntRange(v: Long, type: ValueType) {
        val ok = when (type) {
            ValueType.I8 -> v in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()
            ValueType.I16 -> v in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()
            ValueType.I32 -> v in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
            else -> true
        }
        if (!ok) throw ValueParseException("$v does not fit in ${type.name}")
    }

    private fun checkUIntRange(v: ULong, type: ValueType) {
        val max = when (type) {
            ValueType.U8 -> 0xFFuL
            ValueType.U16 -> 0xFFFFuL
            ValueType.U32 -> 0xFFFF_FFFFuL
            else -> ULong.MAX_VALUE
        }
        if (v > max) throw ValueParseException("$v does not fit in ${type.name}")
    }
}

/** Wire-level error codes crossing the bridge. Kept in sync with core/protocol. */
object WireCode {
    const val OK = 0
    const val STALE_GENERATION = 1
    const val NO_SESSION = 2
    const val BAD_QUERY = 3
    const val UNSUPPORTED = 4
    const val NOT_FOUND = 5
    const val BUSY = 6
    const val DENIED = 7
    const val FAILED = 8
    const val NOT_WRITABLE = 9

    fun message(code: Int): String = when (code) {
        OK -> "OK"
        STALE_GENERATION -> "The session changed. Refresh and try again."
        NO_SESSION -> "No active session. Start one in AetherPlay Space."
        BAD_QUERY -> "The query could not be understood."
        UNSUPPORTED -> "This operation is not supported by the current provider."
        NOT_FOUND -> "Nothing matched that request."
        BUSY -> "Another operation is still running."
        DENIED -> "The calling app is not authorized."
        FAILED -> "The operation failed."
        NOT_WRITABLE -> "That location is read-only."
        else -> "Unknown result ($code)"
    }
}

/** Capability bits exchanged during the bridge handshake. */
object Capability {
    const val SEARCH = 1
    const val REFINE = 2
    const val READ = 4
    const val WRITE = 8
    const val FREEZE = 16
    const val UNDO = 32

    fun mask(vararg caps: Int): Int = caps.fold(0) { m, c -> m or c }
    fun supports(mask: Int, cap: Int): Boolean = mask and cap != 0
    fun describe(mask: Int): List<String> = buildList {
        if (supports(mask, SEARCH)) add("search")
        if (supports(mask, REFINE)) add("refine")
        if (supports(mask, READ)) add("read")
        if (supports(mask, WRITE)) add("write")
        if (supports(mask, FREEZE)) add("freeze")
        if (supports(mask, UNDO)) add("undo")
    }
}

/** Protocol negotiation: equal major is required; minor differences negotiate to the lower. */
object Protocol {
    const val MAJOR = 1
    const val MINOR = 0

    sealed interface Verdict {
        data class Compatible(val effectiveMajor: Int, val effectiveMinor: Int) : Verdict
        data class MajorMismatch(val labMajor: Int, val spaceMajor: Int) : Verdict
    }

    fun negotiate(labMajor: Int, labMinor: Int, spaceMajor: Int, spaceMinor: Int): Verdict =
        if (labMajor != spaceMajor) {
            Verdict.MajorMismatch(labMajor, spaceMajor)
        } else {
            Verdict.Compatible(labMajor, minOf(labMinor, spaceMinor))
        }
}
