package eu.metatools.kepler.old

import eu.metatools.kepler.R
import eu.metatools.kepler.R2
import eu.metatools.kepler.Vec


/**
 * Applies gravity between two bodies.
 * @param body The first body.
 * @param reference The second body.
 * @param gravityConstant The gravity constant of the body.
 */
fun accGravity(body: Body, reference: Body, gravityConstant: R = 6.674e-11): AtT<R2> = { t ->
    val dir = body.pos(t) - reference.pos(t)
    if (dir.squaredLength == 0.0)
        Vec.zero
    else {
        val m1 = body.m(t)
        dir.normalized() * (gravityConstant * m1 / dir.squaredLength)
    }
}

/**
 * Returns acceleration for a given global directional acceleration.
 * @param direction The direction of the acceleration. Should be normalized.
 * @param acceleration The scalar acceleration in that direction.
 */
fun accDir(direction: AtT<R2>, acceleration: AtT<R>): AtT<R2> = { t ->
    val dir = direction(t)
    val acc = acceleration(t)
    dir * acc
}

/**
 * Returns acceleration for the given applied local force.
 * @param reference The reference body, used for rotation, mass and center of mass.
 * @param direction The direction of the force, relative to reference. Should be normalized.
 * @param force The scalar force in the direction at the displacement.
 */
fun accLocal(reference: Body, direction: AtT<R2>, force: AtT<R>): AtT<R2> = { t ->
    val m = reference.m(t)
    val rot = reference.rot(t)
    val acc = direction(t).rotate(rot) * force(t) / m
    acc
}

/**
 * Returns rotational acceleration for the given applied local force.
 * @param reference The reference body, used for rotation, mass and center of mass.
 * @param displacement The displacement of the force relative to the reference.
 * @param direction The direction of the force, relative to reference. Should be normalized.
 * @param force The scalar force in the direction at the displacement.
 */
fun accRotLocal(reference: Body, displacement: AtT<R2>, direction: AtT<R2>, force: AtT<R>): AtT<R> = { t ->
    val com = reference.com(t)
    val m = reference.m(t)
    val rot = reference.rot(t)
    val acc = direction(t).rotate(rot) * force(t) / m
    val d = (displacement(t) - com).rotate(rot)
    val accRot = d.x * acc.y - d.y * acc.x
    accRot
}

/**
 * Calculates the force@t for a reference body and the Isp at a given point and time.
 * @param reference The reference body, used for position for the Isp value.
 * @param isp The Isp at a position and time.
 * @param massFlow The mass flow of the propellant.
 * @param standardGravity The standard gravity, defaults to 10.0.
 */
fun forceFromIsp(reference: Body, isp: AtTAndP<R>, massFlow: AtT<R>, standardGravity: R = 10.0): AtT<R> = { t ->
    standardGravity * isp(t, reference.pos(t)) * -massFlow(t)
}