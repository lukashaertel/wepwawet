package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec

/**
 * Status object, holds values for the bodies.
 */
data class Status(
        val pos: Vec,
        val rot: Double,
        val com: Vec,
        val mass: Double,
        val posDot: Vec,
        val rotDot: Double,
        val comDot: Vec,
        val massDot: Double)

/**
 * Copies the current values of the body to a status.
 */
fun Body.toStatus() = Status(
        pos,
        rot,
        com,
        mass,
        posDot,
        rotDot,
        comDot,
        massDot)