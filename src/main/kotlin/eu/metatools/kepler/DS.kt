package eu.metatools.kepler

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure

/**
 * Alias for derivative structure.
 */
typealias DS = DerivativeStructure

/*
 * Addition.
 */

/**
 * Adds a constant to a [DS].
 */
operator fun DS.plus(other: Double): DS =
        add(other)

/**
 * Adds a [DS] to a [DS].
 */
operator fun DS.plus(other: DS): DS =
        add(other)

/**
 * Adds a [DS] to a constant.
 */
operator fun Double.plus(other: DS): DS =
        other.add(this)


/*
 * Subtraction.
 */

/**
 * Subtracts a constant from a [DS].
 */
operator fun DS.minus(other: Double): DS =
        subtract(other)

/**
 * Subtracts a [DS] to a [DS].
 */
operator fun DS.minus(other: DS): DS =
        subtract(other)

/**
 * Subtracts a [DS] from a constant.
 */
operator fun Double.minus(other: DS): DS =
        other.createConstant(this).subtract(other)

/*
 * Multiplication.
 */

/**
 * Multiplies a [DS] with a constant integer.
 */
operator fun DS.times(other: Int): DS =
        multiply(other)

/**
 * Multiplies a [DS] with a constant.
 */
operator fun DS.times(other: Double): DS =
        multiply(other)

/**
 * Multiplies a [DS] with a [DS].
 */
operator fun DS.times(other: DS): DS =
        multiply(other)

/**
 * Multiplies a constant integer with a [DS].
 */
operator fun Int.times(other: DS): DS =
        other.multiply(this)

/**
 * Multiplies a constant with a [DS].
 */
operator fun Double.times(other: DS): DS =
        other.multiply(this)

/*
 * Division.
 */

/**
 * Divides a [DS] by a constant.
 */
operator fun DS.div(other: Double): DS =
        divide(other)

/**
 * Divides a [DS] by a [DS].
 */
operator fun DS.div(other: DS): DS =
        divide(other)

/**
 * Divides a constant by a [DS].
 */
operator fun Double.div(other: DS): DS =
        other.createConstant(this).divide(other)

/*
 * Negation.
 */

/**
 * Negates a [DS].
 */
operator fun DS.unaryMinus(): DS =
        negate()

/*
 * Remainder.
 */

/**
 * Computes the reminder of dividing a [DS] by a constant.
 */
operator fun DS.rem(other: Double): DS =
        remainder(other)

/**
 * Computes the reminder of dividing a [DS] by a [DS].
 */
operator fun DS.rem(other: DS): DS =
        remainder(other)

/**
 * Computes the reminder of dividing a constant by a [DS].
 */
operator fun Double.rem(other: DS): DS =
        other.createConstant(this).remainder(other)

/*
 * Powers.
 */

/**
 * The [DS] squared.
 */
fun DS.squared() = pow(2)

/**
 * The [DS] cubed.
 */
fun DS.cubed() = pow(3)

/**
 * The square root of the [DS] cubed.
 */
fun DS.cubedSqrt() = cubed().sqrt()

