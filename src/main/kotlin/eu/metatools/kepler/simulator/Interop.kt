package eu.metatools.kepler.simulator

import eu.metatools.kepler.tools.Vec


/**
 * Number of components in a compressed array.
 */
internal const val components = 6

/**
 * Position of [n]-th element.
 */
internal fun DoubleArray.pos(n: Int) = Vec(get(n * components + 0), get(n * components + 1))

/**
 * Rotation of [n]-th element.
 */
internal fun DoubleArray.rot(n: Int) = get(n * components + 2)

/**
 * Center-of-mass of [n]-th element.
 */
internal fun DoubleArray.com(n: Int) = Vec(get(n * components + 3), get(n * components + 4))

/**
 * Mass of [n]-th element.
 */
internal fun DoubleArray.mass(n: Int) = get(n * components + 5)

/**
 * Adds acceleration to the position of the [n]-th element.
 */
internal fun DoubleArray.addPos(n: Int, acc: Vec) {
    set(n * components + 0, get(n * components + 0) + acc.x)
    set(n * components + 1, get(n * components + 1) + acc.y)
}

/**
 * Adds acceleration to the rotation of the [n]-th element.
 */
internal fun DoubleArray.addRot(n: Int, acc: Double) {
    set(n * components + 2, get(n * components + 2) + acc)
}

/**
 * Adds acceleration to the center-of-mass of the [n]-th element.
 */
internal fun DoubleArray.addCom(n: Int, acc: Vec) {
    set(n * components + 3, get(n * components + 3) + acc.x)
    set(n * components + 4, get(n * components + 4) + acc.y)
}

/**
 * Adds acceleration to the mass of the [n]-th element.
 */
internal fun DoubleArray.addMass(n: Int, acc: Double) {
    set(n * components + 5, get(n * components + 5) + acc)
}