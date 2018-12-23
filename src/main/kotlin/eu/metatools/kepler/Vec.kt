package eu.metatools.kepler

// Note: R and toR are used to allow moving quickly to other data types by changing conversion and type.

/**
 * Alias for type of value.
 */
typealias R = Double

const val zero: R = 0.0


/**
 * Converts a number to a value of [R].
 */
fun Number.toR() = toDouble()

/**
 * Converts a double array to its component.
 */
fun DoubleArray.toR() = single()


/**
 * Vector name alias.
 */
typealias R2 = Vec

/**
 * Converts a double array to a vector.
 */
fun DoubleArray.toVec() = Vec(get(0), get(1))

/**
 * Two dimensional vector class.
 */
data class Vec(val x: R, val y: R) {
    /**
     * Constructs a vector from arbitrary scalars as components [x] and [y].
     */
    constructor(x: Number, y: Number) : this(x.toR(), y.toR())

    companion object {
        /**
         * Zero-vector.
         */
        val zero = Vec(0.toR(), 0.toR())

        /**
         * Left-vector, [x] is negative one.
         */
        val left = Vec((-1).toR(), 0.toR())

        /**
         * Up vector, [y] is one.
         */
        val up = Vec(0.toR(), 1.toR())

        /**
         * Right-vector, [x] is one.
         */
        val right = Vec(1.toR(), 0.toR())

        /**
         * Up vector, [y] is negative one.
         */
        val down = Vec(0.toR(), (-1).toR())
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
    operator fun times(scale: R) =
            Vec(x * scale, y * scale)

    /**
     * Division of [x] and [y] components by a scalar.
     */
    operator fun div(scale: R) =
            Vec(x / scale, y / scale)

    /**
     * Multiplication of [x] and [y] components with an arbitrary scalar.
     */
    operator fun times(scale: Number) =
            scale.toR().let {
                Vec(x * it, y * it)
            }

    /**
     * Division of [x] and [y] components by a scalar.
     */
    operator fun div(scale: Number) =
            scale.toR().let {
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
    fun rotate(angle: R): Vec {
        val ca = StrictMath.cos(angle)
        val sa = StrictMath.sin(angle)
        return Vec(x * ca - y * sa, x * sa + y * ca)
    }

    /**
     * True if vector is zero on [x] and [y].
     */
    fun isEmpty() =
            x == 0.toR() && y == 0.toR()

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
    val length by lazy { StrictMath.hypot(x, y) }

    /**
     * Angle of vector.
     */
    val angle by lazy { StrictMath.atan2(y, x) }

    /**
     * Formats the vector.
     */
    override fun toString() =
            "($x, $y)"
}

/**
 * Multiplication of a scalar and [Vec.x] and [Vec.y] components of [vec].
 */
operator fun R.times(vec: Vec) =
        Vec(vec.x * this, vec.y * this)

/**
 * Multiplication of an arbitrary scalar and [Vec.x] and [Vec.y] components of [vec].
 */
operator fun Number.times(vec: Vec) =
        toR().let {
            Vec(vec.x * it, vec.y * it)
        }