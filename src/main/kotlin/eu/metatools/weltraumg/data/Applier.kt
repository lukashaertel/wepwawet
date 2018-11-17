package eu.metatools.weltraumg.data

import kotlinx.serialization.Serializable
import java.util.*

/**
 * Type of time
 */
typealias Time = Double

/**
 * Type of real values.
 */
typealias R = Double

/**
 * Physically simulated real value.
 */
interface Sim {
    /**
     * The distance covered in time.
     */
    fun s(t: Time): R

    /**
     * The velocity covered in time.
     */
    fun v(t: Time): R

    /**
     * The force covered in time.
     */
    fun a(t: Time): R
}

/**
 * Creates a superposed simulation.
 */
operator fun Sim.plus(other: Sim) = Superposition(this, other)

/**
 * Implements the superposition of simulations.
 */
@Serializable
data class Superposition(val left: Sim, val right: Sim) : Sim {
    override fun s(t: Time) = left.s(t) + right.s(t)

    override fun v(t: Time) = left.v(t) + right.v(t)

    override fun a(t: Time) = left.a(t) + right.a(t)
}

/**
 * Applies forces at given times.
 */
@Serializable
data class Applier(val forces: TreeMap<Time, R>) : Sim {
    constructor(vararg forces: Pair<Time, R>) : this(TreeMap(mapOf(*forces)))

    override fun s(t: Time): R {
        val fs = forces.headMap(t, true)
        var r = fs.firstEntry()
        if (r == null)
            return 0.0

        var n = fs.higherEntry(r.key)
        var v = 0.0
        var s = 0.0
        while (n != null) {
            val dt = n.key - r.key

            s += r.value * dt * dt / 2.0 + v * dt
            v += r.value * dt
            r = n
            n = fs.higherEntry(n.key)
        }

        val dt = (t - r.key)
        return s + r.value * dt * dt / 2.0 + v * dt
    }

    override fun v(t: Time): R {
        throw NotImplementedError()
    }

    override fun a(t: Time) = forces.floorEntry(t)?.value ?: 0.0

    fun consolidate(t: Time) {
        forces[t] = a(t)
        forces.headMap(t, false).clear()
    }
}

/**
 * Applies constant values at given times.
 */
@Serializable
data class Const(val s: R, val v: R, val f: R) : Sim {
    override fun s(t: Time) = s

    override fun v(t: Time) = v

    override fun a(t: Time) = f
}