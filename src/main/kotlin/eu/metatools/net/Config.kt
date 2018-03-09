package eu.metatools.net

import java.nio.charset.Charset


/**
 * The default port used for TCP.
 */
val defaultPortTcp = 54555

/**
 * Common configuration for [Host] and [Peer].
 * @property portTcp The TCP port.
 * @property portUdp The UDP port. If null is given, the TCP port is decremented by 1.
 * @property classMapper The class mapper for serialization.
 * @property charset The charset to use for strings.
 * @property writeBufferSize The write buffer size for the underlying server or client.
 * @property objectBufferSize The object buffer size for the underlying server or client.
 * @property connectTimeout The time after which connection is deemed failed.
 * @property discoveryTimeout The timeout for server discovery.
 */
data class Config(
        val classMapper: ClassMapper,
        val portTcp: Int = defaultPortTcp,
        val portUdp: Int? = null,
        val charset: Charset = Charsets.UTF_8,
        val writeBufferSize: Int = 16384,
        val objectBufferSize: Int = 2048,
        val connectTimeout: Int = 5000,
        val discoveryTimeout: Int = 5000)

/**
 * Gets the UDP port or the default.
 */
val Config.portUdpOrDefault get() = portUdp ?: (portTcp - 1)