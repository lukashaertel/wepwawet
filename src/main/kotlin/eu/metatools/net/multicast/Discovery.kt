package eu.metatools.net.multicast

import java.net.InetSocketAddress


/**
 * Discovery message.
 */
sealed class Discovery<T> {
    /**
     * The source address.
     */
    abstract val from: InetSocketAddress

    /**
     * The source data.
     */
    abstract val data: T
}

/**
 * Message on server first appearing to the discovery.
 */
data class Appeared<T>(override val from: InetSocketAddress, override val data: T) : Discovery<T>()

/**
 * Message on server changing the data.
 */
data class Changed<T>(override val from: InetSocketAddress, override val data: T, val before: T) : Discovery<T>()

/**
 * Message on server no longer discoverable.
 */
data class Disappeared<T>(override val from: InetSocketAddress, override val data: T) : Discovery<T>()