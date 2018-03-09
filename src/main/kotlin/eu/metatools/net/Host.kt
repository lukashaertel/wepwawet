package eu.metatools.net

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.experimental.Deferred
import java.util.*
import kotlinx.coroutines.experimental.async


/**
 * Host operations as async methods.
 */
interface AsyncHost {
    fun start(): Deferred<Unit>
    fun bind(): Deferred<Unit>
    fun stop(): Deferred<Unit>
    fun sendToUDP(connectionID: Int, any: Any?): Deferred<Unit>
    fun sendToTCP(connectionID: Int, any: Any?): Deferred<Unit>
    fun sendToAllUDP(any: Any?): Deferred<Unit>
    fun sendToAllTCP(any: Any?): Deferred<Unit>
    fun sendToAllExceptUDP(connectionID: Int, any: Any?): Deferred<Unit>
    fun sendToAllExceptTCP(connectionID: Int, any: Any?): Deferred<Unit>
}

/**
 * Host operations as suspend methods.
 */
interface SuspendedHost {
    suspend fun start()
    suspend fun bind()
    suspend fun stop()
    suspend fun sendToUDP(connectionID: Int, any: Any?)
    suspend fun sendToTCP(connectionID: Int, any: Any?)
    suspend fun sendToAllUDP(any: Any?)
    suspend fun sendToAllTCP(any: Any?)
    suspend fun sendToAllExceptUDP(connectionID: Int, any: Any?)
    suspend fun sendToAllExceptTCP(connectionID: Int, any: Any?)
}

/**
 * A server that uses generic links and automatic configuration.
 */
class Host<C : Any>(val config: Config) : Server(
        config.writeBufferSize,
        config.objectBufferSize,
        Marshalling(config.classMapper, config.charset)) {

    /**
     * Maintains the actual listeners that are registered with the underlying server.
     */
    private val actualListeners = WeakHashMap<LinkListener<C>, Listener>()

    /**
     * Overrides the base method, as Kryo is substituted by Kotlin serialization and method calls will fail due to
     * invalid casts.
     */
    override fun getKryo() =
            null

    /**
     * Returns a new [Link] connection with an empty data value.
     */
    override fun newConnection() =
            Link(null)

    /**
     * Adds a [LinkListener], effectively wrapping the untyped connections in calls to [Link] connections.
     */
    fun addListener(linkListener: LinkListener<C>): Boolean {
        // Listener already added
        if (linkListener in actualListeners)
            return false

        val listener = object : Listener() {
            override fun connected(connection: Connection?) {
                @Suppress("unchecked_cast")
                linkListener.connected(connection as Link<C>)
            }

            override fun disconnected(connection: Connection?) {
                @Suppress("unchecked_cast")
                linkListener.disconnected(connection as Link<C>)
            }

            override fun idle(connection: Connection?) {
                @Suppress("unchecked_cast")
                linkListener.idle(connection as Link<C>)
            }

            override fun received(connection: Connection?, `object`: Any?) {
                @Suppress("unchecked_cast")
                linkListener.received(connection as Link<C>, `object`)
            }
        }

        actualListeners[linkListener] = listener
        addListener(listener)
        return true
    }

    /**
     * Removes a [LinkListener].
     */
    fun removeListener(linkListener: LinkListener<C>): Boolean {
        val listener = actualListeners.remove(linkListener) ?: return false
        removeListener(listener)
        return true
    }

    /**
     * Binds the server using the given configuration.
     */
    fun bind() {
        bind(config.portTcp, config.portUdpOrDefault)
    }

    /**
     * Gets the async versions of the host methods.
     */
    val async: AsyncHost by lazy {
        object : AsyncHost {
            override fun start() = async {
                this@Host.start()
            }

            override fun bind() = async {
                this@Host.bind()
            }

            override fun stop() = async {
                this@Host.stop()
            }

            override fun sendToUDP(connectionID: Int, any: Any?) = async {
                this@Host.sendToUDP(connectionID, any)
            }

            override fun sendToTCP(connectionID: Int, any: Any?) = async {
                this@Host.sendToTCP(connectionID, any)
            }

            override fun sendToAllUDP(any: Any?) = async {
                this@Host.sendToAllUDP(any)
            }

            override fun sendToAllTCP(any: Any?) = async {
                this@Host.sendToAllTCP(any)
            }

            override fun sendToAllExceptUDP(connectionID: Int, any: Any?) = async {
                this@Host.sendToAllExceptUDP(connectionID, any)
            }

            override fun sendToAllExceptTCP(connectionID: Int, any: Any?) = async {
                this@Host.sendToAllExceptTCP(connectionID, any)
            }
        }
    }

    /**
     * Gets the suspend versions of the host methods.
     */
    val suspended: SuspendedHost by lazy {
        object : SuspendedHost {
            override suspend fun start() =
                    async.start().await()

            override suspend fun bind() =
                    async.bind().await()

            override suspend fun stop() =
                    async.stop().await()

            override suspend fun sendToUDP(connectionID: Int, any: Any?) =
                    async.sendToUDP(connectionID, any).await()

            override suspend fun sendToTCP(connectionID: Int, any: Any?) =
                    async.sendToTCP(connectionID, any).await()

            override suspend fun sendToAllUDP(any: Any?) =
                    async.sendToAllUDP(any).await()

            override suspend fun sendToAllTCP(any: Any?) =
                    async.sendToAllTCP(any).await()

            override suspend fun sendToAllExceptUDP(connectionID: Int, any: Any?) =
                    async.sendToAllExceptUDP(connectionID, any).await()

            override suspend fun sendToAllExceptTCP(connectionID: Int, any: Any?) =
                    async.sendToAllExceptTCP(connectionID, any).await()
        }
    }
}
