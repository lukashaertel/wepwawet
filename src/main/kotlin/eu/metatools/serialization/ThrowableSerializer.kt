package eu.metatools.serialization

import kotlinx.serialization.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object ThrowableSerializer : KSerializer<Throwable> {
    private const val initialCapacity = 256

    override val serialClassDesc = object : KSerialClassDesc {
        override val kind: KSerialClassKind
            get() = KSerialClassKind.CLASS
        override val name: String
            get() = "kotlin.Throwable"

        override fun getElementIndex(name: String): Int {
            return KInput.UNKNOWN_NAME
        }

        override fun getElementName(index: Int): String {
            throw IllegalArgumentException("Cannot enumerate field of Throwable.")
        }
    }

    override fun load(input: KInput): Throwable {
        val length = input.readIntValue()
        val bytes = ByteArray(length) { input.readByteValue() }

        return ObjectInputStream(ByteArrayInputStream(bytes, 0, length)).readObject() as Throwable
    }

    override fun save(output: KOutput, obj: Throwable) {
        val target = ByteArrayOutputStream(initialCapacity)
        ObjectOutputStream(target).writeObject(obj)
        val bytes = target.toByteArray()
        output.writeIntValue(bytes.size)
        for (byte in bytes)
            output.writeByteValue(byte)
    }

}