package eu.metatools.serialization

import kotlinx.serialization.ElementValueInput
import kotlinx.serialization.ElementValueOutput
import org.apache.mina.core.buffer.IoBuffer
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
 * Element value input from a MINA [IoBuffer].
 */
class IoBufferInput(val ioBuffer: IoBuffer, val charset: Charset = Charsets.UTF_8) : ElementValueInput() {
    private val decoder by lazy { charset.newDecoder() }

    override fun readBooleanValue(): Boolean {
        return ioBuffer.get() == ONE_BYTE
    }

    override fun readByteValue(): Byte {
        return ioBuffer.get()
    }

    override fun readCharValue(): Char {
        return ioBuffer.char
    }

    override fun readDoubleValue(): Double {
        return ioBuffer.double
    }

    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T {
        return java.lang.Enum.valueOf(enumClass.java, readStringValue())
    }

    override fun readFloatValue(): Float {
        return ioBuffer.float
    }

    override fun readIntValue(): Int {
        return ioBuffer.int
    }

    override fun readLongValue(): Long {
        return ioBuffer.long
    }

    override fun readNotNullMark(): Boolean {
        return ioBuffer.get() == ZERO_BYTE
    }

    override fun readShortValue(): Short {
        return ioBuffer.short
    }

    override fun readStringValue(): String {
        return ioBuffer.getPrefixedString(decoder)
    }
}


// TODO: Enum ordinals.

/**
 * Element value output from a MINA [IoBuffer].
 */
class IoBufferOutput(val ioBuffer: IoBuffer, val charset: Charset = Charsets.UTF_8) : ElementValueOutput() {
    private val encoder by lazy { charset.newEncoder() }

    override fun writeBooleanValue(value: Boolean) {
        ioBuffer.put(if (value) ONE_BYTE else ZERO_BYTE)
    }

    override fun writeByteValue(value: Byte) {
        ioBuffer.put(value)
    }

    override fun writeCharValue(value: Char) {
        ioBuffer.putChar(value)
    }

    override fun writeDoubleValue(value: Double) {
        ioBuffer.putDouble(value)
    }

    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) {
        writeStringValue(value.name)
    }

    override fun writeFloatValue(value: Float) {
        ioBuffer.putFloat(value)
    }

    override fun writeIntValue(value: Int) {
        ioBuffer.putInt(value)
    }

    override fun writeLongValue(value: Long) {
        ioBuffer.putLong(value)
    }

    override fun writeNotNullMark() {
        ioBuffer.put(ZERO_BYTE)
    }

    override fun writeNullValue() {
        ioBuffer.put(NULL_BYTE)
    }

    override fun writeShortValue(value: Short) {
        ioBuffer.putShort(value)
    }

    override fun writeStringValue(value: String) {
        ioBuffer.putPrefixedString(value, encoder)
    }
}