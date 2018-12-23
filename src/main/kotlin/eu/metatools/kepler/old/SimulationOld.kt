package eu.metatools.kepler.old

import eu.metatools.kepler.*
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator
import java.util.*

interface Body {
    /**
     * Center of mass at time.
     */
    val com: AtT<R2>

    /**
     * Mass at time.
     */
    val m: AtT<R>

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
     * Acceleration at time.
     */
    val acc: AtT<R2>

    /**
     * Rotational acceleration at time.
     */
    val accRot: AtT<R>
}

class Settable<T>(val default: T, val target: ComplexBody) : AtT<T> {
    private val backing = TreeMap<R, T>()

    fun set(time: R, value: T) {
        if (backing.floorEntry(time)?.value != value) {
            target.invalidateFrom(time)
            target.materialize(time)
            backing[time] = value
        }
    }

    override fun invoke(time: R): T {
        return backing.floorEntry(time)?.value ?: default
    }
}

data class ConstantBody(
        override val com: AtT<R2>,
        override val m: AtT<R>,
        override val pos: AtT<R2>,
        override val rot: AtT<R>,
        override val vel: AtT<R2>,
        override val velRot: AtT<R>,
        override val acc: AtT<R2>,
        override val accRot: AtT<R>) : Body

data class Sample(
        val pos: R2,
        val rot: R,
        val vel: R2,
        val velRot: R,
        val acc: R2,
        val accRot: R)

class ComplexBody(
        val initialT: R,
        val initialPos: R2,
        val initialRot: R,
        val initialVel: R2,
        val initialVelRot: R,
        val resolution: R = 1.0 / 4.0) : Body {

    private val backing = TreeMap<R, Sample>()

    var sourceCOM = emptyList<AtT<R2>>()

    override val com: AtT<R2>
        get() = { sourceCOM.fold(Vec.zero) { l, r -> l + r(it) } }

    var sourceMass = emptyList<AtT<R>>()

    override val m: AtT<R>
        get() = { sourceMass.fold(0.toR()) { l, r -> l + r(it) } }

    var sourceAcc = emptyList<AtT<R2>>()

    override val acc: AtT<R2>
        get() = { sourceAcc.fold(Vec.zero) { l, r -> l + r(it) } }

    var sourceAccRot = emptyList<AtT<R>>()

    override val accRot: AtT<R>
        get() = { sourceAccRot.fold(0.toR()) { l, r -> l + r(it) } }

    fun invalidateTo(time: R) =
            calculatedTo(time).clear()

    fun invalidateFrom(time: R) =
            calculatedFrom(time).clear()

    fun materialize(time: R) {
        backing[time] = Sample(pos(time), rot(time), vel(time), velRot(time), acc(time), accRot(time))
    }

    fun calculatedTo(time: R) =
            backing.headMap(time, false)

    fun calculatedFrom(time: R) =
            backing.tailMap(time, true)

    fun update(time: R) {
        if (backing.isEmpty())
            materialize(initialT)

        while (backing.lastKey() + resolution <= time)
            materialize(backing.lastKey() + resolution)

//        var last = backing.lastEntry()
//        while (last.key < time) {
//
//            val (pos, rot, vel, velRot, acc, accRot) = last.value
//
//            val newPos = pos + acc * resolution * resolution / 2.0 + vel * resolution
//            val newRot = rot + accRot * resolution * resolution / 2.0 + velRot * resolution
//            val newVel = vel + acc * resolution
//            val newVelRot = velRot + accRot * resolution
//            val newAcc = acc(last.key + resolution)
//            val newAccRot = accRot(last.key + resolution)
//
//            backing[last.key + resolution] = Sample(newPos, newRot, newVel, newVelRot, newAcc, newAccRot)
//            last = backing.lastEntry()
//        }
    }

    override val pos: AtT<R2>
        get() = { t ->
            if (backing.isEmpty()) {
                initialPos
            } else {
                val interpolator = HermiteInterpolator()
                for ((st, sv) in backing.headMap(t, true))
                    interpolator.addSamplePoint(st,
                            doubleArrayOf(sv.pos.x, sv.pos.y),
                            doubleArrayOf(sv.vel.x, sv.vel.y),
                            doubleArrayOf(sv.acc.x, sv.acc.y))
                interpolator.value(t).toVec()
            }
        }

    override val rot: AtT<R>
        get() = { t ->
            if (backing.isEmpty()) {
                initialRot
            } else {
                val interpolator = HermiteInterpolator()
                for ((st, sv) in backing.headMap(t, true))
                    interpolator.addSamplePoint(st,
                            doubleArrayOf(sv.rot),
                            doubleArrayOf(sv.velRot),
                            doubleArrayOf(sv.accRot))
                interpolator.value(t).toR()
            }
        }

    override val vel: AtT<R2>
        get() = { t ->
            if (backing.isEmpty()) {
                initialVel
            } else {
                val interpolator = HermiteInterpolator()
                for ((st, sv) in backing.headMap(t, true))
                    interpolator.addSamplePoint(st,
                            doubleArrayOf(sv.vel.x, sv.vel.y),
                            doubleArrayOf(sv.acc.x, sv.acc.y))
                interpolator.value(t).toVec()
            }
        }

    override val velRot: AtT<R>
        get() = { t ->
            if (backing.isEmpty()) {
                initialVelRot
            } else {
                val interpolator = HermiteInterpolator()
                for ((st, sv) in backing.headMap(t, true))
                    interpolator.addSamplePoint(st,
                            doubleArrayOf(sv.velRot),
                            doubleArrayOf(sv.accRot))
                interpolator.value(t).toR()
            }
        }
}

