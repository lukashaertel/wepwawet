package eu.metatools.kepler

import org.apache.commons.math3.ode.AbstractIntegrator
import org.apache.commons.math3.ode.FirstOrderConverter
import org.apache.commons.math3.ode.SecondOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
import java.lang.Math.toDegrees
import kotlin.properties.Delegates.observable

/**
 * Computation context.
 */
data class Context(val t: Double, val pos: Vec, val rot: Double, val vel: Vec, val velRot: Double)

/**
 * Performs single body simulation.
 */
class SingleBodySimulation : SecondOrderDifferentialEquations {
    /**
     * Compiled acceleration function, as a result of the superposition of all passed acceleration functions.
     */
    var compiledAcc: (Context) -> Vec = { Vec.zero }
        private set

    /**
     * Compiled rotational acceleration function, as a result of the superposition of all passed rotational
     * acceleration functions.
     */
    var compiledAccRot: (Context) -> Double = { 0.0 }
        private set

    /**
     * Individual accelerator functions.
     */
    var acc by observable(listOf<Context.() -> Vec>()) { _, _, t ->
        if (t.isEmpty())
            compiledAcc = { Vec.zero }
        else
            compiledAcc = { c -> t.map { it(c) }.reduce { a, b -> a + b } }
    }

    /**
     * Add accelerator functor.
     */
    fun addAcc(f: Context.() -> Vec) {
        acc += f
    }

    /**
     * Individual rotational accelerator functions.
     */
    var accRot by observable(listOf<Context.() -> Double>()) { _, _, t ->
        if (t.isEmpty())
            compiledAccRot = { 0.0 }
        else
            compiledAccRot = { c -> t.map { it(c) }.sum() }
    }

    /**
     * Add rotational accelerator functor.
     */
    fun addAccRot(f: Context.() -> Double) {
        accRot += f
    }

    /**
     * Composes an double array representing composed configuration.
     */
    fun compose(pos: Vec, rot: Double, vel: Vec, velRot: Double) =
            doubleArrayOf(pos.x, pos.y, rot, vel.x, vel.y, velRot)

    /**
     * Dimension of the simulationis 1 * 3.
     */
    override fun getDimension() = 3

    override fun computeSecondDerivatives(t: Double, y: DoubleArray, yDot: DoubleArray, yDDot: DoubleArray) {
        // Compute context form input Y and Y prime.
        val context = Context(t, Vec(y[0], y[1]), y[2], Vec(yDot[0], yDot[1]), yDot[2])

        // Compute accelerations.
        val acc = compiledAcc(context)
        val accRot = compiledAccRot(context)

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
     * Integrates this [SingleBodySimulation] with the given integrator and initial configuration.
     */
    fun integrate(integrator: AbstractIntegrator,
                  t0: Double, pos0: Vec, rot0: Double, vel0: Vec, velRot0: Double, t: Double): Context {
        val initial = doubleArrayOf(pos0.x, pos0.y, rot0, vel0.x, vel0.y, velRot0)
        val output = initial.clone()
        if (t != t0)
            integrator.integrate(firstOrder, t0, initial, t, output)
        return Context(t, Vec(output[0], output[1]), output[2], Vec(output[3], output[4]), output[5])
    }
}


fun main(args: Array<String>) {
    val sbs = SingleBodySimulation()
    sbs.addAcc {
        Gravity.acc(Vec(100.0, 100.0), 1e+16, pos)
    }

    sbs.addAcc {
        val forward = Vec.left.rotate(rot)
        val f = stepOnSmooth(t, 16.0) - stepOnSmooth(t, 17.0)
        forward * 20.0 * f
    }

    val int = DormandPrince853Integrator(1.0e-8, 100.0, 1.0e-10, 1.0e-10)

    val initial = Vec(100.0, 0.0)
    val f = { t: Double -> sbs.integrate(int, 0.0, initial, 0.0, Vec(90.0, 0.0), 0.0, t) }
    val g = { t: Double ->
        val r = f(t)
        doubleArrayOf(r.pos.length, r.vel.length, toDegrees(r.pos.angle))
    }

    plot {
        range(0.0, 10.0)
        add { stepOnSmooth(it, 3.0) }
    }
    plot {
        range(0.0, 40.0)
        addMulti(listOf("Pos size", "Vel size", "phi"), g)
    }
}