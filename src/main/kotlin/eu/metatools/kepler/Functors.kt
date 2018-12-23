package eu.metatools.kepler

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

    /**
     * Applies gravity between two bodies.
     */
    fun dsAcc(body: DSVec, bodyMass: DS, ref: DSVec) =
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

    /**
     * Returns acceleration for the given applied local force.
     */
    fun dsAcc(refMass: DS, refRotation: DS, force: DSVec) =
            force.rotate(refRotation) /
                    refMass

    /**
     * Returns rotational acceleration for the given applied local force.
     */
    fun dsAccRot(refMass: DS, refRotation: DS, force: DSVec, dirToCOM: DSVec) =
            dirToCOM.rotate(refRotation) cross force.rotate(refRotation) /
                    refMass
}
