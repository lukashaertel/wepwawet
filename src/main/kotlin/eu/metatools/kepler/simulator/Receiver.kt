package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec

/**
 * Receives effect changes and presents the current status.
 */
interface Receiver : Body {
    /**
     * Receives positional acceleration.
     */
    fun accPos(posAcc: Vec)

    /**
     * Receives rotational acceleration.
     */
    fun accRot(rotAcc: Double)

    /**
     * Receives acceleration of center of mass.
     */
    fun accCom(comAcc: Vec)

    /**
     * Receives acceleration of mass.
     */
    fun accMass(massAcc: Double)
}