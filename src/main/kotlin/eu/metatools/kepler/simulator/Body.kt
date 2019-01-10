package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec

interface Body {
    /**
     * Bounding sphere radius.
     */
    val bounds: Double

    /**
     * Hull of the body.
     */
    val hull: Hull

    /**
     * Position of the body.
     */
    val pos: Vec

    /**
     * Rotation of the body.
     */
    val rot: Double

    /**
     * Center of mass.
     */
    val com: Vec

    /**
     * Mass.
     */
    val mass: Double


    /**
     * Change rate of position / velocity.
     */
    val posDot: Vec

    /**
     * Change rate of rotation / angular velocity.
     */
    val rotDot: Double

    /**
     * Change rate of center of mass.
     */
    val comDot: Vec

    /**
     * Change rate of mass.
     */
    val massDot: Double
}