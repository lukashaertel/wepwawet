package eu.metatools.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryonet.Client
import eu.metatools.common.ReceiveChannelWith
import eu.metatools.common.doublesTo
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Deferred
import java.net.InetAddress

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.CoroutineContext


/**
 * Peer operations as async methods.
 */
interface AsyncPeer {
    fun start(): Deferred<Unit>
    fun stop(): Deferred<Unit>
    fun connect(inetAddress: InetAddress): Deferred<Unit>
    fun discover(): Deferred<List<InetAddress>>
    fun sendUDP(any: Any?): Deferred<Int>
    fun sendTCP(any: Any?): Deferred<Int>
}

/**
 * Peer operations as suspend methods.
 */
interface SuspendedPeer {
    suspend fun start()
    suspend fun stop()
    suspend fun connect(inetAddress: InetAddress)
    suspend fun discover(): List<InetAddress>
    suspend fun sendUDP(any: Any?): Int
    suspend fun sendTCP(any: Any?): Int
}


/**
 * A client that uses automatic configuration. Unlike [Host], connections are not typed, as there is only one connection
 * and data can be maintained elsewhere.
 */
class Peer(val config: Config) : Client(
        config.writeBufferSize,
        config.objectBufferSize,
        Marshalling(config.classMapper, config.charset)) {
    /**
     * Overrides the base method, as Kryo is substituted by Kotlin serialization and method calls will fail due to
     * invalid casts.
     */
    override fun getKryo() =
            null

    /**
     * Discovers servers on the given port.
     */
    fun discover(): List<InetAddress> =
            discoverHosts(config.portUdpOrDefault, config.discoveryTimeout)

    /**
     * Connects to the [InetAddress] using the configured ports.
     */
    fun connect(inetAddress: InetAddress) {
        connect(config.connectTimeout, inetAddress, config.portTcp, config.portUdpOrDefault)
    }


    /**
     * Gets the async versions of the peer methods.
     */
    val async: AsyncPeer by lazy {
        object : AsyncPeer {
            override fun start() = async {
                this@Peer.start()
            }

            override fun stop() = async {
                this@Peer.stop()
            }

            override fun connect(inetAddress: InetAddress) = async {
                this@Peer.connect(inetAddress)
            }

            override fun discover() = async {
                this@Peer.discover()
            }

            override fun sendUDP(any: Any?) = async {
                this@Peer.sendUDP(any)
            }

            override fun sendTCP(any: Any?) = async {
                this@Peer.sendTCP(any)
            }
        }
    }

    /**
     * Gets the suspend versions of the peer methods.
     */
    val suspended: SuspendedPeer by lazy {
        object : SuspendedPeer {
            override suspend fun start() =
                    async.start().await()

            override suspend fun stop() =
                    async.stop().await()

            override suspend fun connect(inetAddress: InetAddress) =
                    async.connect(inetAddress).await()

            override suspend fun discover() =
                    async.discover().await()

            override suspend fun sendUDP(any: Any?) =
                    async.sendUDP(any).await()

            override suspend fun sendTCP(any: Any?) =
                    async.sendTCP(any).await()
        }
    }
}

/**
 * Does one round of discovery sending all found results to the channel.
 */
fun Peer.produceDiscover(context: CoroutineContext = DefaultDispatcher, minRate: Int = 125) =
        produce<InetAddress>(context) {
            // Create a concurrent set of discovered addresses.
            val found = ConcurrentHashMap<InetAddress, InetAddress>()

            // Start the discovery jobs as children of the producer for all timeouts.
            for (rate in minRate doublesTo config.discoveryTimeout)
                launch(coroutineContext) {
                    for (discovered in discoverHosts(config.portUdpOrDefault, rate))
                        if (found.put(discovered, discovered) == null)
                            send(discovered)
                }
        }

/**
 * Change in browse state.
 */
sealed class BrowseResult {
    /**
     * The associated address.
     */
    abstract val inetAddress: InetAddress
}

/**
 * Indicates a server being newly discovered.
 */
data class BrowseAdd(override val inetAddress: InetAddress) : BrowseResult()

/**
 * Indicates a server dropping from results.
 */
data class BrowseRemove(override val inetAddress: InetAddress) : BrowseResult()


typealias BrowseChannel = ReceiveChannelWith<Set<InetAddress>, BrowseResult>

/**
 * Produces browse results unless stopped. Also provides all current results via [ReceiveChannelWith.data].
 */
fun Peer.browse(context: CoroutineContext = DefaultDispatcher, minRate: Int = 125): BrowseChannel {
    // Set of currently found servers.
    var found = setOf<InetAddress>()

    // Produce changes while also providing current results.
    return BrowseChannel({ found }, produce(context) {
        // While producer is active.
        while (isActive) {
            // Collect all results into a set.
            val current = produceDiscover(coroutineContext, minRate).toSet()

            // Handle changes.
            for (a in (found subtract current))
                send(BrowseRemove(a))
            for (a in (current subtract found))
                send(BrowseAdd(a))

            // Transfer state.
            found = current
        }
    })
}