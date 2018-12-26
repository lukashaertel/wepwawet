package eu.metatools.kepler

import java.util.*

/**
 * Couples a settable map with discretization via [discreteSmooth].
 */
class Coupling(val initialValue: Double,
               val width: Double = 0.1,
               val invalidating: (Double) -> Unit = {},
               val invalidated: (Double) -> Unit = {}) {
    /**
     * Activation values.
     */
    private val backing = TreeMap<Double, Double>()

    /**
     * Sets the value at time [x] to [value].
     */
    operator fun set(x: Double, value: Double) {
        if (backing.floorEntry(x)?.value != value) {
            invalidating(x - 2.0 * width)
            backing[x] = value
            invalidated(x - 2.0 * width)
        }
    }

    /**
     * Gets the value at [x].
     */
    operator fun get(x: Double) =
            discreteSmooth(x, backing, initialValue)
}