package eu.metatools.kepler.dgl

import eu.metatools.kepler.Vec

/**
 * Iterates the vector components.
 */
fun Vec.asIterable() = listOf(x, y)

/**
 * Vector component subscript.
 */
val Vec.xx get() = Vec(x, x)

/**
 * Vector component subscript.
 */
val Vec.xy get() = Vec(x, y)

/**
 * Vector component subscript.
 */
val Vec.yx get() = Vec(y, x)

/**
 * Vector component subscript.
 */
val Vec.yy get() = Vec(y, y)

/**
 * Sets some doubles in the array.
 */
operator fun DoubleArray.set(positions: IntRange, values: DoubleArray) =
        (positions zip values.asIterable()).forEach { (i, v) -> set(i, v) }

/**
 * Sets some doubles in the array from a vector.
 */
operator fun DoubleArray.set(positions: IntRange, values: Vec) =
        (positions zip values.asIterable()).forEach { (i, v) -> set(i, v) }