package eu.metatools.kepler

import org.apache.commons.math3.ode.AbstractIntegrator
import org.apache.commons.math3.ode.FirstOrderConverter
import org.apache.commons.math3.ode.SecondOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
import java.lang.Math.toDegrees
import java.util.*
import kotlin.properties.Delegates.observable

/**
 * Computation context.
 */
data class Context(val t: Double, val pos: Vec, val rot: Double, val vel: Vec, val velRot: Double) {
    /**
     * Converts the context to a double array of position, rotation and derivatives. Drops [t].
     */
    fun toDoubleArray() =
            doubleArrayOf(
                    pos.x, pos.y, rot,
                    vel.x, vel.y, velRot)
}

/**
 * With [t], converts the contents of the array to a context.
 */
fun DoubleArray.toContext(t: Double) =
        Context(t, Vec(get(0), get(1)), get(2), Vec(get(3), get(4)), get(5))

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
     * Dimension of the simulation is 1 * 3.
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
    fun integrate(integrator: AbstractIntegrator, from: Context, t: Double): Context {
        val initial = from.toDoubleArray()
        val output = from.toDoubleArray()
        if (t != from.t)
            integrator.integrate(firstOrder, from.t, initial, t, output)
        return output.toContext(t)
    }
}

/**
 * Memorization control for single body simulation.
 */
class ContinuousBodyIntegrator(val singleBodySimulation: SingleBodySimulation,
                               val integrator: AbstractIntegrator,
                               val from: Context) {
    /**
     * Memorize previous configuration.
     */
    private val backing = TreeMap<Double, Context>()

    /**
     * Returns the samples calculated at the given points.
     */
    fun calculated(from: Double = Double.NaN, to: Double = Double.NaN): NavigableMap<Double, Context> =
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
     * Perform integration on [SingleBodySimulation.integrate] using the appropriate closest integrated value.
     */
    fun integrate(t: Double): Context {
        // Get base or lower value.
        val base = backing.floorEntry(t)?.value ?: from

        // Return the value if it is exactly on the computation point, otherwise compute new
        // value, store and return it.
        return if (base.t == t)
            base
        else
            singleBodySimulation.integrate(integrator, base, t).also {
                backing[t] = it
            }
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

    val initial = Context(0.0, Vec(100.0, 0.0), 0.0, Vec(90.0, 0.0), 0.0)
    val cbi = ContinuousBodyIntegrator(sbs, int, initial)

    val f = { t: Double -> cbi.integrate(t) }
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