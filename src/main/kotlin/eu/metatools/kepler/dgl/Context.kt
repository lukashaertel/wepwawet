package eu.metatools.kepler.dgl

import eu.metatools.kepler.tools.Vec
import eu.metatools.kepler.tools.times

/**
 * Computation context single body.
 */
data class Context(val t: Double, val pos: Vec, val rot: Double, val vel: Vec, val velRot: Double) {
    companion object {
        /**
         * Linear interpolation for [t] between 0.0 and 1.0.
         */
        fun lerp(t: Double, a: Context, b: Context) =
                (1.0 - t).let { it ->
                    Context(it * a.t + t * b.t,
                            it * a.pos + t * b.pos,
                            it * a.rot + t * b.rot,
                            it * a.vel + t * b.vel,
                            it * a.velRot + t * b.velRot)
                }

        /**
         * Linear interpolation for [t] between [u] and [v].
         */
        fun lerp(t: Double, u: Double, a: Context, v: Double, b: Context) =
                if (u == v)
                    a
                else
                    lerp((t - u) / (v - u), a, b)
    }

}

/**
 * Converts the context to a double array of position, rotation and derivatives. Drops [t].
 */
fun Context.toDoubleArray() =
        doubleArrayOf(
                pos.x, pos.y, rot,
                vel.x, vel.y, velRot)

//  0        1        2       0        1        2
//  x  y  r  x  y  r  x  y  r x' y' r' x' y' r' x' y' r'
//                          size * 3

fun List<Context>.toDoubleArray(): DoubleArray {
    return DoubleArray(size * 6) {
        if (it < size * 3)
            when (it.rem(3)) {
                0 -> get(it / 3).pos.x
                1 -> get(it / 3).pos.y
                else -> get(it / 3).rot
            }
        else
            when (it.rem(3)) {
                0 -> get(it / 3 - size).vel.x
                1 -> get(it / 3 - size).vel.y
                else -> get(it / 3 - size).velRot
            }
    }
}

/**
 * With [t], converts the contents of the array to a context.
 */
fun DoubleArray.toContext(t: Double) =
        Context(t, Vec(get(0), get(1)), get(2), Vec(get(3), get(4)), get(5))

fun DoubleArray.toContexts(t: Double) =
        List(size / 6) {
            Context(t,
                    Vec(get(it * 3 + 0), get(it * 3 + 1)), get(it * 3 + 2),
                    Vec(get(size / 2 + it * 3 + 0), get(size / 2 + it * 3 + 1)), get(size / 2 + it * 3 + 2))
        }