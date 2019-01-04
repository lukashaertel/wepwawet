package eu.metatools.kepler.simulator

import eu.metatools.kepler.Gravity
import eu.metatools.kepler.math.lerp
import eu.metatools.kepler.tools.Vec
import eu.metatools.kepler.tools.addMulti
import eu.metatools.kepler.tools.plot
import eu.metatools.kepler.tools.skipItem
import org.apache.commons.math3.ml.clustering.Clusterable
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import org.apache.commons.math3.ode.AbstractIntegrator
import org.apache.commons.math3.ode.FirstOrderConverter
import org.apache.commons.math3.ode.SecondOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
import org.apache.commons.math3.util.FastMath.ceil
import org.apache.commons.math3.util.FastMath.floor
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

interface Body {
    /**
     * Bounding sphere radius.
     */
    val bounds: Double

    /**
     * Hull of the body.
     */
    val hull: List<Vec>

    /**
     * Position of the body.
     */
    val pos: Vec

    /**
     * Rotation of the body.
     */
    val rot: Double

    /**
     * Center of mass.
     */
    val com: Vec

    /**
     * Mass.
     */
    val mass: Double


    /**
     * Change rate of position / velocity.
     */
    val posDot: Vec

    /**
     * Change rate of rotation / angular velocity.
     */
    val rotDot: Double

    /**
     * Change rate of center of mass.
     */
    val comDot: Vec

    /**
     * Change rate of mass.
     */
    val massDot: Double
}

/**
 * Receives effect changes and presents the current status.
 */
interface EffectReceiver : Body {
    /**
     * Receives positional acceleration.
     */
    fun accPos(posAcc: Vec)

    /**
     * Receives rotational acceleration.
     */
    fun accRot(rotAcc: Double)

    /**
     * Receives acceleration of center of mass.
     */
    fun accCom(comAcc: Vec)

    /**
     * Receives acceleration of mass.
     */
    fun accMass(massAcc: Double)
}

/**
 * Universal effect specification on single bodies with respect to N other bodies..
 */
interface Universal {
    /**
     * Apply N body effect on the receiver with [other] as the context.
     */
    fun universal(on: EffectReceiver, other: List<Body>, t: Double)
}

/**
 * Initial value specification, time source and local effect on body.
 */
abstract class Definition(
        override val bounds: Double = 1.0,
        override val hull: List<Vec> = emptyList(),
        override val pos: Vec = Vec.zero,
        override val rot: Double = 0.0,
        override val com: Vec = Vec.zero,
        override val mass: Double = 1.0,
        override val posDot: Vec = Vec.zero,
        override val rotDot: Double = 0.0,
        override val comDot: Vec = Vec.zero,
        override val massDot: Double = 0.0) : Body {
    /**
     * Time source.
     */
    abstract val time: Double

    /**
     * Apply local zero body effect on the receiver.
     */
    open fun local(on: EffectReceiver, t: Double) = Unit
}

/**
 * Registered and simulated body.
 */
interface Registration : Body {
    /**
     * Notifies a change at the [Definition]s time source value.
     */
    fun notifyChange(t: Double)

    /**
     * // TODO Verify
     * Unregisters the object.
     */
    fun unregister()
}

/**
 * The default integrator to use.
 */
val defaultIntegrator = DormandPrince853Integrator(1.0e-8, 100.0, 1.0e-8, 1.0e-8)

/**
 * The default resolution to use.
 */
const val defaultResolution = 0.05

/**
 * The default capacity to use.
 */
const val defaultCapacity = 40


/**
 *
 * @property universal The universal effects, e.g., directional acceleration, massive bodies.
 * @property integrator The integrator to use for evaluating the differential equations.
 * @property capacity Capacity of simulation.
 * @property resolution The time slotting resolution. Only multiples of this will be simulated, other points of time
 * are interpolated instead.
 */
