package eu.metatools.net

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.KryoSerialization
import com.esotericsoftware.kryonet.Serialization
import eu.metatools.common.objectSerializer
import eu.metatools.serialization.ByteBufferInput
import eu.metatools.serialization.ByteBufferOutput
import kotlinx.serialization.serializer
import java.nio.ByteBuffer
import java.nio.charset.Charset


/**
 * Uses Kotlin serialization and a [ClassMapper] to encode and decode for Kryonet.
 */
class Marshalling(val mapper: ClassMapper, val charset: Charset = Charsets.UTF_8) : Serialization {
    companion object {
        /**
         * Special ID for null values.
         */
        private const val ID_NULL = -1

        /**
         * Special ID for TCP Registration.
         */
        private const val ID_REGISTER_TCP = -2

        /**
         * Special ID for UDP Registration.
         */
        private const val ID_REGISTER_UDP = -3

        /**
         * Special ID for Keep Alive Message.
         */
        private const val ID_KEEP_ALIVE = -4

        /**
         * Special ID for Host discovery.
         */
        private const val ID_DISCOVER_HOST = -5

        /**
         * Special ID for Ping.
         */
        private const val ID_PING = -6

        /**
         * Byte of value 0.
         */
        private const val ZERO_BYTE: Byte = 0

        /**
         * Byte of value 1.
         */
        private const val ONE_BYTE: Byte = 1
    }

    /**
     * Reads the id field based on [ClassMapper.numSize].
     */
    private fun getId(buffer: ByteBuffer): Int =
            when (mapper.numSize) {
                NumSize.BYTE -> buffer.get().toInt()
                NumSize.SHORT -> buffer.short.toInt()
                NumSize.INT -> buffer.int
            }

    /**
     * Writes the id field based on [ClassMapper.numSize].
     */
    private fun putId(buffer: ByteBuffer, int: Int) =
            when (mapper.numSize) {
                NumSize.BYTE -> buffer.put(int.toByte())
                NumSize.SHORT -> buffer.putShort(int.toShort())
                NumSize.INT -> buffer.putInt(int)
            }

    override fun getLengthLength() =
            4

    override fun readLength(buffer: ByteBuffer) =
            buffer.int

    override fun writeLength(buffer: ByteBuffer, length: Int) {
        buffer.putInt(length)
    }

    override fun read(connection: Connection?, buffer: ByteBuffer): Any? {
        // Get the type identity.
        val typeId = getId(buffer)

        // Return appropriately deserialized.
        return when (typeId) {
        // Null values do not call serialization.
            ID_NULL -> null

        // Framework messages are handled differently.
            ID_REGISTER_TCP -> FrameworkMessage.RegisterTCP().apply { connectionID = buffer.int }
            ID_REGISTER_UDP -> FrameworkMessage.RegisterUDP().apply { connectionID = buffer.int }
            ID_KEEP_ALIVE -> FrameworkMessage.KeepAlive()
            ID_DISCOVER_HOST -> FrameworkMessage.DiscoverHost()
            ID_PING -> FrameworkMessage.Ping().apply {
                id = buffer.int
                isReply = buffer.get() == ONE_BYTE
            }

        // Otherwise use mapper.
            else -> mapper[typeId].serializer().load(ByteBufferInput(buffer, charset))
        }
    }

    override fun write(connection: Connection?, buffer: ByteBuffer, value: Any?) {
        when (value) {
        // Null values do not call serialization.
            null -> putId(buffer, ID_NULL)

        // Framework messages are handled differently.
            is FrameworkMessage.RegisterTCP -> {
                putId(buffer, ID_REGISTER_TCP)
                buffer.putInt(value.connectionID)
            }
            is FrameworkMessage.RegisterUDP -> {
                putId(buffer, ID_REGISTER_UDP)
                buffer.putInt(value.connectionID)
            }
            is FrameworkMessage.KeepAlive -> putId(buffer, ID_KEEP_ALIVE)
            is FrameworkMessage.DiscoverHost -> putId(buffer, ID_DISCOVER_HOST)
            is FrameworkMessage.Ping -> {
                putId(buffer, ID_PING)
                buffer.putInt(value.id)
                buffer.put(if (value.isReply) ONE_BYTE else ZERO_BYTE)
            }

        // Otherwise use mapper.
            else -> {
                val typeId = mapper[value]
                putId(buffer, typeId)
                value.objectSerializer().save(ByteBufferOutput(buffer, charset), value)
            }
        }
    }
}