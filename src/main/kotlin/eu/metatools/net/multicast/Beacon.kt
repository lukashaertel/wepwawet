package eu.metatools.net.multicast

import eu.metatools.serialization.ByteBufferInput
import eu.metatools.serialization.ByteBufferOutput
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.serializer
import java.net.*
import java.nio.ByteBuffer

/**
 * Starts a job handling discovery requests.
 * @param config The config to use for discovery.
 * @param data The discovery info generator.
 */
fun <T : Any> launchBeacon(config: DiscoveryConfig<T>, data: () -> T) = launch {
    // Obtain serializer
    val serializer = config.announceType.serializer()

    // Initialize a datagram socket.
    val socket = DatagramSocket(config.port - 1)
    val group = InetAddress.getByName(config.group)

    try {
        while (isActive) {
            // Serialize new data object.
            val buffer = ByteBuffer.allocate(config.initialCapacity)
            buffer.put(Byte.MAX_VALUE)
            serializer.save(ByteBufferOutput(buffer, config.charset), data())


            // Serialize resize object, increase size by margin so that truncation can be detected.
            val resize = ByteBuffer.allocate(5)
            resize.put(Byte.MIN_VALUE)
            resize.putInt(buffer.position() + config.resizeMargin)

            // Send resize and wait a bit.
            socket.send(DatagramPacket(resize.array(), resize.arrayOffset(), resize.position(), group, config.port))
            delay(config.intraDelay)

            // Send data and wait.
            socket.send(DatagramPacket(buffer.array(), buffer.arrayOffset(), buffer.position(), group, config.port))
            delay(config.interDelay)
        }
    } finally {
        // Close the socket.
        socket.close()
    }
}

/**
 * Starts a job discovery active hosts from [launchBeacon].
 * @param config The config to use for discovery.
 * @return Returns a channel that produces
 */
fun <T : Any> produceBeacons(config: DiscoveryConfig<T>) = produce<Discovery<T>> {
    // Obtain serializer
    val serializer = config.announceType.serializer()

    // Connect a multicast socket.
    val socket = MulticastSocket(config.port)
    socket.soTimeout = config.receiveTimeout
    val group = InetAddress.getByName(config.group)
    socket.joinGroup(group)

    // Initialize byte array.
    var bytes = ByteArray(config.initialCapacity)

    // Initialize the backing.
    val state = mutableMapOf<InetSocketAddress, Pair<Long, T>>()

    try {
        while (isActive) {
            // Receive the datagram or null if timed out.
            val packet = try {
                DatagramPacket(bytes, bytes.size).apply { socket.receive(this) }
            } catch (_: SocketTimeoutException) {
                null
            }


            // Get the receive time.
            val now = System.currentTimeMillis()

            // Check if receiving did not time out.
            if (packet != null) {
                // Wrap the datagram value.
                val buffer = ByteBuffer.wrap(bytes, packet.offset, packet.length)

                when (buffer.get()) {
                // Message is resize request.
                    Byte.MIN_VALUE -> {
                        // Get new size, resize byte array if request is bigger.
                        val size = buffer.int
                        if (size > bytes.size)
                            bytes = ByteArray(size)
                    }

                // Message is data.
                    Byte.MAX_VALUE -> {
                        // Check if length is equal to received data, then, truncation occured.
                        if (packet.length < bytes.size) {

                            // Compose origin and receive the data.
                            val from = InetSocketAddress(packet.address, packet.port)
                            val data = serializer.load(ByteBufferInput(buffer, config.charset))


                            // Try to find old information.
                            val old = state[from]

                            // Check if old data was present.
                            if (old == null) {
                                // If not yet mapped, send appeared.
                                send(Appeared(from, data))
                            } else if (old.second != data) {
                                // If already appeared, send a change.
                                send(Changed(from, data, old.second))
                            }

                            // Reassign time.
                            state[from] = now to data
                        }
                    }
                }
            }

            // Always dispose of entries that did not renew their data via mutable iteration.
            val iterator = state.entries.iterator()
            while (iterator.hasNext()) {
                // Get the current item.
                val (k, v) = iterator.next()

                // If exceeded TTL, remove and send disappeared.
                if (v.first + config.ttl < now) {
                    send(Disappeared(k, v.second))
                    iterator.remove()
                }
            }
        }
    } finally {
        socket.leaveGroup(group)
        socket.close()
    }
}