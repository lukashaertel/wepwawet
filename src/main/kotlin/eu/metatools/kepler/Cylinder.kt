package eu.metatools.kepler

import org.apache.commons.math3.util.FastMath.sqrt

/**
 * Floating point range of doubles.
 */
typealias DoubleRange = ClosedFloatingPointRange<Double>

/**
 * Infinite range.
 */
val always = Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY

/**
 * True if double range is non-empty infinite range.
 */
fun DoubleRange.isAlways() =
        start.isInfinite() && endInclusive.isInfinite() && start < endInclusive

/**
 * Intersects two sheared cylinders, returns the z-section where they intersect or null if no intersection.
 * @param posA The base position of the first cylinder.
 * @param posDotA The shear (or change rate) of the first cylinder.
 * @param radiusA The radius of the first cylinder.
 * @param posB The base position of the second cylinder.
 * @param posDotB The shear (or change rate) of the second cylinder.
 * @param radiusB The radius of the second cylinder.
 * @return Returns the first and last z position where the cylinders intersect or null.
 */
fun shearCylinderIntersection(
        posA: Vec, posDotA: Vec, radiusA: Double,
        posB: Vec, posDotB: Vec, radiusB: Double): DoubleRange? {
    // Difference of position and shear (or velocity).
    val d = posA - posB
    val dDot = posDotA - posDotB

    // Squared radius
    val rSquared = (radiusA + radiusB).squared()

    // d², d°d', d'²
    val dSquared = d dot d
    val dDDot = d dot dDot
    val dDotSquared = dDot dot dDot

    if (dDotSquared == 0.0)
        return always.takeIf { dSquared <= rSquared }

    // Part before root
    val minusPOverTwo = -dDDot / dDotSquared

    // First part in root.
    val pOverTwoSquared = minusPOverTwo * minusPOverTwo

    // Second part in root.
    val q = (dSquared - rSquared) / dDotSquared

    // If root would be less than zero, no solution possible.
    if (q > pOverTwoSquared)
        return null

    // Shift from first part.
    val i = sqrt(pOverTwoSquared - q)

    // Return range.
    return (minusPOverTwo - i)..(minusPOverTwo + i)
}