@Deprecated("Not working")
class Simulator(
        val universal: Universal,
        val integrator: AbstractIntegrator = defaultIntegrator,
        val capacity: Int = defaultCapacity,
        val resolution: Double = defaultResolution) {

    /**
     * List of all bodies in the simulation.
     */
    private val bodies = arrayListOf<SimulatorRegistration>()

    /**
     * Integrate to [t] if no value present.
     */
    private fun integrateIfAbsent(t: Double) {
        // No body, already completed.
        if (bodies.isEmpty())
            return

        // Get time the simulation was run to.
        val t0 = bodies.first().lastTime

        // Bodies simulated to that time, already completed.
        if (t0 >= t)
            return

        // Otherwise cluster to an appropriate size.
        KMeansPlusPlusClusterer<SimulatorRegistration>(2 * bodies.size / capacity).apply {
            // Simulate all clusters.
            for (c in cluster(bodies)) {
                // Get size of the points.
                val size = c.points.size

                // Create equation mapper.
                val equations = FirstOrderConverter(object : SecondOrderDifferentialEquations {
                    /**
                     * N times all components.
                     */
                    override fun getDimension() = size * components

                    /**
                     * Current 0th derivative.
                     */
                    private var currentY = DoubleArray(0)

                    /**
                     * Current 1st derivative.
                     */
                    private var currentYDot = DoubleArray(0)

                    /**
                     * Current 2nd derivative.
                     */
                    private var currentYDDot = DoubleArray(0)

                    /**
                     * List of bodies as effect receviers, only initialized once as integration method is called
                     * frequently.
                     */
                    private val bodies = List<EffectReceiver>(size) {
                        object : EffectReceiver {
                            override val bounds: Double
                                get() = c.points[it].bounds
                            override val hull: List<Vec>
                                get() = c.points[it].hull

                            override val pos: Vec
                                get() = currentY.pos(it)
                            override val rot: Double
                                get() = currentY.rot(it)
                            override val com: Vec
                                get() = currentY.com(it)
                            override val mass: Double
                                get() = currentY.mass(it)

                            override val posDot: Vec
                                get() = currentYDot.pos(it)
                            override val rotDot: Double
                                get() = currentYDot.rot(it)
                            override val comDot: Vec
                                get() = currentYDot.com(it)
                            override val massDot: Double
                                get() = currentYDot.mass(it)

                            override fun accPos(posAcc: Vec) {
                                currentYDDot.addPos(it, posAcc)
                            }

                            override fun accRot(rotAcc: Double) {
                                currentYDDot.addRot(it, rotAcc)
                            }

                            override fun accCom(comAcc: Vec) {
                                currentYDDot.addCom(it, comAcc)
                            }

                            override fun accMass(massAcc: Double) {
                                currentYDDot.addMass(it, massAcc)
                            }
                        }
                    }

                    override fun computeSecondDerivatives(t: Double, y: DoubleArray, yDot: DoubleArray, yDDot: DoubleArray) {
                        // Transfer current source and target arrays.
                        currentY = y
                        currentYDot = yDot
                        currentYDDot = yDDot

                        // Reset derivative.
                        yDDot.fill(0.0)

                        // Run local simulation for all bodies.
                        c.points.forEachIndexed { i, s ->
                            s.definition.local(bodies[i], t)
                        }

                        // Run universal simulation for all bodies.
                        bodies.forEachIndexed { i, e ->
                            universal.universal(e, bodies.skipItem(i), t)
                        }
                    }
                })

                // Create initial array from all bodies in the cluster.
                val y0 = DoubleArray(size * components * 2) {
                    if (it < size * components)
                        when (it.rem(components)) {
                            0 -> c.points[it / components].lastStatus.pos.x
                            1 -> c.points[it / components].lastStatus.pos.y
                            2 -> c.points[it / components].lastStatus.rot
                            3 -> c.points[it / components].lastStatus.com.x
                            4 -> c.points[it / components].lastStatus.com.y
                            else -> c.points[it / components].lastStatus.mass
                        }
                    else
                        when (it.rem(components)) {
                            0 -> c.points[it / components - size].lastStatus.posDot.x
                            1 -> c.points[it / components - size].lastStatus.posDot.y
                            2 -> c.points[it / components - size].lastStatus.rotDot
                            3 -> c.points[it / components - size].lastStatus.comDot.x
                            4 -> c.points[it / components - size].lastStatus.comDot.y
                            else -> c.points[it / components - size].lastStatus.massDot
                        }
                }

                // Create an output array of appropriate size.
                val y = DoubleArray(size * components * 2)

                // Integrate equations from initial values to y.
                integrator.integrate(equations, t0, y0, t, y)

                // Transfer new status objects to the backings of the bodies.
                c.points.forEachIndexed { i, s ->
                    s.backing[t] = Status(
                            Vec(y[i * components + 0], y[i * components + 1]),
                            y[i * components + 2],
                            Vec(y[i * components + 3], y[i * components + 4]),
                            y[i * components + 5],

                            Vec(y[(size + i) * components + 0], y[(size + i) * components + 1]),
                            y[(size + i) * components + 2],
                            Vec(y[(size + i) * components + 3], y[(size + i) * components + 4]),
                            y[(size + i) * components + 5])
                }
            }
        }
    }

    /**
     * Status object, holds values for the bodies.
     */
    private data class Status(
            val pos: Vec,
            val rot: Double,
            val com: Vec,
            val mass: Double,
            val posDot: Vec,
            val rotDot: Double,
            val comDot: Vec,
            val massDot: Double)

    /**
     * Simulation registry, creates a clusterable status holding object that performs registration and invalidation
     * methods.
     */
    private inner class SimulatorRegistration(val definition: Definition) : Registration, Clusterable {
        /**
         * Initialization time.
         */
        val initialTime =
                definition.time

        /**
         * Initialization values.
         */
        val initialStatus =
                Status(definition.pos,
                        definition.rot,
                        definition.com,
                        definition.mass,
                        definition.posDot,
                        definition.rotDot,
                        definition.comDot,
                        definition.massDot)

        /**
         * Values of integration at time t.
         */
        val backing = TreeMap<Double, Status>()

        /**
         * Last time in the backing.
         */
        val lastTime: Double
            get() = backing.lastEntry()?.key ?: initialTime

        /**
         * Last status in the backing.
         */
        val lastStatus: Status
            get() = backing.lastEntry()?.value ?: initialStatus

        fun getStatus(t: Double) =
                if (t <= initialTime)
                    initialStatus
                else
                    backing[t] ?: initialStatus

        override fun getPoint() = lastStatus.pos.let {
            doubleArrayOf(it.x, it.y)
        }

        override val bounds: Double
            get() = definition.bounds

        override val hull: List<Vec>
            get() = definition.hull

        override val pos: Vec
            get() = evaluate(definition.time, Status::pos, Vec::lerp)

        override val rot: Double
            get() = evaluate(definition.time, Status::rot, ::lerp)

        override val com: Vec
            get() = evaluate(definition.time, Status::com, Vec::lerp)

        override val mass: Double
            get() = evaluate(definition.time, Status::mass, ::lerp)

        override val posDot: Vec
            get() = evaluate(definition.time, Status::posDot, Vec::lerp)

        override val rotDot: Double
            get() = evaluate(definition.time, Status::rotDot, ::lerp)

        override val comDot: Vec
            get() = evaluate(definition.time, Status::comDot, Vec::lerp)

        override val massDot: Double
            get() = evaluate(definition.time, Status::massDot, ::lerp)

        override fun notifyChange(t: Double?) {
            reset(t ?: definition.time)
        }

        override fun unregister(t: Double?) {
            bodies -= this
            reset(t ?: definition.time)
        }

        /**
         * Evaluates the values at [t] over [resolution] by getting from status with [get] and interpolating the
         * values with [lerp].
         */
        inline fun <E> evaluate(t: Double, get: (Status) -> E, lerp: (E, E, Double) -> E): E {
            // Return initial value if before initial time.
            if (t <= initialTime)
                return get(initialStatus)

            // Get slot of lower and higher keys.
            val fe = floor(t / resolution) * resolution
            val ce = ceil(t / resolution) * resolution

            // Check if on slot boundary.
            if (fe == ce) {
                // Integrate the slot boundary.
                integrateIfAbsent(fe)

                // Return the item directly.
                return get(getStatus(fe))
            }

            // Integrate both ends.
            integrateIfAbsent(fe)
            integrateIfAbsent(ce)

            // Interpolate between both ends.
            return lerp(
                    get(getStatus(fe)),
                    get(getStatus(ce)),
                    (t - fe) / (ce - fe))
        }
    }

    /**
     * Adds a registration based on the definition.
     */
    fun register(definition: Definition): Registration =
            SimulatorRegistration(definition).also {
                // Reset all other bodies.
                reset(it.initialTime)
                integrateIfAbsent(it.initialTime)

                // Add the new registration.
                bodies += it
            }

    /**
     * Resets all computation from and including the time [from].
     */
    fun reset(from: Double) {
        for (b in bodies)
            b.backing.tailMap(from, true).clear()
    }

    /**
     * Drops all computations to but not including the time [to].
     */
    fun drop(to: Double) {
        // Clear old values.
        for (b in bodies)
            b.backing.headMap(to, false).clear()
    }
}


fun main(args: Array<String>) {

    val sim = Simulator(object : Universal {
        override fun universal(on: EffectReceiver, other: List<Body>, t: Double) {
            for (o in other)
                if ((o.pos - on.pos).squaredLength > 50.0)
                    on.accPos(Gravity.acc(o.pos, o.mass, on.pos))
        }
    })

    var time = 0.0

    val star = sim.register(object : Definition(
            bounds = 20.0,
            mass = 1e13) {
        override val time: Double
            get() = time
    })

    val ship = sim.register(object : Definition(
            bounds = 3.0,
            pos = Vec.right * 400,
            posDot = Vec.up * 100) {
        override val time: Double
            get() = time
    })

    val ship2 = sim.register(object : Definition(
            bounds = 3.0,
            pos = Vec.up * 400,
            posDot = Vec.left * 100
    ) {
        override val time: Double
            get() = time
    })

    plot {
        range(0.0, 12.0)
        addMulti(listOf("x1", "y1", "x2", "y2", "x3", "y3")) {
            time = it
            doubleArrayOf(star.pos.x, star.pos.y, ship.pos.x, ship.pos.y, ship2.pos.x, ship2.pos.y)
        }
    }

}

