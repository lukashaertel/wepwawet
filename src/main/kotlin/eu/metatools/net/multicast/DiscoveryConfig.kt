package eu.metatools.net.multicast

import java.nio.charset.Charset
import kotlin.reflect.KClass

/**
 * Discovery settings.
 * @property port The port to use for discovery.
 * @property group The group IP to use.
 * @property receiveTimeout The time to wait for a package before timeout.
 * @property ttl The time a discovery is seen as alive.
 * @property intraDelay The time to wait between sending a size declaration and the actual object.
 * @property interDelay The time to wait between sending a discovery message.
 * @property charset The charset to use for strings in serialization.
 * @property initialCapacity The initial capacity to read data into.
 * @property resizeMargin The margin to add to the byte array to detect truncation and cover larger objects.
 * @property portServer The port that the server datagram socket uses. This port is never used but if overlaps with
 * other ports occur, this can be remapped. Defaults to [port] + 1
 */
data class DiscoveryConfig<T : Any>(
        val port: Int,
        val group: String,
        val announceType: KClass<T>,
        val receiveTimeout: Int = 500,
        val ttl: Int = 3000,
        val intraDelay: Int = 500,
        val interDelay: Int = 1500,
        val charset: Charset = Charsets.UTF_8,
        val initialCapacity: Int = 512,
        val resizeMargin: Int = 64,
        val portServer: Int = port + 1)