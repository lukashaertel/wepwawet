package eu.metatools.kepler.tools

import org.apache.commons.math3.ml.clustering.Clusterable
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer

data class ClusterableItem<E>(val item: E, val at: DoubleArray) : Clusterable {
    override fun getPoint() = at

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClusterableItem<*>

        if (item != other.item) return false
        if (!at.contentEquals(other.at)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = item?.hashCode() ?: 0
        result = 31 * result + at.contentHashCode()
        return result
    }

}

/**
 * Applies a default clustering to [k]clusters using k-means++ and a surrogate clustering point.
 */
inline fun <E> Collection<E>.cluster(k: Int, crossinline block: (E) -> Vec): Map<Vec, List<E>> {
    // Get surrogates.
    val surrogate = map {
        ClusterableItem(it, block(it).toArray())
    }

    // Cluster surrogates.
    val clusters = KMeansPlusPlusClusterer<ClusterableItem<E>>(k).cluster(surrogate)

    // Associate surrogates by their center points.
    return clusters.associate { cluster ->
        // Get center point and unroll item surrogates.
        val center = cluster.center.point.toVec()
        val items = cluster.points.map { it.item }

        center to items
    }
}