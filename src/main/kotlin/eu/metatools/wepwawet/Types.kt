package eu.metatools.wepwawet

import com.google.common.collect.ComparisonChain
import kotlinx.serialization.Serializable
import java.util.*

typealias Time = Long

fun Int.toTime() = toByte()
fun Long.toTime() = toByte()

typealias Inner = Short

fun Int.toInner() = toShort()
fun Long.toInner() = toShort()

typealias Method = Byte

fun Int.toMethod() = toByte()
fun Long.toMethod() = toByte()

@Serializable
data class Author(val msb: Long, val lsb: Long) : Comparable<Author> {
    companion object {
        val MIN_VALUE= Author(Long.MIN_VALUE, Long.MIN_VALUE)
        val MAX_VALUE= Author(Long.MAX_VALUE, Long.MAX_VALUE)

        fun random() = UUID.randomUUID().let {
            Author(it.mostSignificantBits, it.leastSignificantBits)
        }
    }

    override fun compareTo(other: Author) = ComparisonChain.start()
            .compare(msb, other.msb)
            .compare(lsb, other.lsb)
            .result()
}