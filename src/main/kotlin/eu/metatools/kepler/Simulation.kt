package eu.metatools.kepler

import java.util.*

interface Body {
    /**
     * Center of mass and mass a time.
     */
    val comAndMass: R2R

    /**
     * Position at time.
     */
    val pos: AtT<R2>

    /**
     * Rotation at time.
     */
    val rot: AtT<R>

    /**
     * Velocity at time.
     */
    val vel: AtT<R2>

    /**
     * Rotational velocity at time.
     */
    val velRot: AtT<R>

    /**
     * Acceleration and rotational acceleration at time.
     */
    val accAndRotAcc: R2R
}

typealias R2R = AtT<Pair<R2, R>>

class Settable<T>(val default: T, val invalidates: ComplexBody) : AtT<T> {
    private val backing = TreeMap<R, T>()

    fun set(time: R, value: T) {
        if (backing.floorEntry(time)?.value != value) {
            invalidates.invalidateFrom(time)
            backing[time] = value
        }
    }

    override fun invoke(time: R): T {
        return backing.floorEntry(time)?.value ?: default
    }
}

data class ConstantBody(
        override val comAndMass: R2R,
        override val pos: AtT<R2>,
        override val rot: AtT<R>,
        override val vel: AtT<R2>,
        override val velRot: AtT<R>,
        override val accAndRotAcc: R2R) : Body

class ComplexBody(
        val initialT: R,
        val initialPos: R2,
        val initialRot: R,
        val initialVel: R2,
        val initialVelRot: R,
        val resolution: R) : Body {

    private data class Stored(
            val pos: R2,
            val rot: R,
            val vel: R2,
            val velRot: R)

    private val backing = TreeMap<R, Stored>()

    private fun calcTo(time: R) {
        if (backing.isEmpty())
            backing[initialT] = Stored(initialPos, initialRot, initialVel, initialVelRot)

        var last = backing.lastEntry()
        while (last.key < time) {
            val (acc, accRot) = accAndRotAcc(last.key)
            val newPos = last.value.pos + acc * (resolution * resolution / 2.0) + last.value.vel * resolution
            val newRot = last.value.rot + accRot * (resolution * resolution / 2.0) + last.value.velRot * resolution
            val newVel = last.value.vel + acc * resolution
            val newVelRot = last.value.velRot + accRot * resolution

            backing[last.key + resolution] = Stored(newPos, newRot, newVel, newVelRot)
            last = backing.lastEntry()
        }
    }

    var masses = emptyList<R2R>()

    var accelerators = emptyList<R2R>()

    fun invalidateFrom(time: R) =
            backing.tailMap(time, true).clear()

    fun invalidateTo(time: R) =
            backing.headMap(time, false).clear()

    override val pos: AtT<R2>
        get() = { t ->
            calcTo(t)
            backing[t]?.pos ?: backing.liftTwo(t) { x0, y0, x1, y1, x ->
                linearInterp(x0, y0.pos, x1, y1.pos, x)
            }
        }

    override val rot: AtT<R>
        get() = { t ->
            calcTo(t)
            backing[t]?.rot ?: backing.liftTwo(t) { x0, y0, x1, y1, x ->
                linearInterp(x0, y0.rot, x1, y1.rot, x)
            }
        }

    override val vel: AtT<R2>
        get() = { t ->
            calcTo(t)
            backing[t]?.vel ?: backing.liftTwo(t) { x0, y0, x1, y1, x ->
                linearInterp(x0, y0.vel, x1, y1.vel, x)
            }
        }

    override val velRot: AtT<R>
        get() = { t ->
            calcTo(t)
            backing[t]?.velRot ?: backing.liftTwo(t) { x0, y0, x1, y1, x ->
                linearInterp(x0, y0.velRot, x1, y1.velRot, x)
            }
        }

    override val comAndMass get() = comAndMassFrom(masses)

    override val accAndRotAcc = { t: R ->
        accelerators.fold(Vec.zero to 0.0) { (la, lr), r ->
            r(t).let { (ra, rr) ->
                la + ra to lr + rr
            }
        }
    }
}

