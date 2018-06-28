package eu.metatools.net.mina.bx

import kotlinx.serialization.Serializable
import java.util.*

/**
 * A unique ID that is serializable.
 * @property msb The most significant bits.
 * @property lsb The least significant bits.
 */
@Serializable
data class Id(val msb: Long, val lsb: Long) {
    constructor(uuid: UUID) : this(uuid.mostSignificantBits, uuid.leastSignificantBits)
}