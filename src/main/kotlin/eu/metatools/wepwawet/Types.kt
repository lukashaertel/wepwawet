package eu.metatools.wepwawet

import com.google.common.collect.ComparisonChain
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Type of timestep.
 */
typealias Timestep = Long

fun Int.toTime() = toByte()
fun Long.toTime() = toByte()

/**
 * Type of intra-timestep.
 */
typealias Inner = Short

fun Int.toInner() = toShort()
fun Long.toInner() = toShort()

/**
 * Method identity.
 */
typealias Method = Byte

fun Int.toMethod() = toByte()
fun Long.toMethod() = toByte()

/**
 * Author type.
 */
@Serializable
data class Author(val msb: Long, val lsb: Long) : Comparable<Author> {
    companion object {
        val MIN_VALUE = Author(Long.MIN_VALUE, Long.MIN_VALUE)
        val MAX_VALUE = Author(Long.MAX_VALUE, Long.MAX_VALUE)

        fun random() = fromUUID(UUID.randomUUID())

        fun fromUUID(uuid: UUID) =
                Author(uuid.mostSignificantBits, uuid.leastSignificantBits)
    }

    fun toUUID() = UUID(msb, lsb)

    override fun compareTo(other: Author) = ComparisonChain.start()
            .compare(msb, other.msb)
            .compare(lsb, other.lsb)
            .result()
}