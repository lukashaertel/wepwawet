package eu.metatools.kontor

import eu.metatools.kontor.serialization.DataInputInput
import eu.metatools.kontor.serialization.DataOutputOutput
import eu.metatools.kontor.tools.Prosumer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.jgroups.*
import org.jgroups.Message
import org.jgroups.conf.ConfiguratorFactory
import org.jgroups.conf.ProtocolStackConfigurator
import org.jgroups.util.ByteArrayDataInputStream
import org.jgroups.util.ByteArrayDataOutputStream
import java.io.*
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.jgroups.conf.ProtocolConfiguration
import org.jgroups.protocols.SHARED_LOOPBACK
import org.jgroups.protocols.UDP

/**
 * Provides network interaction as a cluster node. [inbound] will receive all incoming messages, [outbound] will take
 * all outgoing messages. Network status may be queried by calling [members] or by subscribing to [management].
 */
class KontorCluster(
        val charset: Charset = Charsets.UTF_8,
        val stateProsumer: Prosumer<*> = Prosumer.DISCARD,
        val classes: List<KClass<*>>,
        val stateTimeout: Long = 10000,
        val configurator: ProtocolStackConfigurator = DEFAULT_CONFIGURATOR)
    : KontorJGroups<From<Any?, Address>, To<Any?, Address>>, KontorNetworked<Address> {
    constructor(charset: Charset, stateProsumer: Prosumer<*>, vararg classes: KClass<*>) : this(
            charset = charset,
            stateProsumer = stateProsumer,
            classes = listOf(*classes))

    constructor(stateProsumer: Prosumer<*>, vararg classes: KClass<*>) : this(
            stateProsumer = stateProsumer,
            classes = listOf(*classes))

    constructor(charset: Charset, vararg classes: KClass<*>) : this(
            charset = charset,
            classes = listOf(*classes))

    constructor(vararg classes: KClass<*>) : this(
            classes = listOf(*classes))

    companion object {
        /**
         * The default configurator used for the [configurator] property.
         */
        val DEFAULT_CONFIGURATOR = ConfiguratorFactory.getStackConfigurator("local.xml")
    }

    /**
     * Associated to serializer.
     */
    private val serializers = classes.map { it.serializer() }

    /**
     * Associate by the serialized class, retain index for writing to streams.
     */
    private val indication = classes.withIndex().associate { it.value to (it.index to it.value.serializer()) }

    /**
     * Mutable address list for connection maintenance.
     */
    private var mutableMembers = listOf<Address>()

    /**
     * Writes the index in the appropriate size.
     */
    private fun writeId(output: DataOutput, index: Int) =
            if (classes.size <= Byte.MAX_VALUE)
                output.writeByte(index)
            else if (classes.size <= Short.MAX_VALUE)
                output.writeShort(index)
            else
                output.writeInt(index)

    /**
     * Reads the index in the appropriate size.
     */
    private fun readId(input: DataInput) =
            if (classes.size <= Byte.MAX_VALUE)
                input.readByte().toInt()
            else if (classes.size <= Short.MAX_VALUE)
                input.readShort().toInt()
            else
                input.readInt()

    /**
     * Decodes one item from a data stream.
     */
    private fun decode(stream: DataInput): Any? {
        // Read the indication
        val index = readId(stream)
        val item: Any?
        if (index == 0) {
            // If 0, decode is just assigning null
            item = null
        } else {
            // Get serializer from list of known serializers
            val serializer = serializers[index - 1]

            // Decode appropriately
            val decoder = DataInputInput(stream, charset)
            item = decoder.read(serializer)
        }
        return item
    }

    /**
     * Encodes one item into a data stream.
     */
    private fun encode(dataOutput: DataOutput, msg: Any?) {
        // Make stream to write to
        if (msg == null) {
            // Write null indicator
            writeId(dataOutput, 0)
        } else {
            // Get serializer from indication
            val (index, serializer) = indication.getValue(msg::class)
            writeId(dataOutput, index + 1)

            // Do cast to avoid type problems, we checked this before with serializableClass
            @Suppress("UNCHECKED_CAST")
            serializer as KSerializer<Any>

            // Encode properly
            val encoder = DataOutputOutput(dataOutput, charset)
            encoder.write(serializer, msg)
        }
    }

    /**
     * Main JChannel instance, maintaining connections, status, and handlers.
     */
    internal val channel = JChannel(configurator).apply {
        receiver = object : ReceiverAdapter() {
            override fun getState(output: OutputStream) {
                //Encode from provider
                encode(DataOutputStream(output), stateProsumer.get())
            }

            override fun setState(input: InputStream?) {
                // Decode into receiver
                @Suppress("unchecked_cast")
                (stateProsumer as Prosumer<Any?>).set(decode(DataInputStream(input)))
            }

            override fun viewAccepted(view: View) {
                // Make delta set before setting state
                val add = view.members - mutableMembers
                val remove = mutableMembers - view.members
                mutableMembers = view.members

                if (!management.isClosedForSend) {
                    // Bind for job
                    val members = members

                    // Launch send job
                    launch(CommonPool) {
                        for (r in remove)
                            management.send(Disconnected(members, r))

                        for (a in add)
                            management.send(Connected(members, a))
                    }
                }
            }

            override fun receive(msg: Message) = runBlocking {
                // Get a readable stream and decode into receiver channel
                val stream = ByteArrayDataInputStream(msg.rawBuffer, msg.offset, msg.length)
                inbound.send(From(decode(stream), msg.src))
            }
        }
    }

    /**
     * Gets the currently connected members.
     */
    val members get() = mutableMembers


    /**
     * The channel to receive connection updates.
     */
    override val management = Channel<Network<Address>>()

    /**
     * Starts requesting a state, state provision and consumption will be handled by [stateProsumer].
     */
    fun requestState() {
        channel.getState(null, stateTimeout)
    }

    override val inbound = Channel<From<Any?, Address>>()

    override val outbound = actor<To<Any?, Address>>(CommonPool) {
        // Share one stream
        val stream = ByteArrayDataOutputStream()
        // For all incoming messages
        for (msg in channel) {
            // Reset position and encode into stream
            stream.position(0)
            encode(stream, msg.content)

            // Dispatch on target type
            when (msg) {
                is ToOnly<Any?, Address> ->
                    this@KontorCluster.channel.send(msg.to, stream.buffer(), 0, stream.position())
                is ToAll<Any?, Address> ->
                    this@KontorCluster.channel.send(null, stream.buffer(), 0, stream.position())
                is ToAllExcept<Any?, Address> ->
                    for (m in this@KontorCluster.members)
                        if (m != msg.except)
                            this@KontorCluster.channel.send(m, stream.buffer(), 0, stream.position())
            }
        }
    }


    override fun start(clusterName: String) = launch(CommonPool) {
        channel.connect(clusterName)
    }

    override fun stop() = launch(CommonPool) {
        channel.disconnect()
    }

    override fun shutdown() = launch(CommonPool) {
        channel.close()
    }
}