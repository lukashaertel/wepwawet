package eu.metatools.kepler

import eu.metatools.kepler.tools.Vec
import eu.metatools.kepler.math.stepOnSmooth
import org.apache.commons.math3.util.FastMath.sqrt

const val gravityConstant: Double = 6.674e-11

/**
 * The [Double] squared.
 */
fun Double.squared() = this * this

/**
 * The [Double] cubed.
 */
fun Double.cubed() = this * this * this

/**
 * The square root of the [Double] cubed.
 */
fun Double.cubedSqrt() = sqrt(cubed())

/**
 * Gravity accelerator functor, based on
 * * Body position
 * * Body mass
 * * Reference position
 *
 * Implicitly relying on [gravityConstant].
 */
object Gravity {
    /**
     * Applies gravity between two bodies.
     */
    fun acc(body: Vec, bodyMass: Double, ref: Vec) =
            (body - ref) * gravityConstant * bodyMass /
                    (body - ref).length.cubed()
}

/**
 * Local accelerator functors, based on
 * * Reference mass
 * * Reference rotation
 * * Force vector
 */
object Local {
    /**
     * Returns acceleration for the given applied local force.
     */
    fun acc(refMass: Double, refRotation: Double, force: Vec) =
            force.rotate(refRotation) /
                    refMass

    /**
     * Returns rotational acceleration for the given applied local force.
     */
    fun accRot(refMass: Double, refRotation: Double, force: Vec, dirToCOM: Vec) =
            dirToCOM.rotate(refRotation) cross force.rotate(refRotation) /
                    refMass
}

/**
 * Directional force.
 */
object Force {
    /**
     * Computes the force at [t] to achieve an impulse change of [impulse] at [t0] (over [duration] seconds).
     */
    fun forceForImpulse(impulse: Vec, t0: Double, t: Double, duration: Double = 1.0) =
            impulse * (stepOnSmooth(t, t0 - 0.5 * duration) - stepOnSmooth(t, t0 + 0.5 * duration)) / duration

    /**
     * Returns acceleration for the given applied directional force.
     */
    fun acc(refMass: Double, force: Vec) =
            force / refMass
}