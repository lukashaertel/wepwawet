package eu.metatools.serialization

import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlinx.serialization.ElementValueInput
import kotlinx.serialization.ElementValueOutput
import java.nio.ByteBuffer
import java.nio.CharBuffer

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
 * Element value input from a NIO [ByteBuffer].
 */
class ByteBufferInput(val byteBuffer: ByteBuffer, val charset: Charset = Charsets.UTF_8) : ElementValueInput() {
    private val decoder by lazy { charset.newDecoder() }

    override fun readBooleanValue(): Boolean {
        return byteBuffer.get() == ONE_BYTE
    }

    override fun readByteValue(): Byte {
        return byteBuffer.get()
    }

    override fun readCharValue(): Char {
        return byteBuffer.char
    }

    override fun readDoubleValue(): Double {
        return byteBuffer.double
    }

    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T {
        return java.lang.Enum.valueOf(enumClass.java, readStringValue())
    }

    override fun readFloatValue(): Float {
        return byteBuffer.float
    }

    override fun readIntValue(): Int {
        return byteBuffer.int
    }

    override fun readLongValue(): Long {
        return byteBuffer.long
    }

    override fun readNotNullMark(): Boolean {
        return byteBuffer.get() == NULL_BYTE
    }

    override fun readShortValue(): Short {
        return byteBuffer.short
    }

    override fun readStringValue(): String {
        val size = byteBuffer.int
        val target = CharBuffer.allocate(size)
        decoder.decode(byteBuffer, target, false)
        target.position(0)
        return target.toString()
    }
}


// TODO: Enum ordinals.

/**
 * Element value output from a NIO [ByteBuffer].
 */
class ByteBufferOutput(val byteBuffer: ByteBuffer, val charset: Charset = Charsets.UTF_8) : ElementValueOutput() {
    private val encoder by lazy { charset.newEncoder() }

    override fun writeBooleanValue(value: Boolean) {
        byteBuffer.put(if (value) ONE_BYTE else ZERO_BYTE)
    }

    override fun writeByteValue(value: Byte) {
        byteBuffer.put(value)
    }

    override fun writeCharValue(value: Char) {
        byteBuffer.putChar(value)
    }

    override fun writeDoubleValue(value: Double) {
        byteBuffer.putDouble(value)
    }

    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) {
        writeStringValue(value.name)
    }

    override fun writeFloatValue(value: Float) {
        byteBuffer.putFloat(value)
    }

    override fun writeIntValue(value: Int) {
        byteBuffer.putInt(value)
    }

    override fun writeLongValue(value: Long) {
        byteBuffer.putLong(value)
    }

    override fun writeNotNullMark() {
        byteBuffer.put(ZERO_BYTE)
    }

    override fun writeNullValue() {
        byteBuffer.put(NULL_BYTE)
    }

    override fun writeShortValue(value: Short) {
        byteBuffer.putShort(value)
    }

    override fun writeStringValue(value: String) {
        byteBuffer.putInt(value.length)
        encoder.encode(CharBuffer.wrap(value), byteBuffer, true)
    }
}