package eu.metatools.kepler.math


/**
 * Floating point range of doubles.
 */
typealias DoubleRange = ClosedFloatingPointRange<Double>

/**
 * Empty range that never valuated.
 */
val never = Double.NaN..Double.NaN

/**
 * Infinite range.
 */
val always = Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY


/**
 * True if double range is [never].
 */
fun DoubleRange.isNever() =
        start.isNaN() && endInclusive.isNaN()

/**
 * True if double range is non-empty infinite range.
 */
fun DoubleRange.isAlways() =
        start.isInfinite() && endInclusive.isInfinite() && start < endInclusive
