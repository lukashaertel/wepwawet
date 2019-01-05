package eu.metatools.kepler.simulator

import eu.metatools.kepler.math.Lerp
import eu.metatools.kepler.math.lerp
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
        val massDot: Double) : Lerp<Status> {
    override fun lerp(to: Status, x: Double) =
            Status(pos.lerp(to.pos, x),
                    lerp(rot, to.rot, x),
                    com.lerp(to.com, x),
                    lerp(mass, to.mass, x),
                    posDot.lerp(to.posDot, x),
                    lerp(rotDot, to.rotDot, x),
                    comDot.lerp(to.comDot, x),
                    lerp(massDot, to.massDot, x))

}

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