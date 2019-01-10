package eu.metatools.kepler.math

/**
 * Object that can be linearily interpolated to another instance of itself.
 */
interface Lerp<T> {
    /**
     * Interpolates between this and [to] by factor [x].
     */
    fun lerp(to: T, x: Double): T
}

/**
 * Interpolates between this and [to] using [selfX] and [toX] as the respective positions
 * and [x] as a factor within.
 */
fun <T> Lerp<T>.lerp(selfX: Double, toX: Double, to: T, x: Double): T =
        if (selfX == toX)
            to
        else
            lerp(to, (x - selfX) / (toX - selfX))

fun lerp(a: Float, to: Float, x: Double) =
        (a * (1.0 - x) + to * x).toFloat()


fun lerp(a: Double, to: Double, x: Double) =
        a * (1.0 - x) + to * x

/**
 * Converts the [Double] to and [Lerp] of [Double].
 */
fun Float.asLerp() = object : Lerp<Float> {
    override fun lerp(to: Float, x: Double) =
            lerp(this@asLerp, to, x)
}
/**
 * Converts the [Double] to and [Lerp] of [Double].
 */
fun Double.asLerp() = object : Lerp<Double> {
    override fun lerp(to: Double, x: Double) =
            lerp(this@asLerp, to, x)
}