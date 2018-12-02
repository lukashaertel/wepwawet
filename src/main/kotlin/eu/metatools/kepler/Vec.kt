package eu.metatools.kepler


data class Vec(val x: R, val y: R) : NumberLike<Vec> {
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
        val ca = StrictMath.cos(angle)
        val sa = StrictMath.sin(angle)
        return Vec(x * ca - y * sa, x * sa + y * ca)
    }

    fun normalized() = length.let { Vec(x / it, y / it) }

    infix fun dot(other: Vec) = x * other.x + y * other.y

    val squaredLength by lazy { x * x + y * y }

    val length by lazy { StrictMath.sqrt(squaredLength) }

    val angle by lazy { StrictMath.atan2(y, x) }
}
