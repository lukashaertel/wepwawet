package eu.metatools.kepler.math

import eu.metatools.kepler.squared
import org.apache.commons.math3.util.FastMath.*

/**
 * Commonly used, factor at which curve reaches 0.999
 */
fun tanhFactor(width: Double) =
        10.0 / (width * 3.0)

/**
 * Function on [x] that is one after [at] (point five at that exact point). [width] gives the radius of the gap which
 * the function takes to turn on.
 */
fun stepOnSmooth(x: Double, at: Double, width: Double = 0.1) =
        (tanh((x - at) * tanhFactor(width)) + 1.0) /
                2.0

/**
 * Secant-hyperbolicus.
 */
fun sech(x: Double) = 1.0 / cosh(x)

/**
 * Derivative of [stepOnSmooth].
 */
fun stepOnSmoothPrime(x: Double, at: Double, width: Double = 0.1) =
        tanhFactor(width) * sech((x - at) * tanhFactor(width)).squared() /
                2.0

/**
 * Integral of [stepOnSmooth].
 */
fun stepOnSmoothIntegrated(x: Double, at: Double, width: Double = 0.1) =
        (log(cosh((x - at) * tanhFactor(width))) + x * tanhFactor(width)) /
                (2.0 * tanhFactor(width))

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
 * Computes the radius for step on smooth. Since tanh is point-symmetric, this is linear scaling of [A] over
 * two [scale]s.
 */
fun radiusForSmooth(A: Double, scale: Double = 1.0) =
        A / (scale * 2.0)