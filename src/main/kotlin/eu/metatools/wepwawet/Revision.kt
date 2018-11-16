package eu.metatools.wepwawet

import com.google.common.collect.ComparisonChain

/**
 * Revision with author in [eu.metatools.rome.Repo].
 */
data class Revision(
        val time: Time,
        val inner: Inner,
        val author: Author) : Comparable<Revision> {
    override fun compareTo(other: Revision) = ComparisonChain.start()
            .compare(time, other.time)
            .compare(inner, other.inner)
            .compare(author, other.author)
            .result()

    override fun toString() = "$time.$inner/$author"
}