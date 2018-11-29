package eu.metatools.kepler

import java.lang.StrictMath.*
import java.util.*

// TODO: MODELING :(
interface Translatable<T> {
    operator fun plus(other: T): T

    operator fun minus(other: T): T
}

interface Scalable<T> {
    operator fun times(scale: R): T

    operator fun div(scale: R): T
}


data class Vec(val x: R, val y: R) : Translatable<Vec>, Scalable<Vec> {
    companion object {
        val left = Vec(-1.0, 0.0)

        val up = Vec(0.0, 1.0)

        val right = Vec(1.0, 0.0)

        val down = Vec(0.0, -1.0)

        val zero = Vec(0.0, 0.0)
    }

    override operator fun plus(other: Vec) = Vec(x + other.x, y + other.y)

    override operator fun minus(other: Vec) = Vec(x - other.x, y - other.y)

    override operator fun times(scale: R) = Vec(x * scale, y * scale)

    override operator fun div(scale: R) = Vec(x / scale, y / scale)

    operator fun unaryMinus() = Vec(-x, -y)

    fun normal() = Vec(-y, x)

    fun antinormal() = Vec(y, -x)

    fun rotate(angle: R): Vec {
        val ca = cos(angle)
        val sa = sin(angle)
        return Vec(x * ca - y * sa, x * sa + y * ca)
    }

    fun normalized() = length.let { Vec(x / it, y / it) }

    infix fun dot(other: Vec) = x * x + y * y

    val squaredLength by lazy { x * x + y * y }

    val length by lazy { sqrt(squaredLength) }

    val angle by lazy { atan2(y, x) }
}


typealias R = Double

typealias R2 = Vec

operator fun <T> ((T) -> R).times(r: R): (T) -> R = { t -> this(t) * r }
operator fun <T> ((T) -> R).div(r: R): (T) -> R = { t -> this(t) / r }
operator fun <T> ((T) -> R).plus(r: R): (T) -> R = { t -> this(t) + r }
operator fun <T> ((T) -> R).minus(r: R): (T) -> R = { t -> this(t) - r }
operator fun <T> ((T) -> R).unaryMinus(): (T) -> R = { t -> -this(t) }

// TODO: Reference systems (what if position is needed, optional, etc).
typealias AtT<T> = (time: R) -> T

typealias AtP<T> = (pos: R2) -> T

typealias AtTAndP<T> = (time: R, pos: R2) -> T

fun <T> constT(value: T): AtT<T> = { value }

fun <T> constP(value: T): AtP<T> = { value }

fun <T> constTAndP(value: T): AtTAndP<T> = { _, _ -> value }

fun <T> AtT<T>.constP(): AtTAndP<T> = { time, _ -> this(time) }

fun <T> AtP<T>.constT(): AtTAndP<T> = { _, pos -> this(pos) }

const val zero: R = 0.0

const val epsilon: R = 0.0001

val epsilonX = Vec(epsilon, zero)

val epsilonY = Vec(zero, epsilon)

val AtT<R>.ddtR
    get(): AtT<R> = { t ->
        (this(t) - this(t - epsilon)) / epsilon
    }

val AtP<R>.ddxR
    get(): AtP<R> = { t ->
        (this(t) - this(t - epsilonX)) / epsilon
    }

val AtP<R>.ddyR
    get(): AtP<R> = { t ->
        (this(t) - this(t - epsilonY)) / epsilon
    }

val <T> AtT<T>.ddt where T : Scalable<T>, T : Translatable<T>
    get(): AtT<T> = { t ->
        (this(t) - this(t - epsilon)) / epsilon
    }

val <T> AtP<T>.ddx where T : Scalable<T>, T : Translatable<T>
    get(): AtP<T> = { t ->
        (this(t) - this(t - epsilonX)) / epsilon
    }

val <T> AtP<T>.ddy where T : Scalable<T>, T : Translatable<T>
    get(): AtP<T> = { t ->
        (this(t) - this(t - epsilonY)) / epsilon
    }

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
fun accGravity(body: Body, reference: Body, gravityConstant: AtT<R>): R2R = { t ->
    val dir = body.pos(t) - reference.pos(t)
    val g = gravityConstant(t)
    val (_, m1) = body.comAndMass(t)
    val (_, m2) = reference.comAndMass(t)
    dir.normalized() * (g * m1 * m2 / dir.squaredLength) to zero
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

inline fun <K, V, R> NavigableMap<K, V>.inex(k: K, interp: (K, V, K, V, K) -> R): R {
    // Next higher and lower entry
    val nhe = ceilingEntry(k)
    val nle = floorEntry(k)

    return if (nhe == null && nle == null) {
        throw IllegalStateException("empty")
    } else if (nhe == null) {
        // Next next lower entry
        val nnle = lowerEntry(nle.key)

        // If only one, interpolate between single entry, otherwise between both.
        if (nnle == null)
            interp(nle.key, nle.value, nle.key, nle.value, k)
        else
            interp(nnle.key, nnle.value, nle.key, nle.value, k)
    } else if (nle == null) {
        // Next next higher entry
        val nnhe = higherEntry(nhe.key)

        // If only one, interpolate between single entry, otherwise between both.
        if (nnhe == null)
            interp(nhe.key, nhe.value, nhe.key, nhe.value, k)
        else
            interp(nhe.key, nhe.value, nnhe.key, nnhe.value, k)
    } else {
        // Both present
        interp(nle.key, nle.value, nhe.key, nhe.value, k)
    }
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

    internal val backing = TreeMap<R, Stored>()

    internal fun calcTo(time: R) {
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

    internal fun invalidateFrom(time: R) =
            backing.tailMap(time, true).clear()

    override val pos: AtT<R2>
        get() = { t ->
            calcTo(t)
            backing.inex(t) { t1, a, t2, b, tAt ->
                if (t1 == t2)
                    a.pos
                else
                    a.pos + (b.pos - a.pos) * (tAt - t1) / (t2 - t1)
            }
        }

    override val rot: AtT<R>
        get() = { t ->
            calcTo(t)
            backing.inex(t) { t1, a, t2, b, tAt ->
                if (t1 == t2)
                    a.rot
                else
                    a.rot + (b.rot - a.rot) * (tAt - t1) / (t2 - t1)
            }
        }

    override val vel: AtT<R2>
        get() = { t ->
            calcTo(t)
            backing.inex(t) { t1, a, t2, b, tAt ->
                if (t1 == t2)
                    a.vel
                else
                    a.vel + (b.vel - a.vel) * (tAt - t1) / (t2 - t1)
            }
        }

    override val velRot: AtT<R>
        get() = { t ->
            calcTo(t)
            backing.inex(t) { t1, a, t2, b, tAt ->
                if (t1 == t2)
                    a.velRot
                else
                    a.velRot + (b.velRot - a.velRot) * (tAt - t1) / (t2 - t1)
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

