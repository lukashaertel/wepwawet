package eu.metatools.kepler

const val gravityConstant: R = 6.674e-11

/**
 * Gravity accelerator functor, based on
 * * Body position
 * * Body mass
 * * Reference position
 */
object Gravity {
    /**
     * Applies gravity between two bodies.
     */
    fun acc(body: DSVec, bodyMass: DS, ref: DSVec) =
            (body - ref) * gravityConstant * bodyMass /
                    (body - ref).length.cubed()
}

/**
 * Local accelerator functors, based on
 * * Reference mass
 * * Reference rotation
 * * Force vector
 */
object Local {
    /**
     * Returns acceleration for the given applied local force.
     */
    fun acc(refMass: DS, refRotation: DS, force: DSVec) =
            force.rotate(refRotation) /
                    refMass

    /**
     * Returns rotational acceleration for the given applied local force.
     */
    fun accRot(refMass: DS, refRotation: DS, force: DSVec, dirToCOM: DSVec) =
            dirToCOM.rotate(refRotation) cross force.rotate(refRotation) /
                    refMass
}
