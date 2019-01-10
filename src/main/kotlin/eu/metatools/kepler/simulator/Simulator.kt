package eu.metatools.kepler.simulator

import eu.metatools.kepler.math.lerp
import eu.metatools.kepler.math.shearCylinderIntersection
import eu.metatools.kepler.tools.Vec
import eu.metatools.kepler.tools.cluster
import eu.metatools.kepler.tools.skipItem
import org.apache.commons.math3.ode.AbstractIntegrator
import org.apache.commons.math3.ode.FirstOrderConverter
import org.apache.commons.math3.ode.SecondOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
import org.apache.commons.math3.util.FastMath.*
import java.util.*

class Simulator(
        val universal: Universal,
        val k: Int = 4, val min: Int = 16,
        val resolution: Double = 0.125,
        val integrator: AbstractIntegrator = DormandPrince853Integrator(1.0e-8, 100.0, 1.0e-8, 1.0e-8)) {

    /**
     * In the [resolution] based system, converts the time to a cell number.
     */
    private fun Double.toCell() = floor(this / resolution).toInt()

    /**
     * In the [resolution] based system, converts the time to the next cell number
     */
    private fun Double.toNextCell() = ceil(this / resolution).toInt()

    /**
     * In the [resolution] based system, cionverts the cell number to a time.
     */
    private fun Int.toTime() = this * resolution

    /**
     * Registration of a definition.
     */
    private inner class Reg(val definition: Definition) : Registration {
        /**
         * Origin cell.
         */
        val cellOrigin: Int get() = valuations.firstKey()

        /**
         * Valuations of the registration. Indices are relative to origin cell.
         */
        val valuations = TreeMap<Int, Status>(mapOf(definition.time.toCell() to definition.toStatus()))

        /**
         * Cell at which the registration is shelved. Shelved objects still have effects during their lifetime but will
         * not be simulated anymore after the shelved cell. This will preserve effects they had during their lifetime.
         */
        var cellShelved = Int.MAX_VALUE

        // Static properties.
        override val bounds get() = definition.bounds
        override val hull get() = definition.hull

        // Zeroth derivative of dynamic properties.
        override val pos get() = eval(Status::pos, definition.time, Vec::lerp)
        override val rot get() = eval(Status::rot, definition.time, ::lerp)
        override val com get() = eval(Status::com, definition.time, Vec::lerp)
        override val mass get() = eval(Status::mass, definition.time, ::lerp)

        // First derivative of dynamic properties.
        override val posDot get() = eval(Status::posDot, definition.time, Vec::lerp)
        override val rotDot get() = eval(Status::rotDot, definition.time, ::lerp)
        override val comDot get() = eval(Status::comDot, definition.time, Vec::lerp)
        override val massDot get() = eval(Status::massDot, definition.time, ::lerp)

        operator fun get(c: Int) =
                when {
                    c < cellOrigin ->
                        throw IllegalArgumentException("Get before registration was created")
                    cellShelved <= c ->
                        throw IllegalArgumentException("Get after registration was shelved")
                    else ->
                        valuations[c]
                                ?: throw IllegalArgumentException("Registration has no valuation for $c")
                }


        /**
         * Evaluates the [property] at the time [t] interpolating (if needed) with [interpolate].
         */
        private inline fun <E> eval(property: (Status) -> E, t: Double, interpolate: (E, E, Double) -> E): E {
            // Get cells for evaluated time.
            val c1 = t.toCell()
            val c2 = t.toNextCell()

            // Check if cell on location or between.
            if (c1 == c2) {
                // Cell is on location, assert that it is exactly integrated.
                assertIntegrated(c1)

                // Get value of the property.
                return property(get(c1))
            } else {
                // Cell is between location, assert that left and right locations are integrated.
                assertIntegrated(c1)
                assertIntegrated(c2)
                // Get interpolation value.
                val x = (t - c1.toTime()) / (c2.toTime() - c1.toTime())

                // Interpolated value of left and right property.
                return interpolate(
                        property(get(c1)),
                        property(get(c2)), x)
            }
        }

        override fun notifyChange(t: Double?) {
            // Reset at given or definition time.
            reset((t ?: definition.time).toCell())
        }

        override fun unregister(t: Double?) {
            // Set shelved cell.
            cellShelved = (t ?: definition.time).toCell()

            // Also reset at that cell.
            reset(cellShelved)
        }

        override fun toString() =
                definition.toString()

        override val track: SortedMap<Double, Status>
            get() = valuations.mapKeysTo(TreeMap<Double, Status>()) { it.key.toTime() }

    }

    /**
     * Resets all valuations for the given cell number [c].
     */
    private fun reset(c: Int) {
        // Remove all valuations that are not the origin valuation.
        for (r in regs)
            r.valuations.tailMap(max(r.cellOrigin + 1, c), true).clear()
    }

    /**
     * Asserts that all valid bodies at cell number [c] have valuations.
     */
    private fun assertIntegrated(c: Int) {
        // All addressable registrations at the given cell.
        val addressable = regs.filter { it.cellOrigin < c }

        // Check that there are objects that may be integrated.
        if (addressable.isEmpty())
            return

        // Cells that were integrated on multiple bodies.
        val common = addressable
                .map { it.valuations.keys as Set<Int> }
                .reduce { a, b -> a intersect b }

        // Check for a common base cell for integration.
        val base = common.max()
        if (base != null) {
            integrate(base, c)
            return
        }

        // Get last new introduced body, assert integrated to that body, this will exclude this body. After
        // integration, this cell will have values for this body as well. They might recursively require more steps. The
        // method will terminate as at least one registration is excluded from the recursion.
        val last = addressable.map { it.cellOrigin }.max()
        if (last != null) {
            assertIntegrated(last)
            assertIntegrated(c)
            return
        }
    }

    /**
     * Performs an integration of all registrations, must have valuations at [c0].
     */
    private fun integrate(c0: Int, c: Int) {
        // Do not integrate if the steps are the same.
        if (c0 == c)
            return

        // Get all registrations to consider.
        val consider = regs.filter { c0 in it.cellOrigin..it.cellShelved }

        // Cluster those registrations in k clusters, if a certain number of elements is reached.
        val clusters = consider.cluster(if (consider.size <= min) 1 else k) { it[c0].pos }

        // Integrate all clusters individually.
        for (cluster in clusters)
            integrate(cluster.value, c0, c)
    }

    /**
     * Performs and integration of some registrations, must have valuations at [c0]
     */
    private fun integrate(regs: List<Reg>, c0: Int, c: Int) {
        // Do not integrate if the steps are the same.
        if (c0 == c)
            return

        // Current input and output arrays.
        lateinit var currentY: DoubleArray
        lateinit var currentYDot: DoubleArray
        lateinit var currentYDDot: DoubleArray

        // List of receivers for directing the effects.
        val receivers = List<Receiver>(regs.size) {
            object : Receiver {
                override val bounds: Double
                    get() = regs[it].bounds
                override val hull: Hull
                    get() = regs[it].hull

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

        // Equation mapper describing the changes based on effects.
        val equations = FirstOrderConverter(object : SecondOrderDifferentialEquations {
            override fun getDimension() = regs.size * components

            override fun computeSecondDerivatives(t: Double, y: DoubleArray, yDot: DoubleArray, yDDot: DoubleArray) {
                // Transfer current source and target arrays.
                currentY = y
                currentYDot = yDot
                currentYDDot = yDDot

                // Reset derivative.
                yDDot.fill(0.0)

                // Run local simulation for all bodies.
                regs.forEachIndexed { i, s ->
                    s.definition.local(receivers[i], t)
                }

                // Run universal simulation for all bodies.
                receivers.forEachIndexed { i, e ->
                    universal.universal(e, receivers.skipItem(i), t)
                }
            }
        })

        // Compute the valuations ahead to prevent lots of range checks.
        val valuations = regs.map { it[c0] }

        // Create initial array from all bodies in the cluster.
        val y0 = DoubleArray(regs.size * components * 2) {
            if (it < regs.size * components)
                when (it.rem(components)) {
                    0 -> valuations[it / components].pos.x
                    1 -> valuations[it / components].pos.y
                    2 -> valuations[it / components].rot
                    3 -> valuations[it / components].com.x
                    4 -> valuations[it / components].com.y
                    else -> valuations[it / components].mass
                }
            else
                when (it.rem(components)) {
                    0 -> valuations[it / components - regs.size].posDot.x
                    1 -> valuations[it / components - regs.size].posDot.y
                    2 -> valuations[it / components - regs.size].rotDot
                    3 -> valuations[it / components - regs.size].comDot.x
                    4 -> valuations[it / components - regs.size].comDot.y
                    else -> valuations[it / components - regs.size].massDot
                }
        }

        // Create an output array of appropriate size.
        val y = DoubleArray(regs.size * components * 2)

        // Integrate equations from initial values to y.
        integrator.integrate(equations, c0.toTime(), y0, c.toTime(), y)

        // Transfer new status objects to the valuations of the registrations.
        regs.forEachIndexed { i, r ->
            r.valuations[c] = Status(
                    Vec(y[i * components + 0], y[i * components + 1]),
                    y[i * components + 2],
                    Vec(y[i * components + 3], y[i * components + 4]),
                    y[i * components + 5],

                    Vec(y[(regs.size + i) * components + 0], y[(regs.size + i) * components + 1]),
                    y[(regs.size + i) * components + 2],
                    Vec(y[(regs.size + i) * components + 3], y[(regs.size + i) * components + 4]),
                    y[(regs.size + i) * components + 5])
        }

//        val t = (c - c0).toTime()
//        for ((i, r1) in regs.withIndex()) {
//            val r1v = valuations[i]
//            for ((j, r2) in regs.withIndex()) {
//                if (i >= j) continue
//
//                val r2v = r2[c0]
//
//                val range = shearCylinderIntersection(
//                        r1v.pos, r1v.posDot, r1.bounds,
//                        r2v.pos, r2v.posDot, r2.bounds)
//
//                if (t in range)
//                    println("$r1 and $r2 intersection probable at ${c.toTime()}")
//            }
//        }
    }

    /**
     * Registrations in this simulator.
     */
    private val regs = mutableListOf<Reg>()

    /**
     * Registers a definition and returns the registration handle.
     */
    fun register(definition: Definition): Registration {
        // Create registration with the definition.
        val reg = Reg(definition)

        // Reset all other bodies, as those might experience effects by the new registration.
        reset(reg.cellOrigin)

        // Add and return the registration.
        regs += reg
        return reg
    }

    /**
     * Releases all values and registrations that are not needed to get the values at point [t].
     */
    fun release(t: Double) {
        // Get cell to which to release.
        val c = t.toCell()

        // Track the shelve drop cell.
        var shelveDrop = Int.MAX_VALUE

        // Release in all registrations.
        for (r in regs) {
            // Get the next potential base for the given cell.
            val base = r.valuations.floorKey(c)

            // If there is a new base, drop all values before that and mark new shelve drop.
            if (base != null) {
                r.valuations.headMap(base, false).clear()
                shelveDrop = min(shelveDrop, base)
            }
        }

        // Remove all shelved registrations that are shelved before the minimum drop point.
        regs.removeAll { it.cellShelved < shelveDrop }
    }
}