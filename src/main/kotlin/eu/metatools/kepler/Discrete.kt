package eu.metatools.kepler

import org.apache.commons.math3.util.FastMath.abs
import org.apache.commons.math3.util.FastMath.tanh

/**
 * Function on [x] that is one after [at] (point five at that exact point). [width] gives the radius of the gap which
 * the function takes to turn on.
 */
fun stepOnSmooth(x: Double, at: Double, width: Double = 0.1) =
        (tanh((x - at) * 10.0 / (width * 3.0)) + 1.0) /
                2.0


/**
 * Function on [x] that is one after [at] (point five at that exact point). [width] gives the radius of the gap which
 * the function takes to turn on.
 */
fun dsStepOnSmooth(x: DS, at: Double, width: Double = 0.1) =
        (((x - at) * 10.0 / (width * 3.0)).tanh() + 1.0) /
                2.0

/**
 * Function on [x] that initially has the value [initialValue] and, for each pair in [valueRanges], takes
 * the [Map.Entry.value] after [Map.Entry.key]. 0.999 of the values is reached within [width] of the keys.
 */
fun discreteSmooth(x: Double, valueRanges: Map<Double, Double>, initialValue: Double = 0.0, width: Double = 0.1): Double {
    // Running equation and last value to turn off.
    var eqn = initialValue
    var lastOff = initialValue

    // Iterate all pairs.
    for ((k, v) in valueRanges) {
        // If no change in value, skip.
        if (v == lastOff) continue

        // Turn old value off and new value on.
        eqn += -stepOnSmooth(x, k, width) * lastOff
        eqn += stepOnSmooth(x, k, width) * v

        // Set new value to turn off.
        lastOff = v
    }

    // Return computed equation.
    return eqn
}

/**
 * Function on [x] that initially has the value [initialValue] and, for each pair in [valueRanges], takes
 * the [Map.Entry.value] after [Map.Entry.key]. 0.999 of the values is reached within [width] of the keys.
 */
fun dsDiscreteSmooth(x: DS, valueRanges: Map<Double, Double>, initialValue: Double = 0.0, width: Double = 0.1): DS {
    // Running equation and last value to turn off.
    var eqn = x.createConstant(initialValue)
    var lastOff = initialValue

    // Iterate all pairs.
    for ((k, v) in valueRanges) {
        // If no change in value, skip.
        if (v == lastOff) continue

        // Turn old value off and new value on.
        eqn += -dsStepOnSmooth(x, k, width) * lastOff
        eqn += dsStepOnSmooth(x, k, width) * v

        // Set new value to turn off.
        lastOff = v
    }

    // Return computed equation.
    return eqn
}

/**
 * Function on [x] that is one after [at]. [Double.NaN] at the step on point.
 */
fun stepOnHard(x: Double, at: Double) =
        (abs(x - at) / (x - at) + 1.0) /
                2.0

/**
 * Function on [x] that is one after [at]. [Double.NaN] at the step on point.
 */
fun dsStepOnHard(x: DS, at: Double) =
        ((x - at).abs() / (x - at) + 1.0) /
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

/**
 * Function on [x] that initially has the value [initialValue] and, for each pair in [valueRanges], takes
 * the [Map.Entry.value] after [Map.Entry.key].
 */
fun dsDiscreteHard(x: DS, valueRanges: Map<Double, Double>, initialValue: Double = 0.0): DS {
    // Running equation and last value to turn off.
    var eqn = x.createConstant(initialValue)
    var lastOff = initialValue

    // Iterate all pairs.
    for ((k, v) in valueRanges) {
        // If no change in value, skip.
        if (v == lastOff) continue

        // Turn old value off and new value on.
        eqn += -dsStepOnHard(x, k) * lastOff
        eqn += dsStepOnHard(x, k) * v

        // Set new value to turn off.
        lastOff = v
    }

    // Return computed equation.
    return eqn
}