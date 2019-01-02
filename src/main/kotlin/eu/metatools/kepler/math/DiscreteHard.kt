package eu.metatools.kepler.math

import org.apache.commons.math3.util.FastMath.abs

/**
 * Function on [x] that is one after [at]. [Double.NaN] at the step on point.
 */
fun stepOnHard(x: Double, at: Double) =
        (abs(x - at) / (x - at) + 1.0) /
                2.0

/**
 * Function on [x] that initially has the value [initialValue] and, for each pair in [valueRanges], takes
 * the [Map.Entry.value] after [Map.Entry.key].
 */
fun discreteHard(x: Double, valueRanges: Map<Double, Double>, initialValue: Double = 0.0): Double {
    // Running equation and last value to turn off.
    var eqn = initialValue
    var lastOff = initialValue

    // Iterate all pairs.
    for ((k, v) in valueRanges) {
        // If no change in value, skip.
        if (v == lastOff) continue

        // Turn old value off and new value on.
        eqn += -stepOnHard(x, k) * lastOff
        eqn += stepOnHard(x, k) * v

        // Set new value to turn off.
        lastOff = v
    }

    // Return computed equation.
    return eqn
}
