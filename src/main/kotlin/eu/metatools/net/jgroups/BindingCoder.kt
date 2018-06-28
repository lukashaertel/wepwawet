package eu.metatools.net.jgroups

import eu.metatools.common.asSafe
import eu.metatools.net.Binding
import eu.metatools.net.ClassesMismatching
import eu.metatools.net.Size
import eu.metatools.net.TypeNotRegistered
import eu.metatools.serialization.DataInputInput
import eu.metatools.serialization.DataOutputOutput
import kotlinx.serialization.KSerializer
import org.jgroups.Message
import org.jgroups.util.ByteArrayDataInputStream
import org.jgroups.util.ByteArrayDataOutputStream
import java.io.DataInput
import java.io.DataOutput
import java.nio.charset.Charset

/**
 * Message de-/encoder for [Message]s. Uses a list of defined bindings for multiplexing.
 * @property bindings The bindings that can be serialized and de-serialized.
 * @property charset The charset to use for string encoding.
 * @property initialCapacity The initial capacity for the encoder stream.
 */
data class BindingCoder(
        val bindings: List<Binding<*>>,
        val charset: Charset = Charsets.UTF_8,
        val initialCapacity: Int = 256) {
    companion object {
        /**
         * BindingCodecFactory-pack for primitive types.
         */
        val PRIMITIVE = BindingCoder(Binding.PRIMITIVE_BINDINGS)
    }

    /**
     * The size of the bindings.
     */
    val bindingsSize = Size.of(bindings.size)

    /**
     * Indices of the bindings.
     */
    val ids = bindings.withIndex().associate { (i, v) -> v.kClass to i }

    /**
     * Decodes a value from a received message.
     */
    fun decode(source: Message): Any? {
        return decode(ByteArrayDataInputStream(source.rawBuffer, source.offset, source.length))
    }

    /**
     * Decodes a value from a data input.
     */
    fun decode(stream: DataInput): Any? {
        val id: Int = when (bindingsSize) {
            Size.BYTE -> stream.readByte().toInt()
            Size.SHORT -> stream.readShort().toInt()
            Size.INT -> stream.readInt()
        }

        // If class ID is minus one, the serialized value was null.
        if (id == -1) {
            return null
        }

        // Verify class ID.
        if (id < 0 || id >= bindings.size)
            throw ClassesMismatching(bindings, id)

        return bindings[id].serializer.load(DataInputInput(stream, charset))
    }


    /**
     * Encodes a value into a target message.
     */
    fun encode(target: Message, message: Any?) {
        val stream = ByteArrayDataOutputStream(initialCapacity);
        encode(stream, message)
        target.setBuffer(stream.buffer(), 0, stream.position())

    }

    fun encode(target: DataOutput, message: Any?) {
        // If message to encode is null, perform special serialization.
        if (message == null) {
            // Write minus one.
            when (bindingsSize) {
                Size.BYTE -> target.write(-1)
                Size.SHORT -> target.writeShort(-1)
                Size.INT -> target.writeInt(-1)
            }

            return
        }

        // Determine ID of the message, throw an exception if type not registered.
        val id = ids[message::class]
        if (id == null)
            throw TypeNotRegistered(bindings, message)

        when (bindingsSize) {
            Size.BYTE -> target.write(id)
            Size.SHORT -> target.writeShort(id)
            Size.INT -> target.writeInt(id)
        }

        // Save the object.
        bindings[id].serializer.asSafe<KSerializer<Any>>().save(DataOutputOutput(target, charset), message)
    }


    /**
     * Composes both coders based on the upper boundaries and all bindings.
     */
    infix fun and(other: BindingCoder): BindingCoder {
        // Create a new definition based on upper boundaries.
        val newClasses = bindings + other.bindings
        val newCharset = maxOf(charset, other.charset, Comparator { a, b ->
            a.newEncoder().averageBytesPerChar().compareTo(b.newEncoder().averageBytesPerChar())
        })
        val newInitialCapacity = maxOf(initialCapacity, other.initialCapacity)

        // Return new codec.
        return BindingCoder(newClasses, newCharset, newInitialCapacity)
    }
}