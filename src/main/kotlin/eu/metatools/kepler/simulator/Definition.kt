package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec

/**
 * Initial value specification, time source and local effect on body.
 */
abstract class Definition(
        override val bounds: Double = 1.0,
        override val hull: List<Vec> = emptyList(),
        override val pos: Vec = Vec.zero,
        override val rot: Double = 0.0,
        override val com: Vec = Vec.zero,
        override val mass: Double = 1.0,
        override val posDot: Vec = Vec.zero,
        override val rotDot: Double = 0.0,
        override val comDot: Vec = Vec.zero,
        override val massDot: Double = 0.0) : Body {
    /**
     * Time source.
     */
    abstract val time: Double

    /**
     * Apply local zero body effect on the receiver.
     */
    open fun local(on: Receiver, t: Double) = Unit
}