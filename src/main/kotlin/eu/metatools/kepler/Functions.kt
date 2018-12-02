package eu.metatools.kepler


/**
 * Applies gravity between two bodies.
 * @param body The first body.
 * @param reference The second body.
 * @param gravityConstant The gravity constant of the body.
 */
fun accGravity(body: Body, reference: Body, gravityConstant: R = 6.674e-11): R2R = { t ->
    val dir = body.pos(t) - reference.pos(t)
    if (dir.squaredLength == 0.0)
        Vec.zero to zero
    else {
        val (_, m1) = body.comAndMass(t)
        dir.normalized() * (gravityConstant * m1 / dir.squaredLength) to zero
    }
}

/**
 * Returns acceleration for a given global directional acceleration.
 * @param direction The direction of the acceleration. Should be normalized.
 * @param acceleration The scalar acceleration in that direction.
 */
fun accDir(direction: AtT<R2>, acceleration: AtT<R>): R2R = { t ->
    val dir = direction(t)
    val acc = acceleration(t)
    dir * acc to zero
}

/**
 * Returns acceleration and rotational acceleration for the given applied local force.
 * @param reference The reference body, used for rotation, mass and center of mass.
 * @param displacement The displacement of the force relative to the reference.
 * @param direction The direction of the force, relative to reference. Should be normalized.
 * @param force The scalar force in the direction at the displacement.
 */
fun accLocal(reference: Body, displacement: AtT<R2>, direction: AtT<R2>, force: AtT<R>): R2R = { t ->
    val (com, m) = reference.comAndMass(t)
    val rot = reference.rot(t)
    val acc = direction(t).rotate(rot) * force(t) / m
    val d = (displacement(t) - com).rotate(rot)
    val accRot = d.x * acc.y - d.y * acc.x
    acc to accRot
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

/**
 * Returns the center of mass and total mass from a set of displaced masses.
 */
fun comAndMassFrom(masses: List<R2R>): R2R = { t ->
    val resolved = masses.map { it(t) }
    val total = resolved.fold(zero) { l, r -> l + r.second }
    val com = resolved.fold(Vec.zero) { l, r -> l + r.first * r.second / total }
    com to total
}
