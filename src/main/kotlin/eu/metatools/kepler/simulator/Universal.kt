package eu.metatools.kepler.simulator

/**
 * Universal effect specification on single bodies with respect to N other bodies..
 */
interface Universal {
    /**
     * Apply N body effect on the receiver with [other] as the context.
     */
    fun universal(on: Receiver, other: List<Body>, t: Double) = Unit
}