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
    private fun integrateT(t: Double) {
        // No body, already completed.
        if (bodies.isEmpty())
            return

        val t0 = bodies.first().let {
            it.backing.lastEntry()?.key ?: it.initialTime
        }

        integrateFromTo(t0, t)
    }

    private fun consolidateUntil(t: Double) {
        bodies.flatMap {
            it.backing.headMap(t, true).keys
        }.toSortedSet().windowed(2) { (a, b) ->
            println("consolidating form $a to $b")
            integrateFromTo(a, b)
        }
    }


    /**
     * Integrates all bodies from [t0] to [t].
     */
    private fun integrateFromTo(t0: Double, t: Double) {
        // Select bodies that are active at the time.
        val active = bodies.filter { it.initialTime <= t0 }

        // If all bodies left at that time are already integrated, return.
        if (active.all { it.backing.containsKey(t) })
            return

        // Set the simulated time for the active bodies.
        for (b in active)
            b.simulatedTime = t0

        // Cluster to an appropriate size and integrate clusters individually.
        val clusters = KMeansPlusPlusClusterer<SimulatorRegistration>(2 * bodies.size / capacity).cluster(active)
        for (cluster in clusters)
            integrateCluster(cluster.points, t0, t)
    }

    /**
     * Integrates a part of all bodies clustered by [cluster] from [t0] to [t].
     */
    private fun integrateCluster(cluster: List<SimulatorRegistration>, t0: Double, t: Double) {
        // Select bodies that are active at the time.
        val active = cluster.filter { it.initialTime <= t0 }

        // If all bodies left at that time are already integrated, return.
        if (active.all { it.backing.containsKey(t) })
            return

        // Set the simulated time for the active bodies.
        for (b in active)
            b.simulatedTime = t0

        // Current input and output vectors.
        lateinit var currentY: DoubleArray
        lateinit var currentYDot: DoubleArray
        lateinit var currentYDDot: DoubleArray

        // List of receivers.
        val receivers = List<Receiver>(active.size) {
            object : Receiver {
                override val bounds: Double
                    get() = active[it].bounds
                override val hull: List<Vec>
                    get() = active[it].hull

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

        // Create equation mapper.
        val equations = FirstOrderConverter(object : SecondOrderDifferentialEquations {
            override fun getDimension() = active.size * components

            override fun computeSecondDerivatives(t: Double, y: DoubleArray, yDot: DoubleArray, yDDot: DoubleArray) {
                // Transfer current source and target arrays.
                currentY = y
                currentYDot = yDot
                currentYDDot = yDDot

                // Reset derivative.
                yDDot.fill(0.0)

                // Run local simulation for all bodies.
                active.forEachIndexed { i, s ->
                    s.definition.local(receivers[i], t)
                }

                // Run universal simulation for all bodies.
                receivers.forEachIndexed { i, e ->
                    universal.universal(e, receivers.skipItem(i), t)
                }
            }
        })

        // Create initial array from all bodies in the cluster.
        val y0 = DoubleArray(active.size * components * 2) {
            if (it < active.size * components)
                when (it.rem(components)) {
                    0 -> active[it / components].simulatedStatus.pos.x
                    1 -> active[it / components].simulatedStatus.pos.y
                    2 -> active[it / components].simulatedStatus.rot
                    3 -> active[it / components].simulatedStatus.com.x
                    4 -> active[it / components].simulatedStatus.com.y
                    else -> active[it / components].simulatedStatus.mass
                }
            else
                when (it.rem(components)) {
                    0 -> active[it / components - active.size].simulatedStatus.posDot.x
                    1 -> active[it / components - active.size].simulatedStatus.posDot.y
                    2 -> active[it / components - active.size].simulatedStatus.rotDot
                    3 -> active[it / components - active.size].simulatedStatus.comDot.x
                    4 -> active[it / components - active.size].simulatedStatus.comDot.y
                    else -> active[it / components - active.size].simulatedStatus.massDot
                }
        }

        // Create an output array of appropriate size.
        val y = DoubleArray(active.size * components * 2)

        // Integrate equations from initial values to y.
        integrator.integrate(equations, t0, y0, t, y)

        // Transfer new status objects to the backings of the bodies.
        active.forEachIndexed { i, s ->
            s.backing[t] = Status(
                    Vec(y[i * components + 0], y[i * components + 1]),
                    y[i * components + 2],
                    Vec(y[i * components + 3], y[i * components + 4]),
                    y[i * components + 5],

                    Vec(y[(active.size + i) * components + 0], y[(active.size + i) * components + 1]),
                    y[(active.size + i) * components + 2],
                    Vec(y[(active.size + i) * components + 3], y[(active.size + i) * components + 4]),
                    y[(active.size + i) * components + 5])
        }
    }

    /**
     * Simulation registry, creates a clusterable status holding object that performs registration and invalidation
     * methods.
     */
    private inner class SimulatorRegistration(
            val definition: Definition) : Registration, Clusterable {
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


        private var simulatedTimeBacking = initialTime

        /**
         * Currently simulated time.
         */
        var simulatedTime: Double
            get() = simulatedTimeBacking
            set(value) {
                // Only apply if value changed.
                if (value != simulatedTimeBacking) {
                    // Status is initial status if time is before initial time, otherwise the backing
                    // stored value or initial value if no value present.
                    simulatedStatus = if (value <= initialTime)
                        initialStatus
                    else
                        backing[value] ?: initialStatus

                    // Transfer to backing field.
                    simulatedTimeBacking = value
                }
            }

        /**
         * Currently simulated status.
         */
        var simulatedStatus: Status = initialStatus
            private set

        override fun getPoint() =
                simulatedStatus.pos.let {
                    doubleArrayOf(it.x, it.y)
                }

        /**
         * Values of integration at time t.
         */
        val backing = TreeMap<Double, Status>()

        fun getStatus(t: Double) =
                if (t <= initialTime)
                    initialStatus
                else
                    backing[t] ?: initialStatus

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
                integrateT(fe)

                // Return the item directly.
                return get(getStatus(fe))
            }

            // Integrate both ends.
            integrateT(fe)
            integrateT(ce)

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
                consolidateUntil(it.initialTime)

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
        override fun universal(on: Receiver, other: List<Body>, t: Double) {
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

