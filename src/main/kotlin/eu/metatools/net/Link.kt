package eu.metatools.net

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener


/**
 * A generic link, can store data.
 */
class Link<C : Any>(var data: C?) : Connection()

/**
 * A listener for typed links.
 */
interface LinkListener<C : Any> {
    /**
     * Called on connection.
     */
    fun connected(connection: Link<C>) {
    }

    /**
     * Called on disconnection.
     */
    fun disconnected(connection: Link<C>) {
    }

    /**
     * Called on idle.
     */
    fun idle(connection: Link<C>) {
    }

    /**
     * Called on receive.
     */
    fun received(connection: Link<C>, any: Any?) {
    }
}

/**
 * Builder methods for [LinkListener].
 */
interface LinkListenerBuilder<C : Any> {

    /**
     * Called on connection.
     */
    fun connected(handler: (Link<C>) -> Unit)

    /**
     * Called on disconnection.
     */
    fun disconnected(handler: (Link<C>) -> Unit)

    /**
     * Called on idle.
     */
    fun idle(handler: (Link<C>) -> Unit)

    /**
     * Called on receive.
     */
    fun received(handler: (Link<C>, Any?) -> Unit)
}


/**
 * Builds a link listener.
 */
inline fun <reified C : Any> linkListener(builder: LinkListenerBuilder<C>.() -> Unit): LinkListener<C> {
    // Stores for the actual methods used.
    var onConnected: (Link<C>) -> Unit = {}
    var onDisconnected: (Link<C>) -> Unit = {}
    var onIdle: (Link<C>) -> Unit = {}
    var onReceived: (Link<C>, Any?) -> Unit = { _, _ -> }

    // Implementation of the builder.
    val target = object : LinkListenerBuilder<C> {
        override fun connected(handler: (Link<C>) -> Unit) {
            onConnected = handler
        }

        override fun disconnected(handler: (Link<C>) -> Unit) {
            onDisconnected = handler
        }

        override fun idle(handler: (Link<C>) -> Unit) {
            onIdle = handler
        }

        override fun received(handler: (Link<C>, Any?) -> Unit) {
            onReceived = handler
        }
    }

    // Build to implementation.
    builder(target)

    // Return the new interface.
    return object : LinkListener<C> {
        override fun connected(connection: Link<C>) {
            onConnected(connection)
        }

        override fun disconnected(connection: Link<C>) {
            onDisconnected(connection)
        }

        override fun idle(connection: Link<C>) {
            onIdle(connection)
        }

        override fun received(connection: Link<C>, any: Any?) {
            onReceived(connection, any)
        }
    }
}

/**
 * Builder methods for [Listener].
 */
interface ListenerBuilder {

    /**
     * Called on connection.
     */
    fun connected(handler: (Connection) -> Unit)

    /**
     * Called on disconnection.
     */
    fun disconnected(handler: (Connection) -> Unit)

    /**
     * Called on idle.
     */
    fun idle(handler: (Connection) -> Unit)

    /**
     * Called on receive.
     */
    fun received(handler: (Connection, Any?) -> Unit)
}


/**
 * Builds a listener.
 */
inline fun listener(builder: ListenerBuilder.() -> Unit): Listener {
    // Stores for the actual methods used.
    var onConnected: (Connection) -> Unit = {}
    var onDisconnected: (Connection) -> Unit = {}
    var onIdle: (Connection) -> Unit = {}
    var onReceived: (Connection, Any?) -> Unit = { _, _ -> }

    // Implementation of the builder.
    val target = object : ListenerBuilder {
        override fun connected(handler: (Connection) -> Unit) {
            onConnected = handler
        }

        override fun disconnected(handler: (Connection) -> Unit) {
            onDisconnected = handler
        }

        override fun idle(handler: (Connection) -> Unit) {
            onIdle = handler
        }

        override fun received(handler: (Connection, Any?) -> Unit) {
            onReceived = handler
        }
    }

    // Build to implementation.
    builder(target)

    // Return the new interface.
    return object : Listener() {
        override fun connected(connection: Connection) {
            onConnected(connection)
        }

        override fun disconnected(connection: Connection) {
            onDisconnected(connection)
        }

        override fun idle(connection: Connection) {
            onIdle(connection)
        }

        override fun received(connection: Connection, any: Any?) {
            onReceived(connection, any)
        }
    }
}
