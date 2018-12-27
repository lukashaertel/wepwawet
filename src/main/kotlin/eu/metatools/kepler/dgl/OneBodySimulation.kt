package eu.metatools.kepler.dgl

import eu.metatools.kepler.Vec
import org.apache.commons.math3.ode.AbstractIntegrator
import org.apache.commons.math3.ode.FirstOrderConverter
import org.apache.commons.math3.ode.SecondOrderDifferentialEquations

/**
 * Performs single body simulation.
 */
class OneBodySimulation : SecondOrderDifferentialEquations, Simulation<Context> {
    /**
     * Effect compiler.
     */
    val effects = EffectCompiler<Context, Unit>()

    /**
     * Dimension of the simulation is 1 * 3.
     */
    override fun getDimension() = 3

    override fun computeSecondDerivatives(t: Double, y: DoubleArray, yDot: DoubleArray, yDDot: DoubleArray) {
        // Compute context from input Y and Y prime.
        val context = Context(t, Vec(y[0], y[1]), y[2], Vec(yDot[0], yDot[1]), yDot[2])

        // Compute accelerations.
        val acc = effects.compiledAcc(context, Unit)
        val accRot = effects.compiledAccRot(context, Unit)

        // Transfer computed values.
        yDDot[0] = acc.x
        yDDot[1] = acc.y
        yDDot[2] = accRot
    }

    /**
     * First order DE for this second order DE.
     */
    private val firstOrder by lazy { FirstOrderConverter(this) }


    /**
     * Integrates this [OneBodySimulation] with the given integrator and initial configuration.
     */
    override fun integrate(integrator: AbstractIntegrator, from: Context, t: Double): Context {
        // No integration necessary, return immediately.
        if (from.t == t)
            return from

        // Compute initial array.
        val initial = from.toDoubleArray()

        // Fill output from integration, return as contexts.
        return DoubleArray(initial.size).also {
            integrator.integrate(firstOrder, from.t, initial, t, it)
        }.toContext(t)
    }
}