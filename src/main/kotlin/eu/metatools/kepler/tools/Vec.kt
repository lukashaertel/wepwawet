package eu.metatools.kepler.tools

import eu.metatools.kepler.math.Lerp
import kotlinx.serialization.Serializable
import org.apache.commons.math3.util.FastMath.*

/**
 * Two dimensional vector class.
 */
@Serializable
data class Vec(val x: Double, val y: Double) : Iterable<Double>, Lerp<Vec> {
    /**
     * Constructs a vector from arbitrary scalars as components [x] and [y].
     */
    constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble())

    companion object {
        /**
         * NaN-vector.
         */
        val NaN = Vec(Double.NaN, Double.NaN)

        /**
         * Zero-vector.
         */
        val zero = Vec(0.0, 0.0)

        /**
         * One-vector.
         */
        val one = Vec(1.0, 1.0)

        /**
         * Left-vector, [x] is negative one.
         */
        val left = Vec(-1.0, 0.0)

        /**
         * Up vector, [y] is one.
         */
        val up = Vec(0.0, 1.0)

        /**
         * Right-vector, [x] is one.
         */
        val right = Vec(1.0, 0.0)

        /**
         * Up vector, [y] is negative one.
         */
        val down = Vec(0.0, -1.0)
    }

    /**
     * Pairwise addition of [x] and [y] components.
     */
    operator fun plus(other: Vec) =
            Vec(x + other.x, y + other.y)

    /**
     * Pairwise subtraction of [x] and [y] components.
     */
    operator fun minus(other: Vec) =
            Vec(x - other.x, y - other.y)

    /**
     * Pairwise multiplication of [x] and [y] components.
     */
    operator fun times(other: Vec) =
            Vec(x * other.x, y * other.y)

    /**
     * Pairwise division of [x] and [y] components.
     */
    operator fun div(other: Vec) =
            Vec(x / other.x, y / other.y)

    /**
     * Multiplication of [x] and [y] components with a scalar.
     */
    operator fun times(scale: Double) =
            Vec(x * scale, y * scale)

    /**
     * Division of [x] and [y] components by a scalar.
     */
    operator fun div(scale: Double) =
            Vec(x / scale, y / scale)

    /**
     * Multiplication of [x] and [y] components with an arbitrary scalar.
     */
    operator fun times(scale: Number) =
            scale.toDouble().let {
                Vec(x * it, y * it)
            }

    /**
     * Division of [x] and [y] components by a scalar.
     */
    operator fun div(scale: Number) =
            scale.toDouble().let {
                Vec(x / it, y / it)
            }

    /**
     * Inverse of vector.
     */
    operator fun unaryMinus() =
            Vec(-x, -y)

    /**
     * Vector turned 90 degrees counterclockwise.
     */
    fun normal() = Vec(-y, x)

    /**
     * Vector turned 90 degrees clockwise.
     */
    fun antinormal() = Vec(y, -x)

    /**
     * Rotates the vector by [angle].
     */
    fun rotate(angle: Double): Vec {
        val ca = cos(angle)
        val sa = sin(angle)
        return Vec(x * ca - y * sa, x * sa + y * ca)
    }

    /**
     * True if vector is zero on [x] and [y].
     */
    fun isEmpty() =
            x == 0.toDouble() && y == 0.toDouble()

    /**
     * Normalized vector if not empty, length is one.
     */
    fun normalized() =
            if (isEmpty())
                zero
            else
                length.let { Vec(x / it, y / it) }

    /**
     * Inner product of receiver and [other].
     */
    infix fun dot(other: Vec) = x * other.x + y * other.y

    /**
     * Cross product of receiver and [other].
     */
    infix fun cross(other: Vec) = x * other.y - other.x * y

    /**
     * Squared length of vector.
     */
    val squaredLength by lazy { x * x + y * y }

    /**
     * Length of vector.
     */
    val length by lazy { hypot(x, y) }

    /**
     * Angle of vector.
     */
    val angle by lazy { atan2(y, x) }

    /**
     * Returns the iterator for this vector.
     */
    override fun iterator() =
            listOf(x, y).iterator()

    /**
     * Linear interpolation to vector [to] with [x] between zero and one.
     */
    override fun lerp(to: Vec, x: Double) =
            (1.0 - x).let { ix ->
                Vec(this.x * ix + to.x * x, this.y * ix + to.y * x)
            }

    /**
     * Converts the vector to a double array.
     */
    fun toArray() =
            doubleArrayOf(x, y)

    /**
     * Formats the vector.
     */
    override fun toString() =
            "($x, $y)"
}

/**
 * Multiplication of a scalar and [Vec.x] and [Vec.y] components of [vec].
 */
operator fun Double.times(vec: Vec) =
        Vec(vec.x * this, vec.y * this)

/**
 * Multiplication of an arbitrary scalar and [Vec.x] and [Vec.y] components of [vec].
 */
operator fun Number.times(vec: Vec) =
        toDouble().let {
            Vec(vec.x * it, vec.y * it)
        }

/**
 * Vector component subscript.
 */
val Vec.xx get() = Vec(x, x)

/**
 * Vector component subscript.
 */
val Vec.xy get() = Vec(x, y)

/**
 * Vector component subscript.
 */
val Vec.yx get() = Vec(y, x)

/**
 * Vector component subscript.
 */
val Vec.yy get() = Vec(y, y)

/**
 * Triple product of vectors [a], [b] and [c].
 */
fun tripleProduct(a: Vec, b: Vec, c: Vec) =
        Vec(-(a.x * b.y - a.y * b.x) * c.y, (a.x * b.y - a.y * b.x) * c.x)

/**
 * Converts a double array to a vector.
 */
fun DoubleArray.toVec() = Vec(get(0), get(1))

/**
 * Converts a double array to a vector.
 */
fun DoubleArray.toVec(offset: Int) = Vec(get(offset), get(offset + 1))
