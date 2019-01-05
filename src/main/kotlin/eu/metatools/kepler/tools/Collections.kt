package eu.metatools.kepler.tools

import eu.metatools.kepler.math.lerp
import java.util.*


/**
 * Sets some doubles in the array.
 */
operator fun DoubleArray.set(positions: IntRange, values: DoubleArray) =
        (positions zip values.asIterable()).forEach { (i, v) -> set(i, v) }

/**
 * Sets some doubles in the array.
 */
operator fun DoubleArray.set(positions: IntRange, values: Iterable<Double>) =
        (positions zip values).forEach { (i, v) -> set(i, v) }

/**
 * Converts a double array to a vector.
 */
fun DoubleArray.toVec() = Vec(get(0), get(1))

/**
 * Converts a double array to a vector.
 */
fun DoubleArray.toVec(offset: Int) = Vec(get(offset), get(offset + 1))

/**
 * Skips a specific item in a list get based.
 */
fun <E> List<E>.skipItem(n: Int) = object : AbstractList<E>() {
    override fun get(index: Int): E =
            if (index >= n)
                this@skipItem[index + 1]
            else
                this@skipItem[index]

    override val size: Int
        get() =
            if (this@skipItem.size > n)
                this@skipItem.size - 1
            else
                this@skipItem.size

}

const val defaultSampleNumberPerUnit = 32

/**
 * Samples with [defaultSampleNumberPerUnit] points per one unit.
 */
fun ClosedFloatingPointRange<Double>.sample() = sample(((endInclusive - start) * defaultSampleNumberPerUnit).toInt())

/**
 * Samples with n points over all the range.
 */
fun ClosedFloatingPointRange<Double>.sample(n: Int) = (0..n).map { lerp(start, endInclusive, it / n.toDouble()) }