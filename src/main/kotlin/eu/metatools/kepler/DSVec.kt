package eu.metatools.kepler

import org.apache.commons.math3.util.FastMath.cos
import org.apache.commons.math3.util.FastMath.sin

/**
 * Two dimensional vector class of derivative structures.
 */
data class DSVec(val x: DS, val y: DS) {
    /**
     * Pairwise addition of [x] and [y] components.
     */
    operator fun plus(other: DSVec) =
            DSVec(x + other.x, y + other.y)

    /**
     * Pairwise subtraction of [x] and [y] components.
     */
    operator fun minus(other: DSVec) =
            DSVec(x - other.x, y - other.y)

    /**
     * Pairwise multiplication of [x] and [y] components.
     */
    operator fun times(other: DSVec) =
            DSVec(x * other.x, y * other.y)

    /**
     * Pairwise division of [x] and [y] components.
     */
    operator fun div(other: DSVec) =
            DSVec(x / other.x, y / other.y)

    /**
     * Multiplication of [x] and [y] components with a scalar.
     */
    operator fun times(scale: Int) =
            DSVec(x * scale, y * scale)

    /**
     * Multiplication of [x] and [y] components with a scalar.
     */
    operator fun times(scale: Double) =
            DSVec(x * scale, y * scale)

    /**
     * Multiplication of [x] and [y] components with a scalar.
     */
    operator fun times(scale: DS) =
            DSVec(x * scale, y * scale)

    /**
     * Division of [x] and [y] components by a scalar.
     */
    operator fun div(scale: Double) =
            DSVec(x / scale, y / scale)

    /**
     * Division of [x] and [y] components by a scalar.
     */
    operator fun div(scale: DS) =
            DSVec(x / scale, y / scale)

    /**
     * Inverse of vector.
     */
    operator fun unaryMinus() =
            DSVec(-x, -y)

    /**
     * Vector turned 90 degrees counterclockwise.
     */
    fun normal() = DSVec(-y, x)

    /**
     * Vector turned 90 degrees clockwise.
     */
    fun antinormal() = DSVec(y, -x)

    /**
     * Rotates the vector by [angle].
     */
    fun rotate(angle: Double): DSVec {
        val ca = cos(angle)
        val sa = sin(angle)
        return DSVec(x * ca - y * sa, x * sa + y * ca)
    }

    /**
     * Rotates the vector by [angle].
     */
    fun rotate(angle: DS): DSVec {
        return DSVec(x * angle.cos() - y * angle.sin(), x * angle.sin() + y * angle.cos())
    }

    /**
     * Normalized vector if not empty, length is one.
     */
    fun normalized() =
            length.reciprocal().let {
                DSVec(x * it, y * it)
            }

    /**
     * Inner product of receiver and [other].
     */
    infix fun dot(other: DSVec) = x * other.x + y * other.y

    /**
     * Cross product of receiver and [other].
     */
    infix fun cross(other: DSVec) = x * other.y - other.x * y

    /**
     * Squared length of vector.
     */
    val squaredLength get() = x * x + y * y

    /**
     * Length of vector.
     */
    val length get() = DS.hypot(x, y)

    /**
     * Angle of vector.
     */
    val angle get() = DS.atan2(y, x)

    /**
     * Formats the vector.
     */
    override fun toString() =
            "($x, $y)"

    /**
     * Gets the materialized value.
     */
    val value
        get() =
            Vec(x.value, y.value)
}

/**
 * Multiplication of an arbitrary scalar and [DSVec.x] and [DSVec.y] components of [vec].
 */
operator fun Int.times(vec: DSVec) =
        DSVec(this * vec.x, this * vec.y)

/**
 * Multiplication of a scalar and [DSVec.x] and [DSVec.y] components of [vec].
 */
operator fun Double.times(vec: DSVec) =
        DSVec(this * vec.x, this * vec.y)

/**
 * Multiplication of a scalar and [DSVec.x] and [DSVec.y] components of [vec].
 */
operator fun DS.times(vec: DSVec) =
        DSVec(this * vec.x, this * vec.y)