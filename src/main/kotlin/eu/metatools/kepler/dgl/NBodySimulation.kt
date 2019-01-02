package eu.metatools.kepler.dgl

import eu.metatools.kepler.tools.Vec
import org.apache.commons.math3.ode.AbstractIntegrator
import org.apache.commons.math3.ode.FirstOrderConverter
import org.apache.commons.math3.ode.SecondOrderDifferentialEquations

/**
 * Performs single body simulation.
 */
class NBodySimulation(val max: Int) : SecondOrderDifferentialEquations, Simulation<List<Context>> {
    /**
     * Effect compiler.
     */
    val effects = EffectCompiler<Context, Pair<Int, List<Context>>>()

    /**
     * Dimension of the simulation is max * 1 * 3.
     */
    override fun getDimension() = max * 3

    override fun computeSecondDerivatives(t: Double, y: DoubleArray, yDot: DoubleArray, yDDot: DoubleArray) {
        val contexts = List(y.size / 3) {
            Context(t,
                    Vec(y[it * 3 + 0], y[it * 3 + 1]), y[it * 3 + 2],
                    Vec(yDot[it * 3 + 0], yDot[it * 3 + 1]), yDot[it * 3 + 2])
        }

        contexts.forEachIndexed { i, c ->
            // Assign acceleration.
            effects.compiledAcc(c, i to contexts).let { (x, y) ->
                yDDot[i * 3 + 0] = x
                yDDot[i * 3 + 1] = y
            }

            // Assign rotational acceleration.
            yDDot[i * 3 + 2] = effects.compiledAccRot(c, i to contexts)
        }
    }

    /**
     * First order DE for this second order DE.
     */
    private val firstOrder by lazy { FirstOrderConverter(this) }

    override fun integrate(integrator: AbstractIntegrator, from: List<Context>, t: Double): List<Context> {
        // No integration necessary, return immediately.
        if (from.isEmpty() || from.first().t == t)
            return from

        // Compute initial array.
        val initial = from.toDoubleArray()

        // Fill output from integration, return as contexts.
        return DoubleArray(initial.size).also {
            integrator.integrate(firstOrder, from.first().t, initial, t, it)
        }.toContexts(t)
    }
}