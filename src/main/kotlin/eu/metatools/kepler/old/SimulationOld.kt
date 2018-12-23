package eu.metatools.kepler.old

import eu.metatools.kepler.Vec
import eu.metatools.kepler.toVec
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator
import java.util.*

interface Body {
    /**
     * Center of mass at time.
     */
    val com: AtT<Vec>

    /**
     * Mass at time.
     */
    val m: AtT<Double>

    /**
     * Position at time.
     */
    val pos: AtT<Vec>

    /**
     * Rotation at time.
     */
    val rot: AtT<Double>

    /**
     * Velocity at time.
     */
    val vel: AtT<Vec>

    /**
     * Rotational velocity at time.
     */
    val velRot: AtT<Double>

    /**
     * Acceleration at time.
     */
    val acc: AtT<Vec>

    /**
     * Rotational acceleration at time.
     */
    val accRot: AtT<Double>
}

class Settable<T>(val default: T, val target: ComplexBody) : AtT<T> {
    private val backing = TreeMap<Double, T>()

    fun set(time: Double, value: T) {
        if (backing.floorEntry(time)?.value != value) {
            target.invalidateFrom(time)
            target.materialize(time)
            backing[time] = value
        }
    }

    override fun invoke(time: Double): T {
        return backing.floorEntry(time)?.value ?: default
    }
}

data class ConstantBody(
        override val com: AtT<Vec>,
        override val m: AtT<Double>,
        override val pos: AtT<Vec>,
        override val rot: AtT<Double>,
        override val vel: AtT<Vec>,
        override val velRot: AtT<Double>,
        override val acc: AtT<Vec>,
        override val accRot: AtT<Double>) : Body

data class Sample(
        val pos: Vec,
        val rot: Double,
        val vel: Vec,
        val velRot: Double,
        val acc: Vec,
        val accRot: Double)

class ComplexBody(
        val initialT: Double,
        val initialPos: Vec,
        val initialRot: Double,
        val initialVel: Vec,
        val initialVelRot: Double,
        val resolution: Double = 1.0 / 4.0) : Body {

    private val backing = TreeMap<Double, Sample>()

    var sourceCOM = emptyList<AtT<Vec>>()

    override val com: AtT<Vec>
        get() = { sourceCOM.fold(Vec.zero) { l, r -> l + r(it) } }

    var sourceMass = emptyList<AtT<Double>>()

    override val m: AtT<Double>
        get() = { sourceMass.fold(0.0) { l, r -> l + r(it) } }

    var sourceAcc = emptyList<AtT<Vec>>()

    override val acc: AtT<Vec>
        get() = { sourceAcc.fold(Vec.zero) { l, r -> l + r(it) } }

    var sourceAccRot = emptyList<AtT<Double>>()

    override val accRot: AtT<Double>
        get() = { sourceAccRot.fold(0.0) { l, r -> l + r(it) } }

    fun invalidateTo(time: Double) =
            calculatedTo(time).clear()

    fun invalidateFrom(time: Double) =
            calculatedFrom(time).clear()

    fun materialize(time: Double) {
        backing[time] = Sample(pos(time), rot(time), vel(time), velRot(time), acc(time), accRot(time))
    }

    fun calculatedTo(time: Double) =
            backing.headMap(time, false)

    fun calculatedFrom(time: Double) =
            backing.tailMap(time, true)

    fun update(time: Double) {
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

    override val pos: AtT<Vec>
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

    override val rot: AtT<Double>
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
                interpolator.value(t).single()
            }
        }

    override val vel: AtT<Vec>
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

    override val velRot: AtT<Double>
        get() = { t ->
            if (backing.isEmpty()) {
                initialVelRot
            } else {
                val interpolator = HermiteInterpolator()
                for ((st, sv) in backing.headMap(t, true))
                    interpolator.addSamplePoint(st,
                            doubleArrayOf(sv.velRot),
                            doubleArrayOf(sv.accRot))
                interpolator.value(t).single()
            }
        }
}

