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

/**
 * Applies gravity between two bodies.
 * @param body The first body.
 * @param reference The second body.
 * @param gravityConstant The gravity constant of the body.
 */
fun accGravity(body: Body, reference: Body, gravityConstant: R = 6.674e-11): R2R = { t ->
    val dir = body.pos(t) - reference.pos(t)
    if (dir.squaredLength == 0.0)
        Vec.zero to zero
    else {
        val (_, m1) = body.comAndMass(t)
        dir.normalized() * (gravityConstant * m1 / dir.squaredLength) to zero
    }
}

/**
 * Returns acceleration for a given global directional acceleration.
 * @param direction The direction of the acceleration. Should be normalized.
 * @param acceleration The scalar acceleration in that direction.
 */
fun accDir(direction: AtT<R2>, acceleration: AtT<R>): R2R = { t ->
    val dir = direction(t)
    val acc = acceleration(t)
    dir * acc to zero
}

/**
 * Returns acceleration and rotational acceleration for the given applied local force.
 * @param reference The reference body, used for rotation, mass and center of mass.
 * @param displacement The displacement of the force relative to the reference.
 * @param direction The direction of the force, relative to reference. Should be normalized.
 * @param force The scalar force in the direction at the displacement.
 */
fun accLocal(reference: Body, displacement: AtT<R2>, direction: AtT<R2>, force: AtT<R>): R2R = { t ->
    val (com, m) = reference.comAndMass(t)
    val rot = reference.rot(t)
    val acc = direction(t).rotate(rot) * force(t) / m
    val d = (displacement(t) - com).rotate(rot)
    val accRot = d.x * acc.y - d.y * acc.x
    acc to accRot
}

/**
 * Calculates the force@t for a reference body and the Isp at a given point and time.
 * @param reference The reference body, used for position for the Isp value.
 * @param isp The Isp at a position and time.
 * @param massFlow The mass flow of the propellant.
 * @param standardGravity The standard gravity, defaults to 10.0.
 */
fun forceFromIsp(reference: Body, isp: AtTAndP<R>, massFlow: AtT<R>, standardGravity: R = 10.0): AtT<R> = { t ->
    standardGravity * isp(t, reference.pos(t)) * -massFlow(t)
}

/**
 * Returns the center of mass and total mass from a set of displaced masses.
 */
fun comAndMassFrom(masses: List<R2R>): R2R = { t ->
    val resolved = masses.map { it(t) }
    val total = resolved.fold(zero) { l, r -> l + r.second }
    val com = resolved.fold(Vec.zero) { l, r -> l + r.first * r.second / total }
    com to total
}


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
    var masses = emptyList<R2R>()

    var accelerators = emptyList<R2R>()

    override val comAndMass get() = comAndMassFrom(masses)

    internal data class Stored(
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

    override val accAndRotAcc = { t: R ->
        accelerators.fold(Vec.zero to 0.0) { (la, lr), r ->
            r(t).let { (ra, rr) ->
                la + ra to lr + rr
            }
        }
    }
}

