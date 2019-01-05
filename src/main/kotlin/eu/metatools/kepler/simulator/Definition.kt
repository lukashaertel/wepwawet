package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec
import org.apache.commons.math3.util.FastMath.sqrt

/**
 * Initial value specification, time source and local effect on body.
 */
abstract class Definition(
        final override val hull: List<Vec> = emptyList(),
        final override val pos: Vec = Vec.zero,
        final override val rot: Double = 0.0,
        final override val com: Vec = Vec.zero,
        final override val mass: Double = 1.0,
        final override val posDot: Vec = Vec.zero,
        final override val rotDot: Double = 0.0,
        final override val comDot: Vec = Vec.zero,
        final override val massDot: Double = 0.0) : Body {
    final override val bounds = hull.map { it.squaredLength }.max()?.let { sqrt(it) } ?: 0.0

    /**
     * Time source.
     */
    abstract val time: Double

    /**
     * Apply local zero body effect on the receiver.
     */
    open fun local(on: Receiver, t: Double) = Unit
}