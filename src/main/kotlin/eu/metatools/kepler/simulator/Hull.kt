package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec
import eu.metatools.kepler.tools.sampleDouble
import org.apache.commons.math3.util.FastMath.sqrt

/**
 * Convex part of a hull.
 */
data class Convex(val points: List<Vec>) {
    companion object {
        /**
         * Empty convex shape.
         */
        val empty = Convex(emptyList())

        /**
         * Rectangle of width [w] and height [h].
         */
        fun rect(w: Number, h: Number): Convex {
            val rw = w.toDouble() / 2.0
            val rh = h.toDouble() / 2.0
            return Convex(listOf(
                    Vec(-rw, -rh),
                    Vec(-rw, rh),
                    Vec(rw, rh),
                    Vec(rw, -rh)))
        }

        /**
         * Diamond of width [w] and height [h].
         */
        fun diamond(w: Number, h: Number): Convex {
            val rw = w.toDouble() / 2.0
            val rh = h.toDouble() / 2.0
            return Convex(listOf(
                    Vec(-rw, 0),
                    Vec(0, rh),
                    Vec(rw, 0),
                    Vec(0, -rh)))
        }


        /**
         * Circle of radius [r] and subdivided into [seg] segments.
         */
        fun circle(r: Number, seg: Int = 64) =
                Convex((0.0..2.0 * Math.PI).sampleDouble(seg).map {
                    Vec.right.rotate(-it) * r
                })
    }

    /**
     * Support in given [direction].
     */
    fun support(direction: Vec) =
            points.maxBy { it dot direction } ?: Vec.NaN

    /**
     * Support vector and index in given [direction].
     */
    fun supportWithIndex(direction: Vec) =
            points.withIndex().maxBy { (_, v) -> v dot direction } ?: IndexedValue(-1, Vec.NaN)

    /**
     * Bounds of the the shape.
     */
    val bounds by lazy {
        points.map { it.squaredLength }.max()?.let { sqrt(it) } ?: 0.0
    }
}

/**
 * Applies the function to all points.
 */
fun Convex.map(block: (Vec) -> Vec) =
        Convex(points.map(block))

/**
 * Applies the filter to all points.
 */
fun Convex.filter(block: (Vec) -> Boolean) =
        Convex(points.filter(block))

/**
 * Returns the value of the Minkowski difference in [direction] for shapes [convexB] subtracted from [convexA].
 */
fun supportMD(convexA: Convex, convexB: Convex, direction: Vec) =
        convexA.support(direction) - convexB.support(-direction)

/**
 * Gets the maximum size of the simplex described by the Minkowski difference.
 */
fun arityMD(convexA: Convex, convexB: Convex) =
        convexA.points.size + convexB.points.size

/**
 * Hull consisting of multiple convex parts.
 */
data class Hull(val parts: List<Convex>) {
    companion object {
        /**
         * Empty hull.
         */
        val empty = Hull(emptyList())
    }

    /**
     * Bounds of the the hull.
     */
    val bounds by lazy {
        parts.map { it.bounds }.max() ?: 0.0
    }
}

/**
 * Applies the function to all parts.
 */
fun Hull.map(block: (Vec) -> Vec) =
        Hull(parts.map { it.map(block) })

/**
 * Applies the filter to all parts.
 */
fun Hull.filter(block: (Vec) -> Boolean) =
        Hull(parts.map { it.filter(block) })