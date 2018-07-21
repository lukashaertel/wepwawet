package eu.metatools.voronois.tools

import eu.metatools.common.asyncReduce
import eu.metatools.common.awaitAll
import eu.metatools.common.splits
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.util.*

typealias Cost = Int

interface Prop {
    val cost: Cost
}

interface Distributor<P : Prop> {
    val skipVisited: Boolean
    /**
     * True if the distributor does not write this property.
     */
    fun valid(from: P, at: XY): Boolean

    /**
     * Distributes a property to a point
     */
    fun next(from: P, at: XY, to: XY): P
}

        /**
         * Initial setup: origin point, property at origin point and distributor configuration
         */
typealias Init<P> = Triple<XY, P, Distributor<P>>

        /**
         * Point extension function
         */
typealias Extend = (XY) -> List<XY>

/**
 * Computes a prop field based on initial properties at positions and some distributors. For each point, the extension
 * is computed and analyzed based on the given distributor.
 * @param inits The initial properties at positions with the associated distributions.
 * @param size The size of the resulting field.
 * @param extend The point extension, does not need to remain in bounds, default is [XY.around].
 * @param comparisonUnits The number of points compared by one job when merging.
 */
inline fun <reified P : Prop> propField(
        inits: List<Init<P>>, size: Size,
        noinline extend: Extend = XY::around,
        comparisonUnits: Int = 512): Deferred<Field<P?>> {
    // No inits given, field will remain empty.
    if (inits.isEmpty())
        return async {
            Field.create<P?>(size, null)
        }
    // TODO: Include distribution power (i.e., fractional value of how many extensions a point has)
    // TODO: Generalize merging of fields.

    val processes = inits.map { (oxy, op, d) ->
        async {
            // Initialize result field, with initial point set.
            val target = Field.create<P?>(size, null)

            // Initialize queue with initial point, if it is inside bounds and valid.
            val queue = LinkedList<Pair<P, XY>>()
            if (oxy in target && d.valid(op, oxy)) {
                target[oxy] = op
                queue.offer(op to oxy)
            }

            if (d.skipVisited)
                while (!queue.isEmpty()) {
                    // While there are points to handle, take one point.
                    val (p, xy) = queue.poll()

                    // Extend point.
                    for (e in extend(xy)) {
                        // Off bounds, skip.
                        if (e !in target)
                            continue

                        // If assignment is present, skip.
                        if (target[e] != null)
                            continue

                        // Get candidate for write, if invalid, skip.
                        val next = d.next(p, xy, e)
                        if (!d.valid(next, e))
                            continue

                        // Write target and offer as next origin.
                        target[e] = next
                        queue.offer(next to e)
                    }
                }
            else
                while (!queue.isEmpty()) {
                    // While there are points to handle, take one point.
                    val (p, xy) = queue.poll()


                    // Extend point.
                    for (e in extend(xy)) {
                        // Off bounds, skip.
                        if (e !in target)
                            continue

                        val next = d.next(p, xy, e)
                        if (!d.valid(next, e))
                            continue

                        // Get existing and distributed assignment.
                        val existing = target[e]

                        // If there is an assignment that is better, skip.
                        if (existing != null && next.cost >= existing.cost)
                            continue

                        // Write target and offer as next origin.
                        target[e] = next
                        queue.offer(next to e)
                    }
                }

            // After the process, target is assigned and the return value.
            target
        }
    }

    // Merge results and return.
    return asyncReduce(processes) { a, b ->
        // Split points for parallel computation.
        a.points().splits(comparisonUnits).map {
            async {
                // For all points in the subsection, find best.
                for ((p, va) in it) {
                    val vb = b[p]

                    // Value is overwritten if second is the only defined value or a better value.
                    if (va == null && vb != null)
                        a[p] = vb
                    else if (va != null && vb != null && vb.cost < va.cost)
                        a[p] = vb

                }
            }
        }.awaitAll()

        // After completion, return value of first, as it is the target field.
        a
    }
}