package eu.metatools.net.mina

import eu.metatools.common.asSafe
import eu.metatools.net.*
import eu.metatools.serialization.IoBufferInput
import eu.metatools.serialization.IoBufferOutput
import kotlinx.serialization.KSerializer
import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.session.IoSession
import org.apache.mina.filter.codec.*
import java.nio.charset.Charset

/**
 * Codecs provider, implements codec factory. Uses a list of defined bindings for multiplexing.
 * @property bindings The bindings that can be serialized and de-serialized.
 * @property messageSize The maximum message size as an abstract capacity.
 * @property charset The charset to use for string encoding.
 * @property initialCapacity The initial capacity for the encoder buffer.
 */
data class BindingCodecFactory(
        val bindings: List<Binding<*>>,
        val messageSize: Size = Size.SHORT,
        val charset: Charset = Charsets.UTF_8,
        val initialCapacity: Int = 256) : ProtocolCodecFactory {
    companion object {
        /**
         * BindingCodecFactory-pack for primitive types.
         */
        val PRIMITIVE = BindingCodecFactory(Binding.PRIMITIVE_BINDINGS)
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
     * State of the decoder in the session.
     * @property id The currently loaded ID.
     * @property length The currently loaded length.
     */
    private data class DecoderState(var id: Int = Int.MIN_VALUE, var length: Int = Int.MIN_VALUE) {
        /**
         * True if the ID is set.
         */
        val idSet get() = id != Int.MIN_VALUE

        /**
         * True if the length is set.
         */
        val lengthSet get() = length != Int.MIN_VALUE
    }

    /**
     * Common decoder used by codec.
     */
    private fun createDecoder() = object : CumulativeProtocolDecoder() {
        override fun finishDecode(session: IoSession, out: ProtocolDecoderOutput) {
            session.removeAttribute(DecoderState::class);
        }

        override fun doDecode(session: IoSession, buffer: IoBuffer, out: ProtocolDecoderOutput): Boolean {
            // Get or initialize state.
            val state = session.getAttribute(DecoderState::class) as? DecoderState
                    ?: DecoderState().also {
                        session.setAttribute(DecoderState::class, it)
                    }

            // Id block, if not read yet, try to read if enough bytes are present.
            if (!state.idSet) {
                // Check if there are enough bytes.
                if (buffer.remaining() >= bindingsSize.bytes) {
                    // Get class ID.
                    state.id = when (bindingsSize) {
                        Size.BYTE -> buffer.get().toInt()
                        Size.SHORT -> buffer.short.toInt()
                        Size.INT -> buffer.int
                    }

                    // If class ID is minus one, the serialized value was null.
                    if (state.id == -1) {
                        out.write(null)
                        session.removeAttribute(DecoderState::class);
                        return true
                    }

                    // Verify class ID.
                    if (state.id < 0 || state.id >= bindings.size)
                        throw ClassesMismatching(bindings, state.id)
                } else {
                    // Not enough bytes.
                    return false;
                }
            }

            // Length block, if not yet read, try to read if enough bytes are present.
            if (!state.lengthSet) {
                // Assert that the message length can be read.
                if (buffer.remaining() >= messageSize.bytes) {
                    // Get message length.
                    state.length = when (messageSize) {
                        Size.BYTE -> buffer.get().toInt()
                        Size.SHORT -> buffer.short.toInt()
                        Size.INT -> buffer.int
                    }
                } else {
                    // Not enough bytes yet.
                    return false;
                }
            }

            // Data block, if not read yet, try to read if enough bytes are ready.
            if (state.idSet && state.lengthSet) {
                // Assert enough material present for decoding.
                if (buffer.remaining() >= state.length) {
                    // Decode to output.
                    out.write(bindings[state.id].serializer.load(IoBufferInput(buffer, charset)))

                    // Mark as handled.
                    session.removeAttribute(DecoderState::class);
                    return true
                } else {
                    return false;
                }
            }


            // No exit point reached before, therefore serialization is not complete.
            return false
        }
    }

    /**
     * Common decoder used by codec.
     */
    private fun createEncoder() = object : ProtocolEncoder {
        override fun encode(session: IoSession, message: Any?, out: ProtocolEncoderOutput) {
            // If message to encode is null, perform special serialization.
            if (message == null) {
                // Allocate buffer for the class ID only.
                val buffer = IoBuffer.allocate(bindingsSize.bytes, false)

                // Write minus one.
                when (bindingsSize) {
                    Size.BYTE -> buffer.put(-1)
                    Size.SHORT -> buffer.putShort(-1)
                    Size.INT -> buffer.putInt(-1)
                }

                // Flip and write buffer.
                buffer.flip()
                out.write(buffer)
                return
            }

            // Determine ID of the message, throw an exception if type not registered.
            val id = ids[message::class]
            if (id == null)
                throw TypeNotRegistered(bindings, message)

            // Allocate an automatically expanding buffer to encode the message into.
            val buffer = IoBuffer.allocate(initialCapacity, false)
            buffer.isAutoExpand = true

            // Skip after header
            buffer.skip(bindingsSize.bytes + messageSize.bytes)

            // Save the object.
            bindings[id].serializer.asSafe<KSerializer<Any>>().save(IoBufferOutput(buffer, charset), message)

            // Total length of message is the current position minus the header length.
            val length = buffer.position() - bindingsSize.bytes - messageSize.bytes

            // Flip the buffer, limit is now set to position and position is reset.
            buffer.flip()

            // If length may not be encoded, throw an exception.
            if (length !in messageSize.range)
                throw InsufficientCapacity(messageSize, length)

            // Compose the header of the ID and the length.
            when (bindingsSize) {
                Size.BYTE -> buffer.put(id.toByte())
                Size.SHORT -> buffer.putShort(id.toShort())
                Size.INT -> buffer.putInt(id)
            }

            when (messageSize) {
                Size.BYTE -> buffer.put(length.toByte())
                Size.SHORT -> buffer.putShort(length.toShort())
                Size.INT -> buffer.putInt(length)
            }
            // Reset to beginning and write buffer.
            buffer.position(0)
            out.write(buffer)
        }

        override fun dispose(session: IoSession) = Unit
    }

    override fun getDecoder(session: IoSession) =
            createDecoder()

    override fun getEncoder(session: IoSession) =
            createEncoder()

    /**
     * Composes both codecs based on the upper boundaries and all bindings.
     */
    infix fun and(other: BindingCodecFactory): BindingCodecFactory {
        // Create a new definition based on upper boundaries.
        val newClasses = bindings + other.bindings
        val newMessageSize = maxOf(messageSize, other.messageSize)
        val newCharset = maxOf(charset, other.charset, Comparator { a, b ->
            a.newEncoder().averageBytesPerChar().compareTo(b.newEncoder().averageBytesPerChar())
        })
        val newInitialCapacity = maxOf(initialCapacity, other.initialCapacity)

        // Return new codec.
        return BindingCodecFactory(newClasses, newMessageSize, newCharset, newInitialCapacity)
    }
}