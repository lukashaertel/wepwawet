package eu.metatools.serialization

import kotlinx.serialization.ElementValueInput
import kotlinx.serialization.ElementValueOutput
import java.io.DataInput
import java.io.DataOutput
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import kotlin.reflect.KClass

/**
 * Mark for zero-value bytes.
 */
private val ZERO_BYTE: Byte = 0

/**
 * Mark for one-value bytes.
 */
private val ONE_BYTE: Byte = 1

/**
 * Mark for null-value bytes.
 */
private val NULL_BYTE: Byte = -128

/**
 * Element value input from a [DataInput].
 */
class DataInputInput(val dataInput: DataInput, val charset: Charset = Charsets.UTF_8) : ElementValueInput() {
    private val decoder by lazy { charset.newDecoder() }

    override fun readBooleanValue(): Boolean {
        return dataInput.readByte() == ONE_BYTE
    }

    override fun readByteValue(): Byte {
        return dataInput.readByte()
    }

    override fun readCharValue(): Char {
        return dataInput.readChar()
    }

    override fun readDoubleValue(): Double {
        return dataInput.readDouble()
    }

    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T {
        return java.lang.Enum.valueOf(enumClass.java, readStringValue())
    }

    override fun readFloatValue(): Float {
        return dataInput.readFloat()
    }

    override fun readIntValue(): Int {
        return dataInput.readInt()
    }

    override fun readLongValue(): Long {
        return dataInput.readLong()
    }

    override fun readNotNullMark(): Boolean {
        return dataInput.readByte() == ZERO_BYTE
    }

    override fun readShortValue(): Short {
        return dataInput.readShort()
    }

    override fun readStringValue(): String {
        val size = dataInput.readInt()
        val source = ByteArray(size)
        dataInput.readFully(source, 0, size)

        val target = CharBuffer.allocate(size)
        decoder.decode(ByteBuffer.wrap(source), target, false)
        target.position(0)
        return target.toString()
    }
}


// TODO: Enum ordinals.

/**
 * Element value output from a [DataOutput].
 */
class DataOutputOutput(val dataOutput: DataOutput, val charset: Charset = Charsets.UTF_8) : ElementValueOutput() {
    private val encoder by lazy { charset.newEncoder() }

    override fun writeBooleanValue(value: Boolean) {
        dataOutput.writeByte(if (value) ONE_BYTE.toInt() else ZERO_BYTE.toInt())
    }

    override fun writeByteValue(value: Byte) {
        dataOutput.writeByte(value.toInt())
    }

    override fun writeCharValue(value: Char) {
        dataOutput.writeChar(value.toInt())
    }

    override fun writeDoubleValue(value: Double) {
        dataOutput.writeDouble(value)
    }

    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) {
        writeStringValue(value.name)
    }

    override fun writeFloatValue(value: Float) {
        dataOutput.writeFloat(value)
    }

    override fun writeIntValue(value: Int) {
        dataOutput.writeInt(value)
    }

    override fun writeLongValue(value: Long) {
        dataOutput.writeLong(value)
    }

    override fun writeNotNullMark() {
        dataOutput.writeByte(ZERO_BYTE.toInt())
    }

    override fun writeNullValue() {
        dataOutput.writeByte(NULL_BYTE.toInt())
    }

    override fun writeShortValue(value: Short) {
        dataOutput.writeShort(value.toInt())
    }

    override fun writeStringValue(value: String) {
        dataOutput.writeInt(value.length)
        val target = encoder.encode(CharBuffer.wrap(value))
        dataOutput.write(target.array(), 0, target.limit())
    }
}