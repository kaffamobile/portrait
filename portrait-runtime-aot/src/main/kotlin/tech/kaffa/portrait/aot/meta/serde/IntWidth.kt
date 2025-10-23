package tech.kaffa.portrait.aot.meta.serde

import java.io.DataInputStream
import java.io.DataOutputStream

enum class IntWidth(val id: Int, private val maxValue: Int) {
    U8(0, 0xFF),
    U16(1, 0xFFFF),
    U24(2, 0xFFFFFF),
    U32(3, Int.MAX_VALUE);

    fun write(output: DataOutputStream, value: Int) {
        require(value >= 0) { "Negative values are not supported: $value" }
        require(value <= maxValue) { "Value $value exceeds max of $maxValue for $name" }
        when (this) {
            U8 -> output.writeByte(value)
            U16 -> output.writeShort(value)
            U24 -> {
                output.writeByte((value ushr 16) and 0xFF)
                output.writeByte((value ushr 8) and 0xFF)
                output.writeByte(value and 0xFF)
            }
            U32 -> output.writeInt(value)
        }
    }

    fun read(input: DataInputStream): Int {
        val value = when (this) {
            U8 -> input.readUnsignedByte()
            U16 -> input.readUnsignedShort()
            U24 -> {
                val b1 = input.readUnsignedByte()
                val b2 = input.readUnsignedByte()
                val b3 = input.readUnsignedByte()
                (b1 shl 16) or (b2 shl 8) or b3
            }
            U32 -> input.readInt()
        }
        require(value >= 0) { "Negative values are not supported: $value" }
        require(value <= maxValue) { "Value $value exceeds max for $name" }
        return value
    }

    companion object {
        fun forUpperBound(maxValue: Int): IntWidth {
            if (maxValue <= 0) return U8
            if (maxValue <= U8.maxValue) return U8
            if (maxValue <= U16.maxValue) return U16
            if (maxValue <= U24.maxValue) return U24
            return U32
        }

        fun fromId(id: Int): IntWidth {
            return entries.firstOrNull { it.id == id }
                ?: error("Unsupported integer width id: $id")
        }
    }
}
