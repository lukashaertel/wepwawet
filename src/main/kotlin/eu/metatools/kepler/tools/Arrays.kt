package eu.metatools.kepler.tools


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
 * Safe access [get] with wrap-around.
 */
fun CharArray.getIn(i: Int) = get(((i % size) + size) % size)

/**
 * Safe access [get] with wrap-around.
 */
fun ByteArray.getIn(i: Int) = get(((i % size) + size) % size)

/**
 * Safe access [get] with wrap-around.
 */
fun ShortArray.getIn(i: Int) = get(((i % size) + size) % size)

/**
 * Safe access [get] with wrap-around.
 */
fun IntArray.getIn(i: Int) = get(((i % size) + size) % size)

/**
 * Safe access [get] with wrap-around.
 */
fun LongArray.getIn(i: Int) = get(((i % size) + size) % size)

/**
 * Safe access [get] with wrap-around.
 */
fun FloatArray.getIn(i: Int) = get(((i % size) + size) % size)

/**
 * Safe access [get] with wrap-around.
 */
fun DoubleArray.getIn(i: Int) = get(((i % size) + size) % size)

/**
 * Safe access [get] with wrap-around.
 */
fun <T> Array<T>.getIn(i: Int) = get(((i % size) + size) % size)