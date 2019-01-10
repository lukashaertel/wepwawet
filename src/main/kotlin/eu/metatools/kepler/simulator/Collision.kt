package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec
import eu.metatools.kepler.tools.getIn
import eu.metatools.kepler.tools.tripleProduct


private fun windingCW(a: Vec, b: Vec, c: Vec): Boolean {
    val e0 = (b.x - a.x) * (b.y + a.y)
    val e1 = (c.x - b.x) * (c.y + b.y)
    val e2 = (a.x - c.x) * (a.y + c.y)
    return e0 + e1 + e2 >= 0.0
}

private const val epsilon = 1e-6

/**
 * A penetration [point] of [depth].
 */
data class Penetration(val point: Vec, val depth: Double)

/**
 * A collision result with a main axis and all points.
 */
data class Collision(
        val axis: Penetration,
        val centerA: Vec, val centerB: Vec,
        val simplex: MutableList<Vec>)

/**
 * Gilbert–Johnson–Keerthi distance algorithm.
 * @author Lukas Härtel
 * @author Inspired by [Kenton Hamaluik](https://blog.hamaluik.ca/posts/building-a-collision-engine-part-1-2d-gjk-collision-detection/)
 */
fun gjk(posA: Vec, rotA: Double, shapeA: Convex,
        posB: Vec, rotB: Double, shapeB: Convex): Collision? {
    // If one of the bodies is empty, return null.
    if (shapeA.points.isEmpty())
        return null
    if (shapeB.points.isEmpty())
        return null


    // Get absolute shapes.
    val absA = shapeA.map { it.rotate(rotA) + posA }
    val absB = shapeB.map { it.rotate(rotB) + posB }

    // Triangle that is being built.
    lateinit var triA: Vec
    lateinit var triB: Vec
    lateinit var triC: Vec
    var vl = 0

    // Direction for support picking.
    var direction = Vec.zero


    // Simplex evolution.
    loop@ while (true) {
        when (vl) {
            0 -> direction = posB - posA
            1 -> direction = -direction
            2 -> {
                val cb = triB - triA
                val c0 = -triA

                direction = tripleProduct(cb, c0, cb)
            }
            3 -> {
                val a0 = -triC
                val ab = triB - triC
                val ac = triA - triC

                val abPerp = tripleProduct(ac, ab, ab)
                val acPerp = tripleProduct(ab, ac, ac)

                when {
                    abPerp dot a0 > 0 -> {
                        vl--
                        direction = abPerp
                    }
                    acPerp dot a0 > 0 -> {
                        triB = triC
                        vl--
                        direction = acPerp
                    }
                    else -> break@loop
                }
            }
        }

        val new = supportMD(absA, absB, direction)
        if (new dot direction < 0.0)
            return null

        when (vl) {
            0 -> triA = new
            1 -> triB = new
            2 -> triC = new
        }

        vl++
    }

    // Compute initial simplex from triangle points and account for misaligned winding.
    val simplex = if (windingCW(triA, triB, triC))
        mutableListOf(triA, triB, triC)
    else
        mutableListOf(triA, triC, triB)

    // Iterate at max the size of the polygons.
    repeat(arityMD(absA, absB) + simplex.size) {
        // Select edge of minimum distance to origin.
        val (i, norm, dist) = simplex.mapIndexed { i, v ->
            // Compute normal and distance
            val norm = (simplex.getIn(i + 1) - v).normalized().normal()
            val dist = v dot norm

            // Return edge.
            Triple(i, norm, dist)
        }.minBy { (_, _, d) -> d } ?: throw IllegalStateException("No vertices given")

        // Get support in direction of normal and distance of that point.
        val support = supportMD(absA, absB, norm)
        val distSupport = support dot norm

        when {
            // TODO: Contract existing simplex.
            distSupport < dist - epsilon -> throw IllegalStateException("Simplex out of bounds")
            distSupport > dist + epsilon -> simplex.add(i + 1, support)

            else -> {
                // Get opposite support distances.
                val distSupportA = absA.support(norm) dot norm
                val distSupportB = absB.support(-norm) dot norm

                val centerA = absA.points.fold(Vec.zero to 0.0) { (c, s), r ->
                    val m = -(r dot norm) + distSupportB
                    if (m < 0.0)
                        c + r * m to s + m
                    else
                        c to s
                }.let { (c, s) -> c / s }

                val centerB = absB.points.fold(Vec.zero to 0.0) { (c, s), r ->
                    val m = -(r dot norm) + distSupportA
                    if (m > 0.0)
                        c + r * m to s + m
                    else
                        c to s
                }.let { (c, s) -> c / s }

                // Support on boundary by epsilon, collision occurred with given separating axis.
                return Collision(Penetration(norm, dist), centerA, centerB, simplex)
            }
        }
    }

    return null
}
//// Compute penetrations.
//val penetrationA = absA.points.mapNotNull {
//    Penetration(it, -(it dot norm) + distSupportB).takeIf { it.depth < 0.0 }
//}
//
//val penetrationB = absB.points.mapNotNull {
//    Penetration(it, (it dot -norm) + distSupportA).takeIf { it.depth > 0.0 }
//}
//

fun main(args: Array<String>) {
    val a = Convex.rect(1, 1)
    val b = Convex.diamond(1, 1)
    val x = gjk(
            Vec.zero, 0.0, a,
            Vec.right * 0.8, 0.0, b)
    println(x)
}