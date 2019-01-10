package eu.metatools.kepler.tools

import eu.metatools.kepler.math.lerp

/**
 * Default rate at which ranges are samples.
 */
const val defaultSampleNumberPerUnit = 32


/**
 * Samples with [defaultSampleNumberPerUnit] points per one unit.
 */
fun ClosedFloatingPointRange<Float>.sampleFloat() = sampleFloat(((endInclusive - start) * defaultSampleNumberPerUnit).toInt())

/**
 * Samples with n points over all the range.
 */
fun ClosedFloatingPointRange<Float>.sampleFloat(n: Int) = (0..n).map { lerp(start, endInclusive, it / n.toDouble()) }

/**
 * Samples with [defaultSampleNumberPerUnit] points per one unit.
 */
fun ClosedFloatingPointRange<Double>.sampleDouble() = sampleDouble(((endInclusive - start) * defaultSampleNumberPerUnit).toInt())

/**
 * Samples with n points over all the range.
 */
fun ClosedFloatingPointRange<Double>.sampleDouble(n: Int) = (0..n).map { lerp(start, endInclusive, it / n.toDouble()) }