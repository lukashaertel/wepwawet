package eu.metatools.kepler.dgl

import org.apache.commons.math3.ode.AbstractIntegrator
import java.util.*

/**
 * Memorization control for  simulation.
 */
class ContinuousIntegrator<T>(val simulation: Simulation<T>,
                              val integrator: AbstractIntegrator,
                              val from: T) {
    /**
     * Memorize previous configuration.
     */
    private val backing = TreeMap<Double, T>()

    /**
     * Returns the samples calculated at the given points.
     */
    fun calculated(from: Double = Double.NaN, to: Double = Double.NaN): NavigableMap<Double, T> =
            if (from == Double.NaN) {
                if (to == Double.NaN) {
                    backing
                } else {
                    backing.headMap(to, false)
                }
            } else {
                if (to == Double.NaN) {
                    backing.tailMap(from, true)
                } else {
                    backing.subMap(from, true, to, false)
                }
            }

    /**
     * Drops all contexts that were precalculated up to but not including the given time [to]. Used to discard of
     * states that are not needed anymore. Resetting before this point will cause a full recalculation from the original
     * configuration.
     */
    fun drop(to: Double) {
        backing.headMap(to, false).clear()
    }

    /**
     * Invalidates the precomputed calculations from and including the given time [from].
     */
    fun reset(from: Double) {
        backing.tailMap(from, true).clear()
    }

    /**
     * Perform integration on [OneBodySimulation.integrate] using the appropriate closest integrated value.
     */
    fun integrate(t: Double): T {
        // Get base or lower value.
        val base = backing.floorEntry(t)

        // Return the value if it is exactly on the computation point, otherwise compute new
        // value, store and return it.
        return if (base?.key == t)
            base.value
        else
            simulation.integrate(integrator, base?.value ?: from, t).also {
                backing[t] = it
            }
    }
}