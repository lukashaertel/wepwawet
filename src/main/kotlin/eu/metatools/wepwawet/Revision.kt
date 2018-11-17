package eu.metatools.wepwawet

import com.google.common.collect.ComparisonChain

/**
 * Revision with author in [eu.metatools.rome.Repo].
 */
data class Revision(
        val timestep: Timestep,
        val inner: Inner,
        val author: Author) : Comparable<Revision> {
    override fun compareTo(other: Revision) = ComparisonChain.start()
            .compare(timestep, other.timestep)
            .compare(inner, other.inner)
            .compare(author, other.author)
            .result()

    override fun toString() = "$timestep.$inner/$author"
}